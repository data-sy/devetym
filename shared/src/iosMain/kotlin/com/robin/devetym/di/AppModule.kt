package com.robin.devetym.di

import com.robin.devetym.data.local.DriverFactory
import com.robin.devetym.data.local.createDatabase
import com.robin.devetym.db.DevEtymDatabase
import com.robin.devetym.ui.platform.AppActions
import com.robin.devetym.ui.platform.AppearanceStore
import com.robin.devetym.ui.platform.ConsentStore
import com.robin.devetym.ui.platform.DeviceInfo
import com.robin.devetym.ui.platform.OnboardingStore
import kotlinx.coroutines.runBlocking
import org.koin.core.module.Module
import org.koin.dsl.module
import platform.Foundation.NSDate
import platform.Foundation.timeIntervalSince1970

/** 플랫폼 현재 시각 actual (M7 §3-1). */
actual fun epochMillis(): Long = (NSDate().timeIntervalSince1970 * 1000).toLong()

/**
 * iOS 플랫폼 Koin 모듈 (M7 §3-2). seam·deviceId는 **스텁 바인딩**(actual M8). ⚠️ 실 바인딩 완전성 실기기 이월(§4).
 */
fun iosPlatformModule(): Module = module {
    single<DevEtymDatabase> { createDatabase(DriverFactory()) }
    single<DeviceIdProvider> { UserDefaultsDeviceIdProvider() }
    single<AppActions> { IosAppActions() }
    single<AppearanceStore> { UserDefaultsAppearanceStore() }
    single<OnboardingStore> { UserDefaultsOnboardingStore() }
    single<ConsentStore> { UserDefaultsConsentStore() }   // M9-후속 §2-F
    single<DeviceInfo> { IosDeviceInfo() }
}

/**
 * iOS(Swift) 진입 — 파일명 `AppModule.kt`라 facade `AppModuleKt`로 병합(기존 `AppModuleKt.doInitKoin()`
 * Swift 호출부 무편집 유지). preload+initKoin을 `runBlocking`으로 동기 완료(첫 프레임 이전 — §3-4 순서 불변식).
 * ⚠️ Swift 호출부 실컴파일은 Xcode(축 밖) — 검증 천장(실기기).
 *
 * M9 WU-4B — iOS 크래시 리포팅은 이제 **commonMain 단일 KMP 배선**(Sentry Cocoa via `sentry-kotlin-multiplatform`)이
 * 담당한다. Swift `SentrySDK.start` 불요 — DSN은 빌드타임 코드젠 상수(`initKoin` 기본값, 루트 .env →
 * `generateSentryConfig`)로 공통 `CrashReporter.init`에 전달된다(구 Info.plist `SentryDsn` 경로 제거).
 * 빈/누락 DSN이면 no-op(개발/CI 안전).
 */
fun doInitKoin() = runBlocking {
    initKoin(iosPlatformModule())
}
