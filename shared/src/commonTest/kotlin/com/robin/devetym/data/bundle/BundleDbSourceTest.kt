package com.robin.devetym.data.bundle

import com.robin.devetym.model.TermEntry
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * M3 슬라이스 §6-A — `BundleDbSource` 매칭 (엔진 무관 순수, JVM+네이티브 양쪽 실행).
 *
 * 인메모리 `InMemoryBundleDbSource(fixtureEntries)`로 실 파일 IO 없이 정규화 매칭·인덱스 first-wins·
 * 대소문자 무시를 네이티브 실행 축에서도 실측한다. 실 번들 alias 보존은 §6-B(androidUnitTest)가 재확인.
 */
class BundleDbSourceTest {

    private fun te(keyword: String, aliases: List<String> = emptyList(), category: String = "기타") =
        TermEntry(
            keyword = keyword,
            aliases = aliases,
            category = category,
            summary = "s",
            etymology = "e",
            namingReason = "n",
        )

    private val source = InMemoryBundleDbSource(
        listOf(
            te("mutex", listOf("뮤텍스", "mutual exclusion"), "동시성"),
            te("react", listOf("리액트"), "패턴"),
            te("reactor", category = "패턴"),
        ),
    )

    @Test
    fun test_search_정확매칭_반환() {
        assertEquals("mutex", source.search("mutex")?.keyword)
    }

    @Test
    fun test_search_alias매칭_반환() {
        // aliases가 검색 집합에 편입됐음 — 한글 음차·풀네임 둘 다.
        assertEquals("mutex", source.search("뮤텍스")?.keyword)
        assertEquals("mutex", source.search("mutual exclusion")?.keyword)
    }

    @Test
    fun test_search_대소문자무시_반환() {
        // Native lowercase 실측.
        assertEquals("react", source.search("REACT")?.keyword)
        assertEquals("react", source.search("React")?.keyword)
        assertEquals("react", source.search("  react  ")?.keyword) // trim도 함께
    }

    @Test
    fun test_search_미발견_null() {
        assertNull(source.search("존재하지않는용어"))
    }

    @Test
    fun test_search_정규화충돌_리스트앞선엔트리_반환() {
        // 두 엔트리가 같은 정규화 alias("분기")를 공유 — 실 번들 3충돌(집계/분기/샤딩)의 최소 재현.
        // §3-1 first-wins(putIfAbsent 상당): 리스트 앞선(먼저 삽입된) 엔트리를 결정적으로 반환한다.
        val collide = InMemoryBundleDbSource(
            listOf(
                te("branch", listOf("분기"), "네트워크"),
                te("fork", listOf("분기"), "동시성"),
            ),
        )
        assertEquals("branch", collide.search("분기")?.keyword) // last-wins면 "fork"가 나와 실패
    }

    @Test
    fun test_search_빈입력_null() {
        assertNull(source.search(""))
        assertNull(source.search("   "))
    }

    @Test
    fun test_autocomplete_prefix매칭_목록() {
        // keyword prefix — "reac"는 react·reactor 둘 다. aliases(리액트)는 대상 아님.
        val result = source.autocomplete("reac").map { it.keyword }.toSet()
        assertEquals(setOf("react", "reactor"), result)
    }

    @Test
    fun test_autocomplete_대소문자무시_그리고_alias는대상아님() {
        assertEquals(setOf("react", "reactor"), source.autocomplete("REAC").map { it.keyword }.toSet())
        // "뮤텍스"는 alias라 autocomplete 대상 아님.
        assertTrue(source.autocomplete("뮤텍").isEmpty())
    }

    @Test
    fun test_autocomplete_빈prefix_빈목록() {
        assertTrue(source.autocomplete("").isEmpty())
        assertTrue(source.autocomplete("   ").isEmpty())
    }
}
