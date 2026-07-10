package com.robin.devetym.repository

import com.robin.devetym.analytics.AnalyticsService
import com.robin.devetym.data.bundle.BundleDbSource
import com.robin.devetym.data.local.LocalTermStore
import com.robin.devetym.data.local.toDto
import com.robin.devetym.data.local.toEntity
import com.robin.devetym.data.normalizeKeyword
import com.robin.devetym.data.remote.ClaudeException
import com.robin.devetym.data.remote.TermGenerator
import com.robin.devetym.db.SearchHistory
import com.robin.devetym.db.Term
import com.robin.devetym.model.Category
import com.robin.devetym.model.Source
import com.robin.devetym.model.TermEntry
import com.robin.devetym.model.TermResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * 3계층 read-through 오케스트레이터 (M4 슬라이스 §3, spec 2-3). ViewModel(M5)이 의존하는 **유일 인터페이스**.
 *
 * ⚠️ **전제조건 — 단일-writer 직렬화(DR-2)**: 소비자(M5)는 **같은 `normalizeKeyword(keyword)`에 대한
 * `fetch`/`refresh`/`toggleBookmark`를 직렬화**해 동시 실행하지 않는다. 충돌 도메인은 raw 입력이 아니라
 * 정규화된 저장 키(모든 쓰기가 `term.keyword=normalizeKeyword(input)` 단일 로우에 비원자 RMW)다 —
 * 서로 다른 표기(`React`/`react`/`REACT`)라도 같은 로우를 건드리므로 직렬화 단위를 정규화 키로 잡아야
 * lost-update(북마크·`seenAt` 조용한 되돌림)가 막힌다. `refresh`의 RMW 창은 네트워크 왕복 전체라 좁지 않다.
 */
interface TermRepository {
    suspend fun fetch(keyword: String): TermResult
    suspend fun refresh(keyword: String): TermResult
    fun autocomplete(prefix: String): List<TermEntry>
    suspend fun toggleBookmark(entry: TermEntry): Boolean
    fun bookmarkedTerms(): Flow<List<Term>>
    fun recentSearches(limit: Int): Flow<List<SearchHistory>>
    suspend fun deleteSearchHistory(keyword: String)
    suspend fun clearAllSearchHistory()
}

class TermRepositoryImpl(
    private val bundle: BundleDbSource,
    private val api: TermGenerator,
    private val store: LocalTermStore,
    private val analytics: AnalyticsService,
    private val clock: () -> Long,   // createdAt/seenAt/searchedAt 주입(매퍼에 Clock 없음 — M2 §3-4)
) : TermRepository {

    // DR-2 단일-writer 강제(M7 §3-5): 정규화 키로 키잉된 Mutex로 fetch/refresh/toggleBookmark를 직렬화한다.
    // 맵-가드 원자성은 commonMain·네이티브 가능한 coroutines Mutex로(JVM synchronized 금지 — 네이티브 레이스).
    // 비재진입 — op 최상단 1회 획득(orchestrate→buildAiRow는 같은 락 재획득 안 함, 데드락 없음).
    // ⚠️ 진짜 병렬 강제는 구조(single 배선 + 이 Mutex)로 담보하며 4축으로 자칭하지 않는다(실기기 이월).
    private val keyLocksGuard = Mutex()
    private val keyLocks = mutableMapOf<String, Mutex>()

    private suspend fun <T> withKeyLock(rawKeyword: String, block: suspend () -> T): T {
        val key = normalizeKeyword(rawKeyword)
        if (key.isEmpty()) return block()   // 빈 키는 저장/RMW 없음 — 잠금 불필요
        val lock = keyLocksGuard.withLock { keyLocks.getOrPut(key) { Mutex() } }
        return lock.withLock { block() }
    }

    override suspend fun fetch(keyword: String): TermResult =
        withKeyLock(keyword) { orchestrate(keyword, useCache = true) }

    /** 명시적 새로고침 — 로컬 AI 캐시(3단)를 건너뛰고 네트워크로 서버 최신본 강제, pinned `seenAt` 갱신(INV-6). */
    override suspend fun refresh(keyword: String): TermResult =
        withKeyLock(keyword) { orchestrate(keyword, useCache = false) }

    private suspend fun orchestrate(keyword: String, useCache: Boolean): TermResult {
        // 1. 정규화 — 빈 입력이면 네트워크·저장 없이 즉시 NotDevTerm.
        val key = normalizeKeyword(keyword)
        if (key.isEmpty()) return TermResult.NotDevTerm

        // 2. 번들(로컬 head) — 히트 시 히스토리만 저장(term 미저장, lazy).
        bundle.search(key)?.let { entry ->
            store.upsertSearch(key, clock())
            return TermResult.Found(entry, Source.BUNDLE)
        }

        // 3. 로컬 AI 캐시 — source==AI 로우만 캐시. pinned(seenAt!=null)도 그대로 반환(INV-6).
        //    refresh(useCache=false)만 이 단계를 건너뛴다.
        if (useCache) {
            val cached = store.selectByKeyword(key)
            if (cached != null && cached.source == Source.AI.name) {
                store.upsertSearch(key, clock())
                return TermResult.Found(cached.toDto(), Source.AI)
            }
        }

        // 4. 네트워크(프록시 read-through, 클라 투명) — 원본 keyword(대소문자 보존, M3 §3-2).
        val result = try {
            api.generate(keyword)
        } catch (e: ClaudeException) {
            analytics.logError(keyword, e)   // 오류만 로깅(§3-6). 저장 안 함, 전파.
            throw e
        }
        return when (result) {
            is TermResult.Found -> {
                // category clamp(§3-5) + 저장 keyword 정본화(§3-4 AD-1 — 조회 키와 단일 정본).
                val clamped = result.entry.copy(keyword = key, category = clampCategory(result.entry.category))
                store.upsertTerm(buildAiRow(key, clamped, isRefresh = !useCache))
                store.upsertSearch(key, clock())
                TermResult.Found(clamped, Source.AI)
            }
            // NotDevTerm/PossibleTypo는 히스토리·term 저장 안 함(lazy, spec 2-3).
            is TermResult.NotDevTerm -> result
            is TermResult.PossibleTypo -> result
        }
    }

    /**
     * AI 응답을 저장할 `Term` 구성 (§3-4 보존 목록·DR-M2-2).
     * 기존 로우: `createdAt`·`isBookmarked`·`source`·`seenAt` 보존(refresh만 `seenAt` 갱신). 신규: `seenAt=clock()` pin.
     */
    private fun buildAiRow(key: String, entry: TermEntry, isRefresh: Boolean): Term {
        val existing = store.selectByKeyword(key)
        return if (existing != null) {
            entry.toEntity(
                source = Source.valueOf(existing.source),      // 보존(AI 캐시는 AI 유지, 번들 북마크는 BUNDLE 유지)
                createdAt = existing.createdAt,                // 보존 — 정렬 안정성(DR-M2-2)
                isBookmarked = existing.isBookmarked == 1L,    // 보존 — 북마크 소실 방지
                seenAt = if (isRefresh) clock() else existing.seenAt,  // refresh만 pinned 갱신(INV-6)
            )
        } else {
            // 신규 AI 로우는 처음 본 시점에 pin(INV-6, §3-4 정본 — 상속 M2 주석의 seenAt=null 예시는 번들 경로용).
            entry.toEntity(source = Source.AI, createdAt = clock(), isBookmarked = false, seenAt = clock())
        }
    }

    override fun autocomplete(prefix: String): List<TermEntry> = bundle.autocomplete(prefix)

    override suspend fun toggleBookmark(entry: TermEntry): Boolean = withKeyLock(entry.keyword) {
        val key = normalizeKeyword(entry.keyword)   // 조회·저장 키 단일 정본(§3-4)
        val existing = store.selectByKeyword(key)
        if (existing != null) {
            val newValue = existing.isBookmarked == 0L
            store.upsertTerm(existing.copy(isBookmarked = if (newValue) 1L else 0L)) // 나머지 필드 전부 보존
            newValue
        } else {
            // 미존재(번들 용어) → source=BUNDLE, isBookmarked=true 저장. 저장 keyword=key(원형 저장 시 un-bookmark 미발견→중복).
            store.upsertTerm(
                entry.copy(keyword = key).toEntity(
                    source = Source.BUNDLE, createdAt = clock(), isBookmarked = true, seenAt = null,
                ),
            )
            true
        }
    }

    override fun bookmarkedTerms(): Flow<List<Term>> = store.bookmarked()

    override fun recentSearches(limit: Int): Flow<List<SearchHistory>> = store.recent(limit.toLong())

    // 삭제도 normalizeKeyword로 write 경로(upsertSearch(key))와 대칭화(§3-4 삭제 경로 정본화).
    override suspend fun deleteSearchHistory(keyword: String) = store.deleteSearch(normalizeKeyword(keyword))

    override suspend fun clearAllSearchHistory() = store.clearAllSearch()

    /** AI 응답 category가 6집합 밖이면 `기타`로 clamp(§3-5, M3 §7-4). 번들·캐시 경로는 clamp 안 함. */
    private fun clampCategory(c: String): String = if (c in Category.CANONICAL) c else Category.ETC
}
