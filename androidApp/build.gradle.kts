plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.kotlinAndroid)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
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

        // M9 WU-4 — 크래시 DSN 주입(BuildConfig). 시크릿 미커밋: gradle 프로퍼티(-PSENTRY_DSN / local.properties)
        // 또는 환경변수 SENTRY_DSN로 주입, 없으면 빈 문자열 → CrashReporter no-op(개발·CI 안전).
        val sentryDsn = (project.findProperty("SENTRY_DSN") as String?) ?: System.getenv("SENTRY_DSN") ?: ""
        buildConfigField("String", "SENTRY_DSN", "\"$sentryDsn\"")
    }

    buildFeatures {
        buildConfig = true   // M9 WU-4 — SENTRY_DSN BuildConfig 필드 생성
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
