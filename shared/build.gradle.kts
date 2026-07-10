import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidLibrary)          // 클래식 KMP android 타깃(com.android.library)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.kotlinSerialization)
    alias(libs.plugins.skie)
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
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
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
}
