package com.robin.devetym.data.local

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.native.NativeSqliteDriver
import com.robin.devetym.db.DevEtymDatabase

/** iOS actual — `NativeSqliteDriver`(Kotlin/Native SQLite, 성숙 트랙레코드, ADR-0003). */
actual class DriverFactory {
    actual fun createDriver(): SqlDriver =
        NativeSqliteDriver(DevEtymDatabase.Schema, "devetym.db")
}
