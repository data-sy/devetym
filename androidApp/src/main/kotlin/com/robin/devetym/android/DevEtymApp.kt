package com.robin.devetym.android

import android.app.Application
import com.robin.devetym.di.androidPlatformModule
import com.robin.devetym.di.initKoin
import kotlinx.coroutines.runBlocking

/**
 * Android 셸 진입 (M7 §3-4) — startKoin 배선(architecture §4.7). preload+initKoin을 `runBlocking`으로
 * **동기 완료**(첫 프레임/`getKoin()` 이전 — async-init 레이스 차단). `Context`는 플랫폼 팩토리에만 전달.
 */
class DevEtymApp : Application() {
    override fun onCreate() {
        super.onCreate()
        runBlocking { initKoin(androidPlatformModule(this@DevEtymApp)) }
    }
}
