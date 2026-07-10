package com.robin.devetym.crash

import kotlin.test.Test

/**
 * M9 WU-4 — 크래시 리포팅 seam 가드. **미설정(빈/누락 DSN)에서 안전한 no-op**(예외 없이 통과)인지 단언한다.
 * `expect`/`actual`이라 양 플랫폼 actual이 실행된다: `:shared:testDebugUnitTest`(Android actual = Sentry
 * 미초기화 조기 리턴) · `:shared:iosSimulatorArm64Test`(iOS actual = no-op). DSN 미주입 개발/CI 안전성 계약.
 */
class CrashReporterTest {

    @Test
    fun init_withBlankOrNullDsn_isSafeNoOp() {
        // 초기화 조건(DSN 유효) 미충족 시 SDK를 건드리지 않고 조용히 반환해야 한다 — 예외 시 테스트 실패.
        CrashReporter.init(null)
        CrashReporter.init("")
        CrashReporter.init("   ")
    }

    @Test
    fun init_isIdempotent() {
        // 재진입 호출이 예외를 던지지 않음(멱등 계약). 미설정이므로 실제 전송은 없음.
        CrashReporter.init(null)
        CrashReporter.init(null)
    }
}
