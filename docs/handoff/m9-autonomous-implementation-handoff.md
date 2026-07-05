# M9 자율주행 착수 핸드오프 — 출시 준비·실기기 검증 (2026-07-05)

> **성격: 다음 세션이 읽고 바로 착수하는 forward 실행 계획.** 진행 상태 정본은 [ROADMAP](../../ROADMAP.md)(M9),
> 산출 명세 정본은 [M9 슬라이스](../specs/m9-release-verification-draft.md). 이 핸드오프는 그 둘로 흡수되는
> **실행 순서·게이트 지도**다(시점성 상태는 여기 쌓지 말고 ROADMAP에).
>
> **착수법**: 새 세션에서 `@docs/handoff/m9-autonomous-implementation-handoff.md` + `@docs/specs/m9-release-verification-draft.md`
> 주고 **"실행"**. 브랜치 `feat/m9-release-verification`(m8 위 스택).

---

## 0. 모드 결정 (왜 비준 하네스 없이 자율주행인가)

M0~M8은 *코드* 마일스톤이라 마일스톤마다 적대 비준 하네스를 태웠다. **M9는 종류가 다르다** — `[AI]` 산출물이
**테스트·정적 리포트·지그**고, 그 정확성 오라클은 **4축 green 그 자체**다. 그래서:

- **별도 비준 하네스 생략.** 비준이 테스트 마일스톤에 더할 값(거짓 green 함정 사냥)은 **design-review 2회**(내부 서브에이전트 + 사람 fresh-session, 2026-07-05)가 이미 수행 — 부분스텁·DB 드롭·네이티브 축 부재를 잡아 스펙에 못박음(§3-1·§4·§5·§7 Q1·Q6). **design-review 재실행도 불필요**(수정은 조이기/이관형이라 새 주장 없음).
- **자율주행 + eyes-open.** 열린 질문(Q1~Q6)은 결함이 아니라 **결정** — 메모리 `milestone-human-gate-relaxed`대로 아래 §1 기본값으로 eyes-open 수용하고 `[AI]` 구현·4축 green까지 자율 관통. 사람 체크포인트 = **완성물 아침 리뷰**(수용 residual 로그) + 아래 §4 `[사람]` 컨펌 지점.
- **불변 안전선**(완화 대상 아님): 브랜치 보존 · push/게시 금지(사람 지시 대기) · 젠더중립 네이밍 · 진행상태는 ROADMAP(디스크). `[사람]` 게이트(실기기·서명·심사)는 환원 불가라 자율 금지.

---

## 1. 열린 질문 eyes-open 기본값 (착수 시 수용 — 아침 리뷰서 override 가능)

각 결정은 스펙 §7의 제안을 따르되, 신규 좌표 2건(Q1·Q6)은 착수 시 사람이 한 번 sanity-check할 값으로 표시.

| # | 결정 | 기본값(수용) | 근거 |
|---|---|---|---|
| **Q1** | Robolectric 도입 vs 정적대조 | **Robolectric 채택, §3-1도 재사용** ⚠️신규좌표 | §3-3(iii)(iv) Intent/클립보드가 이미 Robolectric 하드 의존(순수 JVM서 "not mocked" 즉사) → 도입 비용 이미 지불. §3-1만 정적대조로 떼면 판정 분리 위험 |
| **Q6** | `iosSimulatorArm64Test` 소스셋 | **신설** ⚠️신규좌표 | §3-2·§3-3(v)가 착지할 축이 현재 미구성(`build.gradle.kts`에 `commonTest`·`androidUnitTest`만). 신설 안 하면 B1 못 닫음 |
| Q2 | 네이티브 DB 범위 | 인메모리로 쿼리 정확성 폐쇄 + 로케일/디스크 `[사람]` 잔여 | 스펙 §7-2 제안 |
| Q3 | 대비비 판정선 | AA=게이트, AAA=참고 | 스펙 §7-3 제안 |
| Q4 | 개인정보 라벨 정본 | `site/privacy-policy.md` 단일 출처 고정 | 스펙 §7-4 제안 |
| Q5 | M9 완료 정의 | 스모크 주행 통과+지그 산출까지 done, **게시는 사람 지시 대기**(DoD에 걸되 자율 금지) | 스펙 §7-5 제안 |

**⚠️ Q1·Q6 = 신규 빌드 좌표.** 착수 첫 스텝에서 이 둘을 세우고 **4축 green 회귀 0을 먼저 확인**한 뒤 본 구현. 세우다 링크/스코프 리스크가 크면 그 자리에서 멈추고 아침 리뷰로 올림(자율로 스텁 우회 금지 — §3-1 부분스텁 금지 정신).

---

## 2. `[AI]` 구현 순서 (design-review 가드레일 내장)

착수 → 순서대로. 각 스텝 끝에 **4축 green 회귀 0** 확인.

0. **신규 좌표 셋업(Q1·Q6)**: `shared/build.gradle.kts`에 Robolectric(androidUnitTest) + `iosSimulatorArm64Test` 소스셋·`NativeSqliteDriver` 테스트 의존 배선. → 빈 축이라도 4축 green 확인 후 진행.
1. **§3-1 실 Android Koin 그래프 완전성** (`test_androidPlatformModule_실그래프_전seam_해석`)
   - 실 `androidPlatformModule(context)` (스텁 아님) 조립, `KoinAppDependencies` 전 seam `koin.get()` 강제 해석.
   - **🔒 가드레일(design-review Finding 1)**: 6 바인딩(`DevEtymDatabase`·`DeviceIdProvider`·`AppActions`·`AppearanceStore`·`OnboardingStore`·`DeviceInfo`) 전부 실 해석. **DB는 하중 바인딩** — `KoinGraphTest.kt:38,47`의 in-memory JDBC 스텁으로 되돌리기 **금지**, `[사람]`으로 드롭도 금지(드롭≈스텁). DB가 Robolectric서 실 해석 안 되면 §3-1은 "절반 폐쇄" green 못 냄 → `[AI→사람]` 정적대조로 격하. **각 바인딩 실/스텁 주석 표기**.
2. **§3-2 `NativeSqliteDriver` 네이티브 왕복** (`test_nativeSqliteDriver_왕복`, `iosSimulatorArm64Test`)
   - 인메모리 네이티브 드라이버로 스키마 create·upsert·`bookmarked`(createdAt DESC)·`recent`(LIMIT)·nullable 바인드 왕복.
   - 로케일/디스크는 `[사람]` 잔여(Q2).
3. **§3-3 seam 로직 계약 테스트**
   - (i) `AppearanceStore.set`→emit (ii) `mode→dark` 매핑 (iii) Android Intent 구성(Robolectric Shadow) (iv) 클립보드(ShadowClipboardManager) (v) **iOS 키부재 기본다크** (`test_ios_외관_키부재_기본다크`, `iosSimulatorArm64Test`): NSUserDefaults `appearance_mode` clear 후 `UserDefaultsAppearanceStore().mode.value == 2` (`IosSeams.kt:56-59` objectForKey==null 분기).
4. **§3-4 라이선스 로드** (`test_licenses_readBytes_비어있지않음`): OFL 3종 `Res.readBytes` 비어있지않음·디코드.
5. **§3-5 정적 리포트**: 대비비 WCAG 계산(AA 미달 쌍) + contentDescription 커버리지 스캔.

---

## 3. `[AI→사람]` 지그 산출 (사람 컨펌 대기물)

`[AI]` green 후 산출. 사람은 지그 위에서 **관측·컨펌만**(수 분):
- §3-4 **아이콘 렌더 대조 시트**(밀도별·원형/스퀘어 마스크) — Artifact/PNG 몽타주
- §3-5 **접근성 감사 대본**(TalkBack/VoiceOver 주행 순서)
- §3-6 **스토어 메타/개인정보 라벨 초안**(정본 = `site/privacy-policy.md`, Q4) + **서명·업로드 가이드**
- §3-7 **실기기 스모크 대본**(ROADMAP 아침 체크리스트 1~4 구체화)

---

## 4. `[사람]` 컨펌 지점 지도 (자율 금지 — 여기서 사람 대기)

**중간 컨펌 (지그 산출 직후, 실기기 전):**
- ☐ 아이콘 렌더 대조 시트 확인(모양·크롭·배경 대비)
- ☐ 스토어 메타/개인정보 라벨 초안 검토·수정
- ☐ 서명·업로드 가이드 검토

**최종 게이트 (M9 완료 = 이것들 통과 후 게시):**
- ☐ **실기기 스모크 대본 주행** (Android 에뮬/실기기 + iOS 시뮬/실기기) — 첫 기동 크래시 없음(전 seam)→검색·상세·북마크·히스토리→외관 3모드→메일/공유/클립보드/평가→온보딩 1회성→라이선스. **iOS 첫 기동이 §3-1 iOS 잔여(실-그래프)를 커버.**
- ☐ seam 실동작(메일앱 열림·공유시트·클립보드 붙여넣기·외관 실전환)
- ☐ 아이콘 홈스크린 실렌더·스플래시·라이선스 실스크롤
- ☐ TalkBack/VoiceOver 실주행·Dynamic Type
- ☐ iOS appiconset (Xcode 빌드)
- ☐ **코드 서명 (keystore/인증서 실생성)** — 비밀키라 사람
- ☐ **스토어 심사 제출·게시** — 외부 대면, 사람 지시 대기(Q5)

---

## 5. 아침 리뷰 체크포인트 (사람)

자율 관통 후 사람이 아침에 확인:
- 4축 green(회귀 0) + 신규 축(`iosSimulatorArm64Test`) green
- **수용 residual 로그**: DB 강등 발생 여부(§3-1), 로케일/디스크 `[사람]` 이관(§3-2), Q1·Q6 신규좌표 실제 상태
- 지그 산출물 존재 → §4 중간 컨펌으로 진행

## 6. 명시적 비-보증

`[AI]` green은 실기기 동작·심사 통과를 보증하지 않는다. `[사람]` 게이트(§4)가 M9의 종결. 이 분리를 최종 핸드오프에 명시(거짓 green 금지).
