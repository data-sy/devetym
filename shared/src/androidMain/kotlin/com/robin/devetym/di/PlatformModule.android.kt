package com.robin.devetym.di

import android.content.Context
import com.robin.devetym.data.local.DriverFactory
import com.robin.devetym.data.local.createDatabase
import com.robin.devetym.db.DevEtymDatabase
import com.robin.devetym.ui.platform.AppActions
import com.robin.devetym.ui.platform.AppearanceStore
import com.robin.devetym.ui.platform.DeviceInfo
import com.robin.devetym.ui.platform.NoopAppActions
import com.robin.devetym.ui.platform.StubAppearanceStore
import com.robin.devetym.ui.platform.StubDeviceInfo
import org.koin.core.module.Module
import org.koin.dsl.module

/** 플랫폼 현재 시각 actual (M7 §3-1). */
actual fun epochMillis(): Long = System.currentTimeMillis()

/**
 * Android 플랫폼 Koin 모듈 (M7 §3-2). `Context`는 androidMain에서만 참조(commonMain 미유입). seam·
 * deviceId는 **스텁 바인딩**(actual 실구현·런타임 검증 M8). ⚠️ 실 플랫폼 바인딩 완전성은 실기기 이월(§4).
 */
fun androidPlatformModule(context: Context): Module = module {
    single<DevEtymDatabase> { createDatabase(DriverFactory(context)) }
    single<DeviceIdProvider> { object : DeviceIdProvider { override fun get() = "devetym-android" } }
    single<AppActions> { NoopAppActions() }
    single<AppearanceStore> { StubAppearanceStore() }
    single<DeviceInfo> { StubDeviceInfo() }
}
