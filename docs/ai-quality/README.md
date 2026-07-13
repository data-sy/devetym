# AI 프롬프트·품질 문서함 — 인덱스

DevEtym AI 어원 생성의 **시스템 프롬프트·도구 스키마·품질 기준**을 확립한 프롬프트 엔지니어링 기록.

> ⚠️ **정본은 이 폴더가 아니다.** 프롬프트·3도구 스키마의 정본 = commonMain [`ClaudePrompt.kt`](../../shared/src/commonMain/kotlin/com/robin/devetym/data/remote/ClaudePrompt.kt)·[`ClaudeDto.kt`](../../shared/src/commonMain/kotlin/com/robin/devetym/data/remote/ClaudeDto.kt), 결정 락 = [**ADR-0007**](../adr/0007-ai-prompt-quality.md). 이 문서들은 그 정본에 이르는 **근거·측정·재사용 프롬프트 기록**이다.
>
> ⚠️ **이 문서들은 `dev-etymology`(iOS) 시기 기록**이라 `ClaudeAPIService.swift`·`DevEtym/…`·"iOS 앱" 같은 서술이 남아 있다. 아래 매핑표로 읽는다(역사 서술은 보존, 재사용 프롬프트의 첨부 지시부만 commonMain 경로로 갱신됨).

## 버전 사슬

```
prompt-review-brief(v1) → handoff-v1 → prompt-review-brief-v2 → claude-ai-opening-prompt-v2
   → [probe 측정: 8 cell × 15 keyword = 120 호출] → probe-analysis-v2 → handoff-v2(Path A 최종)
```

| 파일 | 무엇 | 관계 |
|---|---|---|
| [prompt-review-brief.md](prompt-review-brief.md) | v1 리뷰 브리핑(원본 프롬프트 전문·도구정의). namingReason 아직 300자. | 최초 브리프 |
| [handoff-v1.md](handoff-v1.md) | v1 리뷰 결과 5변경 지시서(few-shot bug·300→270·alias 스코프·sanity·drift). | brief(v1) 산출 |
| [prompt-review-brief-v2.md](prompt-review-brief-v2.md) | v2 브리핑(v1 적용 후 상태·실측 5샘플·가설 7.x). | brief(v1) **대체** |
| [claude-ai-opening-prompt-v2.md](claude-ai-opening-prompt-v2.md) | v2 대화 오프닝(페르소나·대화 규칙). brief-v2를 첨부로 참조. | brief-v2 **짝** |
| [probe-analysis-v2.md](probe-analysis-v2.md) | 직교성 factorial 측정 분석(§8 = Path A 최종 결정). | handoff-v2 **근거** |
| [handoff-v2.md](handoff-v2.md) | v2 채택 3변경 지시서(alias_strict·null guard·metrics fix). closing/selfcheck는 v3 보류. | probe §8 실행 지시 |

## 현재 형상 (정본 요약 — 상세는 ADR-0007)

- **채택**: v1 5변경 + v2 **Path A**(alias_strict + null guard). commonMain에 계승 확증.
- **미채택(v3 보류)**: closing·selfcheck — 측정상 최강이나 MVP latency(+5s)·비용(2~3배) 임계 초과. 재개 baseline = probe-analysis-v2 §8 + 1-cell probe 차단조건.
- **도구 3분기**: `return_term_entry` / `return_not_dev_term` / `return_possible_typo`.

## 경로 매핑 (문서 서술 → devetym 정본)

| 문서 속 참조 | devetym(KMP) 대응 |
|---|---|
| `ClaudeAPIService.swift`(systemPrompt·tools) | `shared/src/commonMain/…/data/remote/ClaudePrompt.kt`(`SYSTEM_PROMPT`·`TOOLS`) + `ClaudeDto.kt`(`object Tools`) |
| `TermEntry.swift`·`allowedCategories` | `shared/…/model/TermEntry.kt`·`Category.kt`(`Category.CANONICAL`) |
| `DevEtym/DevEtym/Resources/terms.json` | `shared/src/commonMain/composeResources/files/terms.json` |
| `Scripts/prompt-probe/…`(iOS repo 상대) | [`Scripts/prompt-probe/`](../../Scripts/prompt-probe/)(devetym 이관본 — WU-2) |
| "iOS 18+ 네이티브 앱" | KMP(iOS·Android) 앱 |

## 재측정 하네스

프롬프트 회귀·직교성 재측정은 [`Scripts/prompt-probe/`](../../Scripts/prompt-probe/)(devetym 이관본). 품질 게이트 파이프라인은 [`docs/db-expand/`](../db-expand/)·[`Scripts/db-expand/`](../../Scripts/db-expand/).
