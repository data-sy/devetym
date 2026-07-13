# 핸드오프 — 번들 DB 수동 확장 라운드 (round-004~, 오케스트레이터+밸리데이터 세션)

> **새 세션 시작 한 줄:** *"`docs/db-expand/archive/handoff-round-004.md` 읽고 이어서 진행해줘."*
> 진행 상태 정본은 디스크 — 충돌 시 [`ROADMAP.md`](../../../ROADMAP.md) "Now" · [`spec.md`](../spec.md) · [`rounds/round-003.md`](../rounds/round-003.md)를 신뢰.

## 이 세션(Claude Code)의 역할

**오케스트레이션 + 코드 밸리데이션 공간이다. generator·critic을 직접 굴리지 않는다.**
- generator(생성)·critic(의미 검증)은 **사람이 claude.ai 탭에서 수동**으로 돌린다. 이 세션은 그 결과물을 받아 **코드 게이트(validator·scope_diff·merge·smoke)** 와 **발주·판정·문서 기록**을 한다.
- 비가역 작업(번들 swap·머지 커밋)은 **사람 승인 게이트**.

## 지금 어디까지 (2026-06-20)

- 번들 DB **590개**. round-001(→510)·round-002(→550)·round-003(→590) 종결·커밋. 브랜치 `feat/bundle-db-pre-launch-expand`.
- 목표 **N=650**. 잔여 **+60**. 다음 = **수동 round-004**(자료구조 long pole 우선, 기타 0).
- **Phase 7 방향 확정: 자동화(API) 보류 → claude.ai 정액 수동 유지.** → **API 전환 제안하지 말 것.**
- 도구: `Scripts/db-expand/{validator,merge,scope_diff}.py`. 프롬프트 **복붙 전용**: `Scripts/db-expand/prompts/{v2-batch,critic-v2}.paste.md` (격리 지시 단락 + 본문, 주석부 제외 — round-003 신설).

## round-003에서 확정된 운용 (round-004도 그대로)

- **dedup은 완전매칭 기준** (정규화 비교는 informational만). round-002 false-positive 교훈.
- **critic 격리는 `*.paste.md` 복붙**: 탭 B는 임시챗 + `critic-v2.paste.md` 전체 붙여넣기. 응답이 순수 JSON 아니고 메타면 오염 → 재실행.
- **summary 하한 보정 유효**: `v2-batch.paste.md`에 "23~27자 한가운데" 명시됨 → cycle1 통과율 크게 개선(round-003 39/40).
- **critic 유지**: round-003에서 critic 고유 검출 1건(의미 오역, validator 미커버) → 축소/제거 보류, 유지.

## ⭐ 사람 ↔ 세션 자료 주고받기 프로토콜 (핵심)

> 태그: **[세션]** = 이 Code 세션 / **[사람-claude.ai]** = 사람이 claude.ai 탭 / **[사람-승인]** = 사람 결정.
> 버스는 **복붙**. 사람이 양쪽을 중계한다.

### 0. 탭 세팅 (라운드 시작 1회, 사람)
- **탭 A = Generator**: 일반 채팅. system에 `prompts/v2-batch.paste.md` **전체** paste.
- **탭 B = Critic**: **반드시 임시 채팅(Temporary chat)**. system에 `prompts/critic-v2.paste.md` **전체** paste.
  - ⚠️ 격리 함정: 일반챗/프로젝트에서 돌리거나 메모리 오염되면 critic이 판정(JSON) 대신 메타 응답 → 임시챗 재실행.

### 1. keyword 큐레이션 + dedup  [세션]
- 이 세션이 `Scripts/db-expand/keywords-round-004.txt` 신규 작성(코어 위주·자료구조 최우선·기타 0).
- dedup: 기존 `terms.json`(590)의 `{keyword} ∪ {모든 alias}` **완전매칭** 차집합.
- 산출: keyword **JSON 배열**을 사람에게 건넨다.

### 2. 생성  [사람-claude.ai 탭 A]
- 사람이 1의 JSON 배열만 탭 A에 paste → 탭 A가 entry **JSON 배열**로 응답 → 그 배열을 이 세션에 복붙.

### 3. validator (코드 1차 게이트)  [세션]
- `python validator.py <배치.json>` → `{passed, failed}`. failed면 깨진 keyword + 구체 fix를 사람에게 → 탭 A에서 **그 keyword만** 재생성(scope leak 방지) → 재검. **100%까지 반복.**

### 4. critic-v2 (의미 검증)  [사람-claude.ai 탭 B, 격리]
- validator 통과 전체 배열을 사람이 탭 B(임시챗)에 paste → 응답 `{passed, failed}` JSON을 이 세션에 복붙. 메타 섞이면 오염 → 재실행.
- failed면 fix_direction을 탭 A에 → 해당 keyword만 재생성 → 3 → 4 다시.

### 5. scope_diff + merge + smoke  [세션]
- 재생성 있었으면 before 스냅샷 만들어 `scope_diff.py before after <failed_keywords>` → scope_leak 0.
- alias 전수 재검(신규 vs 기존, **완전매칭**). merge.py → 590+N. BundleDBService 디코딩·검색 smoke(워크트리 비밀파일 부재 → 풀 빌드 대신 이걸로 충분).

### 6. 비가역 swap + 커밋  [사람-승인 → 세션]
- 무손실 검증(기존 무변경·소실 0) 후 **사람 승인** → `terms.json` swap. 커밋: Conventional, **scope 없이**, **Co-Authored-By 금지**, 본인.

### 7. 기록  [세션]
- `rounds/round-004.{json,md}` 작성, `ROADMAP.md` "Now" 갱신, **다음 핸드오프(round-005)** 필요 시 작성. 측정 누적: 통과율·cycle 수·critic 고유 검출 추이(001·002=0, 003=1).

## 가장 먼저 할 것 (새 세션)

1. 정본(ROADMAP·spec·round-003.md) 읽고, 주고받기 프로토콜을 사람에게 한 번 풀어 설명.
2. **round-004 발주안**(batch 크기·카테고리 배분·게이트)을 짜서 사람에게 확정받는다 — 자료구조 최우선(87→103 +16). 잔여 +60이라 batch 60으로 **마지막 라운드**도 가능(또는 40+20 두 라운드). 균등화 목표치(코어 각 ~103): 자료구조 +16·동시성 +13·패턴 +12·DB +11·네트워크 +10.
3. 확정되면 1번 큐레이션부터 루프 시작.

## 규칙 요약

- generator·critic은 사람-claude.ai 수동. 이 세션은 코드 게이트·발주·판정·기록. **API 자동화 제안 금지.**
- critic은 **임시챗 + `critic-v2.paste.md` 전체**(격리). dedup·충돌은 **완전매칭**.
- 비가역(swap·커밋)은 사람 승인. 커밋 scope 없이·co-author 없이.
- 진행 상태 정본은 디스크. 메모리엔 시간 안 타는 사실·교훈만.
