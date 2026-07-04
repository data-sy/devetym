package com.robin.devetym.android

import android.app.Application
import com.robin.devetym.di.initKoin

/** Android 셸 진입 — startKoin 배선(architecture §4.7). */
class DevEtymApp : Application() {
    override fun onCreate() {
        super.onCreate()
        initKoin()
    }
}
