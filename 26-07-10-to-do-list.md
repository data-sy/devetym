# 2026-07-10 To-Do (작업 트래킹용 · 커밋 안 함)

> devetym이 dev-etymology 의존 없이 **자기 완결적**이 되게 만드는 스윕 + 리모트 공개 + M9 잔여 마감.
> 상태 정본은 ROADMAP. 이 파일은 이번 세션 워킹 체크리스트(끝나면 폐기 가능).
>
> **⚙️ 세션 성격 전환(2026-07-10)**: 나머지 WU는 이 세션에서 실행하지 않는다. **계획·설계 + 결정/승인 확정**만 하고,
> 독립 작업단위(WU-1~12)로 끊어 **여러 미래 세션에서 핸드오프로 실행**한다.
> **계획·결정 원장 정본 = [`docs/handoff/26-07-10-selfcontained-migration-plan.md`](docs/handoff/26-07-10-selfcontained-migration-plan.md).**

## 순서

1. [x] **리모트 브랜치에 올리기** — ✅ private `data-sy/devetym` 생성 → 전 브랜치 11개 push → `m1`~`m8` 스택 PR(#1~8) merge-commit 순차 병합(main=M8) → m9 draft PR #9(미머지). 브랜치 전부 보존.
2. [x] **dev-etymology 이관 백로그 생성 (Now 승격)** — ✅ ROADMAP Now에 "이관·자기완결화 트랙" 신설.
3. [x] **기존 백로그 중 이관 해당분을 2번 하위로 이동** — ✅ #1·#3·#4·#12 이관 트랙 하위로.
4. [x] **자기완결성 확보 백로그도 2번 하위로** — ✅ pages.yml·prompt-probe·launch-prep 대조 등 신규 self-completeness 항목 편입.
5. [~] **2~4 모은 백로그 진행** — 안전 이관분만 실행, 나머지는 WU로:
   - [x] `[AI]` pages.yml → `.github/workflows/` 이관. → 실배포는 **WU-1**.
   - [x] `[AI]` Scripts/{db-expand, prompt-probe, generate_db.py} 이관. 검증=**WU-2**.
   - [x] `[AI]` docs/db-expand 이관.
   - [→WU-5] launch-prep/e2e-checklist 대조.
   - [→WU-3] ai-quality → ADR-0007 (**D3 승인 완료**).
   - [→WU-4] 크래시 리포팅 (**D1 SDK=Sentry KMP·D2 도입 확정**).
6. [→WU-6] **미기재 잔여 점검** — DevEtym/ 네이티브 iOS 전수 스윕.
7. [→WU-11·12] **M9 잔여 마감** — 실기기/서명/심사(환원 불가).

## 병행 코드 갭 → WU-8·9·10 ([AI], 미래 세션)
- [→WU-8] #9 클립보드 UI 배선 · [→WU-9] #10 스플래시 · [→WU-10] #11 셸 배선 회귀 가드

## 결정/승인 (이 세션에 확정 — 원장 = 핸드오프 §1)
- **D1** 크래시 SDK = **Sentry KMP** (commonMain 단일 배선·공식 KMP SDK). Crashlytics override 가능.
- **D2** 크래시 리포팅 **도입** → ⚠️ 방침 "미수집"→"크래시 진단 최소수집" 갱신(WU-4, 사인오프).
- **D3** ai-quality → **ADR-0007 흡수 승인**.
- **D4** Pages 소스 = GitHub Actions. **D5** 출시후 = ROADMAP 하위제목. **D6** #2 = M9 [외부].

## 추가 할 일 (2026-07-10 추가)

- [ ] `[사람]` **리모트 안전 확인 후 public 전환** — private `data-sy/devetym`가 안전하게 올라간 것 확인되면(빌드/자산/시크릿 노출 점검 후) repo를 public으로. ⚠️ 되돌리기 어려움(인덱싱·캐시) → 전환 전 시크릿·`local.properties`·키 노출 스윕 먼저. (ROADMAP 브랜치 전략의 "public 전환은 추후 사람 결정"과 정합.)
- [ ] `[AI]` **운영 문서 최신화 `/refresh-ops-docs`** — 로드맵·백로그·핸드오프·**README 상태부**를 실제 상태(git·파일트리)와 대조해 stale 제거. ※ 별도 "README 정리" 항목 없음 — README 상태부는 이 명령이 커버(거버넌스·specs는 손 안 대고 승인 요청). 이번 세션 대량 변경(원격 생성·이관·트랙 재편) 반영 겸 한 번 돌리면 좋음.

## 병행: 진행할 버그성 백로그 (이관 아님)

- [ ] #9 클립보드 복사 액션 UI 배선 (dead code)
- [ ] #10 Android 스플래시 화면 배선
- [ ] #11 셸 배선 회귀 가드 (CI/Test)

## 출시 후로 미룰 것 (ROADMAP에서 분리)

- #5 번들 DB 추가 확장 · #6 AI 스트리밍 · #7 프롬프트 서버 이전 · #8 디자인 후속

## 열린 결정 (진행 전 확정)

- [ ] #2 Android 첫 배포 위치 — 이관/출시후 어디도 아닌 **출시 게이트**로 보임 → M9 [외부] 트랙?
- [ ] 출시 후(5·6·7·8) 분리 방식 — ROADMAP 내 "출시 후" 하위 제목 vs 별도 md
- [ ] GitHub repo 공개 범위 — private / public (ROADMAP "그때 결정")

---

## 기존 12 백로그 → 렌즈 매핑

| # | 항목 | 처리 |
|---|------|------|
| 1 | 크래시 리포팅 | → 이관 백로그 하위 |
| 2 | Android 첫 배포 | ⚠️ 열린 결정 (출시 게이트?) |
| 3 | AI 품질 문서 이관 | → 이관 백로그 하위 |
| 4 | db-expand 파이프라인 이관 | → 이관 백로그 하위 |
| 5 | 번들 DB 추가 확장 | → 출시 후 |
| 6 | AI 스트리밍 도입 | → 출시 후 |
| 7 | 프롬프트 서버 이전 | → 출시 후 |
| 8 | 디자인 후속 | → 출시 후 |
| 9 | 클립보드 UI 배선 | → 지금 진행 |
| 10 | Android 스플래시 | → 지금 진행 |
| 11 | 셸 배선 회귀 가드 | → 지금 진행 |
| 12 | dev-etymology 스윕 후 폐기 | → 이관 백로그 하위(=스윕 자체) |
