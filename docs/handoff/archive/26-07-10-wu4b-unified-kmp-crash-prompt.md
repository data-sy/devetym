# WU-4B · 크래시 리포팅 commonMain 단일 KMP 통합 — 새 세션 실행 프롬프트 (2026-07-10 작성)

> ✅ **실행 완료(2026-07-10)** — Approach B(Sentry.xcframework 벤더링 + linkerOpts + Swift 백호환 라이브러리 경로)로 성공. 5축 green + Xcode 시뮬 빌드 SUCCEEDED. 결과 정본 = [WU-4 원장 §5](26-07-10-wu4-crash-reporting-ledger.md). 아래는 실행 당시의 프롬프트(기록 보존).

> **이 문서는 새 독립 세션에 그대로 붙여 넣을 실행 프롬프트다.** WU-4(플랫폼 seam 분리)의 후속 — "근본 해결"인
> commonMain 단일 `sentry-kotlin-multiplatform` 배선을 **자동화 가능한 범위(5축 green)** 내에서 시도한다.
> ROADMAP Later의 "[Arch] 크래시 리포팅 commonMain 단일 KMP 배선으로 통합"을 앞당겨 실행하는 작업.

---

## 붙여넣을 프롬프트 (여기부터)

devetym 프로젝트에서 **크래시 리포팅을 commonMain 단일 KMP 배선으로 통합**하는 스파이크를 진행해줘. 현재는 WU-4에서
**플랫폼 seam 분리**(Android=`sentry-android` 실배선 / iOS=Swift·Sentry Cocoa SPM no-op)로 되어 있는데, 이걸
`io.sentry:sentry-kotlin-multiplatform` **하나로 commonMain에서 배선**하는 게 목표다. 단, **5축 green을 반드시 유지**하고
**기존 iOS Xcode 빌드 파이프라인(XcodeGen+SKIE+embedAndSign)을 깨지 않아야** 한다.

### 0. 먼저 읽을 것 (정본 — 재발견 금지)
- `docs/handoff/26-07-10-wu4-crash-reporting-ledger.md` — WU-4 원장. **왜 단일 KMP가 iOS 테스트 축을 깨는지**의 실측이 여기 있다.
- ROADMAP.md `Later` 섹션 "[Arch] 크래시 리포팅 commonMain 단일 KMP 배선으로 통합" — 3개 후보·판정.
- 메모리 `ios-native-framework-test-axis-constraint` — 핵심 제약 요약.
- 현재 seam 코드: `shared/src/{commonMain,androidMain,iosMain}/kotlin/com/robin/devetym/crash/CrashReporter*.kt`, 배선=`di/AppModule.kt`.

### 1. 문제의 본질 (재확인)
`sentry-kotlin-multiplatform`을 commonMain에 넣으면 klib 해석·iOS 정적 프레임워크 링크(`:shared:linkDebugFrameworkIosSimulatorArm64`)는
**통과**하지만, **`:shared:iosSimulatorArm64Test`(네이티브 테스트 실행파일 링크)가 `ld: framework 'Sentry' not found`로 깨진다**.
이 프로젝트는 **cocoapods 없이** 정적 프레임워크로 iOS를 빌드하기 때문 — 테스트 실행파일 링크는 Sentry Cocoa 심볼을 완전
해석해야 하는데 프레임워크가 제공되지 않는다. 따라서 이 스파이크의 **성패는 `iosSimulatorArm64Test`가 Sentry Cocoa를 해석하게
만들 수 있느냐**에 달려 있다.

### 2. 시도 순서 (A → B, 안 되면 정직 보고)
**Approach A — Kotlin Cocoapods 플러그인 (가장 근본적, 먼저 시도).**
- 전제 확인: `pod --version`(CocoaPods CLI)·네트워크. 없으면 A 스킵하고 B로.
- `shared/build.gradle.kts`에 `kotlin { cocoapods { ... pod("Sentry") { version = "8.58.2" } } }` 적용. 목표는 cocoapods가
  Kotlin/Native cinterop+**테스트 링크**에 Sentry Cocoa 프레임워크를 공급하게 하는 것.
- seam을 되돌려 **단일 commonMain `CrashReporter`**로: commonMain에서 `io.sentry.kotlin.multiplatform.Sentry` 직접 사용
  (`Sentry.init { it.dsn=...; it.sendDefaultPii=false }`, `captureException`, `captureMessage`). androidMain/iosMain actual 삭제,
  `sentry-android` 의존 제거, `libs.versions.toml` 정리.
- ⚠️ **핵심 가드**: cocoapods 플러그인이 **기존 `project.yml` preBuildScript(`embedAndSignAppleFrameworkForXcode`)·SKIE 정적
  프레임워크 소비 방식을 밀어내면 안 된다.** cocoapods가 Podfile/프레임워크 소비를 자기가 소유하려 들어 XcodeGen setup과
  이중화·충돌하는지 실측하라. **충돌해서 iOS Xcode 빌드를 깨뜨리면 A를 백아웃**하고 B로.

**Approach B — Sentry.xcframework 벤더링 + linkerOpts (A 불가 시).**
- `sentry-kotlin-multiplatform`을 commonMain에 유지(단일 배선), 위와 같이 seam 되돌림.
- Sentry Cocoa **정적/동적 xcframework**를 확보(getsentry/sentry-cocoa 릴리스 `Sentry.xcframework.zip` 8.58.2 — Cocoa SDK 매핑은
  KMP 0.27.0 기준). 큰 바이너리이므로 **커밋 위치 신중히**(가능하면 gradle 다운로드/캐시로 비커밋, 불가피하면 `iosApp/vendor/`).
- `shared/build.gradle.kts`에서 `iosSimulatorArm64()`·`iosArm64()`의 test/framework 바이너리에
  `linkerOpts("-F", "<xcframework 시뮬 슬라이스 경로>", "-framework", "Sentry")` 추가 → 테스트 실행파일 링크가 심볼 해석.
- 동적 프레임워크면 테스트 **실행** 시 로더 경로 문제 가능 — 정적 슬라이스 우선. app 링크용 `project.yml` OTHER_LDFLAGS도 동반 필요(주석 이미 있음).

### 3. 하드 제약 (어긋나면 백아웃)
1. **5축 green 회귀 0** — 특히 `:shared:iosSimulatorArm64Test`가 실제로 실행·통과. (`CrashReporterTest`는 단일 배선에 맞게
   유지/조정. `expect/actual`이 사라지면 commonTest 그대로 양 플랫폼 실행.)
2. **기존 iOS Xcode 빌드를 깨지 않는다** — `project.yml`/`.xcodeproj`로 시뮬 빌드가 여전히 성립해야 한다(현재 M9 스모크가 의존).
   Xcode 앱 빌드·실기기 크래시 도달은 축 밖(WU-11)이지만, **기존에 되던 빌드가 안 되게 만들면 안 된다**.
3. **시크릿 미커밋** — DSN은 지금처럼 BuildConfig/Info.plist 주입·비면 no-op 유지.
4. **방침/라벨은 이미 갱신됨**(WU-4). 단일 배선으로 바뀌어도 수집 범위는 동일(스택트레이스·기기/OS, PII off)이라 **방침 재갱신 불필요** —
   변화 있으면만 반영.
5. **되돌리기 가능하게** — 원자적 커밋. 실패하면 이 스파이크 커밋을 revert해 **현재 known-good(seam 분리, 커밋 `eb939a7`) 상태로
   복귀**. 반쯤 깨진 채로 남기지 말 것.

### 4. 검증 (5축)
```
./gradlew :shared:testDebugUnitTest :androidApp:assembleDebug :androidApp:testDebugUnitTest \
          :shared:linkDebugFrameworkIosSimulatorArm64 :shared:iosSimulatorArm64Test
```
(iOS 네이티브 컴파일·링크·테스트 실행 포함 ~5분+. `iosSimulatorArm64Test` 결과 XML에서 `CrashReporterTest` 통과 확인.)

### 5. DoD·마감
- **성공 시**: 단일 commonMain 배선 + 5축 green + 기존 iOS 빌드 무손상. `docs/handoff/26-07-10-wu4-crash-reporting-ledger.md`에
  §5 "단일 통합 완료" 추가(어느 Approach·왜)·ROADMAP Later 항목을 **완료로 이동/삭제**·`libs.versions.toml`/build 정리 반영.
  per-WU 커밋(**push 금지** — 사람 게이트). 메모리 `ios-native-framework-test-axis-constraint`에 "단일 통합 해소(방법=X)" 한 줄 갱신.
- **실패/비자동화 판정 시**: 왜 안 되는지(예: cocoapods CLI 부재, XcodeGen 충돌, 벤더링 바이너리 과대) 원장에 정직 기록,
  스파이크 백아웃해 seam 분리 유지, ROADMAP Later 항목에 "시도했으나 X 이유로 보류" 갱신. **억지로 5축을 노란색/스킵으로 끌고
  가지 말 것.**

### 6. 맥락 (게이트)
사람 게이트 완화 정책(메모리 `milestone-human-gate-relaxed`) 하에 **구현·5축 green까지 자율 진행** OK. 사람 리뷰는 아침 완성물.
안전선 유효: **push 금지·브랜치 보존·시크릿 미커밋**. 이 작업은 ROADMAP상 출시-후 항목이나 사용자가 사람 병목 회피 위해 앞당겨 지시함.

## 프롬프트 끝
