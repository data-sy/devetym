package com.robin.devetym

import com.robin.devetym.resources.Res
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * M9 §3-4 — in-app 라이선스 로드 정확성(M8 DR-2 carry-forward의 로드 절반, `[AI]`).
 *
 * `LicensesScreen`이 로드하는 OFL .txt 3종을 `Res.readBytes`로 실제 읽어 **비어있지 않고 디코드되며** OFL 고지
 * 마커를 담는지 단언한다. ⚠️ 네이티브 축(`:shared:iosSimulatorArm64Test`)에서만 — plain JVM androidUnitTest는
 * compose-resources 런타임 미실행(M1 fixture 실측). 화면 실렌더·스크롤은 `[사람]` 잔여(spec §3-4).
 */
class LicensesLoadTest {

    private val names = listOf("files/ofl_dmsans.txt", "files/ofl_dmmono.txt", "files/ofl_dmserifdisplay.txt")

    @Test
    fun test_licenses_readBytes_비어있지않음() = runTest {
        for (name in names) {
            val bytes = Res.readBytes(name)
            assertTrue(bytes.isNotEmpty(), "$name 로드 결과 비어있음")
            val text = bytes.decodeToString()
            assertTrue(text.length > 100, "$name 디코드 텍스트 너무 짧음(${text.length})")
            assertTrue(
                text.contains("Font Software") && text.contains("Copyright"),
                "$name OFL 라이선스 마커 부재 — 잘못된 파일",
            )
        }
    }
}
