plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.kotlinAndroid)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.sentryAndroidGradle)   // 심볼(mapping) 업로드 전용 — SDK 자동설치·계측 전부 비활성(아래)
}

android {
    namespace = "com.robin.devetym"
    compileSdk = libs.versions.compileSdk.get().toInt()

    defaultConfig {
        applicationId = "com.oddmuffin.devetym"
        minSdk = libs.versions.minSdk.get().toInt()
        targetSdk = libs.versions.targetSdk.get().toInt()
        versionCode = 1
        versionName = "0.1.0"
        // (구 SENTRY_DSN BuildConfig 주입은 제거 — DSN은 shared 코드젠 상수로 단일화. 루트 .env → generateSentryConfig.)
    }

    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

kotlin {
    jvmToolchain(17)
}

// ── Sentry 심볼 업로드(Android mapping·소스컨텍스트 대비 배선) ─────────────────────────────────────────
// auth token은 DSN과 달리 **진짜 시크릿** — 루트 .env의 SENTRY_AUTH_TOKEN(gitignore)로만 공급, 커밋 금지.
// 프로젝트 slug는 .env SENTRY_PROJECT(비밀 아님·사람이 확인 후 기입). 둘 중 하나라도 없으면 업로드 로직
// 자체를 끈다(no-op — 개발/CI 안전). 현재 release minify off라 mapping은 미생성 — 난독화 켤 때를 대비한 배선.
fun envFileValue(key: String): String? = rootProject.file(".env").takeIf { it.exists() }
    ?.readLines()
    ?.firstNotNullOfOrNull { line ->
        line.trim().takeIf { it.startsWith("$key=") }?.substringAfter("=")?.trim()?.trim('"', '\'')
    }
val sentryAuthToken = envFileValue("SENTRY_AUTH_TOKEN") ?: System.getenv("SENTRY_AUTH_TOKEN") ?: ""
val sentryProjectSlug = envFileValue("SENTRY_PROJECT") ?: System.getenv("SENTRY_PROJECT") ?: ""
val sentryUploadEnabled = sentryAuthToken.isNotBlank() && sentryProjectSlug.isNotBlank()

sentry {
    org.set("oddmuffin-studio")
    projectName.set(sentryProjectSlug)
    authToken.set(sentryAuthToken)
    telemetry.set(false)
    // KMP 단일 배선 보호 — 크래시 SDK는 commonMain의 sentry-kotlin-multiplatform 0.27.0 핀이 정본.
    // 이 플러그인의 SDK 자동설치·바이트코드 계측·의존성 리포트가 그 핀/전이 sentry-android를 건드리지 않게 전부 끈다.
    autoInstallation { enabled.set(false) }
    tracingInstrumentation { enabled.set(false) }
    includeDependenciesReport.set(false)
    includeSourceContext.set(false)
    // mapping 업로드는 토큰+slug 갖춰졌을 때만 활성(부재 시 관련 태스크 자체 제외 — no-op 가드).
    includeProguardMapping.set(sentryUploadEnabled)
    autoUploadProguardMapping.set(sentryUploadEnabled)
}

dependencies {
    implementation(project(":shared"))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.core.splashscreen)   // M9 WU-9 — Android 스플래시(iOS UILaunchScreen 정합)
    implementation(compose.runtime)
    implementation(compose.foundation)
    implementation(compose.material3)
    implementation(compose.ui)
    implementation(libs.koin.android)
    testImplementation(libs.junit)   // M9 WU-10 — 셸 배선 회귀 가드(순수 JVM)
}
