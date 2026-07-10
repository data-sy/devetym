# M8 슬라이스 (draft) — 통합·자산·마감 (최종 마일스톤)

> **성격: 비준 하네스 입력용 spec 슬라이스(`-draft.md`).** 단일 [`spec.md`](spec.md) 마감 Phase의 마일스톤 경계. 진행 상태 정본 [`../../ROADMAP.md`](../../ROADMAP.md). 자율 구현 전 적대 비준 대상.
>
> **⚠️ 검증 천장(최대·이 마일스톤의 지배 성격).** M8은 **플랫폼 런타임·시각 자산·실기기** 영역이라 4축 green이 보증하는 범위가 가장 좁다. green = **컴파일·조립·링크 + 순수/그래프 테스트 네이티브 실행 + 자산 패키징 포함**까지. **seam actual의 런타임 동작(mailto/공유/평가/클립보드/외관 영속)·아이콘 시각 충실도·iOS appiconset(Xcode)·접근성(TalkBack/VoiceOver)·Firebase·실기기 시각/상호작용은 이 마일스톤에서 검증되지 않는다** — 「코드 완료·실기기 검증 필요」로 라벨해 사람/실기기로 넘긴다. **거짓 green 금지**(코드 완료분과 실기기 필요분을 명시 분리).

- **마일스톤**: M8 · 통합·자산·마감
- **브랜치**: `feat/m8-integration-assets` (=`feat/m7-koin-wiring` 위에 **스택** 분기)
- **참조**: spec 마감 Phase, [M6 슬라이스](m6-compose-ui-draft.md)(seam 인터페이스·아이콘 상속), [M7 슬라이스](m7-koin-wiring-draft.md)(seam 스텁·DR5-2 VM leak·플랫폼 모듈), **디자인 자산 상속**: `~/dev-etymology/docs/design/icon/assets/v2/`(icon.svg·icon-dark.svg·launch-logo.svg)·`~/dev-etymology/DevEtym/DevEtym/Assets.xcassets/AppIcon.appiconset/`(icon.png·icon-dark.png)·`~/dev-etymology/DevEtym/DevEtym/Resources/Fonts/OFL-*.txt`(appiconset과 동일 앱 루트 `~/dev-etymology/DevEtym/DevEtym/` 아래). 메모리 [ios-design-assets-inheritable].

---

## 1. 목표

M7까지 앱은 실행 가능한 전체로 배선됐으나 **플랫폼 seam이 스텁**(no-op·in-memory)이고 **자산이 미비**(Android 런처 아이콘 부재·폰트 라이선스 미고지)다. M8은 (a) **seam actual**을 각 플랫폼에 구현해 스텁을 대체하고, (b) **Android 런처 아이콘**을 상속 SVG에서 생성해 패키징하며, (c) **OFL 폰트 라이선스 고지**를 앱에 싣고, (d) **온보딩 게이트를 영속화**한다. 앱이 **자산·플랫폼 통합까지 코드 완료**된다 — 단, 런타임 동작·시각 충실도·접근성은 실기기 검증이 필요하며 정직히 라벨한다(검증 천장).

## 2. 스코프

**IN (M8 — 컴파일·조립·링크·패키징으로 검증 가능):**
- **seam actual 구현**(androidMain/iosMain, M7 스텁 대체):
  - `AppActions`: Android=Intent(`ACTION_SENDTO` mailto·`ACTION_SEND` 공유·`ACTION_VIEW` url)·`ClipboardManager`·평가(Play 스토어 url 폴백); iOS=`UIApplication` openURL(mailto·url)·클립보드(`UIPasteboard`)·평가(App Store url)·공유(제약 — §7).
  - `AppearanceStore`: Android=`SharedPreferences`·iOS=`NSUserDefaults` **영속**(외관모드 0/1/2).
- **외관 배선**(`AppRoot`가 `appearance.mode`를 소비해 실제 테마 결정): 순수 commonMain Compose 변경. `assembleDebug`/link는 이 매핑 코드가 **컴파일·조립·링크됨**만 보증하고, set→emit·재구성 전파·single-instance 공유(테마가 실제로 전환되는지)는 **실기기 천장**이다(§3-6). 값이 저장만 되고 테마에 반영 안 되는 inert 방지는 설계 의도이며, 그 전파 정확성을 빌드-검증으로 자칭하지 않는다.
  - `DeviceInfo`: `appVersion`=플랫폼 버전(Android `PackageInfo`·iOS `NSBundle`); `instanceId`=**Firebase 미도입이라 null 유지**(M8 이월 — §7).
  - `DeviceIdProvider`: 영속 UUID(`SharedPreferences`/`NSUserDefaults` 저장·재사용).
- **플랫폼 모듈 바인딩 교체**: `androidPlatformModule`/`iosPlatformModule`이 스텁 대신 actual 바인딩.
- **온보딩 영속 seam**(`OnboardingStore`): 최초 1회 플래그를 영속 저장. `AppRoot`가 in-memory 대신 이를 소비.
- **Android 런처 아이콘**: 상속 `v2/icon.svg`를 `rsvg-convert`로 밀도별 PNG 래스터화 → `mipmap-*/` + adaptive icon(`ic_launcher.xml` foreground/background). `AndroidManifest`가 참조. **패키징은 `assembleDebug`로 검증**(시각 충실도는 천장).
- **OFL 라이선스 고지**: `~/dev-etymology/DevEtym/DevEtym/Resources/Fonts/OFL-*.txt` 3종을 앱 리소스(composeResources)로 싣고 설정→라이선스 화면이 표시(M6 `onOpenLicenses`가 openUrl 대신 화면 표시로 전환 or 유지 — §7).
- **순수/그래프 테스트**: seam actual은 대부분 플랫폼 API라 단위테스트 대상이 아니나, 플랫폼-독립 로직(UUID 형식·외관모드 클램프 등 순수부)이 있으면 네이티브 실측. 그래프 테스트는 실 플랫폼 모듈이 여전히 테스트-스텁으로 대체됨(M7 천장 계승).

**OUT / 검증 천장(실기기·사람·Xcode — 「코드 완료·실기기 검증 필요」 라벨):**
- **seam actual 런타임 동작**(mailto 앱 열림·공유 시트·평가 다이얼로그·클립보드·**외관 전파**): 코드 완료·컴파일 보증, **동작은 실기기**. 외관 매핑 코드(§3-6)는 코드 완료·`assembleDebug`/link 검증(=컴파일·조립·링크)이나, set→emit·재구성·single-instance 공유로 **테마가 실제 전환되는 전파**와 최종 픽셀 렌더 충실도는 실기기 천장이다(전파를 빌드-검증으로 자칭하지 않음).
- **아이콘 시각 충실도·iOS appiconset**: Android 아이콘 패키징은 검증하나 **렌더 모양은 실기기**; iOS appiconset은 **Xcode 빌드**(축 밖) — 상속 PNG 배치만.
- **접근성 감사**(TalkBack/VoiceOver 레이블)·**Dynamic Type**: 코드 레이블만, 감사는 실기기.
- **Firebase App Instance ID**: 의존성 미도입(네이티브 링크 리스크) — `instanceId=null` 유지, 실통합 별도.
- **실 셸 VM 수명주기(ViewModelStore)·DR5-2 실 창**(M7 이월): ViewModelStore 도입은 Compose lifecycle 결착이 커 **범위 밖**(§7 판정) — 필요 시 별도.
- **스플래시(런치스크린)**·**코드 서명·심사·스토어 메타데이터**: 실기기/스토어.
- **서버 `devetym-proxy`**: 별도 트랙.

## 3. 산출 명세

### 3-1. seam actual — `AppActions`

- **androidMain `AndroidAppActions(context)`**: `sendMail`=`Intent(ACTION_SENDTO, "mailto:")`+extra; `share`=`Intent(ACTION_SEND, type=text/plain)`; `openUrl`=`Intent(ACTION_VIEW, uri)`; `copyToClipboard`=`ClipboardManager`; `requestReview`=Play 스토어 url `openUrl` 폴백(In-App Review는 Activity·Play Core 필요 — §7). 모두 `context.startActivity`(FLAG_ACTIVITY_NEW_TASK — Application context).
- **iosMain `IosAppActions()`**: `sendMail`/`openUrl`=`UIApplication.sharedApplication.openURL`(mailto:·https:); `copyToClipboard`=`UIPasteboard.generalPasteboard.string = text`(⚠️ **쓰기 세터** — 게터 접근이 아니다. `generalPasteboard.string`을 *읽기*로 두면 무동작 no-op이 컴파일 green으로 통과하고 실기기 천장이 못 잡는다); `requestReview`=App Store url openURL(StoreKit `requestReview`는 scene 필요 — §7); `share`=제약(UIActivityViewController는 presenting VC 필요 — §7, 최소 no-op or url 폴백).

### 3-2. seam actual — `AppearanceStore`·`OnboardingStore`·`DeviceIdProvider`·`DeviceInfo`

- **`AppearanceStore`**: Android `SharedPreferences("devetym", MODE_PRIVATE)` key `appearance_mode`; iOS `NSUserDefaults.standardUserDefaults` key 동일. `MutableStateFlow`로 현재값 노출, `set`이 저장+방출. 기본 2(다크). ⚠️ **iOS 기본값 함정**: Android `getInt("appearance_mode", 2)`는 부재 시 안전히 2를 주지만, iOS `integerForKey(...)`는 기본 파라미터가 없어 **키 부재 시 조용히 0(시스템)을 반환**한다. 따라서 iOS actual은 `objectForKey("appearance_mode") == null`로 존재를 먼저 판정하고 **부재 시에만 2**를 쓴다(또는 앱 시작 시 `register(defaults: ["appearance_mode": 2])`로 기본 등록). '기본 2'가 Android·iOS 양쪽에서 동일하게 실현돼야 신규 설치 첫 실행의 조용한 발산(Android=다크/iOS=시스템)이 방지된다.
- **`OnboardingStore`**(신규 seam): `val completed: Boolean`·`fun complete()`. Android Prefs/iOS UserDefaults key `onboarding_done`. `AppRoot`가 M7 in-memory `onboarded` 대신 소비(§3-4). ⚠️ **실 바인딩 필수·그래프 마스킹**: 실 `androidPlatformModule`/`iosPlatformModule`에 `OnboardingStore` actual 바인딩을 반드시 추가한다. 그래프 테스트는 테스트-스텁 바인딩만 해석하고 실 플랫폼 모듈은 컴파일-only(M7 천장 계승)라, 실 바인딩 누락 시 첫 기동에서 Koin `NoDefinitionFound` 크래시가 4축 green을 조용히 통과한다. 온보딩은 첫 실행 임계 경로라 대표 항목이나 **동일 마스킹은 온보딩만이 아니라 M8이 손대는 전 actual 바인딩(신규 OnboardingStore + actions·appearance·device·deviceId 교체 5종)에 적용된다** — 어느 것이든 실 모듈에서 누락·타입 불일치가 있으면 같은 조용한 통과가 난다. 따라서 §5 스모크 체크리스트는 온보딩 단일 항목이 아니라 `KoinAppDependencies`가 eager-touch하는 전 seam을 대상으로 넓힌다.
- **`DeviceIdProvider`**: 영속 UUID — 저장된 값 있으면 재사용, 없으면 생성·저장. UUID 생성은 플랫폼(`java.util.UUID`/`NSUUID`).
- **`DeviceInfo.appVersion`**: Android `packageManager.getPackageInfo(...).versionName`; iOS `NSBundle.mainBundle.objectForInfoDictionaryKey("CFBundleShortVersionString")`. `instanceId`=null 유지.

### 3-3. Android 런처 아이콘

- 상속 `~/dev-etymology/docs/design/icon/assets/v2/icon.svg`를 `rsvg-convert`로 mdpi~xxxhdpi(48~192px) PNG 래스터화 → `androidApp/src/main/res/mipmap-*/ic_launcher.png`·`ic_launcher_round.png`. adaptive icon: `mipmap-anydpi-v26/ic_launcher.xml`(foreground=아이콘·background=`@color/ic_launcher_background` 상속 brand `#2E5D3A`). `AndroidManifest` `android:icon="@mipmap/ic_launcher"`. **패키징 검증=`assembleDebug`**(리소스 병합·빌드 성공). 시각은 천장.

### 3-4. 온보딩 영속 (`AppRoot`)

- `AppDependencies`에 `onboarding: OnboardingStore` 추가(또는 seam로). `AppRoot`가 `rememberSaveable { mutableStateOf(false) }` 대신 `onboarding.completed` 초기값 + `onComplete`가 `onboarding.complete()` 호출. 재시작 시 완료면 온보딩 스킵. 테스트 seam(in-memory)로 그래프 테스트 확장.

### 3-5. OFL 라이선스 고지

- `~/dev-etymology/DevEtym/DevEtym/Resources/Fonts/OFL-DMSans.txt`·`~/dev-etymology/DevEtym/DevEtym/Resources/Fonts/OFL-DMMono.txt`·`~/dev-etymology/DevEtym/DevEtym/Resources/Fonts/OFL-DMSerifDisplay.txt`(appiconset과 동일 앱 루트, 리포 루트 `~/dev-etymology/Resources/`가 아님)를 `composeResources/files/`로 이관. 설정 라이선스 항목이 이 텍스트를 화면에 표시(`onOpenLicenses`가 openUrl 대신 in-app 화면 — 최소 텍스트 스크롤). ⚠️ 라이선스 텍스트 로드는 `Res.readBytes`(M3 번들 로더와 동형).

### 3-6. 외관 배선 (`AppRoot` → `AppTheme`)

- 정본 `AppRoot.kt:55`는 현재 `val darkMode = true`로 테마를 하드코딩하고 주석에 '외관모드 실반영은 M8(appearance seam)'라 명시 이월돼 있다. M8은 이 소비처를 완결한다.
- `AppRoot`가 `appearance.mode`를 `collectAsState`로 구독해 `AppTheme(dark = ...)`로 매핑: **0=`isSystemInDarkTheme()`**, **1=false(라이트)**, **2=true(다크)**. 하드코딩 `darkMode = true`를 이 매핑으로 대체.
- 이로써 설정 세그먼트(`SettingsScreen.kt:35·63`은 이미 `appearance.mode`를 읽어 하이라이트를 그리고 `appearance::set`으로 저장)가 선택 시 **실제 앱 테마가 전환**된다 — 값만 영속되고 테마는 다크 고정되는 inert 상태를 제거.
- **검증**: `:androidApp:assembleDebug` + `:shared:linkDebugFrameworkIosSimulatorArm64`는 이 매핑 코드가 **컴파일·조립·링크됨**을 보증한다. set→emit·재구성 전파·single-instance 공유(테마 실제 전환)와 최종 화면 픽셀 렌더는 **실기기 천장**이다 — '배선 완결=assembleDebug 검증'으로 자칭하지 않는다(assembleDebug/link는 전파를 실행·관측하지 않음).

## 4. 설계 불변식

- **seam 경계 유지**(architecture §4.5): actual은 플랫폼 소스셋에만, commonMain은 인터페이스만. `Context`·`UIApplication`은 commonMain 미유입.
- **검증 천장 정직**(§0): 런타임·시각·접근성은 green 자칭 금지. 코드 완료분과 실기기 필요분 명시 분리.
- **영속 일관**: 외관·온보딩·deviceId는 재시작 보존(플랫폼 저장소).
- **자산 상속**: 아이콘·폰트·라이선스는 iOS repo 상속(재설계 아님).
- **브랜치 보존·push 금지·젠더중립 네이밍**.

## 5. 완료 조건 (DoD) — 천장 명시

- **컴파일·조립·링크 green(3축)**: `:shared:testDebugUnitTest` + `:androidApp:assembleDebug`(seam actual·**런처 아이콘 리소스 병합**·OFL 리소스 포함) + `:shared:linkDebugFrameworkIosSimulatorArm64`(iOS seam actual 링크). 신규 좌표(없음 목표 — 플랫폼 API만) 확인.
- **⊕ 네이티브/그래프 실행**: `:shared:iosSimulatorArm64Test` green(순수부 + 기존 결착 유지) + `androidUnitTest` 그래프 테스트(온보딩 seam 포함 해석).
- §6 테스트 통과.
- **명시적 비-보증**: seam 런타임 동작·아이콘 시각·iOS appiconset(Xcode)·접근성·Firebase·실기기는 이 DoD가 보증하지 않음 → 「코드 완료·실기기 검증 필요」 라벨. **이 목록을 최종 핸드오프에 명시**.
- **실기기 스모크 체크리스트(핸드오프 명항목)**: 「첫 기동 시 실 `androidPlatformModule`/`iosPlatformModule`이 `KoinAppDependencies`가 eager-touch하는 **전 seam**(actions·appearance·device·onboarding·deviceId)을 실제로 해석하는지」를 포함한다(M7 open q의 실-플랫폼 그래프 스모크 계열에 편입). 그래프 테스트는 테스트-스텁 모듈만 해석하므로 M8이 손대는 어느 actual 바인딩이든(신규 OnboardingStore + 교체 5종) 실 모듈에서 누락·타입 불일치가 있으면 첫 기동 `NoDefinitionFound` 크래시가 4축 green을 조용히 통과한다 → 실기기 첫 기동으로만 확인된다. 온보딩은 그중 첫 실행 임계 경로라 대표 항목이나 체크 범위는 온보딩 한 항목이 아니라 전 seam이다.

## 6. 테스트

- `test_koin_그래프_해석_온보딩포함` — 온보딩 seam 추가 후에도 그래프 해석 green(테스트 seam eager touch 유지).
- (순수부 있으면) `test_appearanceMode_클램프`·`test_deviceId_형식` 등 플랫폼-독립 로직 네이티브 실측. **없으면 생략**(seam actual은 플랫폼 API라 단위테스트 대상 아님을 정직히 — 천장 위장 금지).
- **기존 결착 유지**: M1~M7 테스트 전부 green(회귀 없음).

## 7. 열린 질문 (비준이 판정할 항목)

1. **iOS 공유(`share`)·In-App Review — presenting VC/scene 제약** — `UIActivityViewController`·StoreKit `requestReview`는 presenting context가 필요한데 `ComposeUIViewController`에서 접근이 제약적. 제안: `share`=최소 스텁 or url 폴백, `requestReview`=App Store url openURL. 실 구현은 실기기/별도. 비준이 이 제약 이월이 정직한지 판정.
2. **In-App Review(Android)** — Play Core 의존성 vs 스토어 url 폴백. 제안: url 폴백(의존성 회피). 비준 판정.
3. **아이콘 래스터화 — `rsvg-convert` 빌드 시 생성 vs 커밋된 PNG** — 제안: **커밋된 PNG**(빌드에 rsvg 의존 안 함, 재현성). 비준 판정.
4. **Firebase instanceId — null 유지(제안) vs 도입** — 제안: null 유지(네이티브 링크 리스크·범위). 비준이 이월이 정직한지 판정.
5. **VM 수명주기(ViewModelStore) — 범위 밖(제안) vs 포함** — M7 이월. 제안: 범위 밖(Compose lifecycle 결착 큼). 비준 판정.
6. **라이선스 표시 — in-app 텍스트 화면(제안) vs openUrl** — 제안: in-app(OFL 텍스트 번들·오프라인). 비준 판정.

## 8. 안전·규율

- **검증 천장 정직**(§0·§5): 런타임·시각·접근성 green 자칭 금지. **거짓 green 금지 — 최종 핸드오프에 실기기 필요분 명시.**
- **자산 상속**: iOS repo 아이콘/폰트/라이선스 계승(재설계 아님).
- 마일스톤 경계 **사람 게이트 완화**(메모리 `milestone-human-gate-relaxed`). 하네스는 push·머지·`-draft` 제거 안 함.
- **브랜치 보존·push 금지·젠더중립 네이밍·진행상태는 ROADMAP(디스크)**.

## Open Questions

> 비준 착수 전 — 비어 있으며 적대 비준이 채운다.

- [x] (비준 종료·RATIFIED → **구현 완료** 2026-07-05) §7 판정(전 제안 채택): **Q1** iOS share=최소 스텁·requestReview=App Store url openURL(presenting VC/scene 제약 실기기). **Q2** Android review=스토어 url 폴백(Play Core 회피). **Q3** 아이콘=**커밋된 PNG**(빌드 rsvg 의존 없음). **Q4** Firebase instanceId=null 유지(별도 트랙). **Q5** VM 수명주기(ViewModelStore)=범위 밖(M8 이월). **Q6** 라이선스=in-app 텍스트 화면(OFL 번들·오프라인). **DR-2 carry-forward(라이선스 네비 슬롯 부재)** 마감: `AppRoot`에 `showLicenses` 오버레이 상태 + `LicensesScreen`(`Res.readBytes` OFL 로드) 추가 — `onOpenLicenses`가 openUrl 대신 이 화면을 띄워 goal(c) 고지 실현(런타임 로드 정확성은 M3 로더와 동형 실기기 천장). 외관 배선(§3-6)·iOS 기본값 함정(§3-2 objectForKey null 체크)·UIPasteboard 세터 반영. verdict 로그: task `wh709vvng`.
- [ ] (실기기·사람) seam 런타임 동작·아이콘 시각·iOS appiconset(Xcode)·접근성 감사·실기기 시각/상호작용·코드 서명·심사.
- [ ] (별도 트랙) Firebase instanceId·VM 수명주기(ViewModelStore·DR5-2 실 창)·서버 proxy.

### 명시 이월 (carry-forward — 미탐색이지만 알려진 클래스)

- [ ] **DR-2 (라이선스 화면 네비 슬롯 부재 → goal(c) 고지 미실현 위험)** — 주장: 설정 '오픈소스 라이선스' 항목이 OFL 텍스트를 in-app 화면으로 표시(`Res.readBytes` 로드, 최소 텍스트 스크롤)해 goal(c) 폰트 라이선스 고지를 완성한다. 파괴 시나리오: 정본 AppRoot의 네비게이션 모델은 탭별 Detail push(`detailKeys[tab]`)와 온보딩 게이트뿐이며 Settings는 리프다 — 전체 화면 Licenses 뷰를 띄울 슬롯이 없다. `onOpenLicenses`는 현재 `deps.actions.openUrl('https://devetym.app/licenses')` 하드코딩. §3-5는 새 라이선스 화면을 AppRoot가 어떻게 호스팅하는지(네비 상태 확장)를 명세하지 않고, §7-6이 openUrl 폴백을 유효 옵션으로 남겨둔다. 결과: 구현자가 네비 확장을 회피해 openUrl 스텁을 유지하면 OFL .txt는 composeResources에 번들(assembleDebug 병합 검증됨)되지만 화면엔 절대 표시되지 않고, 존재하지 않을 수 있는 URL로 빠져 탭해도 무반응 — goal(c)의 '고지 표시'가 green인 채 미실현된다(번들만으로 OFL 최소 준수는 논쟁적이나 UX상 고지는 사라진다). 게다가 `Res.readBytes`는 suspend이고 런타임 경로/인코딩 정확성은 M3 로더와 동일한 실기기 천장(BundleDbSource 주석)이라, 화면을 만들어도 로드 실패는 빌드가 못 잡는다.
