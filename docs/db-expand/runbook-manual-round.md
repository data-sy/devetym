# 수동 라운드 런북 — claude.ai 2탭 흐름 (Phase 1 / Phase 6)

> DB 번들 확장의 **수동 생성 라운드** 진행 순서. Generator(탭 A) ↔ Critic(탭 B)를 claude.ai에서 왕복하고, 통과본을 Claude Code에 넘긴다.
> 전체 설계는 [`spec.md`](./spec.md) 참조. 이 문서는 "어느 순서로 무엇을 붙여넣느냐"만 다룬다.

## 한눈에 보기

```
[준비] 탭 A ← v2-batch.md   /   탭 B ← critic-v1.md   (둘 다 [격리 원칙] 포함본)
   │
   │  ┌──────── claude.ai 안에서만 왕복 (Claude Code 개입 X) ────────┐
   ▼  │                                                              │
탭 A (Generator)   keyword 10개 입력 → JSON 배열 생성                 │
   │                                                                 │
   ▼                                                                 │
탭 B (Critic)      배열 통째로 심사 → {passed, failed}               │
   │   └─ failed 있음 → fix_direction을 탭 A에 → 재생성 ─────────────┘  (최대 3회)
   │
   └─ failed: []  (B 통과)
   │
   │  ───── 여기서 처음으로 Claude Code 등장 (통과본 1회만) ─────
   ▼
validator.py (Claude Code)  결정론적 정량 최종 검사
   │   ├─ 걸림 → 그 항목만 탭 A에서 고쳐 다시 → validator 재확인
   │   └─ 통과
   ▼
round-NNN.json 저장 → Phase 2
```

- claude.ai 채팅(수동): **탭 A 생성·재생성 ↔ 탭 B 심사** — 루프를 채팅 안에서 끝낸다.
- Claude Code(코드): **통과본을 받아 validator로 결정론적 최종 확인 → 저장·이후 단계.**
- ⚠️ validator는 매 루프가 아니라 **B 통과 후 1회.** critic이 정량 룰도 겹쳐 보지만 글자 수 카운팅은 부정확하므로, 코드로 길이를 한 번 못 박는 용도.

## 재생성 루프는 **3회 고정** — 초과 시 멈춤

3회 안에 통과 못 하면 더 돌리지 말 것. 그건 entry 문제가 아니라 **프롬프트/룰 문제 신호** → Claude Code에서 프롬프트를 손본 뒤 라운드 재시작. (round-001에서 alias 룰 모순이 이렇게 잡혔다.)

---

## 순서대로

### 0. 준비 (라운드당 1회)
| 탭 | 시스템 지침 위치에 붙여넣을 것 | 출처 |
|---|---|---|
| **A · Generator** | `v2-batch.md`의 `---` 사이 본문 전체 | `Scripts/db-expand/prompts/v2-batch.md` |
| **B · Critic** | `critic-v1.md`의 `---` 사이 본문 전체 | `Scripts/db-expand/prompts/critic-v1.md` |

- 두 본문 모두 **[격리 원칙]** 포함본인지 확인 (다른 대화·세션·메모리·이전 배치 맥락 차단).
- 프롬프트를 고쳤으면 **반드시 새 본문으로 다시 깔기.** 기존 탭에 이어 말하지 말 것.
- ⏱ **스톱워치 시작** — 사람 손 시간은 Phase 7 자동화 트리거 판단 데이터.

> 1~2단계는 **claude.ai 안에서만** 돈다. Claude Code는 3단계(통과본)에서 처음 등장.

### 1. 탭 A — 생성
- keyword 리스트(예: `keywords-round-001.txt`의 10개)를 사용자 메시지로 입력.
- **출력 형태:** 순수 JSON **배열** `[ {...}, ... ]`. 객체 필드 = `keyword, aliases, category, summary, etymology, namingReason`.
- claude.ai가 ```` ```json ```` 펜스로 감싸면 **펜스만 떼어** 다음 단계로.

### 2. 탭 B — 심사 (+ 재생성 루프, 채팅 안에서)
- 탭 A 배열을 탭 B에 **통째로** 붙여넣기.
- **출력 형태:** 단일 JSON **객체**
  ```json
  { "passed": ["keyword1", ...],
    "failed": [ {"keyword","rule_id","reason","fix_direction"}, ... ] }
  ```
- ⚠️ **B는 고치지 않는다.** 채점지(판정)만 준다. 고치는 손은 항상 탭 A.
- `failed` 있으면 → `fix_direction`을 복사해 탭 A에 붙여넣어 "이대로 고쳐서 다시" → 새 배열로 다시 이 단계 (왕복 **최대 3회**).
- `failed: []` 되면 그 탭 A 최종 배열이 **통과본** → 3단계.

### 3. validator — 결정론적 최종 검사 + 전달 (Claude Code, 1회)
- **여기서 처음으로 Claude Code 등장.** 통과본 배열을 주면 `validator.py`로 길이·카테고리·null·alias 최소·keyword 형식을 코드로 정확히 검사.
- 걸리면 → 그 항목만 탭 A에서 고쳐 다시 → validator 재확인. (보통 길이 — critic이 글자 수를 잘못 센 케이스)
- 통과하면 전달·저장:
  - [ ] 통과본 JSON 배열 → `docs/db-expand/rounds/round-NNN.json`로 저장
  - [ ] ⏱ 사람 손 시간(분)
  - [ ] (선택) 라운드 중 관찰 — generator가 반복해 깬 패턴, critic이 못 잡은 것
- 이후 Claude Code가 Phase 2(일관성 점검) → Phase 3(머지·smoke test) 진행.

---

## 산출물 형태·이름 요약

| 단계 | 형태 | 이름/귀속 |
|---|---|---|
| 탭 A 출력 | JSON **배열** (객체 N개) | 통과 시 → `round-NNN.json` |
| 탭 B 출력 | JSON **객체** `{passed, failed}` | 루프용 중간 판정물 (별도 저장 X) |
| 최종 전달물 | 탭 A 통과 배열 + 사람 손 시간 | Claude Code → Phase 2 |

## 자주 빠뜨리는 것
- 펜스(```` ``` ````) 제거 안 하고 넘기기 — 두 프롬프트 다 "순수 JSON만".
- 프롬프트 수정 후 기존 탭에 이어 말하기 — 새 본문으로 다시 깔아야 반영됨.
- B에게 "고쳐줘"라고 하기 — B는 심사 전용. fix_direction을 A로 가져가야 함.
- 스톱워치 누락 — Phase 7 트리거 데이터가 비면 자동화 판단 근거가 사라짐.
