package com.robin.devetym.data.local

import app.cash.sqldelight.driver.native.inMemoryDriver
import com.robin.devetym.db.DevEtymDatabase
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * M9 §3-2 — `NativeSqliteDriver` 네이티브 DB 실행(M2 B1 잔여 절반 끌어내리기).
 *
 * M2 §6-B DB 왕복은 JVM(JDBC) 전용이라 네이티브 DB 실행(스키마 create·`INSERT OR REPLACE`·`ORDER BY`/`LIMIT`·
 * nullable INTEGER 바인드)은 무측정이었다. 여기서 **인메모리 `NativeSqliteDriver`**(iosMain 드라이버, `iosTest`
 * 소스셋 → `:shared:iosSimulatorArm64Test` 네이티브 실행)로 M2 `.sq` 쿼리를 실제 왕복해 결과를 단언한다.
 *
 * `[사람]` 잔여(spec §3-2): 디스크 경로 드라이버·기기별 SQLite 빌드·TEXT 정렬 **로케일** 의존은 실기기.
 * 여기서 닫는 것은 **쿼리 정확성**(정렬·LIMIT·upsert·nullable 왕복)뿐이다.
 */
class NativeDbRoundtripTest {

    private val driver = inMemoryDriver(DevEtymDatabase.Schema)   // 네이티브 인메모리(디스크 아님 — §3-2 잔여 명시)
    private val q = DevEtymDatabase(driver).devEtymQueries

    @AfterTest fun teardown() = driver.close()

    private fun insert(
        keyword: String, createdAt: Long, bookmarked: Long = 0L,
        seenAt: Long? = null, schemaVersion: Long? = null, promptVersion: String? = null,
    ) = q.insertOrReplaceTerm(
        keyword = keyword, aliases = "[]", category = "기타", summary = "요약", etymology = "어원",
        namingReason = "명명", source = "AI", isBookmarked = bookmarked, createdAt = createdAt,
        seenAt = seenAt, schemaVersion = schemaVersion, promptVersion = promptVersion,
    )

    @Test
    fun test_nativeSqliteDriver_왕복_nullable바인드() {
        // nullable INTEGER/TEXT 바인드: null과 값 둘 다 왕복 보존
        insert("nullable", createdAt = 10L, seenAt = null, schemaVersion = null, promptVersion = null)
        insert("pinned", createdAt = 20L, seenAt = 999L, schemaVersion = 3L, promptVersion = "v2")

        val row1 = q.selectTermByKeyword("nullable").executeAsOne()
        assertNull(row1.seenAt, "네이티브 nullable seenAt 왕복 실패")
        assertNull(row1.schemaVersion)
        assertNull(row1.promptVersion)

        val row2 = q.selectTermByKeyword("pinned").executeAsOne()
        assertEquals(999L, row2.seenAt)
        assertEquals(3L, row2.schemaVersion)
        assertEquals("v2", row2.promptVersion)
    }

    @Test
    fun test_nativeSqliteDriver_insertOrReplace_upsert() {
        insert("dup", createdAt = 1L, bookmarked = 0L)
        insert("dup", createdAt = 2L, bookmarked = 1L)   // 같은 PK → REPLACE(누적 아님)
        val all = q.selectAllTerms().executeAsList().filter { it.keyword == "dup" }
        assertEquals(1, all.size, "INSERT OR REPLACE가 upsert 아님(중복 행)")
        assertEquals(2L, all.single().createdAt, "REPLACE 후 최신 값 미반영")
        assertEquals(1L, all.single().isBookmarked)
    }

    @Test
    fun test_nativeSqliteDriver_bookmarked_createdAt_DESC() {
        insert("old", createdAt = 100L, bookmarked = 1L)
        insert("new", createdAt = 300L, bookmarked = 1L)
        insert("mid", createdAt = 200L, bookmarked = 1L)
        insert("notmarked", createdAt = 400L, bookmarked = 0L)   // 필터 제외

        val marked = q.bookmarked().executeAsList()
        assertEquals(listOf("new", "mid", "old"), marked.map { it.keyword }, "bookmarked createdAt DESC 정렬 오류")
        assertTrue(marked.none { it.keyword == "notmarked" }, "isBookmarked=0이 결과에 포함")
    }

    @Test
    fun test_nativeSqliteDriver_recent_LIMIT_searchedAt_DESC() {
        q.insertOrReplaceSearch("a", 1L)
        q.insertOrReplaceSearch("b", 3L)
        q.insertOrReplaceSearch("c", 2L)

        val top2 = q.recent(limit = 2L).executeAsList()
        assertEquals(2, top2.size, "recent LIMIT 미적용")
        assertEquals(listOf("b", "c"), top2.map { it.keyword }, "recent searchedAt DESC + LIMIT 순서 오류")
    }
}
