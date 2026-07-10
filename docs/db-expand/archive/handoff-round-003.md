# 핸드오프 — 번들 DB 수동 확장 라운드 (round-003~, 오케스트레이터+밸리데이터 세션)

> **새 세션 시작 한 줄:** *"`docs/db-expand/archive/handoff-round-003.md` 읽고 이어서 진행해줘."*
> 진행 상태 정본은 디스크 — 충돌 시 [`ROADMAP.md`](../../../ROADMAP.md) "Now" · [`spec.md`](../spec.md) · [`rounds/round-002.md`](../rounds/round-002.md)를 신뢰.

## 이 세션(Claude Code)의 역할

**오케스트레이션 + 코드 밸리데이션 공간이다. generator·critic을 직접 굴리지 않는다.**
- generator(생성)·critic(의미 검증)은 **사람이 claude.ai 탭에서 수동**으로 돌린다. 이 세션은 그 결과물을 받아 **코드 게이트(validator·scope_diff·merge·smoke)** 와 **발주·판정·문서 기록**을 한다.
- 비가역 작업(번들 swap·머지 커밋)은 **사람 승인 게이트**.

## 지금 어디까지 (2026-06-20)

- 번들 DB **550개**. round-001(→510)·round-002(→550) 종결·커밋. 브랜치 `feat/bundle-db-pre-launch-expand`.
- 목표 **N=650**. 잔여 **+100**. 다음 = **수동 round-003**(자료구조 76·DB 85 위주 보강, 기타 0).
- **Phase 7 방향 확정(2026-06-20): 자동화(API) 보류 → claude.ai 정액 수동 유지.** claude.ai 정액 한계비용 0 > API 종량제. 대량 확장(출시 후 수백 개) 시에만 재검토. → **API 전환 제안하지 말 것.**
- 도구: `Scripts/db-expand/{validator,merge,scope_diff}.py`. 프롬프트: `Scripts/db-expand/prompts/{v2-batch,critic-v2}.md`.

## ⭐ 사람 ↔ 세션 자료 주고받기 프로토콜 (핵심)

> 태그: **[세션]** = 이 Code 세션이 함 / **[사람-claude.ai]** = 사람이 claude.ai 탭에서 함 / **[사람-승인]** = 사람 결정.
> 버스는 **복붙**이다. 사람이 양쪽을 중계한다.

### 0. 탭 세팅 (라운드 시작 1회, 사람)
- **탭 A = Generator**: 일반 채팅. system instruction에 `prompts/v2-batch.md`의 **`---`와 `---` 사이 본문만** paste (위 `>` 주석 줄 절대 포함 금지).
- **탭 B = Critic**: **반드시 임시 채팅(Temporary chat)**. system에 `prompts/critic-v2.md`의 **`---` 사이 본문만** paste.
  - ⚠️ **격리 함정(round-002 실제 발생)**: .md를 주석부까지 통째 넣거나 일반챗/프로젝트에서 돌리면 메모리 오염 → critic이 판정(JSON) 대신 "탭 B가~/이전 라운드~" 같은 **메타 응답**을 함. 그러면 격리 깨진 것 → 임시챗 + 본문만으로 재실행.

### 1. keyword 큐레이션 + dedup  [세션]
- 이 세션이 `Scripts/db-expand/keywords-round-003.txt` 신규 작성(40개 안팎, 코어 위주·자료구조/DB 보강·기타 0).
- dedup: 기존 `terms.json`의 `{keyword} ∪ {모든 alias}` 차집합. **완전매칭 기준으로**(공백·하이픈 정규화 비교는 false-positive 유발 — round-002 교훈. BundleDBService 검색이 완전매칭이라 그 의미와 맞춤. 정규화 비교는 informational로만).
- 산출: keyword **JSON 배열**(`["a","b",...]`)을 사람에게 건넨다.

### 2. 생성  [사람-claude.ai 탭 A]
- 사람이 **1의 JSON 배열만** 탭 A에 paste(파일명·설명 붙이지 말 것).
- 탭 A가 **JSON 배열**(entry들)로 응답. 코드펜스·설명문 있으면 "배열만 다시" 한마디.
- 사람이 **그 배열을 이 세션에 그대로 복붙**.

### 3. validator (코드 1차 게이트)  [세션]
- 이 세션이 받아서 `python validator.py <배치.json>` → `{passed, failed}`.
- `failed` 있으면: 이 세션이 **깨진 keyword + 구체 fix(글자수 등)** 를 정리해 사람에게 줌 → 사람이 탭 A에 넣어 **그 keyword만 재생성**(나머지 손대지 말라 명시 = scope leak 방지) → 재생성분을 다시 이 세션에 복붙 → 재검.
- **validator 100% 될 때까지 반복(최대 3 cycle).** 통과분 합본을 이 세션이 만든다.
- (round-002 관찰: generator가 summary 20자 하한을 상습 미달. 재생성 지시에 "범위 한가운데 노려라(summary 23~27자)" 넣으면 왕복 줄어듦.)

### 4. critic-v2 (의미 검증)  [사람-claude.ai 탭 B, 격리]
- 이 세션이 **validator 통과한 전체 배열**을 사람에게 줌.
- 사람이 **탭 B(임시챗)** 에 paste → 응답 `{passed, failed}` JSON.
- 사람이 그 JSON을 이 세션에 복붙. **순수 JSON이면 격리 성공.** 메타 섞이면 오염 → 재실행(위 경고).
- `failed` 있으면: fix_direction을 탭 A에 → 해당 keyword만 재생성 → 3(validator) → 4(critic) 다시.

### 5. scope_diff + merge + smoke  [세션]
- 재생성 있었으면 `scope_diff.py`로 scope_leak 0 확인.
- alias 레벨 충돌 전수 재검(신규 alias vs 기존 keyword/alias, **완전매칭**). 실충돌이면 신규에서 제거, false-positive면 유지.
- `merge.py existing batch terms.next.json` → 550+N.
- 코드 레벨 smoke: BundleDBService 디코딩·검색(keyword/alias/autocomplete) 결정론 재현(이 워크트리 비밀파일 부재 → 풀 빌드 대신 이걸로 충분, handoff 규칙).

### 6. 비가역 swap + 커밋  [사람-승인 → 세션]
- 이 세션이 무손실 검증(기존 무변경·소실 0) 후 **사람 승인 받고** `terms.next.json → terms.json` swap.
- 커밋: Conventional, **scope 없이**, **Co-Authored-By 금지**, 작성자 본인.

### 7. 기록  [세션]
- `rounds/round-003.{json,md}` 작성(발주안 + 측정·관찰·판정), `ROADMAP.md` "Now" 갱신.
- 측정 누적: 통과율·cycle 수·사람 손 시간·critic 고유 검출 추이(round-001·002 = 0).

## 가장 먼저 할 것 (새 세션)

1. 위 정본(ROADMAP·spec·round-002.md) 읽고, **이 주고받기 프로토콜을 사람에게 먼저 한 번 풀어 설명**한다(사람이 뭘 어디에 붙이고 뭘 돌려주는지).
2. 이어서 **round-003 발주안**(batch 크기·카테고리 배분·게이트)을 짜서 사람에게 확정받는다 — 자료구조(long pole)·DB 우선, 기타 0. round-002 개선(완전매칭 dedup·critic 임시챗·summary 하한 여유) 반영.
3. 확정되면 1번 큐레이션부터 위 루프 시작.

## 규칙 요약

- generator·critic은 사람-claude.ai 수동. 이 세션은 코드 게이트·발주·판정·기록. **API 자동화 제안 금지**(정액 수동 유지 결정).
- critic은 **임시챗 + 본문만**(격리). 응답이 메타면 오염.
- dedup·충돌은 **완전매칭** 기준.
- 비가역(swap·커밋)은 사람 승인. 커밋 scope 없이·co-author 없이.
- 진행 상태 정본은 디스크. 메모리엔 시간 안 타는 사실·교훈만.
