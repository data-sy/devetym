package com.robin.devetym.crash

import io.sentry.kotlin.multiplatform.Sentry

/**
 * 크래시 리포팅 (M9 WU-4B · D1: Sentry) — **commonMain 단일 KMP 배선**. `io.sentry:sentry-kotlin-multiplatform`
 * 하나로 Android(JVM)·iOS(Kotlin/Native) 크래시 리포팅을 공통 코드에서 초기화한다.
 *
 * **왜 단일 배선이 가능한가(WU-4B, 2026-07-10)**: iOS는 cocoapods 없이 정적 프레임워크로 빌드하므로
 * `:shared:iosSimulatorArm64Test` **네이티브 테스트 실행파일 링크**가 Sentry Cocoa 심볼을 완전 해석해야 한다.
 * `shared/build.gradle.kts`가 Sentry Cocoa 정적 xcframework(8.58.2)를 다운로드해 iOS 바이너리 링크에
 * `-F/-framework Sentry`로 공급 → 테스트 링크·실행이 성립한다(seam 분리 불필요). Android는 KMP SDK가
 * `sentry-android`를 전이 배선한다.
 *
 * **방침 정합(§2-2 크래시 진단)**: `sendDefaultPii=false` — PII 미부착(스택트레이스·기기 모델·OS/앱 버전 등
 * 최소 진단만). 애널리틱스(Firebase)와 별개 — 애널리틱스는 계속 미수집.
 */
object CrashReporter {
    private var initialized = false

    /**
     * 앱 진입 시 **1회** 호출(`initKoin` 최상단 — 초기화 이후 조기 크래시부터 포착). `dsn`이 null/blank면
     * 초기화하지 않는다(멱등: 이미 초기화됐어도 재진입 무시). DSN은 빌드타임 코드젠 상수 [SENTRY_DSN]
     * (루트 .env → `generateSentryConfig`)이 `initKoin` 기본값으로 주입 — 플랫폼 공통 단일 경로.
     * 미주입(빈 DSN)이면 안전한 no-op — 개발/CI 안전.
     */
    fun init(dsn: String?) {
        if (initialized || dsn.isNullOrBlank()) return
        Sentry.init { options ->
            options.dsn = dsn
            options.sendDefaultPii = false // PII 미부착(방침 §2-2 정합)
        }
        initialized = true
    }

    /** 배선 실증용 — 캡처 경로 성립 확인(수동 테스트 크래시). 미초기화 시 SDK가 안전하게 무시. */
    fun captureTestMessage(message: String) {
        Sentry.captureMessage(message)
    }

    /** 잡힌 예외를 진단 리포트로 전송. */
    fun capture(throwable: Throwable) {
        Sentry.captureException(throwable)
    }
}
