# DevEtym v2 직교성 검증 — Probe

`closing` × `selfcheck` × `alias_strict` 세 변경의 2³=8 cell × 15 keyword
fully crossed factorial 측정. 각 변경이 *의도한 효과를 실제로 내는지*와
*변경 간 직교성/상호작용*을 raw 응답 + 메트릭 CSV로 검증.

---

## 디렉터리 구조

```
devetym_v2_probe/
├── prompts/
│   ├── components.py  ─ 변경 컴포넌트 string 정의 (코드네임 의미 docstring)
│   ├── tools.py       ─ Claude tool 정의 3종
│   ├── build.py       ─ 8 cell 조립 함수와 CELLS dict
│   └── verify.py      ─ sanity check (마커 포함/배제 + hash 분포)
├── keywords.py        ─ 15개 keyword set (in_shot 4 / out_of_shot 6 / branch_check 5)
├── metrics.py         ─ raw 응답에서 메트릭 추출
├── probe_prompt.py    ─ 메인 측정 스크립트
├── requirements.txt
└── README.md
```

---

## 실행

### 1. 환경 준비

가상환경(macOS Homebrew Python 등 PEP 668 환경에서는 필수, 그 외 환경에서도 의존성 격리상 권장):

```bash
python3 -m venv .venv
source .venv/bin/activate
pip install -r requirements.txt
```

활성화된 venv 안에서는 `python` / `pip`가 자동으로 `.venv`의 python3·pip3를 가리킴. 새 셸 세션에서 다시 돌리려면 `source .venv/bin/activate` 한 번만.

API 키 — Anthropic 콘솔 발급 키를 셸 환경변수로:

```bash
export ANTHROPIC_API_KEY=sk-ant-...
```

> ⚠️ 과거에는 `DevEtym/Config.xcconfig`에서 키를 awk로 추출했으나, **백엔드 프록시 전환(H2b) 이후 앱은 키를 보관하지 않는다**(`Config.xcconfig` 비어 있음). 이 dev 툴은 위처럼 **직접 환경변수로** 키를 넣어 쓴다. 프록시 서버용 키(Console 발급)와 같은 키를 재사용해도 되지만, 셸 히스토리에 남지 않도록 주의할 것.

### 2. Sanity check 먼저 (API 호출 없음)

8 cell이 의도된 마커를 정확히 포함/배제하는지 검증. assembly 버그를 측정 전에 잡아냄.

```bash
python -m prompts.verify
```

통과하면 8 cell의 길이·hash 출력. 실패하면 어느 cell의 어느 마커가 잘못됐는지 표시.

### 3. 측정 실행

```bash
python probe_prompt.py
```

- 총 호출: 8 cell × 15 keyword = **120**
- 예상 소요: 15-20분 (직렬)
- 예상 비용: ~$1-2 (Sonnet 4.6, cache 활용)
- 각 cell의 첫 호출은 cache_create, 나머지 14건은 cache_read

콘솔에 keyword마다 한 줄씩 진행 상황 출력:
```
[ 12/120] cookie       ✓ term_entry    naming=247   8.4s  cache=READ
```

### 4. ⚠️ 실행 전 확인

`probe_prompt.py` 상단의 `MODEL` 상수가 본인 production 코드
(`ClaudeAPIService.swift`의 `Constants.claudeModel`)와 일치하는지 확인.
다르면 측정값을 production과 비교할 수 없음.

```python
MODEL = "claude-sonnet-4-6"  # ⚠️ 본인 환경에 맞게 수정
```

---

## 결과 구조

```
results/2026-04-30_1530/
├── manifest.json           ─ run 메타데이터 (model, cells, keywords, total_calls)
├── prompts_used.json       ─ hash → 시스템 프롬프트 풀텍스트 매핑
├── raw/
│   ├── baseline__mutex.json
│   ├── baseline__jpa.json
│   ├── ... (120 파일)
│   └── closing__selfcheck__alias_strict__semafore.json
└── metrics/
    ├── per_response.csv    ─ 응답별 메트릭 (120 행)
    └── summary.csv         ─ cell별 집계 (8 행)
```

### Raw 파일 한 개의 구조

```json
{
  "meta": {
    "cell": "closing",
    "keyword": "idempotent",
    "group": "out_of_shot",
    "prompt_hash": "a375f07f...",
    "model": "claude-sonnet-4-6",
    "timestamp": "...",
    "latency_ms": 8421
  },
  "expected": { "keyword": "idempotent", "group": "out_of_shot", "expected_branch": "term_entry" },
  "response": {
    "content": [
      { "type": "thinking", "thinking": "..." },
      { "type": "tool_use", "name": "return_term_entry", "input": {...} }
    ],
    "usage": { "input_tokens": 5023, "output_tokens": 612, "cache_read_input_tokens": 4500, ... },
    "stop_reason": "tool_use"
  }
}
```

Prompt 풀텍스트는 매번 raw에 중복 저장하지 않고 `prompts_used.json`에서 hash로 참조.
80개 raw에 8벌 중복 저장(~140KB) 대신 매핑 한 번(~30KB).

---

## 결과 해석 — cell별로 보아야 할 메트릭

`metrics/summary.csv`의 컬럼 → 어느 약점 검증인지 매핑:

| 메트릭 | 검증 약점 | 통과 기준 |
|---|---|---|
| `oos_naming_avg`, `oos_naming_max`, `oos_under_270` | 약점 1 (closing) | baseline 대비 평균↓·max↓·통과율↑ |
| `aliases_qualifier` | 약점 3 (alias_strict) | baseline 대비 비율↓ |
| `branch_correct` | 약점 2 (selfcheck) — 분기 정확도 | baseline 대비 비율↑ |
| `latency_avg_ms`, `thinking_chars_avg` | 약점 2 부메트릭 — 비용 영향 | 큰 폭 증가 시 cost 검토 |
| `in_shot_naming_avg` | 회귀 안정성 | baseline 대비 큰 변동 없어야 함 |

### 직교성 판단

main effect 셋(closing, selfcheck, alias_strict 단독)의 효과 합이
fully integrated cell(`closing__selfcheck__alias_strict`)의 효과와 근사하면 직교.
크게 어긋나면 어딘가 상호작용 존재 — 2-way cell들에서 어느 페어인지 추적.

예: `oos_naming_avg`가
- baseline 290, closing 240 → closing 단독 효과 −50
- closing__alias_strict 245 → 비슷, alias_strict 직교
- closing__selfcheck 215 → 추가 감소, selfcheck와 시너지 (직교 아님)

---

## 트러블슈팅

**`Sanity check failed: 마커 누락`** — components.py를 편집하다 마커 문구가 깨졌을 가능성. verify.py의 `MARKERS` dict 문구와 components.py를 다시 맞춤.

**`branch_correct=False`가 baseline cell에서 다수** — production 프롬프트의 분기 판정 baseline 자체가 약함. selfcheck cell에서 개선되는지가 더 중요한 신호.

**`naming_last_sentence`가 비어있음** — namingReason이 종결 부호 없이 끝났을 때 발생. metrics.py의 `split_last_sentence`는 종결 부호가 1개 이하면 전체를 마지막 문장으로 취급. 비어있으면 raw에서 직접 확인.

**Rate limit 자주** — `probe_prompt.py`의 `RETRY_DELAY_SEC`를 늘리거나, cell 사이에 `time.sleep` 추가.
