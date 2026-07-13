package com.robin.devetym.ui

import java.io.File
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * M9 §3-5 — contentDescription 커버리지 정적 스캔(`[AI]`). `ui/`의 그래픽 위젯(Icon/Image)에 contentDescription이
 * 붙었는지 소스 스캔해 누락을 선검출한다. 실제 TalkBack/VoiceOver 감사는 `[사람]` 실기기지만, **누락은 코드로
 * 선검출**(spec §3-5). plain JVM androidUnitTest에서 소스 파일 직접 스캔(fixture 패턴, 작업 디렉터리=shared/).
 *
 * 소견(2026-07-05 실측): 이 앱 UI는 **전면 텍스트/이모지 기반**(Icon/Image/painter/vector 0개). 그래픽
 * contentDescription 누락은 구조적으로 0. 대신 이모지-only Text가 인터랙티브일 때 OS 이모지 TTS에 의존 →
 * `[사람]` TalkBack/VoiceOver 감사 대본(§3-5)의 대상. 이 테스트는 그 사실을 못박고 **회귀 게이트**(누군가
 * 나중에 contentDescription 없는 Icon/Image를 추가하면 실패)로 남긴다.
 */
class ContentDescriptionCoverageTest {

    private val uiDir = File("src/commonMain/kotlin/com/robin/devetym/ui")

    private fun uiSources(): List<File> =
        uiDir.walkTopDown().filter { it.isFile && it.extension == "kt" }.toList()

    @Test
    fun test_contentDescription_커버리지() {
        val files = uiSources()
        assertTrue(files.isNotEmpty(), "ui 소스 스캔 실패 — 작업 디렉터리(${File(".").absolutePath}) 확인")

        val widgetCall = Regex("""\b(Icon|Image)\s*\(""")
        val missing = mutableListOf<String>()
        var graphicalWidgets = 0
        var clickables = 0

        for (f in files) {
            val text = f.readText()
            clickables += Regex("""\.clickable|\bonClick\s*=|\bIconButton\s*\(|\bButton\s*\(""").findAll(text).count()
            for (m in widgetCall.findAll(text)) {
                graphicalWidgets++
                // 호출 본문 근사(다음 400자) 안에 contentDescription이 있는지
                val window = text.substring(m.range.first, minOf(text.length, m.range.first + 400))
                if (!window.contains("contentDescription")) {
                    val line = text.substring(0, m.range.first).count { it == '\n' } + 1
                    missing += "${f.name}:$line ${m.value}"
                }
            }
        }

        println("── contentDescription 커버리지 리포트 ──")
        println("  스캔 파일: ${files.size}")
        println("  그래픽 위젯(Icon/Image): $graphicalWidgets")
        println("  클릭가능 요소(clickable/onClick/Button): $clickables")
        println("  contentDescription 누락: ${missing.size} ${if (missing.isEmpty()) "" else missing}")
        println("  ⚠️ UI가 텍스트/이모지 기반이면 그래픽 누락 0이 정상 — 이모지-only 인터랙티브는 [사람] TalkBack 감사 대상")

        assertTrue(
            missing.isEmpty(),
            "contentDescription 없는 그래픽 위젯 발견(회귀) — 추가 시 반드시 contentDescription 배선: $missing",
        )
    }
}
