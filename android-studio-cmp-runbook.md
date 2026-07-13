# 런북 — Android Studio 셋업 & CMP 에뮬레이터 테스트

> **대상 프로젝트**: DevEtym Compose Multiplatform 마이그레이션 (M0~M8 코드 레벨 완료)
> **목표**: 물리 폰 없이 **에뮬레이터**로 shared 로직 스모크 테스트 (SQLDelight · Koin 그래프 · Ktor · ViewModel)
> **전제 배경**: Xcode/SwiftUI 경험 있음 → "다른 점" 위주 기술

---

## TL;DR (빠른 경로)

1. Android Studio **Otter(2025.2.1) 이상** 설치 → Setup Wizard는 **Standard**
2. Plugins에서 **Kotlin Multiplatform** 설치 → 재시작
3. `File → Open`으로 레포 루트 열기 → **첫 Gradle Sync 대기(느림, 정상)**
4. Device Manager → 에뮬레이터 생성 (Apple Silicon이면 **arm64** 이미지)
5. run config **androidApp** + 에뮬레이터 타깃 → ▶

---

## 0. 사전 조건

- [ ] macOS / Windows / Linux 아무거나 (Android 개발은 OS 무관 — iOS 단계에서만 Mac 필요)
- [ ] 디스크 여유 **최소 10GB+** (SDK + 에뮬레이터 이미지 + Gradle 캐시)
- [ ] 안정적인 네트워크 (첫 sync에서 의존성 대량 다운로드)

---

## 1. 다운로드 & 설치

- URL: `https://developer.android.com/studio`
- 버전: **Android Studio Otter (2025.2.1) 이상 최신 안정판**
  - ⚠️ CMP/KMP 지원이 이 라인부터 정상 동작. 구버전이면 iOS run config·preflight 누락으로 헤맴
  - IntelliJ IDEA를 쓸 경우 **2025.2.2 이상**
- 여러 버전(EAP·나이틀리) 병행 관리가 필요하면 **JetBrains Toolbox** 경유 설치가 편함. 지금은 직접 다운로드 하나로 충분

---

## 2. 첫 실행 — Setup Wizard

- 처음 실행 시 뜨는 마법사에서 **Standard** 선택
- 자동 설치 항목: Android SDK · SDK Platform · Build Tools · Emulator
- ⏳ 수 GB 다운로드로 시간 소요 (Xcode가 커맨드라인 툴·시뮬레이터 챙겨주던 것의 안드로이드판)

---

## 3. KMP 플러그인 설치 (CMP 필수)

`Settings → Plugins → Marketplace → "Kotlin Multiplatform" 검색 → Install → 재시작`

이걸 깔아야 생기는 것:
- Android run configuration (androidApp)
- 환경 preflight 점검
- commonMain의 Compose Preview

> ⚠️ **iOS run config는 이 레포에선 KMP 플러그인만으로 안 뜰 수 있음.** `iosApp`이 Gradle 모듈이 아니라 XcodeGen으로 생성하는 **독립 Xcode 프로젝트**다(`settings.gradle.kts`에 `:iosApp` 없이 `:shared`·`:androidApp`만 include). iOS는 Android Studio가 아니라 `iosApp.xcodeproj`를 Xcode로 여는 별도 경로(맨 아래 "다음 단계"). Android 에뮬레이터 스모크가 목표라 지장 없음.
> Android만 돌릴 예정이어도 KMP 플러그인은 설치해 둘 것.

---

## 4. 프로젝트 열기 + Gradle Sync ⭐ (Xcode와 가장 다른 지점)

1. `File → Open` → **레포 루트 폴더** 선택
2. **Gradle Sync 자동 실행**
   - `build.gradle.kts`를 읽어 의존성 전부 내려받고 프로젝트 구조 구성
   - ⏳ **첫 sync는 매우 느림 (정상)** — 이후 캐시로 빨라짐

**각인할 멘탈 모델**: 모든 빌드 설정이 **Gradle 파일(코드)**에 있음.
Xcode처럼 GUI로 build settings 만지는 게 아니라 `build.gradle.kts`를 직접 편집.
minSdk · 의존성 · 타깃 플랫폼 전부 여기. (Java/Spring의 Gradle 감각 그대로)

---

## 5. 에뮬레이터(AVD) 생성

`Device Manager → Create Device`

| 항목 | 권장 값 |
|---|---|
| 기기 | Pixel 7 / 8 |
| 시스템 이미지 | targetSdk에 맞는 최신 API (이 레포 `targetSdk=36` → **API 36**), **Google APIs 포함** |
| CPU 아키텍처 | Apple Silicon Mac → **arm64** / Intel → x86_64 |

- Download → Finish → ▶로 부팅
- (선택) `Settings → Tools → Emulator → Launch in a tool window` 켜면 별창 대신 IDE 안에 붙음

---

## 6. 초기 기본 설정 체크리스트

- [ ] **Gradle JDK 확인** — `Settings → Build → Build Tools → Gradle`. 번들 JBR(17/21)로 두되 프로젝트 요구 JDK와 일치하는지 확인
- [ ] **메모리 상향**(프로젝트 크면) — `gradle.properties`에 `org.gradle.jvmargs=-Xmx4g`
- [ ] **Compose Preview** — commonMain의 `@Preview` 컴포저블 IDE 미리보기 (SwiftUI Preview 대응물)
- [ ] **KDoctor** — 터미널 `kdoctor` (iOS 갈 때 환경 꼬임 사전 진단; Android만이면 급하진 않음)
- [ ] **키맵** — `Settings → Keymap`에서 macOS 프리셋 확인 (Xcode와 다름)

---

## 7. 실행

1. 상단 run configuration에서 **androidApp** 선택
2. 타깃을 방금 만든 에뮬레이터로 지정
3. ▶ 실행 (`Ctrl+R` / `Shift+F10`)
4. ⏳ 첫 빌드 느림(정상) → 이후 증분 빌드 빨라짐

---

## ✅ 검증 체크포인트 (스모크 목표)

에뮬레이터에서 다음이 도는지 확인 — 이게 M8 잔여 게이트 중 실기기 없이 커버되는 범위:

- [ ] 앱이 크래시 없이 기동 (Koin 그래프 완전성)
- [ ] 로컬 DB 읽기/쓰기 동작 (SQLDelight)
- [ ] 네트워킹 read-through 동작 (Ktor 3계층)
- [ ] ViewModel 상태 흐름 → Compose UI 렌더
- [ ] seam 동작: mailto · 공유 · 클립보드
- [ ] 외관 전환: appearance.mode → 테마 실반영

> 물리 기기가 유의미하게 다른 지점: GPU 성능 프로파일링, 카메라, 실전 배터리·발열. 스모크 단계에선 거의 안 걸림.

---

## 🔧 트러블슈팅

| 증상 | 원인 / 조치 |
|---|---|
| 첫 Gradle sync가 끝없이 느림 | **정상.** 의존성 대량 다운로드 + 캐시 채우는 중 |
| Sync 실패 (버전/의존성 에러) | 로그의 실패 모듈 확인 → `build.gradle.kts` 버전 정합성. 에러 로그 그대로 들고 문의 |
| iOS run config가 안 뜸 | Android Studio 버전이 Otter 미만·KMP 플러그인 미설치, **또는 이 레포처럼 `iosApp`이 Gradle 모듈이 아니라 독립 Xcode 프로젝트인 경우(정상)** → iOS는 `iosApp.xcodeproj`를 Xcode로 |
| 에뮬레이터가 극도로 느림 | Apple Silicon인데 x86_64 이미지 선택함 → arm64로 재생성 |
| 빌드는 되는데 앱 설치 실패 | 에뮬레이터 저장공간 부족 / 콜드부트 재시도 |

---

## 🔁 Xcode → Android Studio 용어 대응

| Xcode / Swift | Android Studio / CMP |
|---|---|
| Xcode (단일 IDE) | Android Studio + KMP 플러그인 |
| 시뮬레이터 | 에뮬레이터 (AVD) |
| SwiftUI Preview | Compose `@Preview` |
| Build Settings (GUI) | `build.gradle.kts` (코드) |
| — (대응 없음) | **Gradle Sync** (설정 다시 읽기) |
| Provisioning 점검 | KDoctor (iOS 단계에서) |

**혼동 주의 3가지**
1. **Sync ≠ Build** — 의존성·gradle 파일 건드리면 sync, 코드만 고치면 build
2. **첫 sync/빌드 느린 건 고장 아님** — 캐시 채우는 중
3. **설정은 GUI가 아니라 Gradle 코드** — 바꿀 게 있으면 `.gradle.kts`부터

---

## 다음 단계 (이 런북 이후)

- Android 에뮬레이터 스모크 통과 → iOS 트랙: `iosApp.xcodeproj`를 Xcode로 열어 서명·시뮬레이터/실기기
- iOS 실기기·서명·심사는 별도 런북으로 분리 권장
