# M9 슬라이스 (draft) — 출시 준비·실기기 검증 (검증·출시 마일스톤)

> **성격: 비준 하네스 입력용 spec 슬라이스(`-draft.md`).** [`spec.md`](spec.md) 이후의 **검증·출시** 경계. 진행 상태 정본 [`../../ROADMAP.md`](../../ROADMAP.md)(M9). 자율 구현/준비 전 적대 비준 대상.
>
> **⚠️ 이 마일스톤은 종류가 다르다.** M0~M8이 *코드*를 4축 green으로 닫았다면, M9의 완료 오라클은 **실기기·사람·스토어 심사**다 — CI로 못 닫고, 주체가 Claude 자율이 아니라 사람(실기기)·Apple/Google(심사)에 의존한다. 그래서 M9의 노동은 "코드를 더 짜는 것"이 아니라 **(a) 사람 게이트로 넘길 뻔한 항목을 AI가 새 테스트/스크립트로 끌어내려 자동으로 닫고, (b) 남는 환원 불가 게이트는 AI가 *확인 지그*(테스트 하네스·대조표·렌더 시트·메타 초안)를 깔아 사람 컨펌을 최소화**하는 것이다. **거짓 green 금지**(실기기 필요분을 코드 완료로 위장하지 않는다) — 이 마일스톤은 그 경계를 정직히 긋는 게 본령이다.

- **마일스톤**: M9 · 출시 준비·실기기 검증
- **브랜치**: `feat/m9-release-verification` (=`feat/m8-integration-assets` 위에 **스택** 분기)
- **참조**: [M8 슬라이스](m8-integration-assets-draft.md)(seam actual·아이콘·검증 천장), [M7 슬라이스](m7-koin-wiring-draft.md)(실 플랫폼 그래프 완전성 DR-1 이월), [M2 슬라이스](m2-local-db-draft.md)(§6-B `NativeSqliteDriver` 실행 갭 B1 잔여 절반), [M6 슬라이스](m6-compose-ui-draft.md)(디자인 토큰·시각 천장), 런북 `../../android-studio-cmp-runbook.md`, 방침 사이트 `../../site/`. ROADMAP Now/아침 체크리스트가 M9 DoD의 원문.

---

## 태그 범례 (M9 전용 — 노동 분담의 정본)

각 항목·DoD 앞에 붙여 **누가 어떻게 닫는지**를 명시한다. M9의 목표는 `[사람]`을 최대한 `[AI]`·`[AI→사람]`으로 내리는 것.

- **`[AI]`** — Claude/CI가 자동 실측·산출한다. 새 테스트(4축 green)·스크립트·정적 대조로 닫힌다. 사람 개입 0.
- **`[AI→사람]`** — AI가 **확인 지그**를 만들어 사람 몫을 최소화한다. 지그 = 테스트 하네스·정적 대조표·아이콘 렌더 대조 시트·대비비 리포트·스토어 메타 초안·서명 가이드 등. 사람은 지그 위에서 **관측·컨펌만**(수 분). AI 산출물은 CI green, 최종 판정은 사람.
- **`[사람]`** — 실기기·코드서명·스토어 심사처럼 AI가 못 닫는 환원 불가 게이트. 사람만 닫는다. 외부 대면이라 **사람 지시 대기**(자율 금지).
- **`[별도 트랙]`** — M9 출시 게이트가 아니라 병렬/후속(서버 proxy·Firebase·VM 수명주기). 릴리즈를 막지 않음.

---

## 1. 목표

M8까지 앱은 **코드 완료**됐으나, ROADMAP이 명시하듯 남은 것은 전부 실기기/사람/스토어 게이트다. M9는 그 게이트를 **첫 스토어 게시까지** 닫는 것을 목표로 하되, 무작정 사람에게 떠넘기지 않는다:

1. **끌어내리기** — 사람 게이트로 분류됐던 항목 중 AI가 테스트로 닫을 수 있는 것을 **새 테스트로 자동화**한다. 우선 대상: (i) 실 플랫폼 Koin 그래프 완전성(M7 DR-1 — 실 `androidPlatformModule` 해석 테스트), (ii) `NativeSqliteDriver` 실제 실행(M2 B1 잔여 절반 — 네이티브 DB 왕복 테스트), (iii) seam **로직**(Intent 구성·클립보드 쓰기·외관 set→emit 전파)의 계약 테스트.
2. **지그 깔기** — 환원 불가 게이트(실기기 렌더·심사·서명)는 AI가 **확인 지그**를 산출해 사람 컨펌을 최소화한다: 아이콘 렌더 대조 시트, 접근성 정적 리포트(대비비·contentDescription 커버리지), 스토어 메타데이터·개인정보 라벨 초안, 서명·업로드 절차 가이드.
3. **정직한 핸드오프** — 지그로도 못 내리는 순수 실기기/심사 항목만 `[사람]`으로 남기고, 최종 체크리스트를 산출한다.

## 2. 스코프

**IN — `[AI]` (새 테스트·스크립트로 자동 검증):**
- 실 플랫폼 Koin 그래프 완전성 테스트(Android측 — §3-1)·`NativeSqliteDriver` 네이티브 실행 테스트(§3-2)·seam 로직 계약 테스트(§3-3).
- 정적 산출물: 접근성 대비비 리포트·contentDescription 커버리지 스캔(§3-5)·라이선스 화면 로드 테스트(§3-4).

**IN — `[AI→사람]` (AI가 지그, 사람은 컨펌):**
- 아이콘 렌더 대조 시트(§3-4)·스토어 메타데이터/개인정보 라벨 초안(§3-6)·keystore 생성·서명·업로드 가이드(§3-6)·실기기 스모크 대본(§3-7).

**IN — `[사람]` (환원 불가):**
- 실기기 첫 기동 스모크·seam 실동작(메일앱/공유시트/클립보드/외관 실전환)·아이콘 홈스크린 실렌더·iOS appiconset(Xcode 빌드)·스플래시·TalkBack/VoiceOver 실주행·**코드 서명·스토어 심사·게시**.

**OUT / `[별도 트랙]`:**
- Firebase App Instance ID 실통합(instanceId=null 유지)·VM 수명주기(ViewModelStore·M7 DR5-2 실 창)·서버 `devetym-proxy`(TS/Worker read-through 캐시). 릴리즈 비차단.

## 3. 산출 명세

### 3-1. `[AI]` 실 플랫폼 Koin 그래프 완전성 (M7 DR-1 끌어내리기 — Android)

- **문제(상속)**: 그래프 테스트는 테스트-스텁 모듈만 해석하고 실 `androidPlatformModule`/`iosPlatformModule`은 컴파일-only라, 실 바인딩 누락 시 첫 기동 `NoDefinitionFound`가 4축 green을 조용히 통과한다(M7 DR-1·M8 §3-2 경고).
- **`[AI]` 처방(Android)**: `androidUnitTest`에서 **실 `androidPlatformModule(context)`**(테스트-스텁 아님)을 Robolectric/모의 `Context`로 조립하고, `KoinAppDependencies`가 eager-touch하는 **전 seam**(actions·appearance·onboarding·device·deviceId + repository 체인)을 `koin.get()`으로 강제 해석해 `NoDefinitionFound`/타입 불일치가 없음을 단언한다. 이로써 **Android측 실-플랫폼 그래프 완전성을 실기기 아닌 JVM 테스트로 닫는다**(DR-1의 Android 절반 폐쇄).
- **`[사람]` 잔여**: iOS `iosPlatformModule`은 JVM 테스트 밖 — `linkDebugFrameworkIosSimulatorArm64`가 링크만 보증하고 해석은 실행 아님. iOS 실-그래프 완전성은 **실기기/시뮬레이터 첫 기동**으로 남는다(§3-7 스모크). ⚠️ Robolectric 도입이 스코프를 키우면(§7 판정) 대안: 실 모듈 정의 목록과 `KoinAppDependencies` 요구 타입을 **정적 대조**하는 `[AI→사람]` 표로 격하.

### 3-2. `[AI]` `NativeSqliteDriver` 네이티브 실행 (M2 B1 잔여 절반 끌어내리기)

- **문제(상속)**: M2 §6-B DB 왕복은 JVM(JDBC) 전용이라 네이티브 DB 실행(스키마 create·`INSERT OR REPLACE`·`ORDER BY`/`LIMIT`·nullable INTEGER 바인드·TEXT 정렬 로케일)은 무측정(B1 잔여 절반, ROADMAP:66).
- **`[AI]` 처방**: `iosSimulatorArm64Test`에서 **인메모리 `NativeSqliteDriver`**로 실제 스키마를 create하고 M2 `.sq` 쿼리(upsert·`bookmarked`(`createdAt DESC`)·`recent`(`LIMIT`)·nullable `seenAt`/`schemaVersion` 바인드)를 왕복 실행해 결과를 단언한다. M2가 JVM에서만 검증한 DB 실행을 **네이티브에서 실측** → B1 잔여 절반 폐쇄(M2 Open Questions의 M8/실기기 이월분을 M9가 AI로 회수).
- **`[사람]` 잔여**: 실기기 파일시스템 드라이버(인메모리 아닌 디스크 경로·동시성·기기별 SQLite 빌드 차이)는 여전히 실기기 — 단, 쿼리 정확성 핵심은 위 테스트가 닫는다. TEXT 정렬 **로케일** 의존이 있으면 기기별 차이는 실기기 확인 항목으로 명시.

### 3-3. `[AI]` seam 로직 계약 테스트 (M8 seam 런타임 갭 부분 끌어내리기)

- **문제(상속)**: M8 seam actual은 컴파일·링크만 보증, set→emit·재구성 전파·OS 핸드오프는 실기기 천장(M8 §3-6·§0).
- **`[AI]` 처방(로직만)**: OS 핸드오프와 **분리 가능한 순수 로직**을 계약 테스트로 닫는다 — (i) `AppearanceStore.set(mode)` → `MutableStateFlow` 방출값 = 입력 mode(0/1/2 클램프 포함) 단언, (ii) 외관 매핑 `mode→dark`(0=시스템/1=false/2=true) 순수 함수 테스트, (iii) Android `AndroidAppActions`의 Intent **구성**(action=`ACTION_SENDTO`·data scheme=`mailto`·`ACTION_SEND` type=`text/plain`) Robolectric `Shadow` 단언, (iv) 클립보드 쓰기 후 `ShadowClipboardManager` 값 단언. **로직·구성은 AI가 닫고, 실제 앱 열림·시트 표시·픽셀 전환만 사람으로 남긴다.**
- **`[사람]` 잔여**: 메일앱 실제 열림·공유시트 표시·클립보드 실붙여넣기·외관 **실전환**(재구성이 화면 색을 바꾸는지). iOS `UIPasteboard.string` **세터**(게터 no-op 함정, M8 §3-1)는 링크만 — 실동작은 실기기.

### 3-4. `[AI]`·`[AI→사람]` 자산 — 라이선스 로드 + 아이콘 렌더 대조

- **`[AI]` 라이선스 화면 로드**: `LicensesScreen`의 `Res.readBytes`(OFL 3종)가 **비어있지 않은 텍스트를 반환**하고 디코드되는지 네이티브/JVM 테스트로 단언(M8 DR-2 carry-forward의 로드 정확성 절반 — 화면 렌더는 사람). OFL .txt 3종이 composeResources에 실재(assembleDebug 병합은 M8이 검증)함을 파일 존재로 재확인.
- **`[AI→사람]` 아이콘 렌더 대조 시트**: 커밋된 `mipmap-*/ic_launcher.png`(17엔트리, M8) + adaptive foreground/background를 **한 장의 대조 시트**(밀도별·원형/스퀘어 마스크 미리보기)로 렌더(HTML/Artifact 또는 PNG 몽타주)한다. 사람은 홈스크린에 깔기 전 시트로 **모양·크롭·배경 대비**를 한눈에 컨펌. iOS appiconset은 **Xcode 빌드**(축 밖) — 상속 PNG 배치만 재확인.
- **`[사람]` 잔여**: 실기기 홈스크린 아이콘 렌더·스플래시(런치스크린) 실표시·라이선스 화면 실스크롤.

### 3-5. `[AI]`·`[AI→사람]` 접근성 정적 리포트

- **`[AI]` 대비비**: `AppColors` 11토큰(라이트/다크, M6)의 전경/배경 조합 **WCAG 대비비를 자동 계산**해 AA(4.5:1 본문·3:1 큰텍스트) 미달 쌍을 리포트한다(순수 계산 — 테스트/스크립트). 미달이면 실기기 전에 토큰 조정 신호.
- **`[AI]` contentDescription 커버리지**: `components/`·`screens/`의 아이콘·이미지·클릭가능 요소에 `contentDescription`/시맨틱이 붙었는지 정적 스캔해 누락 목록 산출(감사 자체는 실기기지만, **누락은 코드로 선검출**).
- **`[AI→사람]` 감사 대본**: 위 리포트를 근거로 TalkBack/VoiceOver **주행 대본**(어느 화면 어느 순서로 무엇을 듣는지)을 산출 → 사람은 대본대로 실기기 1회 주행·컨펌.
- **`[사람]` 잔여**: TalkBack/VoiceOver **실주행**·Dynamic Type 실반영.

### 3-6. `[AI→사람]` 스토어 제출 자산

- **`[AI→사람]` 메타데이터 초안**: Play Store·App Store용 앱 설명·짧은 설명·키워드·카테고리·**개인정보 처리방침 URL**(=`site/` 배포 주소)·**데이터 안전/개인정보 라벨**(수집 항목=방침 §1과 정합: 익명 이용데이터·App Instance ID, 광고ID 미수집)을 초안한다. 사람은 검토·수정·붙여넣기.
- **`[AI→사람]` 서명·업로드 가이드**: Android keystore 생성 명령·`signingConfigs` 배선·AAB 빌드(`bundleRelease`)·Play Console 업로드 절차, iOS 서명·Archive·App Store Connect 절차를 **단계 가이드**로 산출(런북 후속). 실제 keystore 생성·서명은 비밀키라 **사람**.
- **`[사람]` 잔여**: keystore/인증서 실생성·서명·**심사 제출·게시**. 외부 대면 — 사람 지시 대기.

### 3-7. `[사람]` 실기기 스모크 대본 (AI가 대본, 사람이 주행)

- **`[AI→사람]` 대본**: ROADMAP 아침 체크리스트 1~4를 **실기기 주행 순서**로 구체화 — 첫 기동 크래시 없음(전 seam 해석)→검색·상세·북마크·히스토리→외관 3모드 전환→메일/공유/클립보드/평가→온보딩 1회성→라이선스 화면. iOS는 §3-1 잔여(실-그래프)를 이 첫 기동이 커버.
- **`[사람]` 주행**: 위 대본을 Android 실기기/에뮬레이터(런북 경로) + iOS 시뮬레이터/실기기에서 1회 주행·컨펌. **이게 M9 DoD의 핵심 사람 게이트.**

## 4. 설계 불변식

- **검증 천장 정직**(M8 §0 계승): `[AI]`가 닫은 것과 `[사람]`이 닫을 것을 태그로 명시 분리. 실기기 필요분을 green으로 위장 금지.
- **끌어내리기 우선**: 사람 게이트는 AI 테스트로 닫을 수 있는지 먼저 시도하고, 못 내리는 것만 `[사람]`. 태그가 그 판정의 기록.
- **회귀 없음**: M9가 추가하는 테스트(§3-1·3-2·3-3)는 M1~M8 4축 green을 깨지 않는다. 신규 좌표(Robolectric 등) 도입은 §7 판정.
- **출시 자산은 정본 상속**: 방침(`site/`)·아이콘·폰트·토큰은 기존 상속(재설계 아님).
- **브랜치 보존·push/게시 금지(사람 지시 대기)·젠더중립 네이밍·진행상태는 ROADMAP(디스크)**.

## 5. 완료 조건 (DoD) — 태그별

- **`[AI]` green(자동으로 닫는 부분)**:
  - §3-1 실 `androidPlatformModule` 그래프 완전성 테스트 green(Android DR-1 절반 폐쇄).
  - §3-2 `NativeSqliteDriver` 네이티브 DB 왕복 테스트 green(B1 잔여 절반 폐쇄).
  - §3-3 seam 로직 계약 테스트 green(외관 전파·Intent 구성·클립보드 로직).
  - §3-4 라이선스 로드 테스트·§3-5 대비비/커버리지 리포트 산출.
  - 4축(`:shared:testDebugUnitTest`·`:androidApp:assembleDebug`·`:shared:linkDebugFrameworkIosSimulatorArm64`·`:shared:iosSimulatorArm64Test`) green 유지(회귀 0).
- **`[AI→사람]` 지그 산출(사람 컨펌 대기)**: 아이콘 렌더 대조 시트·접근성 감사 대본·스토어 메타/개인정보 라벨 초안·서명·업로드 가이드·실기기 스모크 대본. 산출물은 CI/리포지토리에 존재, 컨펌은 사람.
- **`[사람]` 최종 게이트(M9 완료 = 이것들 통과 후 게시)**: 실기기 스모크 대본 주행 통과·seam 실동작 확인·아이콘/스플래시/라이선스 실렌더·TalkBack/VoiceOver 주행·iOS appiconset(Xcode)·**코드 서명·심사·스토어 게시**.
- **명시적 비-보증**: `[AI]` green은 실기기 동작·심사 통과를 보증하지 않는다 — `[사람]` 게이트가 M9의 종결. 이 분리를 최종 핸드오프에 명시.

## 6. 테스트

- `test_androidPlatformModule_실그래프_전seam_해석` — 실 모듈(스텁 아님) + 모의/Robolectric Context로 `KoinAppDependencies` 전 seam `get()` 해석 green(§3-1).
- `test_nativeSqliteDriver_왕복` — 네이티브 인메모리 드라이버로 스키마 create·upsert·`bookmarked`/`recent` 정렬·LIMIT·nullable 바인드 왕복(§3-2, `iosSimulatorArm64Test`).
- `test_appearance_set_emit`·`test_appearanceMode_dark매핑`·`test_androidActions_intent구성`·`test_clipboard_write` — seam 로직(§3-3).
- `test_licenses_readBytes_비어있지않음` — OFL 로드(§3-4).
- `test_contrast_wcag_리포트`·`test_contentDescription_커버리지` — 정적 리포트(§3-5, 산출형).
- **기존 결착 유지**: M1~M8 테스트 전부 green(회귀 없음).

## 7. 열린 질문 (비준이 판정할 항목)

1. **Robolectric 도입 vs 정적 대조(§3-1)** — 실 `androidPlatformModule` 해석 테스트에 Robolectric(모의 Context)을 도입할지, 아니면 신규 좌표를 피해 실 모듈 정의 vs 요구 타입 **정적 대조표**(`[AI→사람]`)로 격하할지. 제안: Robolectric로 실해석(끌어내리기 최대화) 시도, 스코프/링크 리스크 크면 정적 대조로 폴백. 비준 판정.
2. **네이티브 DB 테스트 범위(§3-2)** — 인메모리 `NativeSqliteDriver`로 쿼리 정확성 왕복이면 B1 잔여를 닫는 데 충분한가, 아니면 TEXT 정렬 로케일·디스크 경로까지 실기기로 남기는 게 정직한가. 제안: 인메모리로 쿼리 정확성 폐쇄 + 로케일/디스크는 `[사람]` 명시 잔여. 비준 판정.
3. **접근성 대비비 판정선(§3-5)** — WCAG AA만인가 AAA도 리포트하나. 제안: AA를 게이트, AAA는 참고. 비준 판정.
4. **스토어 개인정보 라벨의 정본(§3-6)** — 방침 `site/privacy-policy.md`를 라벨 초안의 단일 출처로 고정할지. 제안: 고정(불일치 방지). 비준 판정.
5. **M9 완료 정의** — "실기기 스모크 대본 주행 통과 + 지그 전부 산출"까지를 M9 done으로 보고 **스토어 게시는 별도 사람 지시**로 뗄지, 게시까지 M9에 포함할지. 제안: 게시는 사람 지시 대기 항목으로 M9 DoD에 걸되 자율 실행 금지. 비준 판정.

## 8. 안전·규율

- **검증 천장 정직**(§0·§5): `[AI]`/`[사람]` 태그로 보증 범위 명시. **거짓 green 금지.**
- **끌어내리기 규율**: 사람 게이트는 AI 자동화 시도를 먼저 기록하고 남는 것만 `[사람]`.
- 마일스톤 경계 **사람 게이트 완화**(메모리 `milestone-human-gate-relaxed`): 적대 비준 후 eyes-open 수용·`[AI]` 구현 자율. **단, `[사람]` 게이트(실기기·서명·게시)는 완화 대상 아님** — 외부 대면이라 사람 지시 대기 안전선 유효.
- **브랜치 보존·push/게시 금지·젠더중립 네이밍·진행상태는 ROADMAP(디스크)**. 하네스는 push·머지·`-draft` 제거 안 함.

## Open Questions

> 아래 2건은 **인라인 design-review(2026-07-05, 잘못된 high-traffic perf 렌즈로 주행 — agent-harnesses BACKLOG 참조) 중 렌즈 D(측정 타당성)로 잡힌 catch**를 선반영한 것이다. 렌즈 D는 앱 도메인에도 유효해 렌즈 오류와 무관하게 성립한다. 각 항목은 **요구 수정(fix)까지** 명시하므로, `/design-review`를 devetym 프로파일로 고쳐 **격리 재실행**할 때 "수정이 반영됐나"를 검증한다(단순 "적혀 있나"가 아니라). 나머지 열린 질문은 적대 비준이 채운다.

- [ ] (Blocker 후보·§3-1·렌즈D 측정 타당성) **실 그래프 완전성 테스트의 부분 스텁 함정** — 주장: §3-1이 실 `androidPlatformModule(context)`를 해석해 M7 DR-1(실 바인딩 누락 마스킹)의 Android 절반을 닫는다. 파괴 시나리오: androidPlatformModule의 `single<DevEtymDatabase> { createDatabase(DriverFactory(context)) }`는 Robolectric에서 실 `AndroidSqliteDriver`로 DB를 열어야 하는데, Robolectric SQLite가 스키마 create에 실패하면 구현자가 **기존 `KoinGraphTest` 패턴 그대로** `DevEtymDatabase`만 in-memory JDBC로 되돌려 스텁한다(그 패턴이 이미 코드에 있음 — `KoinGraphTest.testDatabase()`). 그 순간 6개 플랫폼 바인딩 중 **가장 누락·오배선 위험이 큰 DB/드라이버 바인딩이 스텁인 채** "실 그래프 완전성 폐쇄" green이 뜬다 → DR-1이 경고한 바로 그 마스킹(실 바인딩 아님을 닫았다고 착각) 재발. **요구 수정**: §3-1이 **부분 스텁 금지**를 못박는다 — 6개 실 바인딩(db·deviceId·actions·appearance·onboarding·device) 전부를 실 모듈에서 해석하거나, 어느 하나라도 Robolectric서 못 뜨면 그 항목만 `[사람]` 잔여로 **명시 강등(스텁 대체 금지)**. 테스트는 "각 바인딩이 실/스텁 중 무엇인지"를 단언·주석으로 표기해 DB 스텁 강등이 조용히 못 지나가게.
- [ ] (Caution·§3-3·렌즈D 측정 타당성) **seam 로직 테스트가 최고위험 iOS 외관 기본값 로직을 누락** — 주장: §3-3이 seam '로직'을 계약 테스트로 닫는다(외관 set→emit·Intent 구성·클립보드). 파괴 시나리오: 열거된 테스트가 Android(Robolectric Intent/클립보드)와 순수 매핑(mode→dark)만 커버하고, 정작 M8이 고친 **iOS `UserDefaultsAppearanceStore.readMode()`의 `objectForKey==null → 2(다크)` 분기**(`integerForKey`가 키 부재 시 0=시스템을 반환하는 함정, `IosSeams.kt:56-59`)는 빠진다 — 이건 에러 없는 조용한 발산(신규 설치 iOS 첫 실행이 다크가 아니라 시스템)이라 실기기에서도 눈에 안 띌 수 있는 최고위험 seam 로직인데, "seam 로직 닫음" green이 그 위를 덮는다. **요구 수정**: 이 로직은 `iosSimulatorArm64Test`에서 NSUserDefaults 키 clear 후 `UserDefaultsAppearanceStore().mode.value == 2` 단언으로 `[AI]`가 닫을 수 있다(사람 불필요) — §3-3/§6에 `test_ios_외관_키부재_기본다크` 추가. Android 외관 로직만으로 "외관 전파 로직 폐쇄" 자칭 금지.

### 명시 이월 (carry-forward — 미탐색이지만 알려진 클래스)

> 비준 종료 시점의 명시 이월 자리. 현재 비어 있음.
