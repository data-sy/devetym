package com.robin.devetym.ui.theme

import androidx.compose.ui.graphics.Color
import kotlin.math.pow
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * M9 §3-5 — 접근성 대비비 정적 리포트(`[AI]`, WCAG 2.1 계산). AppColors 11토큰(라이트/다크)의 전경/배경 조합
 * 대비비를 계산해 **AA(본문 4.5:1)를 게이트, AAA(7:1)는 참고**(§7 Q3 판정). 미달이면 실기기 전 토큰 조정 신호.
 * 순수 계산이라 commonTest → 양 축 실행.
 */
class AccessibilityContrastTest {

    // WCAG 2.1 상대 휘도(sRGB 선형화) — Color 채널은 0..1 Float.
    private fun linear(c: Float): Double {
        val cs = c.toDouble()
        return if (cs <= 0.03928) cs / 12.92 else ((cs + 0.055) / 1.055).pow(2.4)
    }

    private fun luminance(color: Color): Double =
        0.2126 * linear(color.red) + 0.7152 * linear(color.green) + 0.0722 * linear(color.blue)

    private fun contrast(fg: Color, bg: Color): Double {
        val a = luminance(fg); val b = luminance(bg)
        val hi = maxOf(a, b); val lo = minOf(a, b)
        return (hi + 0.05) / (lo + 0.05)
    }

    private fun grade(ratio: Double): String =
        if (ratio >= 7.0) "AAA" else if (ratio >= 4.5) "AA" else if (ratio >= 3.0) "AA-large" else "FAIL"

    /** 전경(텍스트·인터랙티브) 토큰 × 배경 3종. brand는 장식 fill이라 리포트만(게이트 제외). */
    private fun report(scheme: String, c: AppColors): List<String> {
        val backgrounds = listOf("bg" to c.bg, "surface" to c.surface, "surface2" to c.surface2)
        val foregrounds = listOf(
            "text" to c.text, "textDim" to c.textDim, "textMuted" to c.textMuted,
            "accent" to c.accent, "accent2" to c.accent2, "accentAI" to c.accentAI,
        )
        val fails = mutableListOf<String>()
        println("── WCAG 대비비 리포트 [$scheme] ──")
        for ((fgName, fg) in foregrounds) for ((bgName, bg) in backgrounds) {
            val r = contrast(fg, bg)
            val g = grade(r)
            println("  ${fgName.padEnd(10)}/${bgName.padEnd(8)} ${(r * 100).toInt() / 100.0}  $g")
            if (r < 4.5) fails += "$scheme $fgName/$bgName=${(r * 100).toInt() / 100.0}($g)"
        }
        // brand 참고(게이트 밖)
        for ((bgName, bg) in backgrounds) {
            val r = contrast(c.brand, bg)
            println("  ${"brand".padEnd(10)}/${bgName.padEnd(8)} ${(r * 100).toInt() / 100.0}  ${grade(r)} (참고)")
        }
        return fails
    }

    @Test
    fun test_contrast_wcag_리포트() {
        val fails = report("DARK", DarkColors) + report("LIGHT", LightColors)
        assertTrue(
            fails.isEmpty(),
            "WCAG AA(4.5:1) 미달 전경/배경 쌍 — 실기기 전 토큰 조정 필요: $fails",
        )
    }
}
