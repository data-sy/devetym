# M9-후속 · UX 셸 재설계 — 키보드·네비게이션·플랫폼 액션 (draft)

> **작성 2026-07-13 · 사용자 승인 2026-07-13(같은 날).** 실기기(아이폰 13 mini) 피드백 2차(UX-4~6)·3차(6건)를 통합하는 설계.
> **원칙(사용자 지시): 화면별 땜질 금지.** 증상들이 전부 *셸 계층 부재*(네비·인셋·키보드·플랫폼 액션의
> 정본이 없고 화면마다 국소 패치)에서 나오므로, 셸 계층을 정본화하고 화면은 순수 content로 되돌린다.
> **승인 시 확정된 결정**: 메일 폴백 채택(§2-D)·동의 토글 영속 스텝 1 편입(§2-F).
> **구현은 새 세션에서 스텝별로** — 착지 가이드는 §6. 이 문서 하나로 자기완결(다른 대화 맥락 불요).

## §0 통합 대상 피드백

| # | 증상 (실기기) | 근본 원인 (코드) |
|---|---|---|
| UX-4 | 복사 버튼이 "왜 이 이름인가" 밑인데 어원만 복사 → 오인 | `DetailScreen` `onCopy(entry.etymology)` 단일 필드 |
| UX-5 | 뒤로가기가 휙 바뀜(전환 애니 없음) | 상태 교체 네비 — push/pop 애니메이션 부재 |
| UX-6 | 공유 버튼 무반응 | `IosAppActions.share` no-op 스텁(M8 §7 판정) |
| 3-1 | 키보드가 뜨면 화면 전체가 올라가 헤더·최근 검색이 사라짐 | iOS `ComposeUIViewController` 기본 `onFocusBehavior=FocusableAboveKeyboard`(뷰포트 시프트). 인셋 기반 처리(imePadding) 사용 0 |
| 3-2 | 키보드를 한 번 열면 끌 수 없음 | dismiss 수단 부재(스와이프·탭·스크롤 어디에도 없음) |
| 3-3 | 탭 스와이프 시 키보드가 열려 있음 | 페이지 전환 → 포커스 해제 배선 없음 (닫히는 게 맞다 — iOS 관례) |
| 3-4 | 하단 바 리퀴드 글라스 제안 | — 백로그(출시 전 검토·미확정, §5) |
| 3-5 | 설정의 문의·앱 평가·오류 제보·개인정보 처리방침 + 상세 공유가 전부 안 뜸 | §2-D 참조 — 원인 4중첩 |
| 3-6 | 엣지 스와이프-백이 상세에만 적용. 라이선스는 back이 스크롤 내용물 안에 있고 safe area에 깔려 탭 불가 | 네비 상태·back 어포던스·제스처가 화면마다 산발(§1) |

## §1 현황 진단 — 임시방편 전수 목록 (사용자 요청 점검)

셸 계층이 없어서 생긴 국소 패치들. **스텝 2·3에서 전부 정본 계층으로 흡수·삭제한다.**

1. **네비 상태 4곳 분산** — `pagerState`(탭) + `detailKeys: Map<Tab,String?>`(상세) + `showLicenses: Boolean`(오버레이) + `onboarded: Boolean`(게이트). 라이선스·온보딩은 early-return이라 Scaffold·인셋·제스처 체계 **밖**에 있다.
2. **back 어포던스 화면별 자체 구현** — `DetailScreen` 상단 "← 뒤로" 텍스트(M9 스모크 결함의 응급 패치), `LicensesScreen`은 back이 **스크롤 콘텐츠 안**(스크롤하면 사라짐 + safe area 미처리로 노치에 깔림 — 3-6에서 실기기 확인).
3. **엣지 스와이프-백 ad hoc** — `AppRoot` 상세 분기에만 `pointerInput` 부착. 라이선스 등 다른 push엔 없음.
4. **push/pop 전환 애니 부재** — 즉시 recomposition 교체(UX-5).
5. **safe area 정본 부재** — commonMain에 `WindowInsets` 사용 0 + iOS 셸 `ContentView`가 `ignoresSafeArea(.all)`. Scaffold 안 화면은 우연히 커버되고 밖(온보딩·라이선스)은 구멍. 온보딩 배경 수동 도색(M9 저대비 수정)도 이 구멍의 증상이었다.
6. **키보드 처리 부재** — 3-1·3-2·3-3 전부. iOS 기본 뷰포트 시프트에 의존, `imePadding` 0, dismiss 0.
7. **iOS 플랫폼 액션 seam 4중 결함** (3-5·UX-6의 원인):
   - `share` no-op 스텁 — dead button(심사 리스크).
   - `openURL:`(deprecated 동기 API) 사용 — 실기기(iOS 26)에서 **https URL 포함 전멸** 관찰과 정합. 비동기 `openURL:options:completionHandler:`가 정본.
   - `encode`가 공백·개행만 치환 — **한글 제목("DevEtym 문의") → `NSURL.URLWithString` nil → 조용한 no-op** (코드상 확정 결함).
   - `requestReview`가 플레이스홀더 스토어 URL(`id0000000000`) — 미출시라 실 ID도 없음.
8. **설정 방침 URL stale** — `SettingsScreen`이 `https://devetym.app/privacy`(미보유 도메인 플레이스홀더). 정본은 `https://data-sy.github.io/devetym/privacy-policy`(WU-1 라이브, 스토어 라벨과 동일해야 함).
9. **설정 동의 토글 미영속** — `AppRoot`의 `rememberSaveable`(알려진 잔여, 이번 범위 판단은 §5).

## §2 설계

### A. 네비게이션 계층 정본화 (3-6·UX-5 해소)

**결정: 자체 경량 네비게이터** (navigation-compose 도입 대안 기각 — M6 §7-1 네이티브 링크 리스크 회피 전례 + 출시 직전 신규 의존 금지. 필요 기능이 "탭별 1~2뎁스 push"뿐이라 라이브러리 무게 불요).

- **Route 모델**: `sealed interface Route { data class Detail(val keyword: String); data object Licenses }` — 화면 추가 시 확장.
- **`TabNavState`**: 탭별 백스택 `List<Route>` + `push`/`pop`/`replaceTop`(possibleTypo용)/`popToRoot`(탭 재탭용) — **Compose 무의존 순수 로직, 단위 테스트 대상.** `detailKeys` map·`showLicenses` bool 대체. **라이선스는 Settings 탭 스택 소속으로 이동**(전역 오버레이 폐지 → 인셋·제스처·전환을 자동 상속).
- **`NavContainer`** (컴포저블 1개, 모든 push 화면 공통 제공):
  1. **전환 애니** — `AnimatedContent` 슬라이드: push=오른쪽에서 진입, pop=오른쪽으로 퇴장(+뒤 화면 30% 패럴랙스, iOS 관례 근사). 손가락 추종 인터랙티브 스와이프백은 **후속**(§5) — 전환 애니만으로 "휙" 해소.
  2. **엣지 스와이프-백** — 기존 `isEdgeSwipeBack` 순수 판정 재사용, 컨테이너로 이동해 **모든 push 화면에 일괄 적용**.
  3. **고정 top bar** — back 어포던스("← 뒤로")를 컨테이너 소유로 승격: 스크롤과 무관하게 상단 고정 + safe area 포함. `DetailScreen`·`LicensesScreen`의 인라인 back **삭제**.
- 온보딩은 네비 밖 게이트 유지(단, B의 인셋 래퍼 안으로).

### B. 인셋 정본화

루트에 **`AppSurface`** 래퍼 1개: 배경색 + `WindowInsets.safeDrawing` 처리를 단일 소유. 온보딩의 수동 `.background(colors.bg)` 패치 삭제, 라이선스 노치 깔림 해소. Scaffold 안/밖 화면이 같은 규율을 받는다.

### C. 키보드 규율 (3-1·3-2·3-3)

- **iOS 뷰포트 시프트 차단**: `ComposeUIViewController(configure = { onFocusBehavior = OnFocusBehavior.DoNothing })`.
- **인셋 기반 회피**: 검색 화면 루트 Column에 `Modifier.imePadding()` — 검색 필드가 하단 고정이고 중간이 `weight(1f)`라 **필드만 키보드 위로 오르고 헤더·최근 검색은 그대로**(3-1 요구 그대로, 레이아웃 변경 불요).
- **dismiss 3경로**:
  1. 콘텐츠 영역 **아래 방향 드래그** → `keyboardController.hide()` + `focusManager.clearFocus()` (3-2).
  2. **페이저 페이지 전환 감지**(`LaunchedEffect(pagerState.currentPage)`) → 포커스 해제 (3-3 — "꺼지는 게 맞다").
  3. 상세 진입 등 **네비 이동 시** 해제(2와 같은 지점에서 일괄).
- **Android**: manifest에 `windowSoftInputMode="adjustResize"` 명시(현재 미지정) — imePadding과 정합.

### D. iOS 플랫폼 액션 seam 정상화 (3-5·UX-6)

`IosAppActions` 전면 재작성:

| 항목 | 현재 | 설계 |
|---|---|---|
| URL 열기 | deprecated 동기 `openURL:` | 비동기 `openURL:options:completionHandler:` |
| mailto 인코딩 | 공백·개행만 치환(한글 → nil) | `NSCharacterSet.URLQueryAllowedCharacterSet` percent-encoding (또는 `NSURLComponents` 조립) |
| 공유 | no-op | `UIActivityViewController` 실구현 — 최상위 presented VC 탐색 후 present (iPad popover source 방어) |
| 앱 평가 | 죽은 스토어 URL | StoreKit `requestReview`(씬 기반) — 미출시 상태에서도 안전, 실 앱 ID 불요 |
| 방침 URL | 화면에 stale 리터럴 | `Constants.privacyPolicyUrl = "https://data-sy.github.io/devetym/privacy-policy"` 정본화(스토어 라벨과 단일 소스) |

**메일 폴백 (승인 시 채택)**: `openURL` completionHandler가 실패를 반환하면(메일 계정 미설정 기기 등) 지원 이메일 주소를 클립보드에 복사하고 네이티브 알럿(`UIAlertController` — 공유시트와 같은 present 경로 재사용)으로 "메일 앱을 열 수 없어 주소를 복사했어요"를 안내한다. 앱에 스낵바/토스트 체계가 없으므로 seam 내부에서 플랫폼 네이티브로 처리(commonMain 무변경). Android는 `resolveActivity` 부재 시 동일 정책.

**정직 경계**: 실기기 "전멸"의 1차 유력 원인은 deprecated API+인코딩이지만, 최종 확정은 스텝 1 후 실기기 재검증.

### E. 복사 범위 (UX-4)

`onCopy` 페이로드를 조립 함수로 승격(순수 함수 — 테스트 대상):

```
mutex

어원
<etymology>

왜 이 이름인가
<namingReason>
```

### F. 동의 토글 영속 (§1-9 — 승인 시 스텝 1 편입)

현재 `AppRoot`의 `rememberSaveable`(프로세스 생존만, 재기동 리셋). 기존 seam 패턴 그대로 확장:

- commonMain `ui/platform`에 **`ConsentStore`** 인터페이스(`AppearanceStore`와 동형: `StateFlow<Boolean>` + `set`) + no-op 스텁.
- actual: `PrefsConsentStore`(androidMain SharedPreferences)·`UserDefaultsConsentStore`(iosMain) — 기본값 `true`(현행 UI 기본 유지).
- 플랫폼 모듈 바인딩 + `AppDependencies`에 노출, `AppRoot`의 `rememberSaveable` 대체.
- ⚠️ 이 토글은 현재 표시용(수집 자체가 없음 — 방침 "현재 미수집" 정합, ROADMAP 2026-07-06 결정)이므로 영속만 하고 동작 배선은 범위 밖.

## §3 검증 오라클

- 각 스텝: **5축 green 회귀 0** + iPhone 시뮬 실주행(스크린샷 대조). 순수 로직(TabNavState·copy 페이로드·dismiss 판정)은 commonTest+네이티브 실행.
- **실기기 게이트로 넘어가는 것**: 메일 실전송·공유시트 실동작·StoreKit 평가 프롬프트·키보드 감각(시뮬은 하드웨어 키보드 토글 필요) — 스텝 1·3 완료 후 실기기 재설치 1회에 몰아서.

## §4 스텝 분할 (구현 순서)

| 스텝 | 내용 | 근거 |
|---|---|---|
| **1** | **플랫폼 액션 + 복사 + 동의 영속** — §2-D 전체(메일 폴백 포함) + §2-E + §2-F | 코드 국소·상호 독립·실기기 재검증 대상이라 먼저 몰아 왕복 최소화 |
| **2** | **네비게이션·인셋 계층** — §2-A(Route·TabNavState·NavContainer) + §2-B(AppSurface), §1 임시방편 1~5 흡수·삭제 | 구조의 기반. 화면들 back 삭제·라이선스 스택 편입 포함 |
| **3** | **키보드 규율** — §2-C 전체 | 스텝 2 뒤: dismiss 스와이프와 엣지 스와이프-백 제스처 정책을 한 체계에서 조정 |

각 스텝 = 독립 커밋(기존 관례). 스텝 간 사람 리뷰 지점 선택 가능.

## §5 Open Questions (후속 — 구현 블로커 아님)

1. **인터랙티브 스와이프백**(손가락 추종) — 스텝 2의 전환 애니로 충분한지 실기기 감각 확인 후 후속 결정.
2. **리퀴드 글라스 하단 바**(3-4) — 출시 전 백로그로만 등재(사용자 지시). CMP 네이티브 지원 없음 → 커스텀 blur/반투명(예: haze류 라이브러리 or 자체 그라디언트+반투명). 착수 여부·시점 미확정.

〔이력: 원 OQ였던 메일 폴백·동의 토글 영속은 2026-07-13 승인 시 채택 확정 → §2-D·§2-F로 편입.〕

## §6 구현 핸드오프 — 새 세션 착지 가이드

> 새 세션은 이 §6 + 위 설계만 읽으면 착수 가능해야 한다. 대화 맥락에만 있던 사실은 전부 여기 적었다.

### 환경·관례
- **브랜치**: `feat/m9-release-verification` (m8 위 스택) — 그대로 이어서 작업. **push 금지·브랜치 삭제 금지**(보존 규율), 커밋은 로컬만.
- **커밋 단위**: 스텝당 1커밋, 메시지 관례 예: `feat(m9-ux): …(스텝 1 — 셸 재설계 §2-D·E·F)`. 선례 = `3f1ce6a`·`35874bf`·`720f5d4`.
- **완료 오라클(각 스텝)**: ① **5축 green 회귀 0** ② iPhone 16 Pro 시뮬 실주행 스크린샷 대조 ③ ROADMAP 해당 항목 갱신.

```bash
# 5축
./gradlew :shared:testDebugUnitTest :shared:iosSimulatorArm64Test \
  :shared:linkDebugFrameworkIosSimulatorArm64 :androidApp:assembleDebug :androidApp:testDebugUnitTest
```
- **시뮬 재현 절차**: [스모크 대본](../release/m9-device-smoke-script.md) Tier 1 iOS 블록(빌드→boot→install→launch→screenshot). ⚠️ xcodebuild에 `set -o pipefail` + `-destination "id=…"` (stale 바이너리 함정 선례 — ROADMAP 2026-07-13 로어). 탭/스와이프 주입은 CGEvent 자작 도구 선례([시뮬 스모크 리포트](../release/m9-ios-sim-smoke-report.md) 참조).

### 파일 좌표 (전부 `shared/src/` 기준, 셸 2곳 제외)

| 대상 | 경로 |
|---|---|
| iOS seam(스텝 1 주 대상) | `iosMain/kotlin/com/robin/devetym/di/IosSeams.kt` |
| iOS 플랫폼 모듈 바인딩·doInitKoin facade | `iosMain/kotlin/com/robin/devetym/di/AppModule.kt` |
| Android seam·모듈 | `androidMain/kotlin/com/robin/devetym/di/AndroidSeams.kt` · `PlatformModule.android.kt` |
| seam 인터페이스·no-op 스텁 | `commonMain/kotlin/com/robin/devetym/ui/platform/AppDeps.kt` |
| DI 그래프·deps 홀더 | `commonMain/kotlin/com/robin/devetym/di/AppModule.kt` · `KoinAppDependencies.kt` |
| 상수(방침 URL 신설·supportEmail 기존) | `commonMain/kotlin/com/robin/devetym/Constants.kt` |
| 네비·페이저·동의 rememberSaveable | `commonMain/kotlin/com/robin/devetym/ui/AppRoot.kt` |
| 화면 4종 | `commonMain/kotlin/com/robin/devetym/ui/screens/{Detail,Search,Settings,Licenses}Screen.kt` |
| iOS 진입점(OnFocusBehavior — 스텝 3) | `iosMain/kotlin/com/robin/devetym/MainViewController.kt` |
| iOS 셸(ignoresSafeArea 현황) | `iosApp/iosApp/ContentView.swift` |
| Android manifest(windowSoftInputMode — 스텝 3) | `androidApp/src/main/AndroidManifest.xml` |

### 스텝 1 세부 체크리스트 (§2-D·E·F)
- [ ] `IosAppActions.open` → `openURL:options:completionHandler:` 교체(전 호출처 공통).
- [ ] `encode` → `NSCharacterSet.URLQueryAllowedCharacterSet` percent-encoding(한글 커버). **오라클: 한글 제목 mailto가 nil이 아님을 iosTest로 고정**(URL 조립을 순수 함수로 추출하면 네이티브 테스트 가능).
- [ ] `share` → `UIActivityViewController`(최상위 presented VC 탐색, iPad popover source 방어).
- [ ] `requestReview` → StoreKit `requestReview`(씬 기반).
- [ ] 메일 폴백: openURL 실패 콜백 → 주소 클립보드 복사 + `UIAlertController` 안내. Android는 `resolveActivity` 부재 시 동일 정책.
- [ ] `Constants.privacyPolicyUrl = "https://data-sy.github.io/devetym/privacy-policy"` 신설, `SettingsScreen`의 stale 리터럴 교체.
- [ ] 복사 페이로드 순수 함수(키워드+어원/왜 이 이름인가 라벨, §2-E 포맷) + commonTest. `DetailScreen` `onCopy` 호출부 교체.
- [ ] `ConsentStore` seam 신설·양 플랫폼 actual·모듈 바인딩·`AppRoot` 대체(§2-F). KoinGraph eager-touch 테스트에 추가(기존 그래프 완전성 테스트 패턴).
- [ ] Android 갭 동시 해소 확인: 클립보드 dead-seam(에뮬 스모크 갭 (a)) — 복사 페이로드 교체 시 Android도 동일 호출처라 자동 해소되는지 확인만.
- 시뮬 검증: 방침 URL 열림·복사 페이로드 `pbpaste`·동의 토글 재기동 유지. **실기기 게이트로 이월**: 메일 실전송·공유시트·StoreKit 프롬프트.

### 스텝 2·3 진입 시 주의
- 스텝 2: `TabNavState` 순수 로직부터(commonTest) → UI. 기존 `isEdgeSwipeBack` 판정·테스트 재사용. 탭 재탭 pop 관례 유지. possibleTypo 상세 교체는 `replaceTop`.
- 스텝 3: iOS `OnFocusBehavior.DoNothing` 전환 시 **검색 필드 가림 여부를 시뮬 소프트 키보드(⌘K 토글)로 실확인** — imePadding이 CMP iOS에서 IME 인셋을 실제 반영하는지가 이 스텝의 검증 요체(안 되면 폴백: 검색 필드만 키보드 추종하는 자체 오프셋).
