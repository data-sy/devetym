package com.robin.devetym.di

import com.robin.devetym.data.local.DriverFactory
import com.robin.devetym.data.local.createDatabase
import com.robin.devetym.db.DevEtymDatabase
import com.robin.devetym.ui.platform.AppActions
import com.robin.devetym.ui.platform.AppearanceStore
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
    single<DeviceInfo> { IosDeviceInfo() }
}

/**
 * iOS(Swift) 진입 — 파일명 `AppModule.kt`라 facade `AppModuleKt`로 병합(기존 `AppModuleKt.doInitKoin()`
 * Swift 호출부 무편집 유지). preload+initKoin을 `runBlocking`으로 동기 완료(첫 프레임 이전 — §3-4 순서 불변식).
 * ⚠️ Swift 호출부 실컴파일은 Xcode(축 밖) — 검증 천장(실기기).
 *
 * M9 WU-4 — iOS 크래시 리포팅은 **Swift 층(Sentry Cocoa·SPM)**이 담당한다(iOSApp.swift가 Info.plist
 * `SentryDsn`을 읽어 `SentrySDK.start`). Kotlin `CrashReporter`(iosMain)는 no-op이므로 여기선 DSN을
 * 넘기지 않는다(crashDsn=null). 근거=commonMain `CrashReporter` KDoc(비cocoapods iOS 테스트 링크 제약).
 */
fun doInitKoin() = runBlocking { initKoin(iosPlatformModule()) }
