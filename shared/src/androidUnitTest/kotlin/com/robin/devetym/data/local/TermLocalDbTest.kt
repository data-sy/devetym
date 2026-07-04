package com.robin.devetym.data.local

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.robin.devetym.db.DevEtymDatabase
import com.robin.devetym.model.Category
import com.robin.devetym.model.Source
import com.robin.devetym.model.TermEntry
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * M2 슬라이스 §6-B — 스키마·쿼리 DB 왕복 (JVM in-memory JDBC 드라이버).
 *
 * 네이티브 드라이버는 JVM 단위테스트에서 미실행이므로 JVM에서 도는 `:testDebugUnitTest`(=androidUnitTest)에서
 * `JdbcSqliteDriver(IN_MEMORY)`로 실 DB를 띄워 스키마·쿼리를 실측한다(§7-4). 네이티브 actual 컴파일/링크는
 * §5 링크 green이, 네이티브 매퍼·직렬화 실행은 §6-A의 `:iosSimulatorArm64Test`(B1)가 커버한다.
 * `NativeSqliteDriver` 실행 정확성은 M8 통합/실기기로 이월(B1 잔여 절반).
 */
class TermLocalDbTest {

    private val json = Json  // 매퍼(TermMapper)의 aliasesJson과 동일 기본 인스턴스 — raw aliases 컬럼 비교용

    private fun freshDb(): DevEtymDatabase {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        DevEtymDatabase.Schema.create(driver)
        return DevEtymDatabase(driver)
    }

    private fun dto(
        keyword: String,
        aliases: List<String> = listOf("별칭1", "별칭2"),
        category: String = Category.CONCURRENCY,
    ) = TermEntry(
        keyword = keyword,
        aliases = aliases,
        category = category,
        summary = "요약-$keyword",
        etymology = "어원-$keyword",
        namingReason = "작명-$keyword",
    )

    @Test
    fun test_term_insertOrReplace_selectByKeyword_왕복() {
        val q = freshDb().devEtymQueries
        val original = dto("mutex", aliases = listOf("뮤텍스", "mutual exclusion"))

        // insert 값은 반드시 toEntity 출력에서 나와야 한다(§6-B canary 규율) — DTO 필드 손바인딩 금지.
        val entity = original.toEntity(source = Source.AI, createdAt = 100L, isBookmarked = false, seenAt = null)
        q.insertOrReplaceTerm(
            keyword = entity.keyword,
            aliases = entity.aliases,
            category = entity.category,
            summary = entity.summary,
            etymology = entity.etymology,
            namingReason = entity.namingReason,
            source = entity.source,
            isBookmarked = entity.isBookmarked,
            createdAt = entity.createdAt,
            seenAt = entity.seenAt,
            schemaVersion = entity.schemaVersion,
            promptVersion = entity.promptVersion,
        )

        // 되읽은 raw Term의 컬럼을 toDto() 없이 원본 DTO 값과 직접 단언(대칭 스왑 canary, DR-M2-1 폐쇄).
        // 반응형 bookmarked/recent Flow 소비자가 toDto 없이 직접 읽는 컬럼이 오라클에 고정된다.
        val row = q.selectTermByKeyword("mutex").executeAsOne()
        assertEquals("mutex", row.keyword)
        assertEquals(original.summary, row.summary)
        assertEquals(original.etymology, row.etymology)
        assertEquals(original.namingReason, row.namingReason)
        assertEquals(original.category, row.category)
        assertEquals(json.encodeToString(original.aliases), row.aliases) // raw JSON 문자열 직접 비교
        assertEquals("AI", row.source)
    }

    @Test
    fun test_term_pinning버전컬럼_저장복원() {
        val q = freshDb().devEtymQueries
        val entity = dto("raft").toEntity(source = Source.AI, createdAt = 10L, isBookmarked = true, seenAt = 777L)
        // 버전 컬럼은 toEntity가 DTO에서 안 받으므로 직접 값을 실어 스키마 nullable 동작을 실측.
        q.insertOrReplaceTerm(
            keyword = entity.keyword, aliases = entity.aliases, category = entity.category,
            summary = entity.summary, etymology = entity.etymology, namingReason = entity.namingReason,
            source = entity.source, isBookmarked = entity.isBookmarked, createdAt = entity.createdAt,
            seenAt = entity.seenAt, schemaVersion = 3L, promptVersion = "2026-07",
        )
        val row = q.selectTermByKeyword("raft").executeAsOne()
        assertEquals(777L, row.seenAt)          // pinned 컬럼 처음부터 존재(INV-6)
        assertEquals(3L, row.schemaVersion)     // INV-9
        assertEquals("2026-07", row.promptVersion)

        // nullable 동작: 버전/pinning 없이 저장하면 null 복원.
        val plain = dto("plain").toEntity(source = Source.BUNDLE, createdAt = 20L, isBookmarked = false, seenAt = null)
        q.insertOrReplaceTerm(
            keyword = plain.keyword, aliases = plain.aliases, category = plain.category,
            summary = plain.summary, etymology = plain.etymology, namingReason = plain.namingReason,
            source = plain.source, isBookmarked = plain.isBookmarked, createdAt = plain.createdAt,
            seenAt = plain.seenAt, schemaVersion = plain.schemaVersion, promptVersion = plain.promptVersion,
        )
        val plainRow = q.selectTermByKeyword("plain").executeAsOne()
        assertNull(plainRow.seenAt)
        assertNull(plainRow.schemaVersion)
        assertNull(plainRow.promptVersion)
    }

    @Test
    fun test_bookmarked_isBookmarked1만_createdAt내림차순() {
        val q = freshDb().devEtymQueries
        fun insert(keyword: String, bookmarked: Boolean, createdAt: Long) {
            val e = dto(keyword).toEntity(source = Source.AI, createdAt = createdAt, isBookmarked = bookmarked, seenAt = null)
            q.insertOrReplaceTerm(
                keyword = e.keyword, aliases = e.aliases, category = e.category, summary = e.summary,
                etymology = e.etymology, namingReason = e.namingReason, source = e.source,
                isBookmarked = e.isBookmarked, createdAt = e.createdAt, seenAt = e.seenAt,
                schemaVersion = e.schemaVersion, promptVersion = e.promptVersion,
            )
        }
        insert("old", bookmarked = true, createdAt = 100L)
        insert("new", bookmarked = true, createdAt = 300L)
        insert("mid", bookmarked = true, createdAt = 200L)
        insert("notmarked", bookmarked = false, createdAt = 999L)

        val marked = q.bookmarked().executeAsList()
        assertEquals(listOf("new", "mid", "old"), marked.map { it.keyword }) // createdAt DESC, 비북마크 제외
    }

    @Test
    fun test_recent_searchedAt내림차순_limit적용() {
        val q = freshDb().devEtymQueries
        q.insertOrReplaceSearch("a", 100L)
        q.insertOrReplaceSearch("b", 300L)
        q.insertOrReplaceSearch("c", 200L)

        val recent2 = q.recent(limit = 2L).executeAsList()
        assertEquals(listOf("b", "c"), recent2.map { it.keyword })  // searchedAt DESC + LIMIT 2
    }

    @Test
    fun test_searchHistory_delete_clear() {
        val q = freshDb().devEtymQueries
        q.insertOrReplaceSearch("a", 100L)
        q.insertOrReplaceSearch("b", 200L)
        q.deleteSearch("a")
        assertEquals(listOf("b"), q.recent(limit = 10L).executeAsList().map { it.keyword })
        q.clearAllSearch()
        assertEquals(emptyList(), q.recent(limit = 10L).executeAsList().map { it.keyword })
    }
}
