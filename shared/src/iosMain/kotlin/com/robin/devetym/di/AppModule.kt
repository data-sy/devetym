package com.robin.devetym.di

import com.robin.devetym.data.local.DriverFactory
import com.robin.devetym.data.local.createDatabase
import com.robin.devetym.db.DevEtymDatabase
import com.robin.devetym.ui.platform.AppActions
import com.robin.devetym.ui.platform.AppearanceStore
import com.robin.devetym.ui.platform.DeviceInfo
import com.robin.devetym.ui.platform.NoopAppActions
import com.robin.devetym.ui.platform.StubAppearanceStore
import com.robin.devetym.ui.platform.StubDeviceInfo
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
    single<DeviceIdProvider> { object : DeviceIdProvider { override fun get() = "devetym-ios" } }
    single<AppActions> { NoopAppActions() }
    single<AppearanceStore> { StubAppearanceStore() }
    single<DeviceInfo> { StubDeviceInfo() }
}

/**
 * iOS(Swift) 진입 — 파일명 `AppModule.kt`라 facade `AppModuleKt`로 병합(기존 `AppModuleKt.doInitKoin()`
 * Swift 호출부 무편집 유지). preload+initKoin을 `runBlocking`으로 동기 완료(첫 프레임 이전 — §3-4 순서 불변식).
 * ⚠️ Swift 호출부 실컴파일은 Xcode(축 밖) — 검증 천장(실기기).
 */
fun doInitKoin() = runBlocking { initKoin(iosPlatformModule()) }
