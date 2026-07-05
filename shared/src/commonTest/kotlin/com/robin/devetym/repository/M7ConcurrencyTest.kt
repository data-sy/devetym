package com.robin.devetym.repository

import com.robin.devetym.analytics.PlaceholderAnalyticsService
import com.robin.devetym.data.bundle.InMemoryBundleDbSource
import com.robin.devetym.data.local.LocalTermStore
import com.robin.devetym.data.local.toEntity
import com.robin.devetym.data.remote.TermGenerator
import com.robin.devetym.db.SearchHistory
import com.robin.devetym.db.Term
import com.robin.devetym.model.Source
import com.robin.devetym.model.TermEntry
import com.robin.devetym.model.TermResult
import androidx.lifecycle.viewModelScope
import com.robin.devetym.ui.DetailViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

/**
 * M7 §6 — DR-2 단일-writer(데드락 부재·직렬화 스모크) + DR5-2 쓰기 취소 내성. 4축(네이티브 포함) 실행.
 * ⚠️ 진짜 병렬 강제는 구조(single 배선 + 키 Mutex)로 담보 — 이 테스트는 데드락 부재·계약·취소 내성만 보증.
 */
class M7ConcurrencyTest {

    private val dispatcher = StandardTestDispatcher()

    @BeforeTest fun setup() = Dispatchers.setMain(dispatcher)
    @AfterTest fun teardown() = Dispatchers.resetMain()

    private fun te(keyword: String) = TermEntry(keyword, listOf("별칭"), "동시성", "s", "e", "n")
    private fun repo(store: LocalTermStore, delayMs: Long = 0) = TermRepositoryImpl(
        InMemoryBundleDbSource(emptyList()), FoundGenerator(delayMs), store, PlaceholderAnalyticsService(), clock = { 1L },
    )

    @Test
    fun test_mutex_동일키_직렬화_데드락없음() = runTest(dispatcher) {
        val store = InMemoryStore()
        val repository = repo(store, delayMs = 50)
        // 같은 정규화 키에 refresh(락 전 구간=orchestrate→buildAiRow, 비재진입)+toggleBookmark 인터리브.
        val j1 = launch { repository.refresh("react") }
        val j2 = launch { repository.toggleBookmark(te("React")) }  // 같은 키 — j1 완료까지 대기(직렬화)
        advanceUntilIdle()
        j1.join(); j2.join()   // 도달=데드락 없음(비재진입 확인)
        assertNotNull(store.selectByKeyword("react"))   // refresh 저장 완료, 최종 일관
    }

    @Test
    fun test_toggleBookmark_외부스코프_취소내성() = runTest(dispatcher) {
        val store = InMemoryStore()
        val repository = repo(store)
        // 외부 writeScope=테스트가 통제하는 Job+테스트 디스패처(실 Dispatchers.Default 금지 — runTest가 await 못함).
        val writeScope = CoroutineScope(Job() + StandardTestDispatcher(testScheduler))
        val vm = DetailViewModel(repository, writeScope)
        vm.load("react"); advanceUntilIdle()   // Found(react) 저장, isBookmarked=0
        vm.toggleBookmark()                     // writeScope에 launch
        vm.viewModelScope.coroutineContext.cancelChildren()  // 이탈 모사 — viewModelScope 취소
        advanceUntilIdle()                      // writeScope는 살아있어 toggle 완료
        assertEquals(1L, store.selectByKeyword("react")!!.isBookmarked)  // 반영됨(취소 내성)
    }

    @Test
    fun test_toggleBookmark_기본viewModelScope_취소시_미반영() = runTest(dispatcher) {
        // 판별 대조: writeScope=null → viewModelScope 경로. 취소가 지연-launch 완료를 선행하면 미반영.
        val store = InMemoryStore()
        val repository = repo(store)
        val vm = DetailViewModel(repository, writeScope = null)
        vm.load("kotlin"); advanceUntilIdle()   // Found(kotlin) 저장, isBookmarked=0
        vm.toggleBookmark()                     // viewModelScope.launch 스케줄(StandardTestDispatcher — 미실행)
        vm.viewModelScope.coroutineContext.cancelChildren()  // 실행 전 취소
        advanceUntilIdle()
        assertEquals(0L, store.selectByKeyword("kotlin")!!.isBookmarked)  // 미반영(취소 선행)
    }
}

/** Found를 (선택적 지연 후) 돌려주는 Fake generator. */
private class FoundGenerator(private val delayMs: Long) : TermGenerator {
    override suspend fun generate(keyword: String): TermResult {
        if (delayMs > 0) delay(delayMs)
        return TermResult.Found(TermEntry(keyword, listOf("별칭"), "동시성", "s", "e", "n"), Source.AI)
    }
}

/** in-memory LocalTermStore. */
private class InMemoryStore : LocalTermStore {
    private val terms = mutableMapOf<String, Term>()
    private val searches = mutableMapOf<String, Long>()
    private val bookmarkedFlow = MutableStateFlow<List<Term>>(emptyList())
    private val recentFlow = MutableStateFlow<List<SearchHistory>>(emptyList())
    override fun selectByKeyword(keyword: String): Term? = terms[keyword]
    override fun upsertTerm(term: Term) { terms[term.keyword] = term; refresh() }
    override fun bookmarked(): Flow<List<Term>> = bookmarkedFlow
    override fun recent(limit: Long): Flow<List<SearchHistory>> = recentFlow
    override fun upsertSearch(keyword: String, searchedAt: Long) { searches[keyword] = searchedAt; refresh() }
    override fun deleteSearch(keyword: String) { searches.remove(keyword); refresh() }
    override fun clearAllSearch() { searches.clear(); refresh() }
    private fun refresh() {
        bookmarkedFlow.value = terms.values.filter { it.isBookmarked == 1L }
        recentFlow.value = searches.entries.sortedByDescending { it.value }.map { SearchHistory(it.key, it.value) }
    }
}
