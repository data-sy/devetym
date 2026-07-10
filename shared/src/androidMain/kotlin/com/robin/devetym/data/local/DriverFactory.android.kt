package com.robin.devetym.data.local

import android.content.Context
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import com.robin.devetym.db.DevEtymDatabase

/** Android actual — `AndroidSqliteDriver`는 `Context` 필요(플랫폼 모듈/Koin이 M7에서 주입). */
actual class DriverFactory(private val context: Context) {
    actual fun createDriver(): SqlDriver =
        AndroidSqliteDriver(DevEtymDatabase.Schema, context, "devetym.db")
}
