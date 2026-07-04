package com.robin.devetym.ui

import androidx.lifecycle.viewModelScope
import com.robin.devetym.Constants
import com.robin.devetym.data.local.toEntity
import com.robin.devetym.data.remote.ClaudeException
import com.robin.devetym.db.SearchHistory
import com.robin.devetym.db.Term
import com.robin.devetym.model.Source
import com.robin.devetym.model.TermEntry
import com.robin.devetym.model.TermResult
import com.robin.devetym.repository.TermRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * M5 슬라이스 §6 — ViewModel 상태 로직 (Fake `TermRepository`, 4축 실행).
 *
 * `Dispatchers.setMain(StandardTestDispatcher())` + `runTest(dispatcher)`로 `viewModelScope`를 테스트
 * 스케줄러에 태워 fetch→상태 전이·디바운스·job 취소·오류 매핑을 가상시계로 실측한다. `:iosSimulatorArm64Test`
 * (B1)가 네이티브에서 이 로직을 **실행**으로 검증한다.
 */
class ViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    @BeforeTest fun setup() = Dispatchers.setMain(dispatcher)
    @AfterTest fun teardown() = Dispatchers.resetMain()

    private fun te(keyword: String) = TermEntry(keyword, listOf("별칭"), "동시성", "s", "e", "n")
    private fun found(keyword: String) = TermResult.Found(te(keyword), Source.AI)

    // ── 오류 매핑(순수, 네이티브 실행 핵심) ──────────────────────────────

    @Test
    fun test_toErrorKind_각예외_분류() {
        assertEquals(ErrorKind.Timeout, ClaudeException.Timeout.toErrorKind())
        assertEquals(ErrorKind.Network, ClaudeException.Network(RuntimeException()).toErrorKind())
        assertEquals(ErrorKind.InvalidResponse, ClaudeException.InvalidResponse.toErrorKind())
        assertEquals(ErrorKind.DailyLimitExceeded, ClaudeException.DailyLimitExceeded.toErrorKind())
        assertEquals(ErrorKind.Unknown, IllegalStateException("기타").toErrorKind())
    }

    // ── DetailViewModel ─────────────────────────────────────────────────

    @Test
    fun test_load_성공_Result() = runTest(dispatcher) {
        val repo = FakeRepo(resultFor = { found("mutex") })
        val vm = DetailViewModel(repo)
        vm.load("mutex")
        advanceUntilIdle()
        assertEquals(DetailUiState.Result(found("mutex")), vm.state.value)
    }

    @Test
    fun test_load_ClaudeException_Error매핑() = runTest(dispatcher) {
        val repo = FakeRepo(errorFor = { ClaudeException.DailyLimitExceeded })
        val vm = DetailViewModel(repo)
        vm.load("mutex")
        advanceUntilIdle()
        assertEquals(DetailUiState.Error(ErrorKind.DailyLimitExceeded), vm.state.value)
    }

    @Test
    fun test_load_NotDevTerm_Result아닌Error아님() = runTest(dispatcher) {
        val repo = FakeRepo(resultFor = { TermResult.NotDevTerm })
        val vm = DetailViewModel(repo)
        vm.load("바나나")
        advanceUntilIdle()
        assertEquals(DetailUiState.Result(TermResult.NotDevTerm), vm.state.value)
    }

    @Test
    fun test_load_재진입_이전job취소() = runTest(dispatcher) {
        val repo = FakeRepo(delayMs = 100, resultFor = { found(it) })
        val vm = DetailViewModel(repo)
        vm.load("a")
        advanceTimeBy(50)     // a는 아직 delay 중
        vm.load("b")          // 공유 job 취소 → a 폐기
        advanceUntilIdle()
        val state = vm.state.value
        assertTrue(state is DetailUiState.Result)
        assertEquals("b", (state.result as TermResult.Found).entry.keyword) // a의 늦은 방출이 b를 덮지 않음
    }

    @Test
    fun test_load_취소_Error로전이안함() = runTest(dispatcher) {
        // DR5-4: catch(CancellationException) throw가 catch(Throwable) Error보다 먼저여야 취소가 Error로 안 접힘.
        val repo = FakeRepo(delayMs = 100, resultFor = { found(it) })
        val vm = DetailViewModel(repo)
        vm.load("a")
        advanceTimeBy(50)                             // a suspend 중
        vm.viewModelScope.coroutineContext.cancelChildren()  // 후속 load 없이 진행 job만 취소
        advanceUntilIdle()
        assertEquals(DetailUiState.Loading, vm.state.value) // Error가 아니라 취소 직전 Loading 유지
    }

    @Test
    fun test_refresh_repository_refresh호출() = runTest(dispatcher) {
        val repo = FakeRepo(resultFor = { found(it) })
        val vm = DetailViewModel(repo)
        vm.refresh("mutex")
        advanceUntilIdle()
        assertEquals(listOf("mutex"), repo.refreshKeywords)
        assertEquals(emptyList(), repo.fetchKeywords)   // fetch 아닌 refresh 경로(pinning 우회)
    }

    @Test
    fun test_toggleBookmark_Result_Found일때_위임() = runTest(dispatcher) {
        val repo = FakeRepo(resultFor = { found("mutex") })
        val vm = DetailViewModel(repo)
        vm.load("mutex")
        advanceUntilIdle()
        vm.toggleBookmark()
        advanceUntilIdle()
        assertEquals(1, repo.toggledEntries.size)
        assertEquals("mutex", repo.toggledEntries.single().keyword) // 현재 상태의 entry로 위임
    }

    @Test
    fun test_toggleBookmark_상태가Found아님_noop() = runTest(dispatcher) {
        val repo = FakeRepo(resultFor = { TermResult.NotDevTerm })
        val vm = DetailViewModel(repo)
        vm.load("바나나")            // state = Result(NotDevTerm) — Found 아님
        advanceUntilIdle()
        vm.toggleBookmark()          // guard: 강제 추출 없이 no-op
        advanceUntilIdle()
        assertTrue(repo.toggledEntries.isEmpty())  // 미호출·예외 없음
    }

    // ── SearchViewModel ─────────────────────────────────────────────────

    @Test
    fun test_onQueryChanged_디바운스후_autocomplete() = runTest(dispatcher) {
        val repo = FakeRepo(autocomplete = listOf(te("mutex")))
        val vm = SearchViewModel(repo)
        vm.onQueryChanged("mut")
        advanceTimeBy(Constants.autocompleteDebounceMs - 1)
        assertTrue(vm.suggestions.value.isEmpty())   // 300ms 전엔 조회 안 함
        advanceTimeBy(2)
        assertEquals(listOf("mutex"), vm.suggestions.value.map { it.keyword })
    }

    @Test
    fun test_onQueryChanged_연타_이전job취소() = runTest(dispatcher) {
        val repo = FakeRepo(autocomplete = listOf(te("x")))
        val vm = SearchViewModel(repo)
        vm.onQueryChanged("m");  advanceTimeBy(100)
        vm.onQueryChanged("mu"); advanceTimeBy(100)
        vm.onQueryChanged("mut") // 앞의 둘은 300ms 전 취소
        advanceUntilIdle()
        assertEquals(listOf("mut"), repo.autocompletePrefixes) // 마지막 prefix만 조회
    }

    @Test
    fun test_onQueryChanged_빈입력_빈suggestions_조회안함() = runTest(dispatcher) {
        val repo = FakeRepo(autocomplete = listOf(te("x")))
        val vm = SearchViewModel(repo)
        vm.onQueryChanged("   ")   // trim 후 빈
        advanceUntilIdle()
        assertTrue(vm.suggestions.value.isEmpty())
        assertTrue(repo.autocompletePrefixes.isEmpty())  // autocomplete 미호출
    }

    @Test
    fun test_recent_Flow_노출() = runTest(dispatcher) {
        val repo = FakeRepo()
        val vm = SearchViewModel(repo)
        var latest: List<SearchHistory> = emptyList()
        val job = launch { vm.recent.collect { latest = it } }        // 활성 collector(반응형 관측 규약)
        runCurrent()                                                  // 구독 활성화(stateIn upstream 수집 시작)
        repo.recentFlow.value = listOf(SearchHistory("react", 1L))
        runCurrent()                                                  // 방출 전파
        job.cancel()
        assertEquals(listOf("react"), latest.map { it.keyword })
    }

    @Test
    fun test_commit_정규화_빈이면null() = runTest(dispatcher) {
        val vm = SearchViewModel(FakeRepo())
        vm.onQueryChanged("   ")
        assertEquals(null, vm.commit())
        vm.onQueryChanged("  Mutex  ")
        assertEquals("Mutex", vm.commit())   // trim만(lowercase는 repository 소관)
    }

    // ── Bookmark / History ──────────────────────────────────────────────

    @Test
    fun test_bookmarks_Flow_노출() = runTest(dispatcher) {
        val repo = FakeRepo()
        val vm = BookmarkViewModel(repo)
        var latest: List<Term> = emptyList()
        val job = launch { vm.bookmarks.collect { latest = it } }
        runCurrent()
        repo.bookmarkFlow.value = listOf(te("mutex").toRow())
        runCurrent()
        job.cancel()
        assertEquals(listOf("mutex"), latest.map { it.keyword })
    }

    @Test
    fun test_history_Flow_노출() = runTest(dispatcher) {
        val repo = FakeRepo()
        val vm = HistoryViewModel(repo)
        var latest: List<SearchHistory> = emptyList()
        val job = launch { vm.history.collect { latest = it } }
        runCurrent()
        repo.recentFlow.value = listOf(SearchHistory("kotlin", 2L))
        runCurrent()
        job.cancel()
        assertEquals(listOf("kotlin"), latest.map { it.keyword })
        assertEquals(HistoryViewModel.HISTORY_LIMIT, repo.lastRecentLimit) // 전량(recentSearchLimit 아님)
    }

    @Test
    fun test_removeBookmark_toggle호출() = runTest(dispatcher) {
        val repo = FakeRepo()
        val vm = BookmarkViewModel(repo)
        vm.removeBookmark(te("mutex"))
        advanceUntilIdle()
        assertEquals(listOf("mutex"), repo.toggledEntries.map { it.keyword })
    }

    @Test
    fun test_delete_clearAll_호출() = runTest(dispatcher) {
        val repo = FakeRepo()
        val vm = HistoryViewModel(repo)
        vm.delete("Mutex")   // 정규화는 repository가 수행(위임만 확인)
        vm.clearAll()
        advanceUntilIdle()
        assertEquals(listOf("Mutex"), repo.deletedKeywords)
        assertEquals(1, repo.clearAllCount)
    }
}

/** TermEntry → 최소 `Term` 로우(Flow 방출 fixture용). */
private fun TermEntry.toRow(): Term =
    toEntity(Source.AI, createdAt = 1L, isBookmarked = true, seenAt = 1L)

/** Fake `TermRepository` — 결과/예외/지연 주입 + 호출 기록 + Flow 방출. */
private class FakeRepo(
    private val resultFor: (String) -> TermResult = { TermResult.NotDevTerm },
    private val errorFor: (String) -> Throwable? = { null },
    private val delayMs: Long = 0,
    private val autocomplete: List<TermEntry> = emptyList(),
) : TermRepository {
    val fetchKeywords = mutableListOf<String>()
    val refreshKeywords = mutableListOf<String>()
    val autocompletePrefixes = mutableListOf<String>()
    val toggledEntries = mutableListOf<TermEntry>()
    val deletedKeywords = mutableListOf<String>()
    var clearAllCount = 0
    var lastRecentLimit = -1
    val recentFlow = MutableStateFlow<List<SearchHistory>>(emptyList())
    val bookmarkFlow = MutableStateFlow<List<Term>>(emptyList())

    override suspend fun fetch(keyword: String): TermResult {
        fetchKeywords += keyword
        if (delayMs > 0) delay(delayMs)
        errorFor(keyword)?.let { throw it }
        return resultFor(keyword)
    }

    override suspend fun refresh(keyword: String): TermResult {
        refreshKeywords += keyword
        if (delayMs > 0) delay(delayMs)
        errorFor(keyword)?.let { throw it }
        return resultFor(keyword)
    }

    override fun autocomplete(prefix: String): List<TermEntry> {
        autocompletePrefixes += prefix
        return autocomplete
    }

    override suspend fun toggleBookmark(entry: TermEntry): Boolean {
        toggledEntries += entry
        return true
    }

    override fun bookmarkedTerms(): Flow<List<Term>> = bookmarkFlow

    override fun recentSearches(limit: Int): Flow<List<SearchHistory>> {
        lastRecentLimit = limit
        return recentFlow
    }

    override suspend fun deleteSearchHistory(keyword: String) { deletedKeywords += keyword }
    override suspend fun clearAllSearchHistory() { clearAllCount++ }
}
