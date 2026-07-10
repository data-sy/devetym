package com.robin.devetym.data.local

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.robin.devetym.db.DevEtymDatabase
import com.robin.devetym.db.SearchHistory
import com.robin.devetym.db.Term
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow

/**
 * 로컬 저장 추상화 (M4 슬라이스 §3-2·§7-1) — M2 SQLDelight 쿼리를 감싼다.
 *
 * 오케스트레이션 정책(fetch 순서·pinning·보존 목록)이 **드라이버 없이 Fake로** 4축(네이티브 포함)
 * 실측되게 하는 seam이다. 정책 로직은 전부 `TermRepositoryImpl`에 있고, 이 인터페이스 구현
 * (`SqlDelightTermStore`)은 얇은 위임뿐이다. actual DB 실행은 §6-B(JVM JDBC)가 커버, 네이티브 드라이버
 * 실행은 M8 이월(M2 DR-1 잔여).
 */
interface LocalTermStore {
    fun selectByKeyword(keyword: String): Term?
    fun upsertTerm(term: Term)
    fun bookmarked(): Flow<List<Term>>
    fun recent(limit: Long): Flow<List<SearchHistory>>
    fun upsertSearch(keyword: String, searchedAt: Long)
    fun deleteSearch(keyword: String)
    fun clearAllSearch()
}

/** M2 `devEtymQueries` 위임 구현. 반응형 쿼리는 `.asFlow().mapToList(...)`(ADR-0002·M2 §3-2). */
class SqlDelightTermStore(
    db: DevEtymDatabase,
    private val queryContext: CoroutineDispatcher = Dispatchers.Default,
) : LocalTermStore {
    private val q = db.devEtymQueries

    override fun selectByKeyword(keyword: String): Term? =
        q.selectTermByKeyword(keyword).executeAsOneOrNull()

    override fun upsertTerm(term: Term) {
        q.insertOrReplaceTerm(
            keyword = term.keyword,
            aliases = term.aliases,
            category = term.category,
            summary = term.summary,
            etymology = term.etymology,
            namingReason = term.namingReason,
            source = term.source,
            isBookmarked = term.isBookmarked,
            createdAt = term.createdAt,
            seenAt = term.seenAt,
            schemaVersion = term.schemaVersion,
            promptVersion = term.promptVersion,
        )
    }

    override fun bookmarked(): Flow<List<Term>> =
        q.bookmarked().asFlow().mapToList(queryContext)

    override fun recent(limit: Long): Flow<List<SearchHistory>> =
        q.recent(limit).asFlow().mapToList(queryContext)

    override fun upsertSearch(keyword: String, searchedAt: Long) {
        q.insertOrReplaceSearch(keyword, searchedAt)
    }

    override fun deleteSearch(keyword: String) {
        q.deleteSearch(keyword)
    }

    override fun clearAllSearch() {
        q.clearAllSearch()
    }
}
