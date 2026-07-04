package com.robin.devetym.model

import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * M1 슬라이스 §6 — 모델·직렬화 왕복/하위호환/pass-through 오라클.
 *
 * 오라클은 **객체 동등성**(data class 구조 동등)이지 JSON 키 존재 여부가 아니다 —
 * 그래야 `encodeDefaults` 설정과 무관하게 결정적이다(§6). Json 인스턴스 정책은 M3 소관(§7-3)이라
 * 여기서는 테스트-로컬 기본 인스턴스를 쓴다.
 */
class TermEntrySerializationTest {

    private val json = Json

    @Test
    fun test_TermEntry_직렬화왕복_모든필드보존() {
        val original = TermEntry(
            keyword = "mutex",
            aliases = listOf("뮤텍스", "mutual exclusion"),
            category = Category.CONCURRENCY,
            summary = "상호 배제 잠금",
            etymology = "mutual exclusion의 축약",
            namingReason = "임계 구역 보호를 위한 잠금",
            schemaVersion = 1,
            promptVersion = "p1",
        )
        val decoded = json.decodeFromString<TermEntry>(json.encodeToString(original))
        // 객체 동등성 — 특히 aliases 순서·category 무손실 보존(INV-A).
        assertEquals(original, decoded)
        assertEquals(original.aliases, decoded.aliases) // 순서 포함 명시 단언
    }

    @Test
    fun test_TermEntry_버전필드없는JSON_역직렬화시null() {
        // 기존 번들 shape(버전 필드 없음) 하위호환 — INV-B.
        val bundleShape = """
            {
              "keyword": "react",
              "aliases": ["리액트"],
              "category": "패턴",
              "summary": "UI 라이브러리",
              "etymology": "reactive에서",
              "namingReason": "반응형 렌더링"
            }
        """.trimIndent()
        val decoded = json.decodeFromString<TermEntry>(bundleShape)
        assertNull(decoded.schemaVersion)
        assertNull(decoded.promptVersion)
        assertEquals("react", decoded.keyword)
        assertEquals(listOf("리액트"), decoded.aliases)
    }

    @Test
    fun test_TermEntry_버전필드있는JSON_왕복보존() {
        // 서버 배달 shape(버전 태깅 포함) — INV-9.
        val serverShape = """
            {
              "keyword": "raft",
              "aliases": [],
              "category": "네트워크",
              "summary": "합의 알고리즘",
              "etymology": "이해하기 쉬운 합의를 뗏목에 비유",
              "namingReason": "understandable consensus",
              "schemaVersion": 2,
              "promptVersion": "2026-07"
            }
        """.trimIndent()
        val decoded = json.decodeFromString<TermEntry>(serverShape)
        assertEquals(2, decoded.schemaVersion)
        assertEquals("2026-07", decoded.promptVersion)
        // 왕복 보존: 재인코드→재디코드 후에도 동등.
        assertEquals(decoded, json.decodeFromString<TermEntry>(json.encodeToString(decoded)))
    }

    @Test
    fun test_TermResult_when분기_전수처리() {
        // sealed 3분기 컴파일 타임 전수(DR-3). 아래 when은 **else 브랜치가 없다** —
        // 표현식 형태라 세 subtype을 전부 덮어야만 컴파일된다. subtype이 늘면 여기가
        // 컴파일 에러로 실패해야 한다(그 컴파일 에러가 곧 전수 canary다). else를 추가하지 말 것.
        val cases: List<TermResult> = listOf(
            TermResult.Found(
                TermEntry(
                    keyword = "k",
                    category = Category.ETC,
                    summary = "s",
                    etymology = "e",
                    namingReason = "n",
                ),
                Source.BUNDLE,
            ),
            TermResult.NotDevTerm,
            TermResult.PossibleTypo("suggestion"),
        )
        val labels = cases.map { result ->
            when (result) {
                is TermResult.Found -> "found:${result.source}"
                TermResult.NotDevTerm -> "not-dev"
                is TermResult.PossibleTypo -> "typo:${result.suggestion}"
            }
        }
        assertEquals(listOf("found:${Source.BUNDLE}", "not-dev", "typo:suggestion"), labels)
    }

    @Test
    fun test_카테고리_집합밖값_처리() {
        // 집합 밖 category가 M1 왕복에서 예외 없이 그대로 보존됨(pass-through, INV-A).
        // M1은 거부·정규화하지 않는다(강제는 M3·M4).
        for (outOfSet in listOf("네트웤", "Database", "동시성 ")) { // 오타·영문·trailing space
            val entry = TermEntry(
                keyword = "x",
                category = outOfSet,
                summary = "s",
                etymology = "e",
                namingReason = "n",
            )
            val decoded = json.decodeFromString<TermEntry>(json.encodeToString(entry))
            assertEquals(outOfSet, decoded.category) // 정규화 없이 그대로 보존
        }
    }
}
