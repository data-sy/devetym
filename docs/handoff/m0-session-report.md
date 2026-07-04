# M0 세션 리포트 — iOS interop 결정 + KMP 골격 (2026-07-04)

> 이 세션에서 **무엇을·어떻게** 했는지의 서술 기록. 진행 상태 정본은 [ROADMAP](../../ROADMAP.md),
> 결정 정본은 [ADR-0005](../adr/0005-ios-interop.md). 이 리포트는 그 둘로 흡수되는 참고용 로그다.

## 1. 결과 요약

- **iOS interop = SKIE 확정**([ADR-0005](../adr/0005-ios-interop.md)) — 2026-07 웹 확인 + 빌드 실측.
- **M0 KMP 골격 완료** — `shared + androidApp + iosApp`가 서고, 공유 `Greeting`을 Koin으로 배선해
  **Android(APK) + iOS(시뮬레이터) 양쪽에서 공유 Compose 화면이 실제로 렌더**됨.
- 커밋(브랜치 `feat/m0-kmp-scaffold`, push 안 함):
  - `629d11d` interop 결정(ADR-0005)·버전 매트릭스·gitignore
  - `72c2c13` M0 골격 + green 루프
  - (+ 이 세션 마지막: iOS Xcode 프로젝트·리포트)

## 2. iOS interop 리서치 → 결정

두 후보 SKIE(Touchlab) vs Swift Export(JetBrains)를 2026-07 사실로 비교(기억 아닌 웹 확인).
- Swift Export는 여전히 **Alpha**, 제네릭 미지원·sealed→enum 미지원 → 코어 경계 리스크.
- SKIE는 프로덕션 성숙, devetym 경계(suspend `fetch` + `Flow`/`StateFlow`)에 정확히 맞음.
- 결론: **SKIE**. 재평가 트리거(Swift Export가 Beta+제네릭 커버)는 ADR-0005에 명시.

## 3. 확정 버전 매트릭스 (빌드로 실측 검증)

| 구성요소 | 버전 |
|---|---|
| Kotlin | **2.3.21** |
| Compose Multiplatform | 1.11.1 |
| Android Gradle Plugin | 8.13.0 |
| Gradle | 8.13 |
| SKIE | 0.10.12 |
| compileSdk / minSdk / targetSdk | 36 / 26 / 36 |
| identity | `com.robin.devetym` |

버전은 [`gradle/libs.versions.toml`](../../gradle/libs.versions.toml) 한 곳에서 관리.

## 4. 부딪힌 함정과 해결 (핵심 엔지니어링 로그)

버전 불일치가 KMP 1위 실패원 — 아래는 전부 **빌드가 진실을 알려준** 사례다.

1. **SKIE 0.10.12 ✗ Kotlin 2.4.0.** 웹 조사값("SKIE↔Kotlin 2.0–2.4")은 낙관적이었고, 플러그인이 2.4.0을
   명시적으로 거부(상한 **2.3.21**). → Kotlin 2.3.21로 고정. *교훈: KMP 버전 사실은 웹보다 빌드가 정본.*
2. **AGP 9의 "내장 Kotlin"** 이 앱 모듈 Kotlin을 AGP에 묶어 shared(2.3.21)와 어긋남. → **AGP 8.13 클래식 KMP**
   (`com.android.library` + `androidTarget`)로 전환, Kotlin을 양 모듈에서 2.3.21로 통일.
3. **Gradle 9.2.1(시스템) ↔ AGP 8.13 비호환.** → 래퍼를 **Gradle 8.13**으로 교체(빈 디렉터리에서 생성 후 복사 —
   프로젝트 안에서 생성하면 AGP가 Gradle 9 아래서 config 에러).
4. **CMP 1.11.1은 `iosX64`(구 Intel 시뮬) 미배포.** variant 매칭 실패 → iosX64 타깃 제거(arm64 기기 + Apple Silicon 시뮬만).
5. **identity 불일치**: 스캐폴드에 임의로 `com.devetym`/minSdk24를 씀 → README 정본대로 `com.robin.devetym`/minSdk26으로 정정.
6. **iOS 실행 즉시 abort(SIGABRT).** Compose iOS의 Plist 가드가 `CADisableMinimumFrameDurationOnPhone=true`를
   요구 → Info.plist에 키 추가하니 정상 렌더.

## 5. 검증 근거 (green 루프)

```bash
./gradlew :shared:testDebugUnitTest                      # ✅ GreetingTest
./gradlew :androidApp:assembleDebug                      # ✅ androidApp-debug.apk
./gradlew :shared:linkDebugFrameworkIosSimulatorArm64    # ✅ Shared.framework (SKIE 처리 포함)
```
- iOS: `iosApp.xcodeproj`(XcodeGen) 빌드 → 시뮬레이터 실행 → 화면에 "Hello, iOS 18.5! — DevEtym KMP 골격" 렌더 확인.

## 6. 정리(GC) — 이 세션 다운로드/설치 처리

| 항목 | 판정 |
|---|---|
| Android cmdline-tools · SDK(platform-36, build-tools 36/35) | **유지**(빌드 필수) |
| Gradle 8.13 · Kotlin/Native(~/.konan) 툴체인 | **유지**(빌드 필수) |
| **XcodeGen** | **제거** — `.xcodeproj`를 커밋해 빌드에 불필요. 스펙 변경 시 `brew install xcodegen`로 재설치 |
| scratch(wrapgen)·임시 스크린샷 | **삭제** |
| 삭제된 브랜치 `feat/kmp-scaffold` dangling | `git gc`로 정리 |

## 7. 다음 (M1 — 별도 세션)

`TermEntry`(@Serializable)·`Source`·`TermResult`(sealed)·매퍼([spec 1-1], architecture §4.1).
green 루프가 수렴 오라클로 이미 서 있으니, 모델 추가 후 `:shared:testDebugUnitTest`로 회귀 확인.
