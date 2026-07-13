package com.robin.devetym.di

import com.robin.devetym.ui.AppDependencies
import com.robin.devetym.ui.BookmarkViewModel
import com.robin.devetym.ui.DetailViewModel
import com.robin.devetym.ui.HistoryViewModel
import com.robin.devetym.ui.SearchViewModel
import com.robin.devetym.ui.platform.AppActions
import com.robin.devetym.ui.platform.AppearanceStore
import com.robin.devetym.ui.platform.ConsentStore
import com.robin.devetym.ui.platform.DeviceInfo
import com.robin.devetym.ui.platform.OnboardingStore
import org.koin.core.Koin

/** `AppDependencies` 실구현 (M7 §3-3) — Koin에서 VM·seam 해석. `AppRoot(deps)` 소비. */
class KoinAppDependencies(private val koin: Koin) : AppDependencies {
    override val searchViewModel: SearchViewModel get() = koin.get()
    override val bookmarkViewModel: BookmarkViewModel get() = koin.get()
    override val historyViewModel: HistoryViewModel get() = koin.get()
    override fun createDetailViewModel(): DetailViewModel = koin.get()   // factory
    override val actions: AppActions get() = koin.get()
    override val appearance: AppearanceStore get() = koin.get()
    override val device: DeviceInfo get() = koin.get()
    override val onboarding: OnboardingStore get() = koin.get()
    override val consent: ConsentStore get() = koin.get()   // M9-후속 §2-F
    override fun now(): Long = epochMillis()
}
