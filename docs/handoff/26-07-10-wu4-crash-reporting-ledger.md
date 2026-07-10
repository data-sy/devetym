# WU-4 · 크래시 리포팅(Sentry) 도입 — 완료 원장 (2026-07-10)

> 계획 원장 = [`26-07-10-selfcontained-migration-plan.md`](26-07-10-selfcontained-migration-plan.md) §2 WU-4. 상태 정본 = [ROADMAP](../../ROADMAP.md).
> 결정: D1(SDK=Sentry)·D2(도입)·방침 사인오프(사용자 사전 승인 2026-07-10).

## 1. 핵심 결정 — D1 실측 정정: "commonMain 단일 KMP 배선" → 플랫폼 seam 분리

원안(D1)은 `io.sentry:sentry-kotlin-multiplatform`을 **commonMain 단일 배선**하는 것이었다. 착수 후 실측:

- `sentry-kotlin-multiplatform`을 commonMain에 추가하니 **klib 해석·iOS 네이티브 컴파일·정적 프레임워크 링크는 성공**(Kotlin 2.3.21에서도 OK).
- 그러나 **`:shared:iosSimulatorArm64Test`가 깨짐** — 네이티브 **테스트 실행파일** 링크가 Sentry Cocoa 프레임워크를 못 찾음: `ld: framework 'Sentry' not found`.
- 원인: 이 프로젝트는 **cocoapods 없이** XcodeGen + SKIE **정적 프레임워크**로 iOS를 빌드한다. 정적 앱-프레임워크 링크(`linkDebugFrameworkIosSimulatorArm64`)는 심볼 해석을 앱 링크로 미루므로 `-lsqlite3`처럼 통과하지만, **테스트 실행파일**은 링크 시 심볼을 완전 해석해야 하므로 Sentry Cocoa가 없으면 실패한다.
- Sentry 공식 iOS 통합은 **cocoapods 전제**(`pod("Sentry")`) — 현 setup과 충돌.

→ **사람 결정으로 플랫폼 seam 분리 채택**(commonMain 단일 KMP 배선은 출시 후 백로그로 이연). SDK는 여전히 Sentry(D1 유지).

## 2. 배선 구조 (5축 green)

| 레이어 | 파일 | 내용 |
|---|---|---|
| commonMain | `crash/CrashReporter.kt` | `expect object CrashReporter { init(dsn); captureTestMessage(msg); capture(t) }` — **Sentry 무참조**(iOS 테스트 축 보호) |
| androidMain | `crash/CrashReporter.android.kt` | actual = `io.sentry:sentry-android` 8.48.0. `Sentry.init{ dsn; isSendDefaultPii=false }` — JVM 미포착 예외 핸들러가 크래시 포착 |
| iosMain | `crash/CrashReporter.ios.kt` | actual = **no-op**. iOS 실 크래시는 Swift/Sentry Cocoa(SPM) 담당(WU-11) |
| 배선 | `di/AppModule.kt` | `initKoin(platformModule, crashDsn)` 최상단에서 `CrashReporter.init(crashDsn)`(Koin보다 먼저 — 조기 크래시 포착) |
| Android DSN | `androidApp/build.gradle.kts` · `android/DevEtymApp.kt` | `buildConfig=true` + `SENTRY_DSN`(gradle 프로퍼티 `-PSENTRY_DSN`/env, 없으면 `""`) → `initKoin(..., crashDsn=BuildConfig.SENTRY_DSN)` |
| iOS DSN | `iosApp/Info.plist` · `iosApp/project.yml` | `SentryDsn`=`$(SENTRY_DSN)` 빌드 세팅(기본 `""`). Swift가 읽어 `SentrySDK.start`(WU-11) |
| 테스트 | `commonTest/crash/CrashReporterTest.kt` | 미설정(빈/누락 DSN) no-op·멱등 가드 2건 — 양 플랫폼 actual 실행 |
| 방침 | `site/privacy-policy.md` · `docs/release/m9-store-metadata-draft.md` | §2-2 크래시 진단 신설·§3·§4 정합·스토어 라벨 반영 |
| 카탈로그 | `gradle/libs.versions.toml` | `sentryAndroid = "8.48.0"`, `sentry-android` 라이브러리 |

**시크릿 규율**: DSN은 커밋하지 않는다. 비면 `CrashReporter`가 초기화하지 않아 개발·CI에서 안전한 no-op. 배포 시 주입: Android=`-PSENTRY_DSN=…`/CI 시크릿, iOS=xcconfig/CI(`SENTRY_DSN`).

**방침 정합**: `sendDefaultPii=false`로 PII 미부착. 전송=스택트레이스·기기 모델·OS/앱 버전 등 최소 진단. 수탁자=Sentry. 애널리틱스(Firebase)와 별개 — 계속 미수집.

**5축 green 실측(2026-07-10)**: `:shared:testDebugUnitTest` · `:shared:iosSimulatorArm64Test`(신규 `CrashReporterTest` 2건 네이티브 실행 포함) · `:shared:linkDebugFrameworkIosSimulatorArm64` · `:androidApp:assembleDebug` · `:androidApp:testDebugUnitTest`. 회귀 0.

## 3. iOS Sentry Cocoa 활성화 절차 (WU-11 · 출시 전 필수)

iosMain no-op이라 **iOS 실 크래시는 아직 미포착**. iOS 스토어 출시 전 Swift/SPM으로 켠다(cocoapods 불요·현 XcodeGen setup에 자연 결합). 절차 정본 = [`iosApp/project.yml`](../../iosApp/project.yml) 주석:

1. `project.yml`에 SPM 패키지 추가: `packages: { Sentry: { url: https://github.com/getsentry/sentry-cocoa, majorVersion: 8 } }`.
2. `targets.iosApp`에 `dependencies: [ { package: Sentry } ]`.
3. `iOSApp.swift`에서 `doInitKoin()` 이전에 `SentrySDK.start { $0.dsn = Bundle.main.object(forInfoDictionaryKey:"SentryDsn") as? String ?? ""; $0.sendDefaultPii = false }`.
4. `xcodegen generate` → Xcode SPM resolve → 시뮬 스모크로 테스트 크래시(예: 강제 `fatalError`) Sentry 도달 확인.

⚠️ **지금 켜지 않은 이유**: SPM 패키지 없는 현 커밋 `.xcodeproj`에서 `import Sentry`가 컴파일 실패 → 시뮬 스모크가 깨진다. iOS Xcode 통합은 축 밖(실기기 게이트)이라 WU-11로 스코프.

## 4. 근본 해결 (출시 후 백로그 — ROADMAP Later)

**commonMain 단일 KMP 배선**으로 통합(배선 이원화 해소). 후보·판정은 ROADMAP Later "[Arch] 크래시 리포팅 commonMain 단일 KMP 배선으로 통합" 참조:
- ① **Kotlin Cocoapods 플러그인**(Sentry 공식·가장 완결적, 테스트 축까지 green 유지) — but 현 XcodeGen+SKIE 파이프라인 이중화·침습적. **가장 근본적**.
- ② Sentry KMP Gradle 플러그인 + SPM(`frameworkPath`) — cocoapods 없이 모던하나 순수 gradle 테스트 축 닭-달걀.
- ③ Sentry.xcframework 벤더링 + linkerOpts — 워크어라운드(대용량 바이너리).

**출시 필수 아님**(seam 분리로 크래시 리포팅 양 플랫폼 작동). iOS 네이티브 활성화(§3)는 출시 전(WU-11)이나 이 통합은 출시 후.

## 5. 단일 통합 완료 — WU-4B (2026-07-10, Approach B 채택)

§4의 "근본 해결"을 **앞당겨 실행**(사람 병목 회피 지시). Approach A(Kotlin Cocoapods 플러그인)는 **CocoaPods CLI 부재**(`pod` not found)로 스킵 → **Approach B(Sentry.xcframework 벤더링 + linkerOpts)** 채택. **성공**: commonMain 단일 `sentry-kotlin-multiplatform` 0.27.0 배선 + 5축 green + 기존 iOS Xcode 빌드 무손상(실검증).

**핵심 해소** — §1이 막혔던 `:shared:iosSimulatorArm64Test` 네이티브 테스트 실행파일 링크를 성립시킨 방법:
1. **Sentry Cocoa 정적 xcframework 8.58.2**(KMP 0.27.0 매핑) 다운로드 — `shared/build.gradle.kts`의 `downloadSentryCocoa` Exec 태스크가 `shared/build/sentry/`에 받아 압축해제(**비커밋** — `shared/build/`는 gitignore). `KotlinNativeLink` 태스크 전체가 이 태스크에 `dependsOn`.
2. **linkerOpts**(`binaries.all`)로 iOS 전 바이너리(프레임워크+테스트 실행파일)에 Sentry 정적 슬라이스를 공급: `-F <slice> -framework Sentry` + Sentry Cocoa 의존 시스템 프레임워크(Security·SystemConfiguration·CoreGraphics·UIKit)·`-lc++ -lz`.
3. **Swift 백호환/런타임 라이브러리 경로** — Sentry Cocoa는 ObjC+**Swift 혼합**이라 정적 링크 시 `swiftCompatibilityConcurrency/Packs/56` 미해결이 났다. Xcode 툴체인/SDK에서 `xcrun`으로 동적 계산한 `-L <toolchain>/usr/lib/swift/{iphonesimulator,iphoneos}`·`-L <sdk>/usr/lib/swift` 추가로 해소. **이게 B의 진짜 관문이었다**(핸드오프가 예상 못한 2차 링크 장벽).

**배선 구조 변화**(§2 대비):
- commonMain `crash/CrashReporter.kt` = **plain object**(expect/actual 제거), `io.sentry.kotlin.multiplatform.Sentry` 직접 사용. androidMain/iosMain actual **삭제**.
- Android DSN 경로 불변(BuildConfig→initKoin). **iOS DSN**: iosMain `doInitKoin()`이 `NSBundle.mainBundle.objectForInfoDictionaryKey("SentryDsn")`을 읽어 `initKoin(crashDsn=…)`으로 전달 → 공통 `CrashReporter.init`이 iOS에서 Sentry 초기화(**Swift `SentrySDK.start` 불요** — §3 WU-11 SPM 절차는 폐기).
- iOS Xcode 앱: `Shared.framework`가 이제 Sentry 심볼 참조 → `iosApp/project.yml`에 `dependencies: - framework: ../shared/build/sentry/Sentry.xcframework`(link·no-embed) 추가. `xcodegen generate`로 `.xcodeproj` 재생성(diff 27줄, Sentry 링크 배선만). preBuildScript의 gradle embedAndSign이 `downloadSentryCocoa`를 선행하므로 링크 시점 xcframework 항상 존재.
- 카탈로그: `sentry-android`/`sentryAndroid` 제거(KMP가 전이 배선), `sentry-kotlin-multiplatform`/`sentryKmp=0.27.0` 추가.

**실검증(2026-07-10)**:
- 5축 green(회귀 0): `:shared:testDebugUnitTest` · `:androidApp:assembleDebug` · `:androidApp:testDebugUnitTest` · `:shared:linkDebugFrameworkIosSimulatorArm64` · `:shared:iosSimulatorArm64Test`(`CrashReporterTest` 2건 네이티브 실행·통과).
- **Xcode 시뮬 빌드 SUCCEEDED**(`xcodebuild -scheme iosApp -sdk iphonesimulator -destination 'iPhone 17 Pro' ONLY_ACTIVE_ARCH=YES` — arm64 전용. ⚠️ 제네릭 시뮬 대상은 x86_64를 요구해 gradle 프레임워크 빌드가 깨지는데 이는 프로젝트가 `iosSimulatorArm64`만 지원하는 **기존 제약**·Sentry 무관). 앱에 Sentry Cocoa가 실제 링크됨 → iOS 크래시 포착이 no-op이 아니게 됨(WU-11 SPM 절차 대체).

**되돌리기**: 실패 시 커밋 revert로 seam 분리(`eb939a7`) 복귀 가능하도록 원자적 커밋. (이번엔 성공이라 백아웃 불요.)
