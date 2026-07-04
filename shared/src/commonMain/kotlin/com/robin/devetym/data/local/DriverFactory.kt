package com.robin.devetym.data.local

import app.cash.sqldelight.db.SqlDriver
import com.robin.devetym.db.DevEtymDatabase

/**
 * SQLDelight 드라이버 팩토리 (M2 슬라이스 §3-3).
 *
 * **드라이버만 플랫폼별**(expect/actual): androidMain=`AndroidSqliteDriver`, iosMain=`NativeSqliteDriver`.
 * 위층(Repository·ViewModel)은 이 경계에 영향받지 않는다(ADR-0003 §Consequences Neutral, 교체 비용 낮음).
 *
 * actual의 검증은 **컴파일/링크**다: iOS는 `:shared:linkDebugFrameworkIosSimulatorArm64`,
 * Android는 `:shared:testDebugUnitTest`/`:androidApp:assembleDebug` green으로 확인. 단위테스트(§6-B)는
 * 이 actual을 실행하지 않고 in-memory JDBC 드라이버를 직접 쓴다(네이티브 드라이버는 JVM 단위테스트 미실행).
 */
expect class DriverFactory {
    fun createDriver(): SqlDriver
}

/** 팩토리로 드라이버를 만들어 DB 인스턴스를 생성. Koin 전체 조립(드라이버 바인딩)은 M7. */
fun createDatabase(factory: DriverFactory): DevEtymDatabase =
    DevEtymDatabase(factory.createDriver())
