package com.robin.devetym.crash

import io.sentry.Sentry

/**
 * Android actual (M9 WU-4) — Sentry Android SDK로 크래시 리포팅. `Sentry.init`이 JVM 미포착 예외 핸들러
 * (`UncaughtExceptionHandlerIntegration`)를 설치해 크래시를 포착한다. DSN을 명시 주입하므로 Context 불요.
 *
 * **방침 정합**: `isSendDefaultPii=false` — IP·사용자 식별자 미부착, 스택트레이스·기기/OS 등 최소 진단만.
 */
actual object CrashReporter {
    private var initialized = false

    actual fun init(dsn: String?) {
        if (initialized || dsn.isNullOrBlank()) return
        Sentry.init { options ->
            options.dsn = dsn
            options.isSendDefaultPii = false // PII 미부착(방침 §4 정합)
        }
        initialized = true
    }

    actual fun captureTestMessage(message: String) {
        Sentry.captureMessage(message)
    }

    actual fun capture(throwable: Throwable) {
        Sentry.captureException(throwable)
    }
}
