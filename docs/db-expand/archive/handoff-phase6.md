# 핸드오프 — 번들 DB 확장: Phase 6(본 확장 batch)부터

> **사용법.** 새 세션에서 *"이 파일(`docs/db-expand/archive/handoff-phase6.md`) 읽고 이어서 진행해줘"* 라고 하면 된다.
> 진행 상태의 정본은 디스크다 — 충돌 시 이 핸드오프가 아니라 [`ROADMAP.md`](../../../ROADMAP.md) "Now"와 [`spec.md`](../spec.md), [`rounds/round-001.md`](../rounds/round-001.md)를 신뢰할 것.

## 지금 어디까지 (2026-06-19)

- 브랜치: `feat/bundle-db-pre-launch-expand` (미PR)
- **round-001 종결.** 번들 DB **500 → 510** 머지·커밋 완료 (`e11cf15`). `keyword.lower()` 알파벳 정렬, 기존 무손실.
- **Phase 2A** PASS / **Phase 2B** 임계값 FAIL이나 drift 원인 식별 → 결정 (a)로 머지. **Phase 3 iOS smoke PASS**.
- **Phase 4 마감.** 목표 **N = 650** 확정(사람, 2026-06-19). 현 510 → **+140**. 추가분은 **기타 제외**, 5개 코어 카테고리에 배분해 코어 ≈100 동등화(자료구조 64가 long pole). 자연 도달 시 기타 비중 26.9%→21.1%. spec Done 신호 갱신 완료.
- **Phase 5 산출물 작성 완료(실통과 미확인).**
  - `prompts/critic-v2.md` — critic-v1에서 정량 룰(validator 중복분) 전부 제거, nuanced 4종만(`ALIAS_STRICT`·`ETYMOLOGY_FACT`·`NAMING_COHERENCE`·`NAMING_CLOSING`). 근거: round-001 critic 고유 검출 0.
  - `scope_diff.py` — 재생성 scope leak 검출(`expected/actual_changed`·`scope_leak`·`missing_change`·`added`·`removed`·`clean`). aliases 정렬 비교. 기능검증 6/6. clean이면 exit 0, 아니면 2.

## 정본 문서 (이걸 읽고 신뢰)

| 무엇 | 경로 |
|---|---|
| 마스터 상태·체크리스트 | [`ROADMAP.md`](../../../ROADMAP.md) "Now" (현재 다음=Phase 6) |
| 단계 상세·Phase 6 | [`spec.md`](../spec.md) (Phase 6 섹션, Done 신호 N=650) |
| round-001 회고·목표 N 산정 | [`rounds/round-001.md`](../rounds/round-001.md) |
| 도구 | `Scripts/db-expand/{validator.py, merge.py, scope_diff.py, consistency_a.py, consistency_a_robust.py, api_sample.py}` |
| 프롬프트 | `Scripts/db-expand/prompts/{v2-batch.md, critic-v1.md, critic-v2.md}` |

## 다음 작업: Phase 6 — 확장 batch (30~50 keyword) [혼합]

목적: 목표 N=650을 향해 첫 본 확장 라운드(round-002) 진행. **새 흐름을 실제로 통과시켜 Phase 5 Done도 함께 충족.**

새 흐름: `Generator → validator → critic(v2) → 재생성 → scope_diff → 머지`

### 착수 순서 (Phase 0-1을 round-002용으로 다시)
1. **[AI] keyword 큐레이션** — `Scripts/db-expand/keywords-round-002.txt` 신규 작성. 기존 후보 소진(`keywords.txt`·`keywords_to_add.md`·`keywords-round-001.txt` 전부 머지/사용됨). **기타 비중↓** 위해 5개 코어 카테고리 위주, 특히 자료구조(현 64, long pole) 보강. 30~50개.
2. **[AI] dedup** — 기존 `terms.json`(510)의 `{keyword}` ∪ `{모든 alias}` 차집합으로 충돌 제거.
3. **[사람] Generator/Critic 2탭 실행** — 탭 A=`prompts/v2-batch.md`, 탭 B=**`prompts/critic-v2.md`**(v1 아님). 절차: Generator → validator(정량 1차) → critic(v2, nuanced) → 재생성(최대 3회) → 통과.
4. **[AI] scope_diff** — 재생성 있었으면 `python scope_diff.py before.json after.json <failed_keywords>`로 scope leak 0 확인.
5. **[AI] merge + iOS smoke** — `merge.py`로 `terms.next.json` 생성(충돌 0 assert), smoke test, swap+커밋.
6. **[AI] round-002.md 기록** — 통과율·재시도·사람 손 시간·API 비용(2B 샘플)·분포(카테고리·길이)·critic 고유 검출 누적.

### Phase 6 측정 (라운드별 누적 — Phase 7 판단 입력)
- 최종 통과율 / 평균 재시도 / 라운드당 사람 손 시간 / Phase 2B 샘플 항목당 API 비용·latency.
- critic-v2 고유 검출 추이(round-001은 0) — 계속 0이면 critic 추가 축소/제거 신호.
- 길이 카운팅 오차 누적 관찰(표본 작아 round-001은 신호 약함).

## Phase 6 이후

- **Phase 7 자동화** (트리거 충족 — 사람 손 시간 ≈60분>5분). loop: `Generator(API) → validator → 재생성 → critic(v2) → scope_diff → 머지`.
  - **필수 설계 근거(2B)**: API 단발은 길이 룰 비순응(validator 1/10) — round-001 품질은 validator→재생성 루프 산물. 그래서 loop에 `validator→재생성`이 필수. `api_sample.py`로 공정 재검 가능. API 키는 `Scripts/db-expand/.env.local`(gitignore). 모델 id는 `claude-api` 스킬로 재확인(현 production = `claude-sonnet-4-6`).
  - claude.ai 정액 vs API 전환 손익은 2B 비용 데이터 + Phase 6 누적으로 판단.
- 목표 N=650 도달 시 DB 확장 마일스톤 Done → ROADMAP Done 이동.

## 규칙 (CLAUDE.md / 사용자)

- 커밋: Conventional Commits, **scope 없이**, **Co-Authored-By 트레일러 금지**, 작성자 본인만.
- 진행 상태 정본은 디스크(ROADMAP·round 문서·spec). **메모리에 status 쓰지 말 것.**
- 전문가 에이전트엔 해답 박지 말고 문제·제약만 주고 진단·구현하게 할 것.
- iOS 빌드 시 이 워크트리엔 로컬 비밀파일(`Config.xcconfig`·`GoogleService-Info.plist`)이 없음 — 필요 시 더미로 빌드 통과(둘 다 gitignore). smoke는 BundleDBService 디코딩·검색 경로 결정론 검증으로도 충분.

## 가장 먼저 할 것

round-002 keyword 큐레이션(`keywords-round-002.txt`) 착수 — 코어 카테고리 위주(자료구조 보강), 30~50개. 작성 후 dedup → 사용자에게 2탭 실행을 넘긴다. (목표 N·DB swap은 이미 결정/진행된 지점이므로 그대로 진행, 단 swap·커밋은 smoke 통과 후.)
