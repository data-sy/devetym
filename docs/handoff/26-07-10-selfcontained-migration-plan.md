# 자기완결화·이관·코드 갭 마감 — 작업단위 계획 & 결정 원장 (2026-07-10)

> **성격: 여러 세션에 걸쳐 실행할 forward 계획.** 이 세션은 **실행이 아니라 계획·설계 + 결정/승인 확정**만 했다.
> 각 작업단위(WU)는 **독립 세션 하나로 끊어 실행**하도록 스코프됐다 — 미래 세션은 해당 WU 절만 읽고 바로 착수한다.
> 진행 상태 정본 = [ROADMAP](../../ROADMAP.md)(Now: 이관 트랙·코드 갭 트랙 / M9 [외부]). 시점성 상태는 여기 쌓지 말고 ROADMAP에.
> 세션 워킹 체크리스트(폐기가능) = [`26-07-10-to-do-list.md`](../../26-07-10-to-do-list.md).
> ✅ 커밋 상태: 이 계획 세션 산출(§0)은 **`315ea55`로 커밋됨**. 이후 각 WU 실행분은 그 세션에서 커밋한다 —
> 예: **코드 갭 트랙 WU-8·9·10 → 2026-07-10 커밋**(5축 green). **✅ 이관 트랙 WU-2·3·5·6 → 2026-07-10 완료**(WU-6 = 네이티브 iOS 전수 스윕·자기완결성 확증·안전 이관 2건). **✅ WU-4(크래시 리포팅 Sentry) → 2026-07-10 완료**(플랫폼 seam 분리·5축 green·방침 사인오프 완료 — §2 WU-4 참조). 잔여: **WU-1(Pages — 착수·⚠️ 보류: private+무료 플랜 활성화 불가·[PR #10] 스테이징, §2 WU-1 참조)**·WU-7(폐기·사람 게이트).

---

## 0. 이번 세션에 실제로 한 것 (실행분)

1. **리모트 공개 실행** — private repo `data-sy/devetym` 생성 → 전 브랜치(11) push → `feat/m1`~`feat/m8` 스택 PR(#1~#8) merge-commit 순차 병합 → **main = M8**. m9 = draft PR #9(미머지). **브랜치 11개 전부 보존**(삭제 안 함, 소급 PR 소스).
2. **ROADMAP 재편** — Now에 "이관·자기완결화 트랙" + "코드 갭 수정 트랙" 신설. 기존 12 백로그 재배치: #1·#3·#4·#12 → 이관 트랙 / #2 → M9 [외부] / #9·#10·#11 → 코드 갭 트랙 / #5·#6·#7·#8 → "Later — 출시 후 백로그".
3. **순수 [AI] 이관** (`315ea55` 커밋) — `.github/workflows/pages.yml`, `Scripts/{db-expand, prompt-probe, generate_db.py}`, `docs/db-expand/`.

---

## 1. 결정·승인 원장 (이 세션에 확정 — 미래 세션은 재논의 없이 따른다)

| # | 결정 사항 | 확정 값 | 근거 / 비고 |
|---|-----------|---------|-------------|
| D1 | **크래시 리포팅 SDK** | **Sentry (Kotlin Multiplatform)** `io.sentry:sentry-kotlin-multiplatform` | 공식 KMP SDK → commonMain 단일 배선(프로젝트 "거의 전부 commonMain" 원칙 정합). Crashlytics는 공식 KMP SDK 부재 → 플랫폼 조각 증가. 프라이버시 리전/자체호스팅 세분화. ⚠️ **Google 생태계 통합 원하면 Crashlytics로 override 가능**(미빌드라 가역). |
| D2 | **크래시 리포팅 도입 여부** | **도입한다** | 블로커 #7을 닫는 유일 항목. ⚠️ **방침 반전 주의**: 현 방침 "미수집"(2026-07-06 정합)을 **"크래시 진단 데이터 최소 수집"으로 갱신**해야 함(공개 법무 문서 — WU-4 실행 시 사용자 최종 사인오프). 애널리틱스(Firebase)와 별개 — 애널리틱스는 계속 미수집 유지. |
| D3 | **ai-quality → ADR 흡수 (거버넌스 게이트)** | **승인**: `docs/ai-quality/` 이관 + **ADR-0007 신설**("AI 프롬프트·품질 정본") | 거버넌스면이라 승인 게이트였음 → 여기서 **접근 승인**. 실행 세션이 ADR-0007 초안 작성, 내용은 완성물 아침 리뷰(완화된 게이트)로 확인. |
| D4 | **GitHub Pages 배포 소스** | **GitHub Actions**(migrated `pages.yml` 사용, default-deny site/ 전용) | pages.yml이 `actions/deploy-pages` 기반. Settings→Pages→Source=GitHub Actions. |
| D5 | **출시 후 백로그 분리** | ROADMAP 내 "Later — 출시 후 백로그" 하위 제목(별도 md 아님) | 사용자 결정. ROADMAP=단일 인덱스 유지. #5·#6·#7·#8 소속. |
| D6 | **#2 Android 첫 배포 귀속** | M9 [외부]/[사람] 트랙 | 이관도 출시후도 아닌 **출시 그 자체**. 폐쇄테스트 20명×14일 게이트 = 출시 순서 이슈. |

⚠️ **미리 결정 불가(발견 시 개별 승인)**: WU-6 스윕 중 발견되는 **specs/ADR/거버넌스면 이관분**은 그때 제안+승인. WU-7 원본 repo 폐기는 스윕 전수 완료 후 **사람 최종 확인**.

---

## 2. 작업단위 (WU) — 독립 세션 단위

태그: `[AI]`=자동 실측·CI green · `[AI→사람]`=AI 지그→사람 컨펌 · `[사람→AI]`=사람 결정/승인 후 AI 실행 · `[사람]`=환원 불가(실기기·서명·심사).

### 이관·자기완결화 트랙

**WU-1 · GitHub Pages 실배포 `[AI→사람]` — 착수·⚠️ 보류(2026-07-10)** — M9 blocker 닫기
- 목표: devetym `site/`(방침·약관)를 실제 배포해 **공개 방침 URL** 확보.
- 스코프: `pages.yml`은 이미 이관·커밋됨(`315ea55`). ① pages.yml + site/를 main에 커밋·push(스택 규율상 별도 브랜치→PR) ② repo Settings→Pages Source=GitHub Actions 활성(`[사람]` 또는 `gh api`) ③ Actions 배포 성공 확인 ④ 방침 URL을 [스토어 라벨](../release/m9-store-metadata-draft.md)·site 링크에 반영.
- **✅ Phase A 완료(2026-07-10)**: `origin/main`(M8, `d8a7aaa`) 위 별도 브랜치 `chore/pages-deploy`에 `pages.yml` + `site/` 4파일만 얹어 **[PR #10](https://github.com/data-sy/devetym/pull/10)** 오픈(diff=인프라 5파일뿐·M9 스택 독립·미머지). site/ 시크릿 스윕 clean(방침·약관은 발행 의도 콘텐츠, 지원 이메일 `oddmuffinstudio@gmail.com` 정합).
- **⛔ Phase B 블로커(활성화 불가)**: `gh api -X POST repos/data-sy/devetym/pages -f build_type=workflow` → `422 Your current plan does not support GitHub Pages for this repository`. 원인 = repo **private** + 계정 **GitHub 무료 플랜**. **무료 플랜은 public repo만 Pages 허용**(private Pages는 Pro/Team/Enterprise). → **활성화 = ① public 전환**(사람·환원불가·시크릿/`local.properties`/keystore 스윕 선행) **또는 ② GitHub Pro**(private 유지·유료)에 의존. **사용자 결정으로 보류**.
- **재개 절차(택1 후)**: public 전환 or Pro 확정 → (public이면) 시크릿 스윕·전환 → PR #10 병합(push main → `pages.yml` 트리거) → Pages Source=Actions 확인 → 아래 DoD 검증 → 방침 URL 스토어 라벨 반영.
- DoD: `https://data-sy.github.io/devetym/privacy-policy/` 류 URL 200 응답. default-deny 확인(site/ 밖 미발행).
- 의존: **⚠️ public 전환 또는 GitHub Pro**(무료+private에선 활성화 자체가 막힘 — 2026-07-10 실측). 파일 스테이징(Phase A)은 완료.

**WU-2 · Scripts 파이프라인 이관 검증 `[AI]`**
- 목표: 이관한 `Scripts/{db-expand, prompt-probe}`가 devetym 컨텍스트에서 성립.
- 스코프: README 경로·상대참조 devetym 기준 수정, `requirements.txt` 확인, 스모크 실행(dry). db-expand=claude.ai 정액 수동 경로 유지(API 종량 회피 명시).
- DoD: 각 파이프라인 README가 devetym 경로로 정합, 최소 실행 가능 문서화.
- 의존: 없음(파일 이관 완료).

**WU-3 · ai-quality → ADR-0007 흡수 `[사람→AI, 승인됨 D3]`**
- 목표: AI 프롬프트·품질 정본을 devetym으로. iOS 검증본을 `commonMain`에 계승한 현 상태를 문서 정본화.
- 스코프: `~/dev-etymology/docs/ai-quality/`(6종: opening-prompt-v2·handoff-v1/v2·probe-analysis-v2·prompt-review-brief±v2) → `devetym/docs/ai-quality/`. **ADR-0007** 작성(시스템 프롬프트 버전·도구 스키마 3분기·prompt-review 결론 흡수, `_template.md` 준수). prompt-probe(WU-2)와 상호참조.
- DoD: docs/ai-quality/ 이관, ADR-0007 존재, ROADMAP/architecture에서 참조. 내용 = 아침 리뷰.
- 의존: WU-2(prompt-probe 참조) 느슨.

**WU-4 · 크래시 리포팅 도입 `[사람→AI, SDK=Sentry D1·D2]` — ✅ 완료(2026-07-10)**
- 목표: Sentry 배선 + 방침 갱신.
- **⚠️ D1 실측 정정 → 플랫폼 seam 분리(사람 결정)**: `sentry-kotlin-multiplatform`을 commonMain에 넣으니 `:shared:iosSimulatorArm64Test`(네이티브 테스트 실행파일 링크)가 `ld: framework 'Sentry' not found`로 깨짐 — 비cocoapods 정적 프레임워크(XcodeGen+SKIE) setup 탓(정적 앱-프레임워크 링크는 `-lsqlite3`처럼 통과, 테스트 실행파일은 완전 해석 필요). D1의 "commonMain 단일 배선" 전제가 iOS 네이티브 링크 현실과 충돌 → **seam 분리** 채택.
- **실행분(5축 green)**: ① commonMain `expect object CrashReporter`(Sentry 무참조) + androidMain actual=`io.sentry:sentry-android` 8.48.0(미포착 예외 핸들러) + iosMain actual=no-op(iOS는 Swift/Sentry Cocoa SPM·WU-11) ② `initKoin(platformModule, crashDsn)` 최상단 배선 ③ DSN 주입: Android=`BuildConfig.SENTRY_DSN`(gradle 프로퍼티/env, 비면 no-op·시크릿 미커밋) / iOS=Info.plist `SentryDsn`=`$(SENTRY_DSN)`(project.yml) ④ `sendDefaultPii=false`(방침 정합) ⑤ `CrashReporterTest` 2건(미설정 no-op·멱등, 양 플랫폼 실행) ⑥ **방침 갱신**(사인오프 완료): privacy-policy §2-2 신설·§3·§4 정합·스토어 라벨(Play/App Store) 반영.
- **iOS 잔여(WU-11 연계·출시 전 필수)**: iosMain no-op이라 iOS 실 크래시 미포착 → Swift/SPM으로 Sentry Cocoa 활성화([project.yml](../../iosApp/project.yml) 주석 절차). Android은 실 배선 완료.
- **근본 해결(출시 후)**: commonMain 단일 KMP 배선(cocoapods 등) → ROADMAP Later 백로그.
- 원장: [WU-4 ledger](26-07-10-wu4-crash-reporting-ledger.md). DoD 충족(방침/라벨 정합·5축 green 회귀 0). 사인오프=사용자 사전 승인(2026-07-10).

**WU-5 · launch-prep 대조·잔여 이관 `[AI]`**
- 목표: `docs/launch-prep/` 중 devetym `docs/release/`로 **미승계분만** 이관(중복 방지).
- 스코프: appstore-metadata 초안군 ↔ `m9-store-metadata-draft.md` 대조, `launch-consult-prompt.md`·`e2e-checklist.md` 유용성 판단 후 선별 이관.
- DoD: 파일별 이관/폐기 결정 기록, 미승계분 이관.
- 의존: 없음.

**WU-6 · DevEtym/ 네이티브 iOS 전수 스윕 `[사람→AI]` (step 6)**
- 목표: `~/dev-etymology/DevEtym/`(Xcode) 중 KMP `iosApp`에 **아직 안 넘어온 로직/자산** 발굴 → 파일별 이관/폐기 원장.
- 스코프: Features·Services·Utils·Assets.xcassets·Resources 전수. `.claude/`(agents·commands)·`LICENSE`·`README`도 대조. **자기완결성 최종 점검**(step 6): 코드/CI/자산이 dev-etymology를 런타임·빌드에 참조하는 곳 0인지.
- DoD: 이관/폐기 원장(파일별), 이관 필요분 실이관. ⚠️ specs/ADR/거버넌스 발견분은 개별 승인.
- 의존: WU-3·WU-5(문서 이관) 후가 깔끔(중복 회피).

**WU-7 · dev-etymology 폐기 `[사람]`**
- 목표: 스윕 전수 완료 확인 후 원본 repo 폐기.
- DoD: 사람 최종 확인. ⚠️ devetym 브랜치 보존 규율과 **별개**(원본 repo 얘기).
- 의존: WU-1~6 완료(잔여 이관 0 확인).

### 코드 갭 트랙 (순수 [AI]·병렬 가능)

**WU-8 · #9 클립보드 복사 UI 배선 `[AI]`**
- `copyToClipboard` seam(구현·유닛테스트 있음, dead code) 호출 UI 추가 — 상세 어원 블록 "복사" 어포던스. 4축 green.

**WU-9 · #10 Android 스플래시 배선 `[AI]`**
- `androidApp` splash theme 부재 → Android 12+ `windowSplashScreen`/core-splashscreen, iOS/디자인 자산 정합. 4축 green.

**WU-10 · #11 셸 배선 회귀 가드 `[AI]`**
- manifest `android:name` 실존/기동 CI 미검증(첫 기동 크래시 통과) → Robolectric `MainActivity`/`DevEtymApp` 인스턴스화 스모크 or manifest-vs-소스 정합 체크. iOS `-lsqlite3` 링크 갭과 동류.

### M9 잔여 트랙 (환원 불가 — 사람/외부)

**WU-11 · M9 실기기·시뮬 입력 잔여 `[사람]`** — iOS 시뮬 라이브 탭/idb 입력분·하드웨어 감각(실 메일·앱간 클립보드·실 DPI·햅틱·TalkBack/VoiceOver 실제스처). 접근성 실감사.
**WU-12 · 코드 서명·심사·스토어 게시 `[사람/외부]`** — + #2 Android 첫 배포 폐쇄테스트 게이트(20명×14일). 출시 순서(iOS 심사 직행 vs Android 코호트) 결정.

---

## 3. 의존·순서 지도

- **독립·즉시(병렬 OK)**: ~~WU-1~~(⚠️ **활성화 블로커 — public 전환/Pro 대기**, 파일 스테이징만 완료), WU-2, WU-5, WU-8, WU-9, WU-10.
- **결정 완료 후 실행**: WU-3(D3 승인됨 ✅), ~~WU-4~~(✅ 완료 2026-07-10 — D1을 seam 분리로 실측 정정, 방침 사인오프 완료. iOS 네이티브 활성화만 WU-11 연계).
- **후행**: WU-6(문서 이관 후) → WU-7(스윕 후, 사람) → WU-12(전부 후, 외부).
- 각 WU는 착수 시 자기 세부 핸드오프를 `docs/handoff/`에 만들어도 됨(이 문서는 방향·인덱스).

---

## 4. 미래 세션 사용법

1. 이 문서 §1(결정 원장)·§2(WU) 중 **하나의 WU 절**을 읽는다.
2. ROADMAP Now에서 해당 트랙 현재 상태를 정본으로 확인(이 문서와 충돌 시 ROADMAP·디스크 신뢰).
3. WU 실행 → 완료 시 ROADMAP 갱신 + 필요하면 완료 핸드오프 남김. 결정은 §1에 이미 있음(재논의 금지).
