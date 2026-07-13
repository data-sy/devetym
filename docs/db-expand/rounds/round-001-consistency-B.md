# round-001 일관성 점검 (B) — chat↔API drift 검증

> Phase 2B (API 필요). spec.md Phase 2 (B) 정본 임계값을 따름.
> 재현: `ANTHROPIC_API_KEY=$(cat Scripts/db-expand/.env.local) python3 Scripts/db-expand/api_sample.py --round docs/db-expand/rounds/round-001.json --n 10 --out docs/db-expand/rounds/round-001-consistency-B-api.json`
> 실행일: 2026-06-19 / API 호출 1회 (10 keyword 단일 batch)
> 산출물: [`round-001-consistency-B-api.json`](round-001-consistency-B-api.json) (API 원응답, gitignore 아님 — 분석 보존)

## 결론: 두 임계값 **FAIL** — 단, **drift 원인 식별됨** (spec Phase 2B Done 조건 중 후자 충족)

| Drift threshold | 결과 | 판정 |
|---|---|---|
| validator 통과율 (API도 100% 기대) | API **1/10 (10%)** vs chat 10/10 | ❌ FAIL |
| 같은 keyword pair 길이 편차 ±15% 이내 | **22건 위반** (10 keyword × 3 필드 중) | ❌ FAIL |

## 재현 조건 (production 일치 확인)

- 모델 `claude-sonnet-4-6`, `anthropic-version 2023-06-01`, `max_tokens 8192`, system에 `cache_control` ephemeral — `generate_db.py call_claude` / `ClaudeAPIService.swift`와 동일.
- system prompt = `prompts/v2-batch.md` 본문 (chat 탭이 쓴 것과 동일). raw urllib (SDK 미사용, production 동일).
- 연결 핑 사전 확인: HTTP 200, model `claude-sonnet-4-6`, version 정상.

## drift 성격 (정량)

### validator (API 단발 출력)
- 통과 1/10. 위반 룰 분포: **SUMMARY_LEN 6 / NAMING_LEN 4 / ETYMOLOGY_LEN 3 / HANGUL_ALIAS_MIN1 1**.
- 즉 위반은 거의 전부 **길이 룰**. 형식·카테고리·null·keyword·alias최소는 통과.

### 길이 편차 (chat=cycle2 최종본 vs API=단발)
- 22개 위반 중 **~21개가 API > chat (체계적 overshoot)**. 유일한 단축은 sharding etymology(api 80 < chat 100).
- 대표값: summary `priority-inversion` 40 vs 24(+66.7%), `aba-problem` 41 vs 24(+70.8%) / namingReason `sni` 331 vs 205(+61.5%), `hsts` 294 vs 211(+39.3%).
- 길이 외 항목은 일치도 높음: 카테고리 전부 일치, etymology→namingReason 다리 논리 유지, 톤(건조체) 유지, alias 의미 대체로 일치.
  - 예외: `hsts` alias가 API에서 `["HTTP Strict Transport Security","HSTS"]`로 한글 표기 누락(룰 위반) — chat은 `"HTTP 엄격 전송 보안"` 포함.

## 원인 식별 (이게 핵심 — gross prompt mismatch 아님)

1. **비교 비대칭 (지배적 원인)**: `round-001.json`은 **critic 후 cycle-2 최종본**으로 이미 validator 10/10 통과한 산출물. 이번 API는 **단발 생성(cycle-1 상당)** — validator→재생성 루프를 거치지 않음. round-001.md 기록상 chat cycle-1에도 길이 위반(priority-inversion etymology 125>120)이 있었고 cycle-2에서 교정됨. → "루프 최종본 vs 단발"을 ±15%로 재면 구조적으로 깨진다.
2. **단발 길이 미준수 (실재 신호)**: 그렇더라도 API 단발은 chat cycle-1(위반 1~2건)보다 길이 위반이 훨씬 많다(14건). 단발 생성은 길이 룰을 자기교정 없이 일관 초과한다 — 특히 summary·namingReason. 이는 **API 단발 출력은 validator 비순응이며, chat 워크플로의 round-001 품질은 루프(critic→재생성)에서 나온 것**임을 뒷받침.
3. **stale paste / 지침 누락 아님**: 프롬프트는 정상 전달됨(구조·톤·카테고리·alias 의미 충실). spec이 예로 든 drift 원인(project instructions 누락, system prompt stale)에는 해당하지 않음.

## 함의

- **Phase 7 자동화 설계 근거**: API loop은 `Generator(API) → validator → 재생성`이 반드시 필요(spec Phase 7 loop에 이미 포함). 단발 API는 그대로 쓸 수 없다는 정량 근거 확보.
- **round-001(chat 산출물) 자체는 무결**: 길이 FAIL은 API 단발의 특성이지 chat 생성/round-001.json의 결함이 아님. Phase 1 회귀로는 이 신호가 해소되지 않음(회귀해도 chat 최종본은 동일하게 validator 통과).

## 게이트 결정 (사람 결정 지점 — 미결)

spec Phase 2B는 "두 threshold 통과 OR drift 원인 식별"을 Done으로, "drift 시 Phase 1 회귀"를 규정. 원인은 식별됨. 다음 중 택1 필요:

- (a) **drift 원인 식별로 2B Done 인정 → Phase 3 머지 진행** (round-001은 검증된 chat 최종본, 머지 대상은 그것).
- (b) **공정 재검 먼저**: API 출력을 같은 validator→재생성 루프에 1~2회 태워 chat 최종본과 수렴하는지 확인(추가 API 비용) → API surface 신뢰도 확정 후 Phase 7 설계 입력.
- (c) Phase 1 회귀 — 본 분석상 길이 신호를 해소하지 못하므로 비권장.

### 결정 (2026-06-19): **(a) 채택**
- 사용자 결정: drift 원인 식별로 2B Done 인정 → Phase 3 머지 진행.
- (b) API 공정 재검은 Phase 7 자동화 설계 시 입력으로 흡수 (단발 API는 validator→재생성 루프 필수라는 본 라운드 근거를 그때 활용).
