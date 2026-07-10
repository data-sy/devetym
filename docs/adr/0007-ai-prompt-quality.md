# ADR 0007: AI 프롬프트·품질 정본 — 시스템 프롬프트·도구 스키마·품질 게이트

## Status
Accepted (2026-07-10) — **[`26-07-10 자기완결화 계획`](../handoff/26-07-10-selfcontained-migration-plan.md) D3 승인**(WU-3). `dev-etymology`(iOS) 시기 프롬프트 문서를 [`docs/ai-quality/`](../ai-quality/)로 이관하고, 그 문서들이 정본으로 지목하던 `ClaudeAPIService.swift`를 **commonMain 계승본**([`ClaudePrompt.kt`](../../shared/src/commonMain/kotlin/com/robin/devetym/data/remote/ClaudePrompt.kt)·[`ClaudeDto.kt`](../../shared/src/commonMain/kotlin/com/robin/devetym/data/remote/ClaudeDto.kt))으로 대체·정본화한다. 프롬프트·도구 위치 결정은 [ADR-0004](0004-backend-proxy-boundary.md)·[ADR-0006](0006-server-cache-boundary.md) §6을 계승(재론 아님).

## Context

DevEtym의 AI 어원 생성 품질은 `dev-etymology`(iOS) 시기에 **다라운드 프롬프트 엔지니어링**으로 확립됐다. 그 산출 문서 6종이 [`docs/ai-quality/`](../ai-quality/)로 이관됐다(버전 사슬은 [`docs/ai-quality/README.md`](../ai-quality/README.md)):

```
prompt-review-brief(v1) → handoff-v1 → prompt-review-brief-v2 → claude-ai-opening-prompt-v2
   → [probe 측정: 8 cell × 15 keyword = 120 호출] → probe-analysis-v2 → handoff-v2(Path A 최종)
```

이 과정에서 확립된 것 — **시스템 프롬프트(버전드)·3분기 도구 스키마·품질 게이트** — 은 iOS `ClaudeAPIService.swift`를 "단일 진실 공급원"으로 삼았다. KMP 이관으로 프롬프트·도구가 **commonMain으로 계승**됐으나(두 클라이언트가 같은 프록시 공유 — ADR-0006 §6), 다음이 코드에 못박혀 있지 않아 stale·drift 위험이 있었다:

- **어느 프롬프트 버전이 현재 형상인가**(v1 5변경 + v2 Path A) — 문서만으론 "무엇이 채택/보류됐는지" 추적이 흩어져 있음.
- **무엇이 의도적으로 미채택인가**(closing·selfcheck는 측정상 최강이나 MVP 임계로 보류) — 근거 없이 재도입되면 latency 회귀.
- **정본 위치가 이제 commonMain**이라는 사실 — 이관 문서들이 여전히 `.swift`를 가리켜 misleading.

거버넌스면(프롬프트·품질 기준)이므로 ADR로 고정한다. 이 ADR은 **현재 형상을 정본화하고 v3 재검토의 baseline을 명문화**한다(신규 결정 아님·계승 락).

## Decision

**AI 프롬프트·도구 스키마·품질 게이트의 정본은 commonMain이며, 그 현재 형상은 v1 5변경 + v2 Path A다.** 문서(`docs/ai-quality/`)는 근거·재사용 프롬프트 기록으로 남는다.

### 1. 정본 위치 (single source of truth)
- **시스템 프롬프트 + 3도구 스키마 + 요청 빌더** = [`ClaudePrompt.kt`](../../shared/src/commonMain/kotlin/com/robin/devetym/data/remote/ClaudePrompt.kt) (`SYSTEM_PROMPT`·`TOOLS`·`buildClaudeRequest`).
- **도구명 상수** = [`ClaudeDto.kt`](../../shared/src/commonMain/kotlin/com/robin/devetym/data/remote/ClaudeDto.kt) `object Tools`.
- **카테고리 정본** = `Category.CANONICAL`(6집합: 동시성·자료구조·네트워크·DB·패턴·기타).
- iOS `ClaudeAPIService.swift` **검증본을 그대로 계승**(변형 금지 검증 자산). `docs/ai-quality/`의 `.swift`·`DevEtym/` 경로 참조는 위 commonMain 경로로 읽는다(매핑표 = `docs/ai-quality/README.md`).

### 2. 프롬프트 버전 상태 = v1 5변경 + v2 Path A (열거·락)
**v1** ([handoff-v1](../ai-quality/handoff-v1.md), 리뷰 accept 5항목):
1. few-shot에 `bug` 추가(3→4 예시). 2. **namingReason 상한 300→270자** — 프롬프트 본문 **및** tool schema description 양쪽 동시(drift 금지). 3. aliases 스코프 규칙(약어→풀네임 1:1). 4. sanity test에 `bug` 편입. 5. CI chat↔API drift 체크.

**v2 Path A** ([handoff-v2](../ai-quality/handoff-v2.md), [probe-analysis-v2](../ai-quality/probe-analysis-v2.md) §8 최종):
1. **alias_strict** — 기본 용어가 약어가 아니면 한정 수식어 변형("HTTP cookie", "웹 쿠키")은 alias 아님. **시스템 프롬프트 본문에만** 추가(tool schema description은 원문 유지 — 설계 결정). 2. **null guard** — `'null'·'undefined'·'void'·'None'·'nil'·'NaN'` 같은 프로그래밍 예약어 입력은 빈 입력으로 해석 말고 `return_term_entry`로. 3. `metrics.py` qualifier 측정 fix(측정 도구, [Scripts/prompt-probe](../../Scripts/prompt-probe/)).

계승 확증(commonMain 실체): null guard(`ClaudePrompt.kt` `SYSTEM_PROMPT`)·alias_strict·namingReason 150~270(프롬프트+tool schema 일치)·few-shot 4개가 코드에 존재하고, closing/selfcheck 블록은 **부재**(= Path A와 정확히 일치).

### 3. 도구 3분기 (tool schema branch)
모든 입력에 **정확히 하나**를 호출(텍스트 응답 금지, 프롬프트 `[도구 선택 — 매우 중요]`로 강제):
| 도구 | 발화 조건 | 출력 |
|---|---|---|
| `return_term_entry` | 개발 용어로 판단 | 6필드(keyword·aliases·category∈6집합·summary 20~30·etymology 60~120·namingReason 150~270) |
| `return_not_dev_term` | 개발 용어 아님 | 없음 |
| `return_possible_typo` | 개발 용어 오타 추정 | `suggestion` |

thinking↔`tool_choice` forcing 비호환이라 `tool_choice: auto` + 프롬프트 강제로 구조 보장. 코드 분기 = `ClaudeResponse.toTermResult()`가 첫 `tool_use` 블록 name으로 3분기(`not_dev_term`/`possible_typo`는 예외 아닌 `TermResult` 케이스).

### 4. 미채택 = closing·selfcheck (v3 보류·근거 락)
probe factorial(closing × selfcheck × alias_strict, [probe-analysis-v2](../ai-quality/probe-analysis-v2.md))에서:
- `closing`(결론멘트 금지) **단독은 역효과**(길이 폭증) — 콘텐츠 가이드일 뿐 길이 컨트롤 결여.
- `selfcheck`(thinking 3단 자기검수)는 길이 제어 성공하나 **latency 8s→13s·output 토큰 2~3배**.
- **`closing+selfcheck` 세트만 최강**(oos 6/6 perfect, 시너지 −86.5) — closing이 콘텐츠, selfcheck가 길이 메커니즘.
- 그러나 **MVP retention latency 임계 초과**로 v3 보류(Robin push-back → Path A 최종).

**v3 재개 baseline**: probe-analysis-v2 §8(Path A) + handoff-v2 §3 **production push 차단 조건**(1-cell probe 사후검증). 재도입 전 반드시 재측정.

### 5. 품질 게이트 (정본)
- **결정론 validator**(길이·카테고리·null·alias·keyword 형식/유니크) 100% — [Scripts/generate_db.py](../../Scripts/generate_db.py)·[Scripts/db-expand/validator.py](../../Scripts/db-expand/).
- **LLM critic**(nuanced: alias 의미·etymology 사실성·naming coherence) — db-expand 파이프라인([docs/db-expand](../db-expand/), 별개 트랙).
- **chat↔API drift** ±15% — [Scripts/db-expand/api_sample.py](../../Scripts/db-expand/api_sample.py).
- **probe 재측정 하네스** — [Scripts/prompt-probe](../../Scripts/prompt-probe/)(직교성·회귀 측정).

### 6. commonMain의 의도적 개선 2지점 (iOS 대비·명문화)
1. **AI 질의 입력 keyword 대소문자 보존** — `NaN`→`nan` 뭉갬 방지(입력은 원형, tool **출력** keyword는 소문자 정규화 유지). 2. **category enum 중앙화** — `Category.CANONICAL` 참조(문자열 하드코딩 아님).

## Consequences

### Positive
- 프롬프트 버전 형상·미채택 근거가 코드+ADR에 락 → stale 문서로 인한 오도·근거 없는 latency 회귀 차단.
- v3(closing/selfcheck) 재개 시 baseline·차단조건이 명문 → 재측정 없는 성급 도입 방지.
- 정본이 commonMain임이 확정 → 두 클라이언트 프롬프트 drift 0(단일 소유).

### Negative
- **프롬프트 불변식 회귀 테스트 미존재**(정직 갭): iOS의 `test_systemPrompt_containsFewShotExamples`·`test_fieldLengthConstraints_matchBetweenPromptAndToolSchema`(프롬프트↔tool schema 270자 일치)가 KMP `commonTest`로 미포팅. 현재 `ClaudeApiTest.kt`는 3분기 **동작**만 검증하고 프롬프트 **내용 불변식**은 미검증 → namingReason 상한·few-shot 존재가 조용히 drift할 수 있음. 보강은 출시 후 백로그(선택).

### Neutral
- `docs/ai-quality/` 6종은 **역사적 기록**(dev-etymology 시기 대화·측정)으로 보존 — 정본 아님, 근거·재사용 프롬프트일 뿐. 재사용 프롬프트(opening-v2·brief-v2)의 첨부 참조만 commonMain 경로로 갱신.
- db-expand·prompt-probe 파이프라인은 **claude.ai 정액 수동 경로** 유지(API 종량 회피) — [ADR 별개](../db-expand/spec.md).

## Alternatives Considered

1. **정본을 문서(`docs/ai-quality/`)에 두기** — 기각. 코드가 런타임 실체이므로 문서-정본은 필연적 drift. 문서는 근거·재사용 프롬프트로 강등.
2. **closing/selfcheck 즉시 채택**(측정상 최강) — 기각. MVP latency(+5s)·비용(2~3배) 임계 초과. v3로 보류·재측정 게이트.
3. **ADR 없이 문서 이관만**(WU-3를 순수 파일 복사로) — 기각. 버전 형상·미채택 근거·정본 위치가 코드에 안 박혀 다음 세션이 과거로 회귀. D3가 ADR 신설을 승인한 이유.

## References
- 관련 ADR: [ADR-0004](0004-backend-proxy-boundary.md)(프롬프트 commonMain 소유), [ADR-0006](0006-server-cache-boundary.md) §6(도구 위치 계승)
- 정본 코드: `shared/src/commonMain/kotlin/com/robin/devetym/data/remote/ClaudePrompt.kt`·`ClaudeDto.kt`
- 근거 문서: [`docs/ai-quality/`](../ai-quality/)(README = 버전 사슬·경로 매핑)
- 품질 파이프라인: [`docs/db-expand/`](../db-expand/), [`Scripts/prompt-probe/`](../../Scripts/prompt-probe/)
- 계획 원장: [`docs/handoff/26-07-10-selfcontained-migration-plan.md`](../handoff/26-07-10-selfcontained-migration-plan.md) WU-3·D3
