package com.robin.devetym.repository

import com.robin.devetym.analytics.AnalyticsService
import com.robin.devetym.data.bundle.BundleDbSource
import com.robin.devetym.data.bundle.InMemoryBundleDbSource
import com.robin.devetym.data.local.toEntity
import com.robin.devetym.data.remote.ClaudeException
import com.robin.devetym.data.remote.TermGenerator
import com.robin.devetym.db.SearchHistory
import com.robin.devetym.db.Term
import com.robin.devetym.model.Source
import com.robin.devetym.model.TermEntry
import com.robin.devetym.model.TermResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * M4 슬라이스 §6-A — Fake 협력자 오케스트레이션 (JVM+네이티브 양쪽 실행, DoD 필수).
 *
 * fetch 3단 순서·pinning 스킵(INV-6)·저장 키 정본화(AD-1)·createdAt 보존(DR-M2-2)·category clamp
 * (M3 §7-4)·삭제 정규화를 드라이버 없이 순수 오케스트레이션 로직으로 네이티브 실행 축까지 실측한다.
 */
class TermRepositoryTest {

    // --- Fakes ---

    private class FakeGenerator(private val response: () -> TermResult) : TermGenerator {
        var callCount = 0
        override suspend fun generate(keyword: String): TermResult {
            callCount++
            return response()
        }
    }

    // in-memory LocalTermStore + 호출 카운트/방출.
    private class FakeStore : com.robin.devetym.data.local.LocalTermStore {
        val terms = mutableMapOf<String, Term>()
        val searches = mutableMapOf<String, Long>()
        var upsertTermCount = 0
        var upsertSearchCount = 0
        private val bookmarkedFlow = MutableStateFlow<List<Term>>(emptyList())
        private val recentFlow = MutableStateFlow<List<SearchHistory>>(emptyList())

        override fun selectByKeyword(keyword: String): Term? = terms[keyword]
        override fun upsertTerm(term: Term) {
            terms[term.keyword] = term; upsertTermCount++
            bookmarkedFlow.value = terms.values.filter { it.isBookmarked == 1L }.sortedByDescending { it.createdAt }
        }
        override fun bookmarked(): Flow<List<Term>> = bookmarkedFlow
        override fun recent(limit: Long): Flow<List<SearchHistory>> = recentFlow
        override fun upsertSearch(keyword: String, searchedAt: Long) {
            searches[keyword] = searchedAt; upsertSearchCount++; emitRecent()
        }
        override fun deleteSearch(keyword: String) { searches.remove(keyword); emitRecent() }
        override fun clearAllSearch() { searches.clear(); emitRecent() }
        private fun emitRecent() {
            recentFlow.value = searches.entries.sortedByDescending { it.value }.map { SearchHistory(it.key, it.value) }
        }
    }

    private class FakeAnalytics : AnalyticsService {
        var errorCount = 0
        override fun logSearchResult(keyword: String, result: TermResult) {}
        override fun logError(keyword: String, error: Throwable) { errorCount++ }
    }

    // --- Helpers ---

    private var now = 100L
    private val clock: () -> Long = { now }

    private fun te(
        keyword: String,
        aliases: List<String> = emptyList(),
        category: String = "기타",
        schemaVersion: Int? = null,
        promptVersion: String? = null,
    ) = TermEntry(keyword, aliases, category, "요약", "어원", "이유", schemaVersion, promptVersion)

    private fun term(
        keyword: String,
        source: Source = Source.AI,
        createdAt: Long = 1L,
        isBookmarked: Boolean = false,
        seenAt: Long? = null,
        category: String = "기타",
        schemaVersion: Int? = null,
        promptVersion: String? = null,
    ): Term = te(keyword, category = category, schemaVersion = schemaVersion, promptVersion = promptVersion)
        .toEntity(source = source, createdAt = createdAt, isBookmarked = isBookmarked, seenAt = seenAt)

    private fun repo(
        bundle: BundleDbSource = InMemoryBundleDbSource(emptyList()),
        generator: TermGenerator = FakeGenerator { TermResult.NotDevTerm },
        store: FakeStore = FakeStore(),
        analytics: FakeAnalytics = FakeAnalytics(),
    ) = TermRepositoryImpl(bundle, generator, store, analytics, clock)

    // --- Tests ---

    @Test
    fun test_fetch_빈입력_NotDevTerm_네트워크없음() = runTest {
        val gen = FakeGenerator { TermResult.NotDevTerm }
        val store = FakeStore()
        assertEquals(TermResult.NotDevTerm, repo(generator = gen, store = store).fetch("   "))
        assertEquals(0, gen.callCount)
        assertEquals(0, store.upsertSearchCount)
    }

    @Test
    fun test_fetch_번들히트_FoundBUNDLE_히스토리저장_term미저장() = runTest {
        val bundle = InMemoryBundleDbSource(listOf(te("mutex", listOf("뮤텍스"), "동시성")))
        val gen = FakeGenerator { TermResult.NotDevTerm }
        val store = FakeStore()
        val r = repo(bundle = bundle, generator = gen, store = store).fetch("mutex")
        assertTrue(r is TermResult.Found); assertEquals(Source.BUNDLE, r.source)
        assertEquals(1, store.upsertSearchCount)   // 히스토리 저장
        assertEquals(0, store.upsertTermCount)     // term 미저장(lazy)
        assertEquals(0, gen.callCount)             // 네트워크 미호출
    }

    @Test
    fun test_fetch_alias히트_FoundBUNDLE() = runTest {
        val bundle = InMemoryBundleDbSource(listOf(te("mutex", listOf("뮤텍스"), "동시성")))
        val r = repo(bundle = bundle).fetch("뮤텍스")
        assertTrue(r is TermResult.Found); assertEquals(Source.BUNDLE, r.source)
        assertEquals("mutex", r.entry.keyword)
    }

    @Test
    fun test_fetch_번들미스_로컬AI캐시히트_FoundAI_API스킵() = runTest {
        val store = FakeStore().apply { terms["mutex"] = term("mutex", source = Source.AI) }
        val gen = FakeGenerator { TermResult.NotDevTerm }
        val r = repo(generator = gen, store = store).fetch("mutex")
        assertTrue(r is TermResult.Found); assertEquals(Source.AI, r.source)
        assertEquals(0, gen.callCount)             // 캐시 히트 → API 스킵(INV-1·2)
    }

    @Test
    fun test_fetch_로컬번들북마크로우는캐시아님_API호출() = runTest {
        // source=BUNDLE 로우만 있으면 3단 캐시로 안 침 → 네트워크.
        val store = FakeStore().apply { terms["mutex"] = term("mutex", source = Source.BUNDLE, isBookmarked = true) }
        val gen = FakeGenerator { TermResult.Found(te("mutex", category = "동시성"), Source.AI) }
        repo(generator = gen, store = store).fetch("mutex")
        assertEquals(1, gen.callCount)
    }

    @Test
    fun test_fetch_캐시미스_API호출_FoundAI_upsert_히스토리저장() = runTest {
        val store = FakeStore()
        val gen = FakeGenerator { TermResult.Found(te("mutex", category = "동시성"), Source.AI) }
        val r = repo(generator = gen, store = store).fetch("mutex")
        assertTrue(r is TermResult.Found); assertEquals(Source.AI, r.source)
        assertEquals(1, gen.callCount)
        assertEquals(1, store.upsertTermCount)
        assertEquals(1, store.upsertSearchCount)
        val stored = store.terms["mutex"]!!
        assertEquals("AI", stored.source)
        assertTrue(stored.seenAt != null)          // 신규 AI 로우는 pin(INV-6, §3-4)
    }

    @Test
    fun test_fetch_pinned항목_그대로반환_API스킵() = runTest {
        val store = FakeStore().apply { terms["mutex"] = term("mutex", source = Source.AI, seenAt = 50L) }
        val gen = FakeGenerator { TermResult.NotDevTerm }
        val r = repo(generator = gen, store = store).fetch("mutex")
        assertTrue(r is TermResult.Found); assertEquals(Source.AI, r.source)
        assertEquals(0, gen.callCount)             // pinned도 캐시 반환, API 스킵
    }

    @Test
    fun test_refresh_pinned우회_API호출_seenAt갱신() = runTest {
        now = 100L
        val store = FakeStore().apply {
            terms["mutex"] = term("mutex", source = Source.AI, createdAt = 10L, isBookmarked = true, seenAt = 50L)
        }
        val gen = FakeGenerator { TermResult.Found(te("mutex", category = "동시성"), Source.AI) }
        now = 200L
        repo(generator = gen, store = store).refresh("mutex")
        assertEquals(1, gen.callCount)             // refresh는 pinned 우회 → 네트워크
        val stored = store.terms["mutex"]!!
        assertEquals(200L, stored.seenAt)          // refresh가 seenAt 갱신
        assertEquals(10L, stored.createdAt)        // createdAt 보존(DR-M2-2)
        assertEquals(1L, stored.isBookmarked)      // 북마크 보존
    }

    @Test
    fun test_fetch_NotDevTerm_PossibleTypo_저장안함() = runTest {
        val store = FakeStore()
        assertEquals(TermResult.NotDevTerm, repo(generator = FakeGenerator { TermResult.NotDevTerm }, store = store).fetch("바나나"))
        val r = repo(generator = FakeGenerator { TermResult.PossibleTypo("mutex") }, store = store).fetch("mutx")
        assertTrue(r is TermResult.PossibleTypo)
        assertEquals(0, store.upsertTermCount)
        assertEquals(0, store.upsertSearchCount)   // 히스토리·term 저장 안 함
    }

    @Test
    fun test_fetch_ClaudeException_전파_저장안함_analytics로깅() = runTest {
        val store = FakeStore()
        val analytics = FakeAnalytics()
        val gen = FakeGenerator { throw ClaudeException.DailyLimitExceeded }
        assertFailsWith<ClaudeException.DailyLimitExceeded> {
            repo(generator = gen, store = store, analytics = analytics).fetch("mutex")
        }
        assertEquals(0, store.upsertTermCount)
        assertEquals(0, store.upsertSearchCount)
        assertEquals(1, analytics.errorCount)      // 오류 로깅
    }

    @Test
    fun test_fetch_기존AI로우refresh_createdAt보존_정렬안정() = runTest {
        val store = FakeStore().apply { terms["mutex"] = term("mutex", source = Source.AI, createdAt = 100L) }
        now = 200L
        val gen = FakeGenerator { TermResult.Found(te("mutex", category = "동시성"), Source.AI) }
        repo(generator = gen, store = store).refresh("mutex")
        assertEquals(100L, store.terms["mutex"]!!.createdAt)  // 새 clock(200)로 재설정 안 됨
    }

    @Test
    fun test_fetch_기존북마크로우_source와isBookmarked보존() = runTest {
        val store = FakeStore().apply {
            terms["mutex"] = term("mutex", source = Source.BUNDLE, isBookmarked = true)
        }
        val gen = FakeGenerator { TermResult.Found(te("mutex", category = "동시성"), Source.AI) }
        repo(generator = gen, store = store).fetch("mutex")
        val stored = store.terms["mutex"]!!
        assertEquals("BUNDLE", stored.source)      // source 보존
        assertEquals(1L, stored.isBookmarked)      // 북마크 보존
    }

    @Test
    fun test_fetch_AI응답_집합밖category_기타로clamp() = runTest {
        val store = FakeStore()
        val gen = FakeGenerator { TermResult.Found(te("x", category = "네트웤"), Source.AI) } // 오타(6집합 밖)
        val r = repo(generator = gen, store = store).fetch("x")
        assertTrue(r is TermResult.Found)
        assertEquals("기타", r.entry.category)      // clamp
        assertEquals("기타", store.terms["x"]!!.category)
    }

    @Test
    fun test_fetch_대소문자유의미입력_저장keyword정규화() = runTest {
        val store = FakeStore()
        val gen = FakeGenerator { TermResult.Found(te("React", category = "패턴"), Source.AI) } // 원형 casing
        repo(generator = gen, store = store).fetch("React")
        assertTrue(store.terms.containsKey("react"))   // 정규화 키로 저장(§3-4 AD-1)
        assertNull(store.terms["React"])               // 원형 저장 안 함
    }

    @Test
    fun test_fetch_대소문자유의미입력_재fetch캐시히트_API미호출() = runTest {
        val store = FakeStore()
        val gen = FakeGenerator { TermResult.Found(te("React", category = "패턴"), Source.AI) }
        val r = repo(generator = gen, store = store)
        r.fetch("React")
        assertEquals(1, gen.callCount)
        r.fetch("React")                               // 재fetch → 3단 캐시 히트
        assertEquals(1, gen.callCount)                 // API 미호출(원형 저장이면 영구 miss로 2가 됨)
    }

    @Test
    fun test_fetch_대소문자유의미입력_재fetch_createdAt보존() = runTest {
        val store = FakeStore()
        val gen = FakeGenerator { TermResult.Found(te("REST", category = "네트워크"), Source.AI) }
        val r = repo(generator = gen, store = store)
        now = 100L; r.fetch("REST")
        now = 200L; r.fetch("REST")
        assertEquals(100L, store.terms["rest"]!!.createdAt)   // 캐시 히트로 재설정 없음
    }

    @Test
    fun test_toggleBookmark_대소문자유의미입력_unbookmark_기존로우재사용() = runTest {
        val store = FakeStore()
        val r = repo(store = store)
        assertTrue(r.toggleBookmark(te("React")))      // 신규 저장(true), 키="react"
        assertEquals(false, r.toggleBookmark(te("React")))  // 같은 로우 토글(false), 중복 없음
        assertEquals(1, store.terms.size)              // 중복 로우 없음
        assertEquals(0L, store.terms["react"]!!.isBookmarked)
    }

    @Test
    fun test_fetch_AI응답_버전필드_왕복보존() = runTest {
        val store = FakeStore()
        val gen = FakeGenerator {
            TermResult.Found(te("x", category = "기타", schemaVersion = 2, promptVersion = "2026-07"), Source.AI)
        }
        val r = repo(generator = gen, store = store).fetch("x")
        assertTrue(r is TermResult.Found)
        assertEquals(2, r.entry.schemaVersion)
        assertEquals("2026-07", r.entry.promptVersion)
        assertEquals(2L, store.terms["x"]!!.schemaVersion)   // Int → Long 무손실
    }

    @Test
    fun test_toggleBookmark_기존로우_토글_보존() = runTest {
        val store = FakeStore().apply {
            terms["mutex"] = term("mutex", source = Source.AI, createdAt = 100L, isBookmarked = false, seenAt = 50L)
        }
        val result = repo(store = store).toggleBookmark(te("mutex"))
        assertTrue(result)                             // false → true
        val stored = store.terms["mutex"]!!
        assertEquals(1L, stored.isBookmarked)
        assertEquals(100L, stored.createdAt)           // 보존
        assertEquals("AI", stored.source)              // 보존
        assertEquals(50L, stored.seenAt)               // 보존
    }

    @Test
    fun test_toggleBookmark_번들용어_BUNDLE저장_true() = runTest {
        val store = FakeStore()
        assertTrue(repo(store = store).toggleBookmark(te("mutex")))
        val stored = store.terms["mutex"]!!
        assertEquals("BUNDLE", stored.source)
        assertEquals(1L, stored.isBookmarked)
    }

    @Test
    fun test_bookmarked_recent_Flow_노출() = runTest {
        val store = FakeStore().apply {
            terms["a"] = term("a", isBookmarked = true, createdAt = 1L)
            terms["b"] = term("b", isBookmarked = true, createdAt = 2L)
            // Flow 재계산 트리거
            upsertTerm(terms["a"]!!); upsertTerm(terms["b"]!!)
            upsertSearch("react", 10L)
        }
        val r = repo(store = store)
        assertEquals(listOf("b", "a"), r.bookmarkedTerms().first().map { it.keyword }) // createdAt DESC
        assertEquals(listOf("react"), r.recentSearches(5).first().map { it.keyword })
    }

    @Test
    fun test_deleteSearch_clearAll_위임() = runTest {
        val store = FakeStore().apply { upsertSearch("react", 10L) }  // 정규화 로우 저장
        val r = repo(store = store)
        r.deleteSearchHistory("React")                 // 원형 입력 → normalizeKeyword → "react" 삭제
        assertTrue(store.searches.isEmpty())           // 정규화 대칭(§3-4) — 없으면 no-op으로 잔존
        store.upsertSearch("go", 20L)
        r.clearAllSearchHistory()
        assertTrue(store.searches.isEmpty())
    }
}
