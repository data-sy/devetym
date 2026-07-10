import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidLibrary)          // 클래식 KMP android 타깃(com.android.library)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.kotlinSerialization)
    alias(libs.plugins.skie)
    alias(libs.plugins.sqldelight)              // 로컬 DB(ADR-0003) — .sq → 타입세이프 Kotlin API 생성
}

kotlin {
    androidTarget {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_17)
        }
    }

    // iOS 타깃 — Kotlin/Native. SKIE가 이 프레임워크의 Swift API를 개선(ADR-0005).
    // iosX64(구 Intel 시뮬)는 CMP 1.11에서 미배포 → 제외. 기기(arm64)+Apple Silicon 시뮬만.
    listOf(iosArm64(), iosSimulatorArm64()).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "Shared"
            isStatic = true
        }
    }

    sourceSets {
        commonMain.dependencies {
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.ui)
            implementation(compose.components.resources)   // 번들 terms.json(composeResources) 로드용
            implementation(libs.koin.core)
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.kotlinx.serialization.json)  // M1 JSON 왕복(kotlinx.serialization)
            implementation(libs.sqldelight.runtime)              // M2 로컬 DB — 생성 API 런타임
            implementation(libs.sqldelight.coroutines.extensions) // .asFlow() (반응형 쿼리, ADR-0002)
            // M3 네트워킹(§3-4) — 코어·ContentNegotiation·JSON은 commonMain. 엔진만 플랫폼별.
            implementation(libs.ktor.client.core)
            implementation(libs.ktor.client.content.negotiation)
            implementation(libs.ktor.serialization.kotlinx.json)
            implementation(libs.lifecycle.viewmodel)         // M5 ViewModel + viewModelScope(멀티플랫폼)
            implementation(libs.lifecycle.runtime.compose)   // M6 collectAsStateWithLifecycle
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
            implementation(libs.kotlinx.coroutines.test)     // runTest (suspend 테스트)
            implementation(libs.ktor.client.mock)            // MockEngine (§6-A, 네트워크 무의존)
        }
        androidMain.dependencies {
            implementation(libs.sqldelight.android.driver)   // AndroidSqliteDriver (M2 §3-3 actual)
            implementation(libs.ktor.client.okhttp)          // M3 엔진 actual (androidMain)
            implementation(libs.sentry.android)              // M9 WU-4 — 크래시 리포팅 actual(Android SDK)
        }
        iosMain.dependencies {
            implementation(libs.sqldelight.native.driver)    // NativeSqliteDriver (M2 §3-3 actual)
            implementation(libs.ktor.client.darwin)          // M3 엔진 actual (iosMain)
        }
        // §6-B DB 왕복은 JVM(androidUnitTest)에서만 — JDBC in-memory. 네이티브엔 JDBC 없음(§7-4).
        val androidUnitTest by getting {
            dependencies {
                implementation(libs.sqldelight.sqlite.driver)  // JdbcSqliteDriver(IN_MEMORY)
                implementation(libs.robolectric)               // M9 §3-1·§3-3 — 실 Context/Intent/클립보드 해석
            }
        }
        // M9 §3-2·§3-3(v) 네이티브 실행 test는 `src/iosTest/kotlin`(기본 계층 iosTest 소스셋, 디렉터리 규약)에
        // 둔다 — iosMain 심볼(NativeSqliteDriver·NSUserDefaults) 참조. ⚠️ `:shared:iosSimulatorArm64Test`
        // 축은 M2부터 commonTest를 네이티브 실행해 왔다(신설 아님); iosTest는 그 축에 iosMain-참조 test를 착지시킨다.
    }
}

// 생성 DB: DevEtymDatabase, 패키지 com.robin.devetym.db (M2 §3-1). 소스: src/commonMain/sqldelight.
sqldelight {
    databases {
        create("DevEtymDatabase") {
            packageName.set("com.robin.devetym.db")
        }
    }
}

// 번들 리소스 접근자(Res) 패키지 고정 — commonTest fixture가 실제 terms.json을 로드한다.
compose.resources {
    publicResClass = true
    packageOfResClass = "com.robin.devetym.resources"
    generateResClass = always
}

android {
    namespace = "com.robin.devetym.shared"
    compileSdk = libs.versions.compileSdk.get().toInt()

    defaultConfig {
        minSdk = libs.versions.minSdk.get().toInt()
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    // M9 §3-1·§3-3 — Robolectric가 매니페스트/리소스로 실 Context를 조립하도록(unit test JVM 실행).
    testOptions {
        unitTests {
            isIncludeAndroidResources = true
        }
    }
}
