import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinNativeLink

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidLibrary)          // 클래식 KMP android 타깃(com.android.library)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.kotlinSerialization)
    alias(libs.plugins.skie)
    alias(libs.plugins.sqldelight)              // 로컬 DB(ADR-0003) — .sq → 타입세이프 Kotlin API 생성
}

// ── M9 WU-4B · 크래시 리포팅 commonMain 단일 KMP 배선용 Sentry Cocoa 정적 xcframework ──────────────
// `sentry-kotlin-multiplatform`은 iOS에서 Sentry Cocoa 심볼을 참조한다. 이 프로젝트는 cocoapods 없이
// 정적 프레임워크로 iOS를 빌드하므로, 네이티브 **테스트 실행파일 링크**(:shared:iosSimulatorArm64Test)가
// Sentry Cocoa 프레임워크를 못 찾아 깨졌었다(seam 분리의 원인). 여기서 Sentry 정적 xcframework를 gradle이
// 다운로드(비커밋)하고 iOS 바이너리 링크에 -F/-framework로 공급해 심볼을 해석한다. KMP 0.27.0 ↔ Cocoa 8.58.2.
val sentryCocoaVersion = "8.58.2"
// Sentry Cocoa는 ObjC+**Swift 혼합** 프레임워크다. Kotlin/Native 실행파일(iosSimulatorArm64Test)에 정적 링크하면
// Swift 백호환 정적 라이브러리(swiftCompatibilityConcurrency/Packs/56)와 Swift 런타임 심볼이 필요하다. 이 검색
// 경로들을 현 Xcode 툴체인/SDK에서 동적으로 계산(하드코딩 회피)해 링커에 -L로 공급한다.
fun shellOut(vararg cmd: String): String =
    providers.exec { commandLine(*cmd) }.standardOutput.asText.get().trim()
val xcodeToolchainLibSwift = "${shellOut("xcode-select", "-p")}/Toolchains/XcodeDefault.xctoolchain/usr/lib/swift"
val downloadSentryCocoa by tasks.registering(Exec::class) {
    val sentryDir = layout.buildDirectory.dir("sentry").get().asFile
    val xcframework = File(sentryDir, "Sentry.xcframework")
    outputs.dir(xcframework)
    onlyIf { !xcframework.exists() }
    doFirst { sentryDir.mkdirs() }
    workingDir = layout.buildDirectory.asFile.get().also { it.mkdirs() }
    commandLine(
        "bash", "-lc",
        "set -e; cd sentry; " +
            "curl -sSL -o Sentry.xcframework.zip " +
            "https://github.com/getsentry/sentry-cocoa/releases/download/$sentryCocoaVersion/Sentry.xcframework.zip; " +
            "rm -rf Sentry.xcframework; unzip -q Sentry.xcframework.zip; rm -f Sentry.xcframework.zip"
    )
}
// K/N 링크(테스트 실행파일 포함)는 Sentry.xcframework가 있어야 심볼을 해석한다.
tasks.withType<KotlinNativeLink>().configureEach { dependsOn(downloadSentryCocoa) }

// ── Sentry DSN 빌드타임 주입(경량 코드젠) — 루트 .env → commonMain 상수 ─────────────────────────────
// DSN은 클라이언트 노출 가능한 공개 식별자(시크릿 아님)나, 레포 커밋은 피한다(.env는 gitignore).
// 우선순위: 루트 .env의 SENTRY_DSN → 환경변수 SENTRY_DSN → 빈값(CrashReporter no-op — 개발/CI 안전).
// BuildKonfig 등 플러그인 대신 경량 코드젠 채택(신규 플러그인이 K/N 축을 건드릴 리스크 회피, 사람 결정 2026-07-14).
fun envFileValue(key: String): String? = rootProject.file(".env").takeIf { it.exists() }
    ?.readLines()
    ?.firstNotNullOfOrNull { line ->
        line.trim().takeIf { it.startsWith("$key=") }?.substringAfter("=")?.trim()?.trim('"', '\'')
    }
val sentryDsn: String = envFileValue("SENTRY_DSN") ?: System.getenv("SENTRY_DSN") ?: ""
val generateSentryConfig by tasks.registering {
    val outDir = layout.buildDirectory.dir("generated/sentryConfig/commonMain/kotlin")
    inputs.property("sentryDsn", sentryDsn)
    outputs.dir(outDir)
    doLast {
        val escaped = sentryDsn.replace("\\", "\\\\").replace("\"", "\\\"").replace("$", "\\$")
        val file = outDir.get().file("com/robin/devetym/crash/SentryConfig.kt").asFile
        file.parentFile.mkdirs()
        file.writeText(
            """
            |package com.robin.devetym.crash
            |
            |// 빌드타임 생성 파일 — 편집 금지. 정본: shared/build.gradle.kts `generateSentryConfig`(.env SENTRY_DSN).
            |internal const val SENTRY_DSN: String = "$escaped"
            |""".trimMargin()
        )
    }
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
        // WU-4B: Sentry Cocoa 정적 슬라이스를 이 타깃의 모든 바이너리(프레임워크+테스트 실행파일) 링크에 공급.
        val isSim = iosTarget.name == "iosSimulatorArm64"
        val sentrySlice = if (isSim) "ios-arm64_x86_64-simulator" else "ios-arm64_arm64e"
        val swiftPlatform = if (isSim) "iphonesimulator" else "iphoneos"
        val sentryFrameworksDir =
            layout.buildDirectory.dir("sentry/Sentry.xcframework/$sentrySlice").get().asFile.absolutePath
        val sdkSwiftDir = "${shellOut("xcrun", "--sdk", swiftPlatform, "--show-sdk-path")}/usr/lib/swift"
        iosTarget.binaries.framework {
            baseName = "Shared"
            isStatic = true
        }
        iosTarget.binaries.all {
            // Sentry 정적 프레임워크 + Sentry Cocoa가 요구하는 시스템 프레임워크/라이브러리(정적 링크라 명시 필요) +
            // Swift 백호환/런타임 라이브러리 검색 경로(Sentry의 Swift 심볼 해석).
            linkerOpts(
                "-F", sentryFrameworksDir, "-framework", "Sentry",
                "-framework", "Security",
                "-framework", "SystemConfiguration",
                "-framework", "CoreGraphics",
                "-framework", "UIKit",
                "-L", "$xcodeToolchainLibSwift/$swiftPlatform", // libswiftCompatibility*.a (백호환 정적)
                "-L", sdkSwiftDir,                              // Swift 런타임(libswiftCore 등)
                "-lc++", "-lz"
            )
        }
    }

    sourceSets {
        // 코드젠 상수(SentryConfig.kt)를 commonMain 소스로 등록 — TaskProvider 전달로 컴파일 의존 자동 성립.
        commonMain.configure { kotlin.srcDir(generateSentryConfig) }
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
            implementation(libs.sentry.kotlin.multiplatform)  // M9 WU-4B — 크래시 리포팅 commonMain 단일 배선
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
            implementation(libs.kotlinx.coroutines.test)     // runTest (suspend 테스트)
            implementation(libs.ktor.client.mock)            // MockEngine (§6-A, 네트워크 무의존)
        }
        androidMain.dependencies {
            implementation(libs.sqldelight.android.driver)   // AndroidSqliteDriver (M2 §3-3 actual)
            implementation(libs.ktor.client.okhttp)          // M3 엔진 actual (androidMain)
            // M9 WU-4B — Sentry Android actual은 commonMain의 sentry-kotlin-multiplatform가 전이 제공(직접 의존 제거)
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
