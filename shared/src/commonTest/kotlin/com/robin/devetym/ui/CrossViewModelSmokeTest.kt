package com.robin.devetym.ui

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
import com.robin.devetym.repository.TermRepositoryImpl
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
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
 * M5 슬라이스 §6 교차-VM 배선 스모크.
 *
 * ⚠️ **DR-2 직렬화 강제를 증명하지 않는다**(§3-5 전제 a·b): 단일스레드 test dispatcher에선 최종값이 Mutex
 * 유무와 무관하게 correct로 수렴하고, DI 스코프(M7 single)와 무관하게 손으로 공유를 강제한다. 따라서 이 케이스는
 * **공유-impl 경로가 크래시 없이 일관된 최종 상태로 수렴함을 확인하는 스모크**일 뿐이다. Mutex 강제 실측은 이월.
 */
class CrossViewModelSmokeTest {

    private val dispatcher = StandardTestDispatcher()

    @BeforeTest fun setup() = Dispatchers.setMain(dispatcher)
    @AfterTest fun teardown() = Dispatchers.resetMain()

    private fun te(keyword: String) = TermEntry(keyword, listOf("별칭"), "동시성", "s", "e", "n")

    private fun repo(store: LocalTermStore, delayMs: Long) = TermRepositoryImpl(
        bundle = InMemoryBundleDbSource(emptyList()),
        api = DelayGenerator(delayMs),
        store = store,
        analytics = PlaceholderAnalyticsService(),
        clock = { 1L },
    )

    @Test
    fun test_교차VM_refresh중_removeBookmark_최종일관() = runTest(dispatcher) {
        val store = InMemoryTermStore()
        // 사전: term[react] AI·북마크됨(원형 대소문자 다른 "React"로 해제해도 같은 정규화 로우).
        store.upsertTerm(te("react").toEntity(Source.AI, createdAt = 1L, isBookmarked = true, seenAt = 1L))
        val repository = repo(store, delayMs = 100)
        val detail = DetailViewModel(repository)
        val bookmark = BookmarkViewModel(repository)

        detail.refresh("react")            // 네트워크 지연 진입
        advanceTimeBy(50)                  // refresh 진행 중
        bookmark.removeBookmark(te("React"))  // 같은 정규화 키 해제
        advanceUntilIdle()                 // refresh 네트워크 반환 → buildAiRow 재조회→upsert

        // 단일스레드 인터리빙에선 어느 순서든 최종 isBookmarked=false로 수렴(스모크 — Mutex 유무 무관).
        assertEquals(0L, store.selectByKeyword("react")!!.isBookmarked)
    }

    @Test
    fun test_교차VM_다른키_비간섭() = runTest(dispatcher) {
        val store = InMemoryTermStore()
        val repository = repo(store, delayMs = 100)
        val d1 = DetailViewModel(repository)
        val d2 = DetailViewModel(repository)

        d1.refresh("react")
        d2.refresh("kotlin")   // 다른 키 — 서로의 최종 상태를 오염시키지 않음
        advanceUntilIdle()

        assertNotNull(store.selectByKeyword("react"))
        assertNotNull(store.selectByKeyword("kotlin"))
        assertEquals("react", store.selectByKeyword("react")!!.keyword)
        assertEquals("kotlin", store.selectByKeyword("kotlin")!!.keyword)
    }
}

/** 지연 가능한 Fake `TermGenerator` — generate가 delayMs 만큼 suspend 후 Found. */
private class DelayGenerator(private val delayMs: Long) : TermGenerator {
    override suspend fun generate(keyword: String): TermResult {
        if (delayMs > 0) delay(delayMs)
        return TermResult.Found(TermEntry(keyword, listOf("별칭"), "동시성", "s", "e", "n"), Source.AI)
    }
}

/** in-memory `LocalTermStore` — 실 `TermRepositoryImpl` RMW 경로용(드라이버 없음). */
private class InMemoryTermStore : LocalTermStore {
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
