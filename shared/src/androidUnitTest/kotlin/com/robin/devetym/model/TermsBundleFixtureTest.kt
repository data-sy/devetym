package com.robin.devetym.model

import kotlinx.serialization.json.Json
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * M1 슬라이스 §6 `test_실제번들_terms디코드_aliases내용보존` — DR-1 wire측 M1 부분 폐쇄.
 *
 * 실제 배포 `terms.json`(650, composeResources 배치본)을 디코드해 §3-1 wire 키 계약
 * (JSON 키 = camelCase 프로퍼티명)이 실제 번들 문서에서 지켜짐을 실측한다.
 *
 * **성공 디코드를 오라클로 삼지 않는다**(§7-4): wrong-key·키 생략 JSON도 예외 없이 성공 디코드되어
 * `aliases = emptyList()`를 산출하므로(별칭 소실이 silent), 반드시 aliases *내용*을 단언한다.
 *
 * **배치가 commonTest가 아니라 androidUnitTest인 이유**: compose-resources 런타임(`Res.readBytes`)은
 * plain JVM 로컬 단위테스트에서 Android 자산 리더에 의존해 리소스를 찾지 못한다(MissingResourceException).
 * 그래서 green 오라클 `:shared:testDebugUnitTest`(=androidUnitTest 실행)에서 배포 파일 그 자체를
 * `java.io.File`로 직접 읽는다 — 작업 디렉터리는 `:shared` 모듈 루트라 경로가 결정적이다. 이는 classpath
 * 사본이 아니라 **실제 배포 번들 파일**을 읽으므로 계약 실측으로서 더 충실하다. 앱 런타임의 Res 로드
 * 경로(번들 로더) 회귀 가드는 §7-4대로 M3 `BundleDbSource` DoD로 이월된다.
 */
class TermsBundleFixtureTest {

    // 외부 배포 문서 대상 — 미래 필드 추가에도 견디도록 unknown 키 관용. aliases 내용 단언이 계약 오라클.
    private val json = Json { ignoreUnknownKeys = true }

    private val bundleFile = File("src/commonMain/composeResources/files/terms.json")

    @Test
    fun test_실제번들_terms디코드_aliases내용보존() {
        assertTrue(
            bundleFile.exists(),
            "배포 번들을 찾지 못함: ${bundleFile.absolutePath} (테스트 작업 디렉터리가 :shared 모듈 루트가 아닐 수 있음)",
        )
        val entries = json.decodeFromString<List<TermEntry>>(bundleFile.readText())

        // (a) 항목 수 650 (번들 무결성).
        assertEquals(650, entries.size)

        // (b) aliases를 가진 알려진 term의 aliases *내용*을 단언 — 성공 디코드가 아니라 내용이 오라클.
        val aaTree = entries.first { it.keyword == "aa-tree" }
        assertEquals(listOf("AA 트리", "Arne Andersson tree"), aaTree.aliases) // 순서 포함
        assertEquals("자료구조", aaTree.category)

        // 번들 전반이 wire 키 계약을 지켜 aliases가 무더기로 emptyList로 떨어지지 않았음을 방어적으로 확인:
        // 실제 번들에는 aliases가 있는 term이 다수다. 전부 비어 있으면 계약 위반(키 오배치)의 신호.
        val withAliases = entries.count { it.aliases.isNotEmpty() }
        assertTrue(withAliases > 0, "aliases를 가진 term이 하나도 없다 — wire 키 계약 위반 의심(별칭 silent 소실)")
    }
}
