package com.robin.devetym.crash

/**
 * iOS actual (M9 WU-4) — 현재 **no-op**. iOS 네이티브 크래시 리포팅은 **Sentry Cocoa(SPM)를 Swift 층에서**
 * 초기화한다(iosApp/iOSApp.swift·project.yml — WU-11 Xcode 연계). commonMain에 `sentry-kotlin-multiplatform`을
 * 두면 `:shared:iosSimulatorArm64Test` 실행 링크가 Sentry Cocoa 프레임워크 미해결로 깨지므로(비cocoapods 정적
 * 프레임워크 setup) iosMain엔 Sentry 참조를 두지 않는다. 상세=commonMain `CrashReporter` KDoc.
 *
 * 향후 Kotlin↔Swift 브리지로 이 actual에서 Sentry Cocoa를 호출하려면 cocoapods/SPM 프레임워크 링크가
 * 전제(출시 후 백로그 "commonMain 단일 KMP 배선" 참조).
 */
actual object CrashReporter {
    actual fun init(dsn: String?) { /* iOS: Sentry Cocoa(SPM) Swift 초기화 — WU-11 */ }
    actual fun captureTestMessage(message: String) { /* no-op — Swift 층 담당 */ }
    actual fun capture(throwable: Throwable) { /* no-op — Swift 층 담당 */ }
}
