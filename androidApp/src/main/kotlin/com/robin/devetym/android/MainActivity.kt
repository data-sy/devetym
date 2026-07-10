package com.robin.devetym.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.robin.devetym.di.KoinAppDependencies
import com.robin.devetym.ui.AppRoot
import org.koin.mp.KoinPlatform

/** Android 셸 (M7 §3-4) — `AppRoot`를 그린다(M0 `App()` 대체). Koin은 `DevEtymApp.onCreate`가 이미 기동. */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        // M9 WU-9 — 스플래시(Starting 테마)를 postSplashScreenTheme로 스왑. super.onCreate 이전 호출 규약.
        installSplashScreen()
        super.onCreate(savedInstanceState)
        setContent { AppRoot(KoinAppDependencies(KoinPlatform.getKoin())) }
    }
}
