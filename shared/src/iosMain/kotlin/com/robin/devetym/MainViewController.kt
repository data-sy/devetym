package com.robin.devetym

import androidx.compose.ui.window.ComposeUIViewController
import com.robin.devetym.di.KoinAppDependencies
import com.robin.devetym.ui.AppRoot
import org.koin.mp.KoinPlatform
import platform.UIKit.UIViewController

/**
 * iOS 셸(SwiftUI)이 호스팅하는 Compose 진입점 (M7 §3-4) — `AppRoot`를 그린다(M0 `App()` 대체).
 * Koin은 `doInitKoin()`(iosApp 시작)이 이미 기동.
 */
fun MainViewController(): UIViewController =
    ComposeUIViewController { AppRoot(KoinAppDependencies(KoinPlatform.getKoin())) }
