package com.robin.devetym

import androidx.compose.ui.window.ComposeUIViewController
import com.robin.devetym.ui.App
import platform.UIKit.UIViewController

/** iOS 셸(SwiftUI)이 호스팅하는 Compose 진입점(architecture §3 — iosApp는 얇은 셸). */
fun MainViewController(): UIViewController = ComposeUIViewController { App() }
