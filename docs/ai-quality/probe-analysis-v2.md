# Probe Analysis — v2 라운드 직교성 측정 결과

> `closing × selfcheck × alias_strict` 2³=8 cell × 15 keyword fully crossed factorial 측정 결과.
> Claude.ai 대화로 돌아가 `handoff-v2.md` 작성 시 이 문서를 첨부할 것.

## 메타

- **Run ID**: `2026-05-15_2250`
- **Model**: `claude-sonnet-4-6`
- **Thinking budget**: 2000
- **총 호출**: 120 (8 cell × 15 keyword)
- **데이터 경로**: `Scripts/prompt-probe/results/2026-05-15_2250/`
  - `manifest.json`, `prompts_used.json`
  - `metrics/summary.csv`, `metrics/per_response.csv`
  - `raw/{cell}__{keyword}.json` × 120

## Keyword 구성

- **in_shot (4)**: `mutex`, `jpa`, `daemon`, `bug` — few-shot 예시와 동일/유사. 학습된 톤 재현 검증용
- **out_of_shot (6)**: `idempotent`, `cookie`, `semaphore`, `request`, `boolean`, `null` — few-shot에 없는 어휘. 일반화 검증용
- **branch_check (5)**: `apple`, `lunch` (비개발어), `mutext`, `redus`, `semafore` (오타) — 분기 정확도용

---

## 📊 1. 각 변경의 메인 이펙트 (baseline 대비)

| 측정값 | baseline | closing | selfcheck | alias_strict |
|---|---|---|---|---|
| oos_naming 평균 | 293.7 | **347.0** ↑ | 280.2 ↓ | 272.0 ↓ |
| oos_naming 최대 | 311 | **427** ↑↑ | 353 | 300 ↓ |
| oos_under_270 | 1/6 | 0/5 | 3/6 ↑ | 3/5 ↑ |
| aliases_qualifier | 2/10 | 2/9 | 3/10 ↑ | **1/9** ↓ ✓ |
| branch_correct | 15/15 | 14/15 | 15/15 | 14/15 |
| latency_avg | 8017ms | 7454 | **12783** | 6763 |
| thinking 활용 (chars) | 97 | 133 | **764** ↑↑ | 89 |

### 해석

- **closing 단독 = 역효과.** 의도는 "결론 멘트 금지·새 정보로 마무리"였는데, 모델이 "새 정보"를 **추가**해서 길이를 늘림 (293 → 347, max 427까지 튐). 의도가 빗나감.
- **selfcheck = 길이 제어 성공.** thinking이 ~8배(97 → 764) 활성화돼서 자기검수 동작. 다만 **레이턴시 1.6배** (8s → 13s).
- **alias_strict = 타깃 정확히 명중.** qualifier 2/10 → 1/9. 부작용으로 보이는 길이 단축은 보너스. 단, 별도 회귀 있음 (§3 참조).

---

## 📐 2. 3원 상호작용 — 직교성 판정

`closing__selfcheck` 결과만 따로 놓고 보면 **충격적**:

| 측정값 | 값 |
|---|---|
| oos_naming 평균 | **247.0** (전체 최저) |
| oos_naming 최대 | 262 |
| oos_under_270 | **6/6 (퍼펙트)** |
| branch_correct | 15/15 |
| latency_avg | 13658ms |
| thinking 활용 | 928 |

**합 예측 vs 실측 (oos_naming 평균):**

```
baseline           293.7
+ closing 효과     +53.3   (단독 측정에서 +53.3 bloat)
+ selfcheck 효과   -13.5   (단독 측정에서 -13.5 reduce)
─────────────────────────
예측합            333.5
실측              247.0
─────────────────────────
시너지            -86.5    ← 비직교, 강한 부의 상호작용
```

**해석:** closing 단독은 길이를 뻥튀기하는데, **selfcheck가 같이 켜지면 그 뻥튀기를 자기검수 단계에서 잘라낸다.** closing의 "결론 멘트 금지" 룰이 selfcheck의 "마지막 문장 점검" 단계와 만나서 비로소 동작. 이 둘은 **세트로만 의미 있음.**

전체 8 cell 비교:

| cell | oos_naming_avg | oos_under_270 | branch |
|---|---|---|---|
| baseline | 293.7 | 1/6 | 15/15 |
| closing | 347.0 | 0/5 | 14/15 |
| selfcheck | 280.2 | 3/6 | 15/15 |
| alias_strict | 272.0 | 3/5 | 14/15 |
| **closing + selfcheck** | **247.0** | **6/6** | **15/15** |
| closing + alias_strict | 329.8 | 0/5 | 14/15 |
| selfcheck + alias_strict | 280.4 | 2/5 | 14/15 |
| 3개 모두 | 267.0 | 3/5 | 14/15 |

---

## ⚠️ 3. `null` 키워드 분기 오류 — alias_strict 부작용

`alias_strict` 켜진 4개 셀 모두 `null`을 `not_dev_term`으로 오분류. baseline·selfcheck·closing__selfcheck는 정답.

| cell | null 분기 | thinking 요약 |
|---|---|---|
| baseline | ✅ term_entry | *"The user input is 'null'. This is a development term."* |
| closing | ❌ not_dev_term | "null/empty message" |
| selfcheck | ✅ term_entry | *"... a development term used in programming..."* |
| alias_strict | ❌ not_dev_term | "null/empty message" |
| closing__selfcheck | ✅ term_entry | *"... Wait — 'null' could actually be a development term!"* (selfcheck 자기검수가 구해냄) |
| closing__alias_strict | ❌ not_dev_term | "null/empty message" |
| selfcheck__alias_strict | ❌ not_dev_term | "null/empty input. Not a development term." |
| closing__selfcheck__alias_strict | ❌ not_dev_term | "input is null/empty" |

### 진단

모델이 입력 문자열 `null`을 **"빈 메시지(empty input)"로 파싱**하는 ambiguity가 있음.

**selfcheck의 3단계 절차**:
1. 입력이 세 분기 중 어디인지 판단
2. 개발 용어인 경우 etymology · namingReason 초안
3. namingReason의 마지막 문장이 새 정보를 담는지 점검

→ Step 1에서 "비개발어"로 잘못 잡으면 Step 2~3로 안 넘어감. **selfcheck는 길이 검수용이지 분기 재검토용이 아님.**

`closing__selfcheck`만 우연히 분기 재검토를 한 이유는 명확하지 않음. closing의 "결론 멘트 금지" 룰이 모델의 thinking을 한 번 더 둘러보게 했을 수 있음(약한 가설).

**원인 가설:** alias_strict가 시스템 프롬프트를 길게 만들면서 모델의 priors가 살짝 이동, "empty input" 해석 쪽으로 기울게 함. n=1이라 단정은 못 함.

### 영향 평가

- `null`은 매우 흔한 검색어 (Tony Hoare "billion-dollar mistake"로 유명).
- 1 keyword / 15 측정 keyword 중 하나지만 실제 사용자 검색 빈도는 훨씬 높을 것으로 추정.
- **prod 출시 시 사용자에게 직접 보이는 결함** — 무시 불가.

---

## 💰 4. 비용·레이턴시

| 셀 그룹 | latency_avg | output_tokens (호출당) | thinking_chars |
|---|---|---|---|
| baseline | 8.0s | ~500~600 | ~100 |
| selfcheck on (4 cells) | 12~14s | ~1000~2000 | 700~930 |
| selfcheck off (4 cells) | 6.8~8.0s | ~500~700 | ~90~130 |

- **selfcheck 셀: thinking 700~900 chars 사용** (2000 limit의 35~45%, 여유 있음)
- **호출당 비용: baseline 대비 ~2~3배 증가** (output_tokens 기준)
- **레이턴시: 8s → 13s** (사용자 체감 +5초)
- **Prompt cache**: 첫 호출 cache_create, 나머지 14건 cache_read — 기대대로 동작

---

## 🎯 5. 권고 (handoff-v2.md 기초)

> **→ 최종 결정은 §8 참조.** 이 §5는 *분석 시점 스냅샷*이며, Claude.ai 후속 토론(§7)·MVP 관점 재피드백(§8)을 거쳐 *Path A*로 축소 확정됨.

### 채택 후보

| 항목 | 판정 | 근거 |
|---|---|---|
| **closing + selfcheck (세트로)** | ✅ ADOPT | 6/6 perfect length, 15/15 branch, 강한 시너지. **이번 라운드 메인 변경.** |
| **alias_strict 단독 채택** | ⚠️ CONDITIONAL | qualifier 개선(2/10 → 1/9)은 진짜인데 `null` 분기 회귀가 심각. **null 보호 룰 추가하면 함께 채택, 아니면 보류.** |
| **closing 단독** | ❌ REJECT | 단독으로 쓰면 길이 폭증 (avg 347, max 427) |
| **null 보호 룰 (신규)** | 📌 후보 | 예: `"입력 문자열이 'null', 'undefined', 'void' 같은 값 부재 키워드면 빈 입력으로 해석하지 말고 해당 개발 용어로 처리"`. [도구 선택] 섹션 끝 또는 few-shot에 추가 |

### 묶음 권장안

- **묶음 A (보수적)**: closing + selfcheck만. cache 1회 무효화. 길이 문제 해결, 분기 안정. alias_strict는 다음 라운드(v3)로 미룸.
- **묶음 B (공격적)**: closing + selfcheck + alias_strict + null 보호 룰. cache 1회 무효화에 4가지 변경 묶음. qualifier 개선까지 한 번에. 단 null 보호 룰의 실효성은 따로 검증 안 됨(n=0).

---

## ❓ Claude.ai 전문가와 마저 토론할 항목

1. **묶음 A vs B 선택** — alias_strict의 qualifier 이득과 null 회귀 위험을 어떻게 저울질할 것인가. null 보호 룰을 어디에 어떤 정확한 문구로 넣어야 효과적인가.
2. **null 보호 룰의 작성 위치** — [도구 선택] 섹션 끝 vs few-shot 추가 vs selfcheck step 1 보강. 캐시·길이·priming 측면에서 트레이드오프 비교 필요.
3. **closing 텍스트 미세 조정** — closing 단독이 길이를 늘렸다는 사실은 selfcheck와 세트로 두는 이상 문제 없지만, closing 자체 표현을 더 강하게 "줄여라"로 바꿀 여지가 있는지 (예: "마지막 문장이 새 정보가 없으면 생략" → "마지막 문장이 새 정보가 없으면 **반드시** 생략. namingReason 길이가 250자를 넘으면 마지막 문장을 다시 검토").
4. **selfcheck step 1 보강** — 분기 재검토를 명시적으로 추가할지 (예: "Step 1에서 비개발어로 판단했더라도, 해당 문자열이 프로그래밍 키워드일 가능성을 한 번 더 검토"). 단, 현 selfcheck의 길이 제어 기능을 침범하지 않도록 배치 신중.
5. **레이턴시 트레이드오프 — production 수용 가능 여부.** 첫 검색 13s는 사전 앱 UX 기준으로 허용 범위인가, 아니면 thinking budget을 2000 → 1000으로 줄여 조율할지.

---

## 💬 7. 1차 합의안 — 데이터 분석 기반 (Path B)

§6의 5개 토론 항목을 Claude.ai에 전달한 결과, 데이터 분석 차원에서 정리된 1차 합의안.

### 7.1 Claude.ai가 추가로 짚은 패턴 2개

**(1) closing의 실패 메커니즘 — 콘텐츠 가이드 vs 길이 컨트롤.** closing 단독이 oos_naming을 폭증시킨 6개 케이스의 마지막 문장을 raw에서 보면 모두 *진짜로 새 정보*를 담음 — RFC 표준화 시점(cookie), Dijkstra P/V(semaphore), Hoare billion-dollar mistake(idempotent), ALGOL 60(boolean), Jakarta EE 이관(jpa). 모델은 closing의 *콘텐츠 부분*("새 정보를 담을 것")만 충실히 따르고 *생략 단서*("새로 더할 정보가 없으면 그 문장 자체를 생략한다")를 무시. 즉 closing은 **콘텐츠 가이드**로 작동하고, *길이 컨트롤이 빠져있는 것*이 단독 실패의 본질.

closing__selfcheck가 통하는 이유도 명확해짐: oos 6개 모두 270 이하이면서 마지막 문장은 52~114자로 *여전히 새 정보 보존*. selfcheck는 마지막 문장을 자르는 게 아니라 **본문 분량을 압축**해서 새 정보 마지막 문장이 270 안에 들어가게 함. closing이 콘텐츠 품질을 담당, selfcheck가 길이 메커니즘을 담당 — *서로 다른 차원에서 보완하는 진정한 시너지*.

**(2) null 분기 회귀의 일반화된 원인.** §3에서 *"alias_strict 부작용"*으로 진단한 것을 데이터로 재배열하면 더 일반적인 패턴이 보임:

| 변경 추가 | null 분기 |
|---|---|
| 변경 없음 (baseline) | ✅ |
| closing 단독 | ❌ |
| selfcheck 단독 | ✅ (thinking 1314자) |
| alias_strict 단독 | ❌ |
| closing + selfcheck | ✅ |
| closing + alias_strict | ❌ |
| selfcheck + alias_strict | ❌ |
| 셋 다 | ❌ |

**closing 단독도 alias_strict 단독과 똑같이 null을 망친다.** 즉 *prompt 길이가 길어지는 모든 변경*이 모델의 priors를 "빈 입력" 쪽으로 미세하게 기울이고, *selfcheck 단독*만 thinking 안의 자기검수로 그 priors를 보정. 셋 다 켜진 cell은 selfcheck가 있어도 실패 — prompt가 너무 길어 selfcheck 보정력을 초과.

이 재해석이 §6 Q1·Q2·Q4 답에 직접 영향을 미침.

### 7.2 §6 5개 질문에 대한 합의

| Q | 합의 |
|---|---|
| Q1: 묶음 A vs B | **Path B (공격적)** — null guard를 prompt에 명시적 보호로 추가, alias_strict 함께 채택 |
| Q2: null guard 위치 | **[도구 선택] 본문 + selfcheck step 1 둘 다** — priors 보정 + thinking 재검토 이중 안전망 |
| Q3: closing 텍스트 미세 조정 | **현행 유지** — closing은 콘텐츠 가이드 역할만, 길이 컨트롤은 selfcheck step 3가 담당 |
| Q4: selfcheck step 1·3 보강 | **step 1·3 모두 보강** — step 1: 분기 재검토 명시, step 3: 270자 명시화 |
| Q5: latency 트레이드오프 | **thinking budget 2000 유지** — 직교성 검증 안 된 추가 변수, v4 카드 |

### 7.3 Claude Code의 follow-up 입장

Claude.ai가 합의안 끝에 던진 3개 확인 질문에 대한 Claude Code 입장:

1. **null guard 위치**: 둘 다 채택 찬성. 단 step 1 보강 문구는 본문 룰과 verbatim 중복 피하고 *재검토 트리거*로 단순화 — *"비개발어/빈 입력으로 판정했다면, 입력이 짧은 프로그래밍 예약어 가능성을 한 번 더 검토"*.
2. **step 3 270자 명시화**: 찬성. 270 상한이 이미 prompt에 있음에도 closing 단독에서 평균 347 / 최대 427로 폭발한 데이터가 명시화 필요성을 입증.
3. **메트릭 fix 처리**: handoff에 포함하되 *별도 커밋*으로 분리. production prompt 변경(cache 무효화)과 test 인프라 변경의 회계를 깨끗이.
4. *(추가 권고)*: **사후 1-cell probe 재측정을 권고가 아닌 차단 조건으로 격상**. null guard 본문 룰은 n=0 미측정 상태.

### 7.4 1차 합의안 최종 형태 (Path B)

| # | 변경 | 위치 | 캐시 |
|---|---|---|---|
| 1 | closing 처방 | namingReason 항목 끝 | 무효화 1회 묶음 |
| 2 | selfcheck step 1·3 보강 | [응답 절차] 블록 | 위와 묶음 |
| 3 | alias_strict 처방 | aliases 항목 끝 | 위와 묶음 |
| 4 | null guard | [도구 선택] 섹션 끝 | 위와 묶음 |
| 5 | qualifier 메트릭 false positive fix | metrics.py | 별도 커밋, cache 무관 |

> **§7 요지**: 데이터만 보면 closing+selfcheck 세트가 최강(-86 oos_naming 시너지). 그러나 latency(+5s)·비용(output_tokens 2~3배) 차원이 분석에서 충분히 가중되지 않음. §8에서 product 관점이 추가됨.

---

## 🚦 8. MVP 관점 재피드백 → Path A로 전환 (최종)

§7 합의안을 Robin이 product 관점에서 검토한 결과, latency 비용이 MVP 단계 retention에 critical하다는 push-back. Claude.ai가 self-correction 후 합의안을 축소.

### 8.1 Robin의 push-back

> "MVP 단계에서는 레이턴시가 오히려 더 중요할 것 같다."

13s 절대값이 사용자 인내 임계를 넘는 우려, v1 production이 이미 8s로 임계 안쪽에 걸쳐있는 상태에서 +5초 증가가 **누적 좌절 임계**를 넘긴다는 직관.

### 8.2 Claude.ai self-correction

이전 *"사전 앱이라 13s 정당화 가능"* 입장 철회. 근거 셋으로 재정렬:

1. **절대값 임계**: 2026년 consumer LLM 앱(ChatGPT 모바일·Perplexity·Claude 앱) 평균 응답 3~7s. 사전 앱은 통상 사전(Naver/Daum 1초 미만)과도 비교됨. 13s는 MVP retention에 직접 타격.
2. **누적 좌절 임계**: UX 일반론 1~3s = responsive / 3~10s = slow but accepted / 10s+ = frustrating·abandon risk. v1 8s가 이미 인내 중 — +5초가 임계를 넘김.
3. **MVP 우선순위 정렬**: MVP 성공 지표는 retention. namingReason 270 → 290 차이는 사용자에게 invisible, 13s 첫 인상은 visible. *"Launch가 일어나지 않으면 누적 가치도 0."*

### 8.3 Claude Code의 추가 근거 — 비용 차원

latency 외에 **비용 측면**: selfcheck 셀의 output_tokens가 baseline의 2~3배(~500 → ~1500). 사용자 1만명 × 5검색 = 5만 호출 가정. cache_read로 input은 저렴해도 output은 호출당 비용 그대로 증가. 호출당 $0.005 차이만 잡아도 ~$250/월. 매출 0 MVP에서 무시 못 함.

### 8.4 Claude Code의 강조 — 사후 검증 격상

§7 합의안에서 사후 1-cell probe는 *권고* 수준. Path A에선 **차단 조건**으로 격상 필요:

- null guard 본문 룰은 측정 안 됨 (n=0)
- alias_strict 단독 cell에서 null 실패가 실측됨
- null guard 미동작 시 production에 그대로 회귀 박힘
- 비용 $0.10·시간 2분 — 안 할 이유 없음

null guard 미동작 시 fallback 옵션도 handoff에 명시:

| 우선순위 | 처방 |
|---|---|
| 1 | null guard 문구 강화 (예: "절대 빈 입력으로 해석하지 말고") |
| 2 | alias_strict 보류, null guard만 단독 채택. qualifier 회귀는 받아들이되 null 회귀는 안 받아들임 |
| 3 | v2 통째 보류, v1 유지. selfcheck 단독을 v3 1순위로 격상 (selfcheck 단독은 null 15/15, oos_under_270 3/6, 13s) |

### 8.5 Path A 최종 형태

| # | 변경 | 채택 | 비고 |
|---|---|---|---|
| 1 | closing | ❌ → v3 | selfcheck 세트로만 의미. MVP 보류 |
| 2 | selfcheck | ❌ → v3 | +5s latency · output 2~3배. launch 후 retention 데이터 보고 결정 |
| 3 | alias_strict | ✅ | cookie 4/4 cell 일관 효과, latency 영향 없음(6.7s, baseline보다 빠름) |
| 4 | null guard ([도구 선택] 본문) | ✅ | priors 보정. selfcheck 없으니 step 1 보강은 N/A |
| 5 | qualifier 메트릭 fix | ✅ | 별도 커밋. v3 측정 신뢰도 확보 |
| 6 | 1-cell probe 사후 검증 | ✅ **차단 조건** | null guard 미동작 시 fallback 트리거 |

latency 영향 재확인:
- baseline: 8017ms
- alias_strict 단독: 6763ms ← baseline보다 빠름
- Path A 추정 production: 8s 또는 약간 빠름. 사용자 체감 변화 0이거나 약간 개선.

### 8.6 v3로 보류된 카드

- closing + selfcheck 세트 — launch 후 retention·만족도 데이터로 정당화 검증
- thinking budget 조정(2000 → 1000) — selfcheck와 직교성 측정 함께
- Two-call architecture(Haiku 분기 + Sonnet 응답) — 진지한 latency 최적화 옵션
- selfcheck step 1·3 보강 — selfcheck 도입 시 함께 결정
- 한글 풀네임 alias 처리 — alias_strict 회색지대, v3 측정 보고
- Few-shot 5번째 예시 — 검증 안 된 효과, v3 카드

> **§8 요지**: MVP에서 +5s latency는 retention 임계 초과 + 비용 2~3배. selfcheck 류는 v3로 보류, alias_strict + null guard로 축소. 1-cell probe 사후 검증은 권고 아닌 **차단 조건**. 최종 v2 shape은 본 섹션 — §5는 분석 시점 스냅샷이고 §7은 1차 합의 기록.

---

## 부록: 첨부할 데이터

Claude.ai 대화에 이 문서와 함께 첨부 권장:

- 이 문서 (`docs/ai-quality/probe-analysis-v2.md`) — 임베드된 요약·해석
- `Scripts/prompt-probe/results/2026-05-15_2250/metrics/summary.csv` — 8 cell 집계
- `Scripts/prompt-probe/results/2026-05-15_2250/metrics/per_response.csv` — 120 호출 raw 메트릭
- (선택) `Scripts/prompt-probe/results/2026-05-15_2250/raw/{baseline,closing__selfcheck,selfcheck__alias_strict}__null.json` — null 분기 thinking 비교용
- (선택) `Scripts/prompt-probe/prompts/components.py` — 각 변경의 정확한 텍스트
