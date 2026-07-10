# DB 번들 확장 — 실행 스펙

> 번들 `DevEtym/DevEtym/Resources/terms.json` 확장. 기존 `Scripts/generate_db.py` 파이프라인의 deterministic 부분(validator·merge)은 재사용하고, 생성 방식만 Reflexion 패턴(Generator + Critic) 기반 claude.ai chat batch로 전환.

## 목표

현재 500개 entry → 출시 전 목표 크기 도달. 품질 게이트:
- 정량 룰(길이·카테고리·null·alias·keyword 형식) 100% 통과 (deterministic validator)
- LLM critic의 nuanced 룰 통과 (alias 의미·etymology 사실성·naming coherence)
- chat ↔ API 일관성 통과

## 현재 상태 (2026-05-26)

- **번들 DB**: `DevEtym/DevEtym/Resources/terms.json` (500개 entry)
- **카테고리 분포**: 동시성·자료구조·네트워크·DB·패턴 각 ~70개대, **기타 137개로 편중**. 신규 후보 큐레이션 시 분포 보정 고려.
- **스키마**: `keyword, category, summary, etymology, namingReason, aliases(필수, 최소 1개, 한글 표기 alias 최소 1개)`
- **길이 규약(스펙 확정)**: summary 20~30자 / etymology 60~120자 / namingReason 150~**270**자
  - ⚠️ `generate_db.py` SYSTEM_PROMPT는 namingReason 150~300자. 본 스펙은 v2 acceptance 기준 270자로 통일. batch variant fork 시 갱신.
- **기존 인프라** (재사용 대상):
  - `Scripts/generate_db.py` — dedup, 정량 validator(길이 제외), merge, API batch call 구현됨. 500개 전부 이걸로 생성.
  - `Scripts/prompt-probe/` — v2 acceptance probe 측정 하네스 (앱 runtime tool prompt 전용).
- **소진된 입력**:
  - `Scripts/keywords.txt` 300개 — 전량 terms.json에 머지됨. 신규 batch엔 사용 불가.
  - `Scripts/keywords_to_add.md` — 200→500 라운드 기록. 참조용.

## Done 신호

- [x] terms.json 목표 크기 도달 (**목표 N = 650 도달 2026-06-20**, round-004 종결). 510→550→590→**650**. Phase 6 확장 종결.
  - **배분 원리**: 추가분은 **기타(현재 137) 제외**, 5개 코어 카테고리(동시성·자료구조·네트워크·DB·패턴)에 배분해 코어를 ≈100개 동등선으로 끌어올린다. 자료구조(64)가 long pole. 자연 도달 시 기타 비중 26.9%→21.1%.
  - **근거**: round throughput(round-001 = 10kw/2cycle, 룰 설계 일회성 비용 제외) + critic-v2(정량 룰 제거)·룰 안정화로 keyword당 손 시간↓ + Phase 7 자동화(트리거 충족)로 도달 가능. 수동 Phase 6 ~1라운드 + 자동화로 +140 흡수. 산정 상세: `rounds/round-001.md` "목표 N 결정".
- [ ] 모든 신규 entry deterministic validator 통과 (round-001부터 적용)
- [ ] 라운드별 측정 데이터(통과율·재시도·사람 손 시간) 누적
- [ ] iOS smoke test 통과 (Phase 3 체크리스트)

## 확정 결정

- **Generator 위치**: claude.ai 채팅 탭. 구독 정액 안에서 batch 생성, API 토큰 비용 회피.
- **Critic 위치**: Phase 1·6은 claude.ai 탭. Phase 5에서 정량 룰은 validator에 이미 있으니 critic은 nuanced 룰 전담으로 축소.
- **`Scripts/generate_db.py` 처리: 부분 흡수 (옵션 C)**:
  - `validate()`, `load_existing()`, merge·점진 저장 로직을 `Scripts/db-expand/`로 fork (검증된 코드 재사용).
  - 생성 방식(`call_claude` API batch)은 Reflexion(chat) 흐름으로 대체.
  - `generate_db.py`는 round-001 머지 완료 후 deprecate 표시(주석) → Phase 7 자동화 진입 시 또는 그 직전에 삭제.
- **Batch generation prompt 출처**: `generate_db.py`의 `SYSTEM_PROMPT` (line 50-87)를 base로 fork. **runtime tool prompt(`build_prompt(...)`)는 사용하지 않음** — tool-call 강제 + 단일 entry 응답용이라 batch와 정면 충돌.
- **`prompts.components` import 제한**: `GOAL_AND_TOOL_SECTION`과 `NULL_GUARD_EXTRA`는 batch에 import 금지 (둘 다 `return_term_entry` 도구 호출 강제). 재사용 가능한 atom은 `PERSONA`와 `ACCURACY_AND_CATEGORY`뿐. v2 acceptance의 `ALIAS_STRICT_EXTRA`·`CLOSING_EXTRA`는 텍스트 발췌해서 batch prompt에 통합. `THINKING_BLOCK_SELFCHECK`는 chat에서 thinking이 안 보여 검증 불가 → Phase 2B(API 비교)에서 차이 측정 후 결정(Phase 0-5 참조).
- **Validator scope**: 신규 batch entry 전용. 머지된 `terms.next.json` 전체엔 적용하지 않음 (기존 500개는 grandfather).
- **기존 500개 length 비순응 처리: grandfather (재생성 안 함)**. 현재 500개를 새 길이 룰(summary 20~30 / etymology 60~120 / namingReason 150~270)로 재면 **비순응률 79.2% (전수 실측: 통과 104/500 = 20.8%; 위반 분포 ETYMOLOGY_LEN 313 / SUMMARY_LEN 233 / NAMING_LEN 226)** — `generate_db.py`의 기존 validate()가 길이를 검사하지 않았기 때문. (사전 추정 "47~63%"은 과소평가였음 — round-001 전수 교차검증으로 정정, `rounds/round-001-consistency-A.md`.) 본 마일스톤 목표는 "확장"이지 "재생성"이 아님. → **2-tier DB**(legacy 500 + 신규 compliant) 의도된 상태로 회귀 사유 아님. 단 실측 비순응이 추정보다 높아 **legacy backfill 우선순위 상향 근거**. backfill은 별도 작업으로 분리.
- **재생성 루프 한계**: 3회 고정.
- **alias 룰 충돌 해소 (v2.1, round-001 POC 발견)**: `RULE_ALIAS_MIN1`(한글 alias 필수) ∧ `RULE_ALIAS_STRICT`(번역 금지)가 음차 없는 용어(priority-inversion·persistent-data-structure·sni·hsts 등 — 한글 형태가 표준 번역어뿐)에서 **동시 충족 불가 모순**. Generator·Critic 두 탭이 독립적으로 같은 진단 도달. → STRICT에 **(4) 정착된 한국어 이름** 범주 추가. 판별 축을 "음차 유무"가 아닌 **"이름이냐 설명이냐"**로 잡음 — 그 개념을 부르는 표준 명칭(음차 병기 가능, 예: "커맨드 패턴"+"명령 패턴")은 허용, 서술적 풀이·정의·상위개념·한정수식어 변형은 계속 금지. 근거: BundleDBService가 alias로 검색하는 한국어 사전이라 정당한 동의어 보존 = 검색 적중률. `v2-batch.md`·`critic-v1.md` 동일 문구 동기화 + jpa few-shot closing 위반 교체. validator는 무변경(STRICT는 critic 전담). round-001은 이 룰로 재실행.
  - **v2.1.1 후속(round-001 critic 결과)**: 닫힌 화이트리스트("4범주 중 하나에만")가 "이름이냐 설명이냐" 원칙과 충돌 — merkle-tree의 "hash tree"처럼 정식 영어 동의어(=또 다른 이름)가 4범주 밖이라 false-positive. → 범주 목록을 **닫힌 화이트리스트가 아닌 예시**로 강등하고 원칙이 지배하게, **(5) 다른 언어의 정식 동의어** 허용 명시. 정식 동의어 보존 = alias 검색 적중률.
- **Phase 7 자동화 트리거**: 라운드당 사람 손 시간 > 5분 OR batch size > 50 OR 추가 라운드 3회 이상 계획.

## 명시적 비-목표

- Map-reduce 패턴 (단일 generator로 50 keyword 안전 처리되면 불필요)
- Specialized critic chain (length critic + category critic 분화 — deterministic validator로 충분)
- Multi-agent debate

---

## 두 prompt의 구분 (혼동 방지)

| 이름 | 위치 | 용도 | 출력 형태 |
|---|---|---|---|
| Runtime tool prompt | `Scripts/prompt-probe/prompts/build.py` → `build_prompt(False,False,True)` | iOS 앱 안에서 사용자 단일 keyword query 처리 | tool-call (`return_term_entry` 등) |
| Bundle generation prompt | `Scripts/generate_db.py` `SYSTEM_PROMPT` (line 50-87) | 번들 DB batch 생성 (현재까지 500개 만든 prompt) | JSON array (텍스트) |
| **Batch variant (신규)** | `Scripts/db-expand/prompts/v2-batch.md` (Phase 0 산출물) | claude.ai 탭 paste용 batch generation, v2 acceptance 강화 룰 통합 | JSON array (텍스트) |

본 스펙에서 "v2 production prompt"라는 모호한 표현은 사용하지 않음.

---

## 실행 단계

각 phase 주체: **[수동]** = claude.ai 채팅 작업, **[코드]** = Claude Code 작업, **[혼합]** = 둘 다.

### Phase 0 — Prerequisites [코드]

#### 0-1. 후보 keyword 큐레이션
- `Scripts/keywords.txt`·`keywords_to_add.md`는 소진됨 — **새 후보 리스트 신규 작성** (`Scripts/db-expand/keywords-round-NNN.txt`)
- 카테고리 분포 보정: 기존 기타 137개 편중 → 신규 후보는 기타 비중 낮추기
- 10 keyword 단위(Phase 1) → 30~50 단위(Phase 6)

#### 0-2. dedup (keyword + alias 충돌)
- 기존 terms.json의 `{keyword}` 차집합
- 기존 terms.json의 `{alias 전부}` 차집합 (`keywords_to_add.md`에 있던 안전망 복원)
- 산출물: 신규 keyword 리스트 (충돌 제거)

#### 0-3. Deterministic validator 작성 (Phase 5 미루지 않고 round-001부터 적용)
- `Scripts/db-expand/validator.py` — `generate_db.py`의 `validate()` 함수(line 162-189)를 fork + 길이 검증 추가
- 룰: `CATEGORY_ENUM` / `SUMMARY_LEN(20~30)` / `ETYMOLOGY_LEN(60~120)` / `NAMING_LEN(150~270)` / `NULL_GUARD` / `KEYWORD_FORMAT` / `ALIAS_MIN1` / `HANGUL_ALIAS_MIN1` / `KEYWORD_UNIQUE`
- **fork 시 제거할 것**: `generate_db.py:187-188`의 `if len(terms) < MIN_TOTAL` 검사 — 신규 batch(10~50개) validator에 부적합, MIN_TOTAL=200 기준이라 무조건 실패.
- **scope**: 신규 batch entry 전용. 머지 산출물(`terms.next.json`, 510+개)엔 돌리지 않음 — legacy 500은 grandfather(확정 결정 참조).
- CLI: `python validator.py <input.json>` → stdout JSON `{passed, failed}`

#### 0-4. Merge·load helper fork
- `Scripts/db-expand/merge.py` — `generate_db.py`의 `load_existing()` + 점진적 저장(line 234-238) 로직 fork. Phase 0-2 dedup이 정상이면 충돌 없음, 안전망으로 assert 포함.

#### 0-5. Batch variant prompt 작성
- `Scripts/db-expand/prompts/v2-batch.md`
- base: `generate_db.py SYSTEM_PROMPT` 전문 복사
- 갱신: namingReason 150~300 → 150~**270**
- 통합: v2 acceptance의 알려진 강점 룰
  - `components.py` `ALIAS_STRICT_EXTRA` 텍스트 → aliases 룰 끝에 삽입 ("한정 수식어 변형 금지")
  - `components.py` `CLOSING_EXTRA` 텍스트 → namingReason 룰 끝에 삽입 ("마지막 문장 재진술 금지")
  - selfcheck 블록은 chat thinking이 안 보이니 일단 제외, Phase 2B에서 API 비교 시 차이 측정 후 결정
- 명시: 출력은 array, markdown fence 금지, array 내 keyword 중복 금지

#### 0-6. Critic prompt baseline 작성
- `Scripts/db-expand/prompts/critic-v1.md`
- 룰셋: validator가 이미 잡는 정량 룰 + nuanced 룰 둘 다 (의도적 overlapping — Phase 4 회고에서 "critic이 유니크하게 잡은 항목" 측정용)
- 출력 형식: `{passed: [...], failed: [{keyword, rule_id, reason, fix_direction}, ...]}`

**Done**: 신규 keyword 리스트 / `validator.py` / `merge.py` / `prompts/v2-batch.md` / `prompts/critic-v1.md` 다섯 산출물.

### Phase 1 — Manual 2-session POC (10 keyword) [수동]

- 탭 A (Generator) system instruction: `prompts/v2-batch.md`
- 탭 B (Critic) system instruction: `prompts/critic-v1.md`
- 절차: Generator → **(이 시점에서 validator로 정량 1차 통과 확인)** → Critic → 재생성 (최대 3회) → 통과
- 산출물: `docs/db-expand/rounds/round-001.json`
- 라운드 기록 stopwatch 시작 — Phase 7 트리거 데이터 누적

**Done**: 10 keyword 전부 validator + critic 통과 + round-001.json 저장 + 사람 손 시간 기록.

### Phase 2 — 일관성 점검 (A + B) [코드]

#### (A) 기존 terms.json 베이스라인 비교
- 입력: `round-001.json` + 기존 terms.json 500개에서 **카테고리별 균등 sample** (예: 카테고리당 5개씩 30개)
- **drift gate (실패 시 회귀)**:
  - validator 통과율: 신규 batch와 sample 둘 다 측정 — 신규는 100%여야 함. sample은 legacy grandfather라 **비교 대상 아님** (전수 실측 비순응률 79.2% — 아래 grandfather 항 참조, 기록만).
  - alias 개수 중앙값 동일
  - 톤 빈도(부사·감탄사·과장 형용사) 신규가 sample 대비 명백히 증가하지 않음
- **게이트 민감도 비대칭 (권장 점검 절차)**: 3개 gate 중 베이스라인 샘플 선택에 민감한 건 gate 2(alias 중앙값)뿐이다. gate 1은 신규 batch가 고정이라, gate 3은 신규 tone=0이 바닥이라 샘플과 무관하게 구조적으로 불변. → **다음 라운드부터는 무거운 샘플 스윕을 매번 돌릴 필요 없이 gate 2만 전수(또는 충분히 큰 per-cat)로 확인하면 충분.** 근거: round-001 교차검증(전수 500 + 25샘플 스윕에서 gate 2 불변, gate 2 flip 0건) — `rounds/round-001-consistency-A.md` 교차검증 섹션.
  - ⚠️ open(검토 필요, 아직 정의 변경 안 함): gate 2가 현재 '중앙값 정확히 일치(==)'라 모집단이 얇은 카테고리/라운드에선 ±1로 흔들릴 수 있음. 허용 오차(예: ±1) 도입 여부는 **별도 결정 게이트** — 여기선 기록만.
- **informational only (기록만, gate 아님)**:
  - 길이 평균·분포 — 베이스라인 자체가 길이 룰 비순응(평균 summary 29 / etymology 84 / namingReason 164)이라 ±% 비교 신뢰도 낮음. 시계열 추세 추적용으로만 기록.
- 산출물: `docs/db-expand/rounds/round-001-consistency-A.md`
- > 2026-05-16_0049 probe acceptance 결과는 베이스라인으로 쓰지 않음 (15개 중 다수가 적대 샘플)

#### (B) chat ↔ API drift 검증
- 입력: `round-001.json`에서 5~10개 keyword 샘플
- 같은 `v2-batch.md` prompt + 같은 keyword를 API로 재실행 (`Scripts/db-expand/api_sample.py` 작성, generate_db.py의 `call_claude` 재사용)
- **drift threshold**: validator 통과율 동일, 같은 keyword pair의 길이 편차 ±15% 이내
- 산출물: `docs/db-expand/rounds/round-001-consistency-B.md`

**Done**: 두 threshold 다 통과 OR drift 원인 식별(예: chat이 project instructions 누락, system prompt stale paste). drift 시 Phase 1로 회귀.

### Phase 3 — terms.json 머지 + smoke test [혼합]

- [코드] 머지 정책 결정 (첫 라운드만): 정렬(`keyword.lower()` 알파벳) / 충돌 처리(Phase 0-2가 정상이면 발생 X, assert 안전망) / `terms.next.json` 작성 후 swap
- [코드] `python Scripts/db-expand/merge.py` 실행 → `terms.next.json`
- [수동] iOS smoke test 체크리스트:
  - [ ] Xcode 빌드 성공 (terms.next.json 임시 swap 상태)
  - [ ] 신규 keyword 1~2개 검색 → DetailView 표시
  - [ ] 신규 entry의 **alias로도 검색** 가능 (한글 alias 1개 + 영문 풀네임 alias 1개)
  - [ ] 카테고리 필터에서 신규 entry 노출
  - [ ] 앱 재시작 후 SwiftData 캐시·번들 로딩 정상
  - [ ] 통과 시 `terms.next.json` → `terms.json` swap, commit

**Done**: 위 체크리스트 전부 통과.

### Phase 4 — 회고 [수동]

`docs/db-expand/rounds/round-001.md`에 답:
- **분리 효용**: critic이 validator가 못 잡은 걸 잡았는가 (의도적 overlap이라 측정 가능)
- **길이 카운팅 오차**: critic이 길이를 잘못 셌는가 (LLM은 한글 카운팅에 약함 — Phase 5 critic-v2에서 길이 룰 제거 결정 신호)
- **critic이 유니크하게 기여한 룰**: nuanced 항목(alias 의미, etymology 사실성, naming coherence) 중 어느 것이 실제로 잡혔는가
- **통신 프로토콜**: feedback이 fix를 실제로 유발했는가
- **Scope leak**: 재생성이 다른 entry까지 건드렸는가 (10 keyword는 눈으로 가능)
- **분포 점검**: 카테고리 분포가 의도한 보정 방향대로 갔는가
- **목표 N 결정**: 라운드당 throughput·품질 기준 출시 전 도달 가능한 N 산정 → Done 신호 갱신
- **다음 라운드 변경 후보 3개 이상**

**Done**: Phase 5 발주 항목 확정 + 목표 N 결정.

### Phase 5 — Critic 책임 축소 + scope_diff [코드]

> **상태 (2026-06-19): 산출물 작성·기능검증 완료.** `critic-v2.md`(정량 룰 제거, nuanced 4종만) + `scope_diff.py`(6/6 기능검증). **Phase 5 Done(아래)은 Phase 6 첫 라운드가 새 흐름으로 실제 통과해야 충족** — 산출물은 준비됐고 실통과 미확인.

(validator는 Phase 0에 이미 있으므로 이 단계는 critic 축소 + helper 추가에 집중)

- `Scripts/db-expand/scope_diff.py` 작성
  - 입력: `before.json` / `after.json` / `failed_keywords`
  - 출력: `{expected_changed, actual_changed, scope_leak, missing_change}`
  - list 필드(aliases)는 정렬 후 비교 (순서 노이즈 방지)
- `Scripts/db-expand/prompts/critic-v2.md` 작성
  - Phase 4에서 측정된 "critic이 유니크하게 기여한 룰"만 유지
  - 정량 룰(길이·카테고리·null·alias 최소·keyword 형식)은 모두 제거 (validator 담당)
  - 후보 룰: `ALIAS_STRICT`(의미 판단) / `ETYMOLOGY_FACT`(사실성) / `NAMING_COHERENCE`(어원 ↔ 작명 다리 논리)

새 흐름: Generator → validator → critic(v2) → 재생성 → scope_diff → 머지

**Done**: 다음 라운드(Phase 6)가 `validator → critic(v2) → scope_diff` 흐름으로 통과.

### Phase 6 — 확장 batch (30~50 keyword) [혼합]

Phase 1·2·3·4 동일 흐름, keyword 수 증가. 라운드 기록 측정 항목:
- 최종 통과율 / 평균 재시도 횟수 / 라운드당 사람 손 시간 / Phase 2B 샘플 항목당 API 비용·latency

라운드별 `docs/db-expand/rounds/round-NNN.{json,md}` 누적. 매 라운드 후 분포 점검 (카테고리·길이).

**Done**: 목표 N 도달 + 라운드별 측정 데이터 확보.

### Phase 7 — 자동화 [코드, 트리거 충족 시]

진입 조건: 확정 결정의 트리거. 작업:
- loop script: Generator 호출 → validator → critic 호출 → 재생성 → scope_diff → 머지
- Generator 위치 결정 — claude.ai 유지(정액) vs API 전환(완전 자동)
  - **비용 데이터 출처**: Phase 6의 Phase 2B 샘플 누적값 + Phase 7 진입 전 의도적 API 미니 run 1회(같은 batch를 API로 돌려 비교) → claude.ai 정액이라 chat 측 토큰 비용은 추산 불가, API 비용만 가지고 손익 판단
- Critic은 Anthropic API 전환, Agent sub-agent로 분리 시도
- `Scripts/generate_db.py` 삭제 (validator·merge·prompt 다 fork 완료된 시점)
- 핸드오프 문서 산출 — 어느 책임이 어디로 갔는지 명시

**Done**: 자동화 loop + 라운드당 사람 손 시간 < 1분.

---

## 함정

Phase 0의 critic prompt(critic-v1)를 generator prompt와 충분히 다른 lens로 깎지 않으면 탭 두 개 띄워도 같은 lens가 두 번 도는 것. Phase 1에서 critic이 validator 외 항목을 거의 못 잡으면 신호 — critic-v1을 다시 쓰기 (특히 nuanced 룰의 구체화).

## 라운드 기록 템플릿

`docs/db-expand/rounds/round-NNN.md`:

```
## 라운드 [번호] — [날짜]

- batch size: [N]
- keyword 리스트: [...]
- Generator 라운드 수: [N]
- 최종 통과율: [N/total]
- 라운드당 사람 손 시간: [분]   ← Phase 7 트리거 측정용
- API 비용·latency (Phase 2B 샘플): [...]

- validator 통과: [N/total]
- critic이 추가로 잡은 항목: [N건] / 룰 분포: ...
  ← 0건 가까우면 critic 책임 축소 신호 (Phase 5에서 critic-v2로)
- 길이 카운팅 오차 (critic): [N건]
- Scope leak: [N건] / 항목: [...]

- 일관성 점검 (Phase 2):
  - (A) 기존 sample 베이스라인 비교: [pass/fail + 길이 분포 편차 %]
  - (B) chat↔API drift: [pass/fail + 편차]

- 분포:
  - 카테고리: [현재 누적 / 신규 추가]
  - 길이 평균: summary [X] / etymology [Y] / namingReason [Z]

- 관찰:
  - [generator가 반복적으로 깨는 패턴]
  - [critic이 못 잡은 항목]

- 다음 라운드 변경:
  - [...]
```
