# DevEtym Roadmap (Compose Multiplatform)

DevEtym(개발 어원 사전) CMP 앱의 중장기 작업 계획이자 **진행 상태 정본**. 세부 실행 지시는 [`docs/specs/spec.md`](docs/specs/spec.md)·[`docs/architecture.md`](docs/architecture.md)·각 [ADR](docs/adr/)를 참조.

구축 원칙: **위험이 낮은 코어부터, UI는 마지막.** 거의 전부 `commonMain`, 플랫폼 조각(엔진·드라이버·셸)만 각자.

**서버 캐시·딜리버리는 별도 트랙이 아니라 M1~M8에 빌트인이다.** 3계층 read-through(`로컬/번들 → 서버 D1 캐시 → Claude API`)·local-first pinning을 처음부터 각 마일스톤 범위에 녹인다 — 나중 마이그레이션·리팩토링·"출시 후" 없음. 확정 불변식 INV-1~13과 마일스톤별 상세 스펙은 [`docs/cache-delivery-milestones.md`](docs/cache-delivery-milestones.md), 서버 코드는 별도 repo **`devetym-proxy`**(read-through 캐시로 확장), 계약 결정은 [ADR-0006](docs/adr/0006-server-cache-boundary.md).

---

## Now — 진행 중

- **🎉 M0→M8 마일스톤 아크 전체 완료 (코드 레벨) — 2026-07-05.** M3~M8 전부 적대 비준 → eyes-open/RATIFIED → **4축 green(네이티브 실행 포함)** → 로컬 커밋 → **2026-07-10 push·m1~m8 스택 PR 병합**(main=M8, 아래 브랜치 전략). 앱이 데이터 계층(SQLDelight)→네트워킹(Ktor 3계층 read-through)→오케스트레이터→ViewModel→Compose UI(6화면+디자인시스템)→Koin 배선·앱 셸→플랫폼 seam actual·자산·마감까지 **코드 완결**. 브랜치 m0~m8 + main 전부 보존.
  - **⚠️ 남은 것은 전부 실기기/사람/스토어 게이트 (「코드 완료·실기기 검증 필요」)** → **M9 마일스톤으로 승격**(아래). 상세 체크리스트는 M9 DoD 참조.
  - ✅ **공개 전략 — 2026-07-10 사람 지시로 실행**: private repo `data-sy/devetym` 생성 → 전 브랜치(11개) push → `feat/m1`~`feat/m8` 스택 PR(#1~#8) merge-commit 순차 병합 → **main = M8**. m9는 진행 중이라 draft PR #9(미머지). 브랜치 11개 전부 보존(아래 규율). public 전환은 추후 사람 결정.
  - **⚠️ 정책(2026-07-05): 구현 전 사람 비준 게이트 완화** — M1·M2서 eyes-open 수용이 러버스탬프였음을 경험하고 사용자가 게이트를 제거. **적대 비준 수렴/ESCALATE → Claude가 잔여 residual을 eyes-open 수용 → 구현·4축 green까지 자율 관통**. 사람 리뷰는 **완성물 아침 리뷰**가 체크포인트(수용 residual 로그). 메모리 [milestone-human-gate-relaxed]. 나머지 안전선(push·브랜치보존·하네스 격리)은 유효.
  - **⚠️ 디자인 자산 상속**: M6 토큰·M8 아이콘/스플래시/폰트는 iOS repo(`~/dev-etymology/docs/design` + `Fonts` + `Features`)에 존재 — ROADMAP "작성 예정" 무효. 메모리 [ios-design-assets-inheritable].
- **M9 · 출시 준비·실기기 검증 (진행 중) — 브랜치 `feat/m9-release-verification`(m8 위 스택).** M0→M8이 *코드*를 닫았다면 M9는 그 코드가 **실기기에서 돌고 스토어에 오르는 것**을 닫는다. **앞 마일스톤과 종류가 다르다**: 완료 오라클이 4축 green이 아니라 **실기기·사람·스토어 심사**라서 CI로 못 닫고, 주체가 Claude 자율이 아니라 사람(실기기)·Apple/Google(심사)에 의존한다. 착지물(코드 아님·미푸시): Android Studio 에뮬레이터 런북(`android-studio-cmp-runbook.md`)·개인정보 처리방침 사이트(`site/`). **DoD = 아래 게이트 전부 통과 + 스토어 게시.**
  1. **실기기 실행 스모크**: 첫 기동 시 실 `androidPlatformModule`/`iosPlatformModule`이 `KoinAppDependencies` eager-touch 전 seam(actions·appearance·onboarding·device·deviceId)을 해석하는지(그래프 테스트는 테스트-스텁만 해석 — 실 바인딩 누락 시 첫 기동 `NoDefinitionFound`가 4축 green을 조용히 통과). M2 `NativeSqliteDriver` 실행 정확성(B1 잔여 절반)도 여기.
  2. **seam 런타임 동작**: mailto 앱 열림·공유 시트·앱평가·클립보드·외관모드 실전환(§3-6 배선은 assembleDebug/link 검증이나 set→emit→재구성 전파는 실기기).
  3. **시각/자산**: Android 런처 아이콘 렌더 모양·iOS appiconset(**Xcode 빌드** — 축 밖)·스플래시·라이선스 화면 실렌더.
  4. **접근성 감사**(TalkBack/VoiceOver·Dynamic Type)·**코드 서명·심사·스토어 메타데이터**.
  5. **별도 트랙**: Firebase App Instance ID(instanceId=null 유지)·VM 수명주기(ViewModelStore·M7 DR5-2 실 창)·서버 `devetym-proxy`(TS/Worker read-through 캐시).
  - ⚠️ 스토어 게시·push는 외부 대면이라 **사람 지시 대기**(자율 금지) — 브랜치·공개 전략 및 [milestone-human-gate-relaxed] 안전선과 정합.
  - 📄 스펙: [M9 슬라이스](docs/specs/m9-release-verification-draft.md). **태그 규율**로 노동 분담 명시 — `[AI]`(자동 실측·CI green)·`[AI→사람]`(AI 지그→사람 컨펌만)·`[사람]`(실기기·서명·심사, 환원 불가). 사람 게이트를 최대한 AI 테스트로 끌어내림(실 Koin 그래프 완전성·B1 네이티브 DB 실행 회수).
  - 📋 **출시 준비 마스터 체크리스트 = [`docs/release/LAUNCH-CHECKLIST.md`](docs/release/LAUNCH-CHECKLIST.md)** — 6개 카테고리 × 실제 상태 × 소유자 태그, 블로커 목록. 이 ROADMAP을 상태 정본으로 참조하는 단방향 뷰(충돌 시 ROADMAP 우선).
  - ✅ **`[AI]` 트랙 완료 (2026-07-05, 자율주행·미푸시)**: 신규 좌표 2건 세움 — Robolectric(androidUnitTest) + `iosTest` 소스셋(⚠️ `:shared:iosSimulatorArm64Test` 축 자체는 M2부터 존재; iosTest는 iosMain-참조 test 착지용). 끌어내린 것: **§3-1 실 `androidPlatformModule` 그래프 완전성**(`RealAndroidGraphTest` — 6 바인딩 전부 실 해석, **DB 하중 바인딩 실 AndroidSqliteDriver 왕복까지 강제 — 강등 없음, DR-1 Android 절반 폐쇄**)·**§3-2 `NativeSqliteDriver` 네이티브 왕복**(`NativeDbRoundtripTest` 4건 — upsert·정렬·LIMIT·nullable, B1 잔여 절반 폐쇄)·**§3-3 seam 로직**(Android Intent 구성·클립보드 Robolectric + `resolveDarkMode` 순수함수 추출 + **iOS 키부재 기본다크 `mode==2`** 네이티브)·**§3-4 라이선스 로드**(네이티브 Res.readBytes)·**§3-5 접근성**(WCAG 대비비 36쌍 **전 AA 통과** + contentDescription 커버리지=그래픽위젯 0, 텍스트기반 UI). **4축 green 회귀 0**: `:shared:testDebugUnitTest`(97→**106**) · `:shared:iosSimulatorArm64Test`(83→**93**) · `:shared:linkDebugFrameworkIosSimulatorArm64` · `:androidApp:assembleDebug`. `[AI→사람]` 지그 5종 산출: [`docs/release/`](docs/release/) (아이콘 렌더 시트·접근성 감사 대본·스토어 메타/개인정보 라벨 초안·서명·업로드 가이드·실기기 스모크 대본).
  - ✅ **iOS 시뮬레이터 첫 기동 실증 + 결함 2건 수정 (2026-07-05)**: 실기기 없이 시뮬로 게이트 재구성([스모크 대본](docs/release/m9-device-smoke-script.md) 티어형). 실 앱이 iPhone 16 시뮬서 크래시 없이 부팅 → **실 `iosPlatformModule` 그래프 해석·온보딩 렌더 확인**(§3-1 iOS 잔여의 첫 기동 부분 폐쇄). **시뮬이 4축·[AI] 테스트가 못 잡은 실 결함 2건 포착·수정**: (1) **iOS 앱 링크 불가** — SQLiter cinterop 래퍼 sqlite3 미링크 undefined, `project.yml`/`pbxproj`에 `-lsqlite3`(M0~M8이 Xcode 앱 빌드 미실행이라 미발견) (2) **온보딩 저대비** — Scaffold 이전 early-return이라 테마 배경 미도색(다크 기본에 흰 배경+밝은 텍스트), `OnboardingScreen`에 `.background(colors.bg)`.
  - ✅ **Tier 1 iOS 시뮬 자율 확인 (2026-07-05, 상태 주입·탭 불요)**: `simctl defaults`/컨테이너 `sqlite3` 주입으로 — 온보딩 영속 게이트(스킵)·메인 앱 다크 렌더(4탭 네비)·**외관 3모드 렌더**(mode=1 라이트/2 다크, `resolveDarkMode` 시각 확인)·**실 디스크 SQLite 반응형 쿼리→UI**(최근검색 FlowChip searchedAt DESC, B1 디스크 잔여 폐쇄). 검색 타이핑·탭 네비 등 입력 주입분은 라이브 시뮬 탭/idb 잔여.
  - ✅ **Tier 1 Android 에뮬 스모크 완주 + 첫 기동 크래시 1건 수정 (2026-07-05, adb 탭·타이핑 자율 주행)**: `system-images;android-36;google_apis;arm64-v8a` AVD 셋업 → 부팅 → `installDebug` → adb `input tap/text`로 전 플로우 주행(스크린샷 대조). **🐛 4축 green·Robolectric이 못 잡은 실 첫 기동 크래시 포착·수정**(iOS `-lsqlite3` 링크 버그의 Android 판): `AndroidManifest`가 `.DevEtymApp`/`.MainActivity`(namespace `com.robin.devetym`로 해석)를 가리키나 실 클래스는 `com.robin.devetym.android` 패키지 → 기동 즉시 `ClassNotFoundException: com.robin.devetym.DevEtymApp`로 프로세스 즉사. **`assembleDebug`·Robolectric은 그래프 모듈(`di`)만 검증해 셸 배선 미검출** — M0~M8이 앱을 실기기/에뮬에 한 번도 안 띄워 미발견. → manifest를 `.android.DevEtymApp`/`.android.MainActivity`로 수정(2줄). **주행 확인**: 온보딩 2단계(사전소개+데이터동의) 다크 렌더·영속(재기동 스킵) · 검색 번들히트(`mutex`)·alias(`Arne Andersson tree`→`aa-tree`)·**미스→AI 실 프록시 생성**(`quicksort`, `devetym-proxy` 왕복 — 초회 에뮬 가상 DNS 플레이크 후 wifi 재기동 성공) · 북마크 토글→탭 즉시반영→**재기동 유지**(실 디스크 AndroidSqliteDriver) · 히스토리 누적·개별삭제·영속 · **seam actual**: 메일 `ACTION_SENDTO`→Gmail 실 열림·공유 `ACTION_SEND`→시스템 chooser(포맷 내용)·평가 `ACTION_VIEW`→Play URL(에뮬은 Chrome 폴백) · **외관 3모드 실전환**(다크/라이트/시스템, 시스템은 `cmd uimode night`로 OS 추종 실증) · 라이선스 OFL 실스크롤 · 런처 adaptive 아이콘(`#2E5D3A` 배경, [아이콘 시트](docs/release/m9-icon-render-sheet.html) 대조 일치). **⚠️ 갭 2건(정직 기록, 미수정)**: (a) **클립보드 seam은 dead code** — `AndroidSeams.copyToClipboard`가 구현·유닛테스트(AndroidSeamLogicTest)되나 UI 호출처 전무(어떤 버튼도 미호출) → 에뮬 한계 아닌 앱 미배선(백로그). (b) **Android 전용 스플래시 미배선** — `androidApp`에 splash theme/`themes.xml` 없음, 기본 런처(백로그).
  - 🧹 **검증 환경 산출물(디스크, git 밖) — 나중에 한꺼번에 삭제**: Android 에뮬 이미지·AVD(~7G)·iOS 빌드 dd·gradle 캐시 등 ≈8.2G는 M9 진행 중이라 **전부 보존**. 삭제 체크리스트·명령 = [검증 환경 teardown 원장](docs/release/m9-verification-teardown-ledger.md)(M9 스모크 완전 종료 시 실행 — 고아 쓰레기 방지).
  - ⏳ **남은 것 = 티어형**(자율 금지): **[시뮬/에뮬]** — iOS 시뮬 입력 주입분(라이브 탭/idb)만 잔여(Android 에뮬은 위에서 완주). **[실기기]** — 하드웨어 감각만(실 메일 전송·앱간 클립보드·실 DPI·햅틱·TalkBack/VoiceOver 실제스처). **[외부]** — 코드 서명·심사·게시(지시 대기). ✅ **정합 소견 해소(2026-07-06)**: 방침↔구현 불일치(Firebase 수집 전제 vs `instanceId()=null`)를 "현재 미수집(애널리틱스 없음·Firebase 나중)" 방향으로 정합 — 사용자 결정. [방침](site/privacy-policy.md)·[스토어 라벨](docs/release/m9-store-metadata-draft.md) §3~4 갱신, 이용약관·랜딩·핫픽스 런북 신규(체크리스트 참조). 남은 것: GitHub Pages 배포(방침 URL)·법무 검토·스크린샷([캡처 핸드오프](docs/release/m9-screenshot-capture-handoff.md)).
  - **[외부][사람] Android 첫 배포 (스토어 게이트)** — Play Console·AAB·keystore. 새로 배우는 영역(iOS 배포는 기존 경험 자산). ⚠️ **개인 개발자 계정(2023-11 이후 생성)은 프로덕션 전 폐쇄 테스트 필수** — 최소 20명 테스터 × 14일 연속 후 프로덕션 신청 가능(조직·구계정 예외 가능, 콘솔 확인). iOS는 이 코호트 게이트가 없어 심사 직행 → **출시 순서에 영향**. CI(GitHub Actions) 양쪽 빌드 자동화 검토. (구 백로그 #2 — 이관도 출시후도 아닌 **출시 그 자체**라 M9 [외부] 귀속.)

- **이관·자기완결화 트랙 · dev-etymology → devetym (진행 중 — 2026-07-10 착수, `feat/m9-release-verification` 위 작업).** 순수 [AI] 이관분(pages.yml·Scripts·docs/db-expand)은 **`315ea55` 커밋**, 잔여 WU-1~7 미실행. devetym이 병렬 원본 repo `~/dev-etymology`(원본 네이티브 iOS 앱 + 문서·스크립트·CI) **의존 없이 자기완결**이 되게 이관하고, 스윕 전수 완료 시 dev-etymology를 **폐기**한다. **이관 = 내용 이관 + 설계 이관**(거기서 쓰던 방식을 여기서도 쓰게). 소유 태그 규율은 M9와 동일(`[AI]`·`[AI→사람]`·`[사람→AI]`·`[사람]`). ⚠️ **specs·ADR·데이터/거버넌스면 이관은 승인 게이트**(자동수정 금지 — 발견+제안+승인). **작업단위(WU) 계획·결정 원장 정본 = [`docs/handoff/26-07-10-selfcontained-migration-plan.md`](docs/handoff/26-07-10-selfcontained-migration-plan.md)** — 독립 세션 단위로 끊은 WU-1~12 + 확정 결정(D1~D6: 크래시 SDK=Sentry KMP·ai-quality→ADR-0007 승인 등). 세션 워킹 체크리스트 = [`26-07-10-to-do-list.md`](26-07-10-to-do-list.md)(폐기가능).
  - **[인프라][AI] GitHub Pages 배포 배선** — `~/dev-etymology/.github/workflows/pages.yml`(site/ 전용·**default-deny** 발행 모델: site/ 밖은 절대 미발행) 이관 → devetym `site/`(방침·약관) 실제 배포. **M9 「GitHub Pages 배포(방침 URL)」 blocker를 닫는다**(원격 repo 생김으로 이제 가능). 방침 URL 확정 시 [스토어 라벨](docs/release/m9-store-metadata-draft.md) 반영.
  - **[이관][AI] db-expand 파이프라인** — `docs/db-expand/`(README·spec·runbook-manual-round·rounds·archive) + `Scripts/{db-expand, generate_db.py}` 이관. 번들 DB 생성·검증 파이프라인. claude.ai 정액 수동 경로 유지(API 종량 회피). 구 백로그 #4.
  - **[이관][AI] prompt-probe 파이프라인** — `Scripts/prompt-probe/`(프롬프트 실험 하네스·keywords·metrics·results). AI 품질 회귀 측정 도구.
  - **[이관][사람→AI] AI 품질 문서** — `docs/ai-quality/`(시스템 프롬프트 원문·도구 스키마·probe 분석·prompt-review-brief) → devetym 정본, ADR 흡수(현재 iOS 검증본을 `commonMain`에 계승). ⚠️ **거버넌스면 → 승인 게이트**(ADR 신설·specs 변경은 사람 승인 후 AI 실행). 구 백로그 #3.
  - **[설계이관][사람→AI] 크래시 리포팅·애널리틱스** — iOS 원본은 **Firebase Analytics** 배선(`GoogleService-Info.plist`·`AnalyticsService`·`DeviceIdentifier`). devetym은 현재 `instanceId()=null`(미수집 정합). 크래시 리포팅(Crashlytics/Sentry)은 **양쪽 전무** → 도입은 사람 결정(어느 SDK·수집 범위) 후 AI 배선, 방침 §4(현 "미수집") 갱신 동반. 블로커 #7([LAUNCH-CHECKLIST](docs/release/LAUNCH-CHECKLIST.md) §4·§6) 닫는 유일 항목·iOS/Android 출시 순서 유연성 전제. 구 백로그 #1.
  - **[대조][AI] launch-prep 잔여** — `docs/launch-prep/`(appstore-metadata 초안군·launch-consult-prompt·e2e-checklist.md)가 devetym `docs/release/m9-store-metadata-draft.md`로 이미 승계됐는지 대조, **미승계분만** 이관(중복 방지).
  - **[스윕][사람→AI] DevEtym/ 네이티브 iOS 전수** — `~/dev-etymology/DevEtym/`(Xcode) Features·Services·Utils·Assets.xcassets·Resources 중 KMP `iosApp`에 **아직 안 넘어온 로직/자산** 확인. `.claude/`(agents·commands)·`LICENSE`·`README`도 대조. 산출물 = 파일별 이관/폐기 결정 원장. 구 백로그 #12. 〔선례: 아이콘 파생 SVG 4종 → `docs/design/icon/` 이관 완료(2026-07-05)〕
  - **[폐기][사람] dev-etymology 폐기 결정** — 위 스윕 전수 완료 후 폐기. ⚠️ **devetym 브랜치 보존 규율과 별개**(원본 repo 폐기는 사람 최종 확인). 구 백로그 #12.

- **✅ 코드 갭 수정 트랙 완료 · M9 스모크 발견 (2026-07-10, 자율주행·커밋).** 이관 아님, devetym **내부 결함**. 순수 `[AI]` WU-8·9·10 3건 모두 실행·green·커밋([계획 원장](docs/handoff/26-07-10-selfcontained-migration-plan.md)). **5축 green 회귀 0**: `:shared:testDebugUnitTest` · `:shared:iosSimulatorArm64Test` · `:shared:linkDebugFrameworkIosSimulatorArm64` · `:androidApp:assembleDebug` + **신규 `:androidApp:testDebugUnitTest`**(WU-10 가드 축).
  - ✅ **[UX][AI] WU-8 클립보드 복사 액션 UI 배선** — `copyToClipboard` seam이 양 플랫폼 구현·유닛테스트(AndroidSeamLogicTest)됐으나 **호출 UI 버튼 없음(dead code)**이던 것을 마감. `DetailScreen` 어원 블록에 "어원 복사" 어포던스 추가(`onCopy` 콜백 → `AppRoot`서 `deps.actions::copyToClipboard` 배선). 구 백로그 #9.
  - ✅ **[Android][AI] WU-9 스플래시 화면 배선** — `androidx.core:core-splashscreen`(1.0.1, minSdk 26 백포트) 도입 + `themes.xml`(`Theme.DevEtym.Starting`, brand 배경 `#2E5D3A` + 런처 foreground 아이콘 — **iOS `UILaunchScreen`(Theme/brand + LaunchLogo) 정합**) + manifest 테마 + `MainActivity.installSplashScreen()`. 구 백로그 #10.
  - ✅ **[CI/Test][AI] WU-10 셸 배선 회귀 가드** — **manifest-vs-소스 정합 체크** 채택(경량·정확). `ShellWiringManifestTest`(androidApp 순수 JVM 유닛테스트): 소스 매니페스트 `android:name`(application/activity)을 namespace로 FQCN 해석 → `Class.forName` + 상위형 + 실 셸 클래스 크로스체크. **가드 실증**: `.DevEtymApp`(과거 버그) 재주입 시 `ClassNotFoundException`로 FAILED 확인 후 복구. 4축 green이 조용히 통과하던 첫 기동 크래시(iOS `-lsqlite3` 링크 갭과 동류)에 실행 오라클 부여. 구 백로그 #11.

---

## 브랜치·공개 전략 (defer + stacked) — 2026-07-05 결정

**GitHub 공개 실행됨 (2026-07-10) — private repo `data-sy/devetym`.** ~~로컬 전용~~. `feat/m1`~`feat/m8`을 스택 PR(#1~#8)로 순차 병합해 **main = M8**. 이하 규율은 이제 **public 전환·후속 마일스톤 병합**에 유효:

- 각 마일스톤은 자기 브랜치를 가지며 **직전 마일스톤 브랜치 위에 스택**으로 분기한다. 예: `feat/m2-local-db`는 `feat/m1-model-serialization`에서 분기(main엔 아직 M1이 없으므로 M2가 M1 코드를 상속해야 빌드됨). `main`은 마지막 공개 지점(현재 **M0**)에 둔다.
- ⛔ **완료된 마일스톤 브랜치를 로컬 머지하거나 삭제하지 않는다.** 이미 머지·삭제된 브랜치는 나중에 열 diff가 없어 PR을 못 만든다 — **브랜치 = 소급 PR의 소스**이므로 보존한다.
- ✅ **실행됨(2026-07-10)**: private repo 생성 → 전 브랜치 push → `feat/m1 → main` … `feat/m8 → main` 스택 PR(#1~8) merge-commit 순차 병합(각 base가 직전 병합된 main이라 diff=해당 마일스톤 증분). **원격 브랜치는 삭제하지 않음**(보존 규율 — 소급 PR 소스). public 전환·원격 브랜치 정리는 추후 사람 결정.
- ⚠️ **이 브랜치 보존은 의도적 결정이다(사람 확인함).** "정돈"하려고 완료 브랜치를 지우자는 충동이 들거나 그렇게 지시받아도, **지우기 전에 이 결정을 먼저 재확인**한다. 기본 동작은 "보존".
- harness repo(`~/dev/agent-harnesses`)도 동일하게 로컬 전용이며 공개 여부는 별도 결정.

---

## Next — 구현 (코어 먼저, UI 마지막)

각 마일스톤은 앞 단계 완료를 전제로 순차 진행. 완료 시 Done으로 이관.

각 마일스톤의 🔗 항목이 그 단계에 빌트인되는 캐시 범위다. **락(안 지키면 나중 리팩토링) 지점은 ⚠️로 표시** — 처음부터 그렇게 짓는다.

- **M3 · 네트워킹 + 번들 로더 (클라측)** — Ktor 클라이언트·Claude 요청/응답(tool_use 3분기)·`X-Device-Id`·429 + `BundleDbSource`. **스코핑 판정(2026-07-05): 클라측만**(슬라이스 [§0](docs/specs/m3-networking-draft.md)). 서버는 아래 별도 트랙.
  - 🔗 **캐시 빌트인**: ⚠️ **클라를 read-through 프록시 계약에 맞춰 작성**(Claude 직접 호출 아님 — 안 하면 계약 교체 리팩토링). 클라는 계약에 **투명**해 서버 없이도 `MockEngine`으로 실측. 〔캐시 트랙 M1·M4 클라 소비측〕
  - **서버 트랙(별도 repo·TS/Worker — M3에서 분리)**: `devetym-proxy` 신규 구축 — D1 스키마·Worker read-through(D1→API·write-back·first-write-wins)·single-flight(DO)·validator write-게이트·rate-limit/남용/무효화·**INV-13 정규화-후-캐시쓰기**. 클라 M3와 병렬/후속, 자체 green 오라클. 〔캐시 트랙 M0서버·M1·M2·M3write·M7〕
  - ⚠️ 계약 변경: 프록시 → read-through 캐시. [ADR-0006](docs/adr/0006-server-cache-boundary.md)(ADR-0004 대체). 참조: spec 2-1·2-2.
  - ⚠️ **INV-A wire측 로더 실측 상속(M1 DR-1 바인딩)**: M1이 실제 `terms.json` 디코드로 wire 키 계약을 fixture 실측했으나(슬라이스 §6), **번들 로더(`BundleDbSource`)의 실제 로드 경로**가 aliases 내용을 보존하는지는 **M3 DoD에서 회귀 가드로 테스트**한다 — 실제 배포 `terms.json`을 로더로 로드해 알려진 term의 aliases *내용*을 단언(성공 디코드는 무효 오라클). 근거: M1 슬라이스 §7-4·§8, DR-1 eyes-open 수용.
  - ⚠️ **서버 read-through category 소유(M1 DR-2 바인딩) — 서버 트랙으로 이관**: 클라 M3 스코핑 분리(2026-07-05)로 이 항목은 **서버 트랙 DoD**로 옮겨졌다. 서버가 정규화 이전 원응답을 캐시-히트로 되돌려 클라 정규화를 우회하지 않도록 **정규화-후-캐시쓰기 순서**를 고정한다(집합 밖 category clamp 후 write-back). 클라측 상보 방어(수신 category 정규화)는 **M4**. 정본 불변식: [cache-delivery-milestones](docs/cache-delivery-milestones.md) **INV-13**. 근거: M1 슬라이스 §7-2, DR-2 eyes-open 수용, M3 슬라이스 §4·§7-6.
- **M4 · Repository 오케스트레이터** — `fetch` 3단 흐름·upsert·북마크·히스토리·Analytics. Fake 협력자 테스트.
  - 🔗 **캐시 빌트인**: ⚠️ **3계층 read-through를 처음부터**(로컬/번들 → 네트워크 → 서버 D1 캐시 → API, 2계층으로 짓고 확장 금지). **local-first pinning + 명시적 새로고침** 경로 내장. 〔INV-1·INV-2·INV-6·캐시 트랙 M1소비·M4행위〕 참조: spec 2-3·2-4.
  - ⚠️ **upsert 보존 목록 상속(M2 DR-M2-2)**: `INSERT OR REPLACE`=DELETE+INSERT라 refresh 시 `createdAt`을 `isBookmarked`/`source`와 **함께 보존**해야 `bookmarked`(`createdAt DESC`)가 새로고침마다 조용히 재정렬되지 않는다. pinned(`seenAt`) 로우는 `fetch`가 덮지 않고 `refresh`만 갱신. `toEntity`는 4 DB전용 필드가 필수인자라 read-modify-write 재주입 누락이 **컴파일 에러**(M2가 강제). 근거: M2 슬라이스 §3-2·§3-4·Open Questions.
  - ⚠️ **schemaVersion Int 범위 보장 상속(M2 DR-M2-3)**: `Term.toDto()`의 `Long?→Int?`는 Int 범위에서만 무손실. 서버 배달 경로가 `Int.MAX_VALUE` 초과 `schemaVersion`을 기록하면 silent 절단 → M4/캐시 트랙이 Int 범위 보장(또는 `toDto` 범위 가드). 근거: M2 슬라이스 §4(INV-9)·Open Questions.
- **M5 · ViewModel + StateFlow** — 화면 상태를 sealed로 노출.
  - 🔗 pinned/refresh 상태 노출. 참조: architecture §4.5.
- **M6 · Compose UI** — 검색/상세/북마크/히스토리/온보딩/설정. **반응형 `Flow`로 갱신(수동 재조회 없음, [ADR-0002](docs/adr/0002-code-idiom-principle.md))**.
  - 🔗 **명시적 "새로고침" 어포던스**(INV-6, 본 항목 불변 + 사용자 트리거 갱신)·pinned 표시. 참조: spec 3-x.
  - 선행: **디자인 토큰 확정**(`docs/design/`, 작성 예정) — 색·타이포 값. iOS dark-first·DM 서체를 출발점으로.
- **M7 · 배선·셸** — Koin 조립 마무리, 셸별 권한·진입점.
  - 🔗 서버 배포 배선(`devetym-proxy` wrangler). 참조: architecture §3·§4.7.
- **M8 · 통합·마무리** — 오류 처리 통합·접근성·번들 DB 650(iOS 자산 재사용)·앱 아이콘(Android adaptive + iOS)·스플래시.
  - ℹ️ **번들은 이미 완성돼 있다**(저술 불필요, *재사용*만): `~/dev-etymology/DevEtym/DevEtym/Resources/terms.json` — **650개**, M1 `TermEntry`와 스키마 정합(6필드 + 버전 필드는 없음 → INV-B null default 경로). 카테고리 6집합 분포 균등. 배치는 **M1 구현 착수 시** `commonMain/composeResources`(spec 1-5)로.
  - 🔗 **캐시 빌트인**: **seed 승격 잡**(critic 배치, D1 hot 항목 → 번들 승격 플라이휠)·**콘텐츠 팩 백그라운드 동기화**(버전드 팩·delta/cursor 증분·로컬 병합) 메커니즘 내장 → **출시 1일차부터 가동**(데이터는 릴리즈마다 축적, 리팩토링 아님). 〔INV-11·INV-12·캐시 트랙 M5·M6·M3critic〕 참조: spec 4-x.
  - ⚠️ **`NativeSqliteDriver` 실행 정확성 실측 상속(M2 DR-1 잔여 절반, B1)**: M2 §6-B DB 왕복은 JVM(JDBC) 전용이라 네이티브 DB 실행(스키마 create·`INSERT OR REPLACE`·`ORDER BY`/`LIMIT`·nullable INTEGER 바인드·TEXT 정렬 로케일)은 무측정. B2(네이티브 드라이버 크로스타깃 테스트) 미채택 → **통합/실기기 실행에서 실측**한다. 근거: M2 슬라이스 §5·Open Questions(사람 게이트 추적).

---

## Later — 출시 후 백로그

출시 게이트가 아니라 **출시 이후** 착수하는 항목(미착수/검토). 진행 중 트랙(이관·자기완결화·코드 갭)과 M9 외부 게이트는 위 **Now** 참조. 정리: 구 #1·#3·#4·#12 → 이관 트랙 · #2 → M9 [외부] · #9·#10·#11 → 코드 갭 트랙.

- **[Data] 번들 DB 추가 확장** — 검색 빈도 데이터를 우선순위 입력으로(승격 잡의 hot 선정 입력, M8 플라이휠과 연동).
- **[Arch] AI 스트리밍 도입 검토** — 현재 단발 응답. 토큰 스트리밍(`Flow<String>`)은 이후 선택지(architecture §4.3).
- **[Arch] 프롬프트 서버 이전 검토** — 현재 클라이언트(`commonMain`) 소유. 프롬프트 핫픽스 필요성 커지면 재검토([ADR-0006](docs/adr/0006-server-cache-boundary.md) 유보 항목).
- **[UI] 디자인 후속** — 다크/라이트 폴리시·대비·플랫폼별 미세 조정.
- (아이디어 추가 시 여기로)

---

## Done — 완료

- **M8 · 통합·자산·마감 (최종 *구현* 마일스톤 — 이후 M9는 검증·출시)** — 2026-07-05 (브랜치 `feat/m8-integration-assets`, 로컬 커밋 `ed26f51`·미푸시). M7 스텁을 **seam actual**로 대체: androidMain(`AndroidAppActions`·`PrefsAppearanceStore`·`PrefsOnboardingStore`·`PrefsDeviceIdProvider`·`AndroidDeviceInfo`)·iosMain(`IosAppActions`·`UserDefaults*` 3종·`IosDeviceInfo`), 플랫폼 모듈 5종 바인딩 교체. **외관 배선**(`AppRoot`가 `appearance.mode`→`AppTheme(dark)` 소비, `darkMode=true` inert 제거)·**온보딩 영속**(`OnboardingStore` seam)·**in-app OFL 라이선스**(`LicensesScreen`+`Res.readBytes`, `showLicenses` 오버레이)·**Android 런처 아이콘**(`v2/icon.svg`→rsvg 15 PNG+adaptive+colors+manifest, 커밋 PNG). **green 4축**: `:shared:testDebugUnitTest`(97, KoinGraph 온보딩 포함) · `:androidApp:assembleDebug`(**APK ic_launcher 17엔트리 패키징 실증**) · `:shared:linkDebugFrameworkIosSimulatorArm64`(iOS seams UIKit/Foundation 링크) · **`:shared:iosSimulatorArm64Test`(83, 회귀 0)**. 신규 좌표 0(플랫폼 API만). 참조: [M8 슬라이스](docs/specs/m8-integration-assets-draft.md).
  - **비준 RATIFIED(4R 수렴)**. §7 판정: iOS share=최소 스텁·평가=스토어 url·아이콘=커밋 PNG·Firebase=null 유지·VM수명주기=범위 밖·라이선스=in-app. **정정 반영**: iOS `NSUserDefaults` objectForKey null 체크로 외관 부재시 다크(2) 보장(integerForKey 0 반환 함정)·`UIPasteboard.string` 세터·전 actual 5종 실 모듈 바인딩(그래프 마스킹 방어)·DR-2 carry-forward(라이선스 네비 슬롯) `showLicenses` 오버레이로 마감.
  - **⚠️ 검증 천장(최대)**: seam 런타임 동작·아이콘 시각 충실도·iOS appiconset(Xcode)·접근성·Firebase·실 플랫폼 Koin 그래프 완전성·실기기 시각/상호작용·코드서명·심사는 4축이 보증 안 함 → 「코드 완료·실기기 검증 필요」(Now 아침 체크리스트).
  - 🔗 캐시: seam actual이 로컬-first 상태를 플랫폼 저장소(SharedPreferences/NSUserDefaults)에 영속. 서버·프록시는 별도 트랙. 〔캐시 트랙 M8 행위〕
- **M7 · Koin 배선 + 앱 셸 통합** — 2026-07-05 (브랜치 `feat/m7-koin-wiring`, 로컬 커밋·미푸시). 전 계층을 Koin 그래프로 조립: `di/AppModule`(`appModule(readyBundle)` 팩토리·`suspend initKoin(platformModule)`·`TermRepository`=`single`)·플랫폼 팩토리(`androidPlatformModule(context)`/`iosPlatformModule()`)·`KoinAppDependencies`·`DeviceIdProvider`·`epochMillis` expect/actual·`appWriteScope`. **앱 셸이 처음으로 `AppRoot`를 그린다**(M0 `App()` 삭제): `MainActivity`·`MainViewController`·`DevEtymApp`(`runBlocking { initKoin(...) }`). **green 4축**: `:shared:testDebugUnitTest`(97, KoinGraph 2+M7Concurrency 3) · `:androidApp:assembleDebug` · `:shared:linkDebugFrameworkIosSimulatorArm64` · **`:shared:iosSimulatorArm64Test`(83, M7Concurrency 3 네이티브)**. iosMain `AppModule.kt` facade 병합(`AppModuleKt.doInitKoin()` Swift 무편집)·`iosPlatformModule`·`epochMillis` ios actual 링크 실증. 참조: [M7 슬라이스](docs/specs/m7-koin-wiring-draft.md).
  - **⚠️ 검증 천장(최대)**: green = 컴파일·조립·링크 + 그래프 해석(공통+테스트-플랫폼 스텁, eager touch) + DR-2/DR5-2 순수 실행. **실 androidMain/iosMain 플랫폼 Koin 바인딩 완전성·Xcode 빌드·seam actual·런타임 시각/상호작용은 보증 안 함 → 실기기 이월**. 비준 ESCALATE(6R) 잔여 Blocker DR-1(실 플랫폼 그래프 완전성 4축 결착 불가)=천장 정직 수용.
  - **M4/M5 이월 처리**: **DR-2 단일-writer**=`TermRepositoryImpl` 정규화 키 Mutex(맵-가드 coroutines Mutex·비재진입 데드락 부재) + `single` 배선 **구조 담보**(진짜 병렬 강제는 실기기 이월·자칭 안 함). **DR5-2**=`DetailViewModel` 선택적 `writeScope` **취소 내성 하드닝**('닫음' 철회 — 실 셸 plain `remember` VM leak·DR5-2 실 창은 M8 ViewModelStore 이월). seam·deviceId·온보딩=스텁/in-memory(actual M8).
  - 🔗 캐시 배선 완결: 3계층 read-through·pinning·단일-writer 오케스트레이터가 Koin single로 전 화면 공유. 서버·프록시는 별도 트랙. 〔캐시 트랙 M7 행위〕
- **M6 · Compose UI (디자인시스템+6화면+네비)** — 2026-07-05 (브랜치 `feat/m6-compose-ui`, 로컬 커밋·미푸시). `commonMain/ui/`에 **디자인 시스템**(`theme/`: AppColors 11토큰 라이트/다크·AppFonts 하이브리드[한글=시스템·영문=DM Mono·헤더=DM Serif]·AppTypography 21종·AppDimens·AppTheme+AppScheme, 다크 기본)·**재사용 원자**(`components/`: CategoryBadge·AiBadge·FlowChip·PulsingDots·EmptyState)·**6화면**(`screens/`: Search·Detail·Bookmark·History·Settings·Onboarding, `XxxScreen`(VM 구독 래퍼)+`XxxContent`(순수) 2겹)·**네비**(`AppRoot`: 의존성-0 상태기반 back stack, 4탭+상세 push+온보딩 게이트)·**플랫폼 seam**(`platform/`: AppActions·AppearanceStore·DeviceInfo + no-op 스텁). 폰트 7종 `composeResources/font/`. **green 4축**: `:shared:testDebugUnitTest`(92, DesignSystem 7) · `:androidApp:assembleDebug` · `:shared:linkDebugFrameworkIosSimulatorArm64` · **`:shared:iosSimulatorArm64Test`(80, DesignSystem 7 네이티브)**. `lifecycle-runtime-compose 2.9.6`+폰트 iosSimulatorArm64 링크 실증. 참조: [M6 슬라이스](docs/specs/m6-compose-ui-draft.md).
  - **⚠️ 검증 천장(정직 경계)**: green = **컴파일·조립·링크 + 순수 헬퍼 네이티브 실행**까지. **화면 시각 충실도(간격·폰트 렌더·픽셀·다크 실제색·탭/스와이프/애니메이션)는 보증 안 함 → 「코드 완료·실기기 시각 검증 필요」**. 거짓 green 아님(구조·상태분기·배선·컴파일만 보증). 순수 헬퍼(색/타이포/에러메시지/상대시간/상태표시매핑/카테고리색/isBookmarkedFor) 7종만 네이티브 실측.
  - **비준 RATIFIED(6R 수렴)**. §7 판정: 네비=의존성-0(navigation-compose 링크 리스크 회피), 타이포 21종(정본 Theme.swift), 색상 hex=colorset 정본, RelativeTime=경과 diff 기반(N시간 전 포함). **M5 이월**: DR-4(상세 북마크 상태) `isBookmarkedFor`(`normalizeKeyword` 교차조회 — 저장 키 정본 매치)로 마감, DR5-3(history limit) 전량 유지. **DR5-2(쓰기 유실창)은 정직 이월**(M7 — 파생 읽기⟂쓰기 내구성).
  - 🔗 캐시 소비 UI: 반응형 목록·북마크 별표가 로컬-first 상태를 화면에 표면화. 서버·프록시 불변. 〔캐시 트랙 M6 행위〕
- **M5 · ViewModel + StateFlow** — 2026-07-05 (브랜치 `feat/m5-viewmodel`, 로컬 커밋·미푸시). `commonMain/ui/`에 sealed `DetailUiState`(Loading/Result/Error)·`ErrorKind`+`toErrorKind`(sealed-when canary)·ViewModel 4종(`Detail`·`Search`·`Bookmark`·`History`). `TermRepository`만 주입(architecture §4.5). Detail은 load/refresh 단일 취소 슬롯 공유(refresh가 in-flight load 취소)·취소≠Error·toggleBookmark Found-only guard. Search는 300ms 디바운스·반응형 recent. 목록은 전부 `Flow`→`stateIn`(수동 재조회 없음, ADR-0002), History는 전량 노출. **green 4축**: `:shared:testDebugUnitTest`(ui 20) · `:androidApp:assembleDebug` · `:shared:linkDebugFrameworkIosSimulatorArm64` · **`:shared:iosSimulatorArm64Test`(ui 20 네이티브 실행)**. `lifecycle-viewmodel 2.9.6` iosSimulatorArm64 klib 소비 + `setMain` 네이티브 실행 실측(OQ-1 확정). 참조: [M5 슬라이스](docs/specs/m5-viewmodel-draft.md), spec 3-0·3-2·3-3.
  - **비준 ESCALATE(6R) → 게이트 완화 하 eyes-open**. DR6-2(단일 취소 슬롯) 구현 해소, DR5-1(guard)·DR5-4(취소≠Error) discriminating 테스트 방어. **이월**: OQ-3/DR-2 단일-writer Mutex 강제(M7 single 배선 게이트·다중스레드 실측 — M5는 코드 미착지·제안만), DR-4/DR5-2 상세 북마크 상태 소스+쓰기 유실창(M6), DR5-3 history limit 구체값(사람 게이트), AD-1 `TermRepository.kt` KDoc 과장 정정.
  - 🔗 캐시 소비측: 반응형 `stateIn` 목록이 DB 변경을 자동 반영(로컬-first pinning 표면화). 서버·프록시 계층 불변. 〔캐시 트랙 M5 행위〕
- **M4 · Repository 오케스트레이터** — 2026-07-05 (브랜치 `feat/m4-repository`, 로컬 커밋·미푸시). `commonMain/repository/`에 `TermRepository`(유일 인터페이스) + `TermRepositoryImpl`(3계층 read-through fetch: 정규화→번들→로컬 AI 캐시→네트워크, refresh는 캐시 우회·pinned `seenAt` 갱신, toggleBookmark, 반응형 `Flow`). `LocalTermStore`+`SqlDelightTermStore`(M2 쿼리 위임), `AnalyticsService`+Placeholder, `TermGenerator` 인터페이스(`ClaudeApi` 구현). **green 4축**: `:shared:testDebugUnitTest`(65) · `:androidApp:assembleDebug` · `:shared:linkDebugFrameworkIosSimulatorArm64` · **`:shared:iosSimulatorArm64Test`(53)**. 오케스트레이션(fetch 분기·pinning·clamp·createdAt 보존)이 네이티브 실행으로 실측(Fake 협력자 22건). 참조: [M4 슬라이스](docs/specs/m4-repository-draft.md), spec 2-3·2-4.
  - **⚠️ 상속 폐쇄**: createdAt 보존(M2 DR-M2-2 — §6-A refresh 후 정렬 안정성 실측)·schemaVersion Int범위(DR-M2-3 — 모든 쓰기가 `TermEntry.schemaVersion:Int?` 출처라 구성으로 보장)·AI category clamp(M3 §7-4 — 집합 밖→`기타`). **저장 keyword 정본화(AD-1)**: 모든 저장 경로가 `entry.copy(keyword=normalizeKeyword(input))`로 고정 — 대소문자 유의미 용어(`React`/`REST`)의 3단 캐시 영구 miss·중복 로우·재정렬 차단(§6-A 실측).
  - **비준 ESCALATE(6R, Blocker 1=AD-2) → 게이트 완화 하 eyes-open**. AD-2(M2 매퍼 주석 랜드마인) 주석 경로별 정정으로 해소, AD-3(Fake seam) `TermGenerator` 인터페이스로 해소, AD-1(keyword 소문자화) 번들 keyword 소문자 일관성으로 수용. DR-2 RMW 원자성(**단일-writer 계약을 인터페이스 전제조건으로 M5에 전파**)·DR-3 번들 category 게이트(데이터 트랙)·DR-4 크로스버전 승격은 이월.
  - 🔗 캐시 빌트인: 3계층 read-through·local-first pinning(INV-1·2·6·11) 클라 소비측 완성. 서버 D1 계층은 `ClaudeApi.generate` 안에서 서버가 처리(클라 투명). 〔캐시 트랙 M4 행위〕
- **M3 · 네트워킹 + 번들 로더 (클라측)** — 2026-07-05 (브랜치 `feat/m3-networking`, 로컬 커밋·미푸시). `commonMain`에 `BundleDbSource`(번들 `terms.json` 650 로드·정규화 인덱스 first-wins·keyword/alias 완전매칭·prefix autocomplete) + `ClaudeApi`(Ktor read-through 프록시 호출·`tool_use` 3분기→`TermResult`·`X-Device-Id`·429→DailyLimitExceeded) + 프롬프트/3도구(iOS 검증본 계승) + HttpClient 엔진 `expect`/`actual`(OkHttp/Darwin) + 공유 `AppJson`·`normalizeKeyword`. **green 4축 실측**: `:shared:testDebugUnitTest`(39) · `:androidApp:assembleDebug` · `:shared:linkDebugFrameworkIosSimulatorArm64` · **`:shared:iosSimulatorArm64Test`(31)**. **Ktor 3.5.1 × Kotlin 2.3.21 × serialization 1.9.0 klib 소비를 네이티브 링크·테스트로 실측**(§5 load-bearing). 참조: [M3 슬라이스](docs/specs/m3-networking-draft.md), spec 2-1·2-2, [ADR-0006](docs/adr/0006-server-cache-boundary.md).
  - **⚠️ INV-A 로더측 실측 = 폐쇄(M1 DR-1 바인딩)**: §6-B가 실 배포 `terms.json`을 `InMemoryBundleDbSource` 파서·인덱스에 태워 `aa-tree` aliases 내용·category + **alias 검색 성립**(`search("Arne Andersson tree")`→`aa-tree`)을 단언(성공 디코드는 무효 오라클). M1 fixture 대비 증분 폐쇄점. **네이티브 실행 갭 선제 폐쇄**: §6-A(BundleDbSource 매칭 9 + ClaudeApi×MockEngine 11)가 `:iosSimulatorArm64Test`로 네이티브 실행 — Native Ktor 파이프라인+Anthropic 응답 shape(thinking/text/tool_use) 직렬화 디코드 실측(M1·M2 비준 blocker였던 갭을 M3는 선제 폐쇄).
  - **비준 결과 = ESCALATE(6R, Blocker 1=AD-1) → 게이트 완화 하 eyes-open, AD-1은 구현으로 해소**. 6라운드가 draft를 강화(정규화 seam 제거·키잉vs프롬프트 분리로 대소문자 유의미 용어 어원오염 차단·에러처리 status선검사·flat DTO로 thinking블록 관용). 잔여 Blocker AD-1(2xx 비JSON/빈바디 `NoTransformationFoundException` 미포착)은 **수용이 아니라 구현에서 닫음**(catch 넓혀 InvalidResponse 봉인 + canned 2건). 상세: M3 슬라이스 Open Questions.
  - 🔗 캐시 빌트인: 클라를 read-through 계약(ADR-0006)에 투명하게 작성(리팩토링-0). **서버 트랙(devetym-proxy·INV-13)은 별도 이관**(§0). 클라측 category 정규화·fetch 3단은 M4. 〔캐시 트랙 M1·M4 클라 소비측〕
- **M2 · 로컬 DB** — 2026-07-05 (브랜치 `feat/m2-local-db`, 로컬 커밋·미푸시). SQLDelight 2.3.2([ADR-0003](docs/adr/0003-local-storage.md)): `.sq` 스키마(`term`·`searchHistory`, **pinning `seenAt` + 버전 `schemaVersion`/`promptVersion` 컬럼 처음부터** — INV-6·INV-9·INV-12, 마이그레이션 회피)·반응형 라벨 쿼리(`bookmarked`/`recent`, `.asFlow()` 대상)·드라이버 `expect`/`actual`(`AndroidSqliteDriver`/`NativeSqliteDriver`)·DTO↔엔티티 매퍼(`TermEntry.toEntity()`/`Term.toDto()`, aliases/source는 매퍼 변환). **green 4축 실측**: `:shared:testDebugUnitTest`(17) · `:androidApp:assembleDebug` · `:shared:linkDebugFrameworkIosSimulatorArm64` · **`:shared:iosSimulatorArm64Test`(11, B1 신규 축)**. **Kotlin 2.3.21 × SQLDelight 2.3.2 klib 소비를 네이티브 링크·테스트로 실측**(§5 load-bearing). 참조: [M2 슬라이스](docs/specs/m2-local-db-draft.md), spec 1-2.
  - **⚠️ INV-A 매핑측 실측 = 폐쇄(M1 DR-1 바인딩)**: 매퍼 `toEntity`/`toDto`의 `aliases`(순서)·`category` 무손실 보존을 §6-A 순수 commonTest로 실측(DoD 필수). aliases/source 변환을 매퍼에 둬(컬럼 어댑터 아님) 드라이버 없는 순수 왕복으로 성립. **B1 결착**: §6-A가 `:iosSimulatorArm64Test`로 **네이티브 실행**돼 Native `kotlinx.serialization` 왕복도 실측.
  - **비준 결과 = ESCALATE → 사람 eyes-open + B1 부분 폐쇄**(재비준 안 함). 6라운드가 draft를 강화(INV-9 무손실 M2경로 한정·`toEntity` 4필드 필수인자화로 M4 재주입누락 컴파일에러화·§6-B raw컬럼 canary). 잔존 Blocker(네이티브 실행 갭)를 **B1**(네이티브 실행 축 추가)로 직렬화 절반 폐쇄, DB 실행 절반은 M8 이월. 상세: M2 슬라이스 §5·§8·Open Questions.
  - 🔗 캐시 빌트인: pinning/버전 컬럼 = 로컬 head 계층 저장측(INV-6·INV-9·INV-12). 값 쓰기는 M4. 〔캐시 트랙 M4 저장측〕
- **M1 · 모델·직렬화** — 2026-07-05 (브랜치 `feat/m1-model-serialization`, 로컬 커밋·미푸시). `commonMain/model/`에 `TermEntry`(@Serializable, 버전 필드 옵셔널·INV-9)·`Source` enum·`TermResult` sealed interface·`Category` 정본 6어휘(pass-through, 강제 안 함). kotlinx.serialization JSON 왕복. 번들 `terms.json`(650) → `commonMain/composeResources/files/`(compose-resources 배선). `commonTest` §6 5종 + `androidUnitTest` fixture 1종(실제 번들 aliases 내용 단언). green 3축 실측: `:shared:testDebugUnitTest`(6 pass) · `:androidApp:assembleDebug` · `:shared:linkDebugFrameworkIosSimulatorArm64`. **serialization 1.9.0 ↔ Kotlin 2.3.21 호환 빌드로 실측**(§5 load-bearing). 참조: [M1 슬라이스](docs/specs/m1-model-serialization-draft.md), spec 1-1.
  - **비준 결과 = ESCALATE → 사람 eyes-open 수용**(재비준 안 함). Blocker 3 결착: **DR-3**(sealed `when` else 금지) 슬라이스 §6에서 닫음 · **DR-1**(INV-A 실측 범위) M1 fixture로 wire측 부분 폐쇄 + 매핑측(M2)·로더 회귀(M3) 바인딩 상속(위 M2·M3 ⚠️ 항목) · **DR-2**(서버 캐시-히트 정규화 우회) [cache-delivery-milestones](docs/cache-delivery-milestones.md) **INV-13**(정규화-후-캐시쓰기)로 이관. 상세는 슬라이스 §8·Open Questions.
  - 🔗 캐시 빌트인: entry 계약 = read-through 응답 shape. INV-9 버전 태깅 반영(`schemaVersion`/`promptVersion` 옵셔널). 〔캐시 트랙 M0-클라측〕
- **M0 · KMP 골격** — 2026-07-04 (`feat/m0-kmp-scaffold` → `main`, no-ff 병합). Android APK + iOS 시뮬레이터 실제 실행 확인. `shared + androidApp + iosApp`, Koin `startKoin` 배선, 공유 `Greeting`을 양 플랫폼 Compose 화면에 표시. green 루프 3축: `:shared:testDebugUnitTest` · `:androidApp:assembleDebug` · `:shared:linkDebugFrameworkIosSimulatorArm64`(SKIE 포함). 참조: architecture §3·§5, spec 1-6.
  - ✅ iOS interop 결정: **SKIE**([ADR-0005](docs/adr/0005-ios-interop.md)). 골격 버전(**빌드 실측 확정**): **Kotlin 2.3.21 · CMP 1.11.1 · AGP 8.13.0 · Gradle 8.13 · SKIE 0.10.12**. ⚠️ **SKIE 0.10.12는 Kotlin 최대 2.3.21**(2.4.0 거부, 실측) — SKIE가 새 Kotlin 지원 전엔 앞질러 올리지 말 것.
- **프로젝트 문서 세트 수립** — 2026-07-04
  - **그린필드 CMP 설계로** README·[PRD](docs/product/prd.md)·[아키텍처 설계서](docs/architecture.md)·[ADR 0001~0004](docs/adr/)·[Spec](docs/specs/spec.md) 작성.
  - 동일 제품의 iOS(`dev-etymology`, SwiftUI) 구현에서 **검증된 데이터 흐름·설계 불변식을 계승**(fetch 3단·lazy 저장·upsert·aliases 보존·tool_use 3분기·프록시 계약), 관용구는 **코틀린으로**([ADR-0002](docs/adr/0002-code-idiom-principle.md): 리터럴 포팅 금지, 우회 패턴은 삭제).
  - 결정: CMP(UI까지 공유, [ADR-0001](docs/adr/0001-cross-platform-framework.md)) / 로컬 DB SQLDelight 우선·미확정([ADR-0003](docs/adr/0003-local-storage.md)) / 프록시 계약 계승([ADR-0004](docs/adr/0004-backend-proxy-boundary.md)).
- **repo 개설** — 2026-07-04. `devetym`(git init, 계정 `data-sy` 예약).

---

## 작업 단위 분할 원칙

작은 단일 앱이라 가벼운 구조를 쓴다.
- **Roadmap** — 모든 작업의 단일 인덱스이자 진행 상태 정본 (이 문서).
- **Architecture** — [`docs/architecture.md`](docs/architecture.md), 기술 설계 정본.
- **Spec** — [`docs/specs/spec.md`](docs/specs/spec.md), 구현 명세(Phase 1~4).
- **ADR** — 돌이킬 수 없는 결정 ([`docs/adr/`](docs/adr/)).

## 갱신 규칙

- 마일스톤 착수 시 Now로 이동, 브랜치명 함께 기록.
- 완료 시 Done으로 이동, 완료일·PR 번호 기록. 의사결정이 있었다면 ADR 번호도 함께.
- 새 아이디어는 Later에 먼저 추가하고, 우선순위가 오르면 Next로 승격.
- 보류 작업은 Next에 두고 "보류 사유" 명시.
