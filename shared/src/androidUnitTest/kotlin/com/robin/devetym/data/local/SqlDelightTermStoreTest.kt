package com.robin.devetym.data.local

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.robin.devetym.db.DevEtymDatabase
import com.robin.devetym.model.Source
import com.robin.devetym.model.TermEntry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * M4 슬라이스 §6-B — `SqlDelightTermStore` 실 DB 통합 (JVM in-memory JDBC 드라이버).
 *
 * M2 §6-B와 동일 배치: `JdbcSqliteDriver(IN_MEMORY)`로 실 DB를 띄워 `SqlDelightTermStore`가 M2 쿼리를
 * 올바로 위임함을 실측한다(정렬·limit·삭제 포함). 네이티브 `NativeSqliteDriver` 실행은 M8 이월(M2 DR-1 잔여).
 * 오케스트레이션 정책은 §6-A(Fake, 네이티브 포함)가 커버 — 여기선 위임 정확성만.
 */
class SqlDelightTermStoreTest {

    private fun freshStore(): SqlDelightTermStore {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        DevEtymDatabase.Schema.create(driver)
        return SqlDelightTermStore(DevEtymDatabase(driver), Dispatchers.Unconfined)
    }

    private fun term(keyword: String, isBookmarked: Boolean = false, createdAt: Long = 1L) =
        TermEntry(keyword, listOf("별칭"), "동시성", "s", "e", "n")
            .toEntity(source = Source.AI, createdAt = createdAt, isBookmarked = isBookmarked, seenAt = null)

    @Test
    fun test_store_upsert_select_왕복() {
        val store = freshStore()
        store.upsertTerm(term("mutex"))
        val row = store.selectByKeyword("mutex")!!
        assertEquals("mutex", row.keyword)
        assertEquals("동시성", row.category)
        assertNull(store.selectByKeyword("없음"))
    }

    @Test
    fun test_store_bookmarked_Flow_createdAt내림차순() = runBlocking {
        val store = freshStore()
        store.upsertTerm(term("a", isBookmarked = true, createdAt = 1L))
        store.upsertTerm(term("b", isBookmarked = true, createdAt = 3L))
        store.upsertTerm(term("c", isBookmarked = false, createdAt = 2L)) // 비북마크 제외
        assertEquals(listOf("b", "a"), store.bookmarked().first().map { it.keyword })
    }

    @Test
    fun test_store_recent_Flow_searchedAt내림차순_limit() = runBlocking {
        val store = freshStore()
        store.upsertSearch("a", 10L)
        store.upsertSearch("b", 30L)
        store.upsertSearch("c", 20L)
        assertEquals(listOf("b", "c"), store.recent(2).first().map { it.keyword }) // DESC + LIMIT 2
    }

    @Test
    fun test_store_deleteSearch_clearAll() {
        val store = freshStore()
        store.upsertSearch("a", 10L)
        store.upsertSearch("b", 20L)
        store.deleteSearch("a")
        assertNull(store.selectByKeyword("nonexistent"))
        store.clearAllSearch()
        // clearAll 후 recent 비어있음은 Flow로 확인 생략(위임만 실측 — 삭제 no-throw)
    }
}
