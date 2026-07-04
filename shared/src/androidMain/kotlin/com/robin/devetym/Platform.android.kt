package com.robin.devetym

import android.os.Build

class AndroidPlatform : Platform {
    override val name: String = "Android ${Build.VERSION.SDK_INT}"
}

actual fun platform(): Platform = AndroidPlatform()
