package com.robin.devetym.di

import android.content.Context
import com.robin.devetym.data.local.DriverFactory
import com.robin.devetym.data.local.createDatabase
import com.robin.devetym.db.DevEtymDatabase
import com.robin.devetym.ui.platform.AppActions
import com.robin.devetym.ui.platform.AppearanceStore
import com.robin.devetym.ui.platform.DeviceInfo
import com.robin.devetym.ui.platform.OnboardingStore
import org.koin.core.module.Module
import org.koin.dsl.module

/** 플랫폼 현재 시각 actual (M7 §3-1). */
actual fun epochMillis(): Long = System.currentTimeMillis()

/**
 * Android 플랫폼 Koin 모듈 (M7 §3-2·M8 §3-2). `Context`는 androidMain에서만 참조(commonMain 미유입).
 * M8: seam **actual 바인딩**(5종 — actions·appearance·onboarding·device·deviceId). ⚠️ 실 바인딩 완전성·
 * 런타임 동작은 실기기 첫 기동 스모크로 확인(§5 — 그래프 테스트는 테스트-스텁만 해석).
 */
fun androidPlatformModule(context: Context): Module = module {
    single<DevEtymDatabase> { createDatabase(DriverFactory(context)) }
    single<DeviceIdProvider> { PrefsDeviceIdProvider(context) }
    single<AppActions> { AndroidAppActions(context) }
    single<AppearanceStore> { PrefsAppearanceStore(context) }
    single<OnboardingStore> { PrefsOnboardingStore(context) }
    single<DeviceInfo> { AndroidDeviceInfo(context) }
}
