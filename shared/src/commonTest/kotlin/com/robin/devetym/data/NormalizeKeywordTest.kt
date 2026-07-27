package com.robin.devetym.data

import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * [normalizeKeyword] 케이스 테이블 — **클라-서버 term-key 동치의 정본**
 * (서버 슬라이스 S1 스펙 `docs/specs/server-m0-m1-cache-read-through-draft.md` §6-3).
 *
 * 이 테스트가 존재하는 이유는 클라 회귀 방어만이 아니다. `devetym-proxy`의 `normalizeTermKey`가
 * **이 집합을 복제**해야 서버 D1 캐시 `term_key`와 번들·로컬 키가 같은 공간에 놓인다(INV-3·INV-12).
 * 서버 쪽 동치 테스트(`test_termKey_*`)가 여기 케이스와 짝지어져 있으므로,
 * **케이스를 추가·수정하면 서버 테스트도 같이 고쳐야 한다.**
 *
 * ⚠️ **핵심은 "무엇을 자르는가"가 아니라 "무엇을 자르지 않는가"다.** Kotlin `trim()`은
 * `Char.isWhitespace()` 기준인데, 이는 JS `String.prototype.trim()`·정규식 `\s`와 집합이 다르다.
 * 서버가 `\s`나 JS `trim()`을 그대로 쓰면 동치가 깨지고, 증상은 캐시 미스가 아니라 —
 * 서버는 자기 일관적이라 미스가 나지 않는다 — **INV-12 승격 잡이 흘린 키를 클라가 영영 조회
 * 못 하는 형태로 조용히 샌다.** 그래서 양쪽 경계를 여기서 고정한다.
 *
 * ⚠️ **`java.lang.Character.isWhitespace()`와 혼동하지 말 것.** Kotlin/JVM의 `Char.isWhitespace()`는
 * `Character.isWhitespace(ch) || Character.isSpaceChar(ch)`이고, `isSpaceChar`(Zs/Zl/Zp)가
 * **NBSP U+00A0 · U+2007 · U+202F를 포함**한다. Kotlin/Native `isWhitespaceImpl`도 같은 합집합으로
 * 맞춰져 있다. 아래 [TRIMMED]·[NOT_TRIMMED]는 JVM·iosSimulatorArm64 양축에서 U+0000~U+FFFF 전수
 * 실측한 결과이며(2026-07-27), 자바 문서만 보고 유추한 집합과 다르다.
 *
 * 경계 문자는 눈으로 구분이 안 되고 원시 문자로 넣으면 에디터·복붙 경로에서 조용히 바뀌므로,
 * **전부 코드포인트 정수로만 쓴다.**
 *
 * (엔진 무관 순수 함수 — JVM·네이티브 양축에서 실행된다.)
 */
class NormalizeKeywordTest {

    private companion object {
        /**
         * Kotlin `Char.isWhitespace()`가 **자르는** 집합 전체 = 서버 `WS` 정규식과 동일해야 한다.
         * 실측(양축 동일): 0009-000D 001C-0020 00A0 1680 2000-200A 2028-2029 202F 205F 3000
         */
        val TRIMMED: List<Int> =
            (0x0009..0x000D) + (0x001C..0x0020) + listOf(0x00A0, 0x1680) +
                (0x2000..0x200A) + listOf(0x2028, 0x2029, 0x202F, 0x205F, 0x3000)

        /**
         * `isWhitespace()`가 **자르지 않는** 문자 — JS `trim()`은 U+FEFF를, 일부 정규식 방언은
         * U+0085를 자르므로 서버가 과트림하면 여기서 갈라진다. U+200B/U+180E는 어느 쪽도 안 자른다.
         */
        val NOT_TRIMMED: List<Int> = listOf(0x0085, 0x180E, 0x200B, 0xFEFF)

        const val NBSP = 0x00A0
        const val BOM = 0xFEFF
        const val US = 0x001F        // UNIT SEPARATOR
        const val IDEOGRAPHIC = 0x3000
    }

    private fun pad(cp: Int, s: String) = "${cp.toChar()}$s${cp.toChar()}"
    private fun hex(cp: Int) = "U+" + cp.toString(16).uppercase().padStart(4, '0')

    @Test
    fun test_normalizeKeyword_대문자입력_소문자키() {
        assertEquals("react", normalizeKeyword("React"))
        assertEquals("rest", normalizeKeyword("REST"))
    }

    @Test
    fun test_normalizeKeyword_공백패딩_트림() {
        assertEquals("mutex", normalizeKeyword("  mutex  "))
        assertEquals("mutex", normalizeKeyword("\t\n mutex \r\n"))
    }

    /**
     * ★ C3 — NBSP(U+00A0)는 **자른다.**
     * `Character.isWhitespace`만 보면 제외지만 Kotlin은 `isSpaceChar`와의 합집합이라 포함된다.
     * 서버가 Kotlin 아닌 자바 규격을 복제하면 여기서 갈라진다.
     */
    @Test
    fun test_normalizeKeyword_NBSP패딩_트림() {
        assertEquals("mutex", normalizeKeyword(pad(NBSP, "Mutex")))
    }

    /** ★ C3 — BOM(U+FEFF)은 **자르지 않는다.** JS `trim()`은 자르므로 서버가 그대로 쓰면 과트림. */
    @Test
    fun test_normalizeKeyword_BOM패딩_트림하지않음() {
        assertEquals(pad(BOM, "mutex"), normalizeKeyword(pad(BOM, "Mutex")))
    }

    /** ★ C3 — U+001C~U+001F는 자른다. JS `\s`로는 안 잘리는 구간. */
    @Test
    fun test_normalizeKeyword_U001F패딩_트림() {
        assertEquals("mutex", normalizeKeyword(pad(US, "Mutex")))
    }

    /** ★ C3 — 전각 공백(U+3000). 한글 IME 입력에서 실제로 발생한다. */
    @Test
    fun test_normalizeKeyword_U3000패딩_트림() {
        assertEquals("뮤텍스", normalizeKeyword(pad(IDEOGRAPHIC, "뮤텍스")))
    }

    /** 자르는 쪽 집합 전수 — 서버 정규식이 한 구간이라도 빠뜨리면 여기서 갈라진다. */
    @Test
    fun test_normalizeKeyword_유니코드공백류_전수트림() {
        for (cp in TRIMMED) {
            assertEquals("go", normalizeKeyword(pad(cp, "Go")), "${hex(cp)} 미트림")
        }
    }

    /** 자르지 않는 쪽 전수 — 서버가 과트림하면 여기서 갈라진다. */
    @Test
    fun test_normalizeKeyword_비공백류_전수트림하지않음() {
        for (cp in NOT_TRIMMED) {
            assertEquals(pad(cp, "go"), normalizeKeyword(pad(cp, "Go")), "${hex(cp)} 과트림")
        }
    }

    @Test
    fun test_normalizeKeyword_내부공백_보존() {
        assertEquals("mutual exclusion", normalizeKeyword("  Mutual Exclusion  "))
    }

    @Test
    fun test_normalizeKeyword_빈문자열_빈문자열() {
        assertEquals("", normalizeKeyword(""))
        assertEquals("", normalizeKeyword("   "))
    }
}
