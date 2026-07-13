package com.robin.devetym

import androidx.compose.ui.uikit.OnFocusBehavior
import androidx.compose.ui.window.ComposeUIViewController
import com.robin.devetym.di.KoinAppDependencies
import com.robin.devetym.ui.AppRoot
import org.koin.mp.KoinPlatform
import platform.UIKit.UIViewController

/**
 * iOS 셸(SwiftUI)이 호스팅하는 Compose 진입점 (M7 §3-4) — `AppRoot`를 그린다(M0 `App()` 대체).
 * Koin은 `doInitKoin()`(iosApp 시작)이 이미 기동.
 *
 * M9-후속 셸 재설계 §2-C: 기본 `FocusableAboveKeyboard`는 키보드가 뜰 때 **뷰포트 전체를 밀어**
 * 헤더·최근 검색이 사라진다(실기기 3-1) → `DoNothing`으로 차단하고 인셋 기반(`imePadding`)으로 회피.
 */
fun MainViewController(): UIViewController =
    ComposeUIViewController(configure = { onFocusBehavior = OnFocusBehavior.DoNothing }) {
        AppRoot(KoinAppDependencies(KoinPlatform.getKoin()))
    }
