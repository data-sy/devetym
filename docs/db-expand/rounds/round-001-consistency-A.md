# round-001 일관성 점검 (A) — 기존 terms.json 베이스라인 비교

> Phase 2A (코드만, API 불필요). spec.md Phase 2 (A) 정본 임계값을 따름.
> 재현: `python Scripts/db-expand/consistency_a.py docs/db-expand/rounds/round-001.json DevEtym/DevEtym/Resources/terms.json --per-cat 5`
> 실행일: 2026-06-19

## 결론: **PASS** (3개 drift gate 전부 통과 → Phase 1 회귀 불필요, Phase 2B/머지로 진행 가능)

## 입력

- 신규 batch: `round-001.json` 10개
- 베이스라인 sample: 기존 `terms.json` 500개에서 **카테고리별 균등 5개 = 30개** (결정론적 샘플링: 카테고리별 keyword 알파벳 정렬 후 균등 간격 추출)
- sample keyword 30개 (재현용):
  - DB: acid, dirty-read, not-null, savepoint, window-function
  - 기타: abstraction, deploy, lambda, radixsort, yield
  - 네트워크: amqp, grpc, multicast, sftp, xss
  - 동시성: actor, deadlock, memory-fence, read-write-lock, worker
  - 자료구조: array, deque, list, red-black-tree, vector
  - 패턴: active-record, decorator, layered-architecture, repository, yagni

## Drift gate (실패 시 회귀 — 전부 통과)

### (1) validator 통과율 — ✅ PASS
- **신규 batch: 10/10 = 100%** (필수 조건 충족, `failed: []`)
- 베이스라인 sample: 7/30 = 23.3% — 길이 룰 위반 분포 SUMMARY_LEN 13 / ETYMOLOGY_LEN 17 / NAMING_LEN 19.
  - legacy grandfather라 **비교 대상 아님** (spec 확정 결정: 기존 500개는 길이 룰 비순응 의도된 상태).
  - ⚠️ 관찰: 이 sample의 비순응률(76.7%)이 spec의 사전 추정(47~63%)보다 높음. 균등 샘플링이 더 많은 위반 항목을 포착했거나 전수 비순응률이 추정보다 클 수 있음. **gate가 아니므로 기록만** — legacy backfill(별도 작업) 우선순위 판단 시 참고.

### (2) alias 개수 중앙값 동일 — ✅ PASS
- 신규 중앙값 **2.0** == sample 중앙값 **2.0**
- 분포: 신규 {1:2, 2:7, 3:1} / sample {1:2, 2:22, 3:5, 4:1} — 둘 다 2개 중심, 정합.

### (3) 톤 빈도 (부사·감탄사·과장 형용사) — ✅ PASS
- 신규가 sample 대비 **증가하지 않음** (오히려 낮음).
- 신규: 총 0건 (entry당 0.0 / 1000자당 0.0)
- sample: 총 1건 (entry당 0.033 / 1000자당 0.121) — 과장형용사 "압도적" 1회.
- 어휘셋: `Scripts/db-expand/consistency_a.py`의 `TONE_LEXICON` (부사 16 / 감탄사 6 / 과장형용사 20 표제어). 사전 항목은 사실 서술이어야 한다는 "이름이냐 설명이냐" 원칙의 정량 표지.

## Informational only (gate 아님 — 시계열 추적용 기록)

길이 평균·분포. 베이스라인이 길이 룰 비순응이라 ±% 비교는 신뢰도 낮음. 추세 기록만.

| 필드 | 신규(mean/median/min~max) | sample(mean/median/min~max) | 규약 |
|---|---|---|---|
| summary | 23.5 / 24 / 21~25 | 28.4 / 29 / 16~42 | 20~30 |
| etymology | 100.9 / 98 / 89~117 | 79.4 / 76 / 18~176 | 60~120 |
| namingReason | 199.3 / 201.5 / 176~228 | 164.9 / 191 / 50~355 | 150~270 |

- 신규는 세 필드 전부 규약 내, 분포 폭이 좁고 안정적.
- sample(legacy)은 min~max 폭이 넓고 규약을 자주 벗어남 (etymology 18·176, namingReason 50·355). 길이 룰 도입 전 생성물의 특성으로, 신규 생성 품질이 베이스라인보다 길이 측면에서 더 균일함을 보여줌.

## 교차검증 (전수 + 스윕) — 샘플 운빨 배제

> 재현: `python3 Scripts/db-expand/consistency_a_robust.py docs/db-expand/rounds/round-001.json DevEtym/DevEtym/Resources/terms.json`
> 대조용 전수 지표: `python3 Scripts/db-expand/consistency_a.py … --per-cat 9999` (같은 측정 코드 재사용, 샘플링만 끔)
> 실행일: 2026-06-19

원래 PASS는 카테고리별 균등 30개 샘플 1회 측정이었다. "30개 운빨" 가능성을 배제하기 위해 **sampling 축만** 흔들었다. (gate 1은 신규 10개 고정, gate 3은 신규 tone=0 바닥이라 샘플 선택과 무관히 불변 — 표적은 gate 2.)

### 한 줄 결론: **robust = true** — 30샘플 PASS는 샘플 선택과 무관한 결론이다.

### (1) 전수 베이스라인 (500개 전부)

| 지표 | 30샘플 | 전수 500 | 판정 |
|---|---|---|---|
| gate 1 validator (신규) | 100% | 100% (10/10) | ✅ 불변 |
| gate 2 alias 중앙값 (신규==base) | 2.0 == 2.0 | **2.0 == 2.0** | ✅ 전수에서도 동등 |
| gate 3 tone (신규 ≤ base, /1k자) | 0.0 ≤ 0.121 | 0.0 ≤ 0.201 | ✅ 불변 |

- **gate 2 전수 확정**: 전수 alias 분포 `{2:306, 3:142, 1:46, 4:5, 5:1}` — 500개 중 306개가 정확히 2개라 중앙값 2.0은 압도적으로 안정적. 신규 분포 `{2:7, 1:2, 3:1}`도 2개 중심. 샘플이 아니라 모집단 자체가 중앙값 2.0.

### (2) 샘플링 민감도 스윕 (25개 샘플)

- per_cat ∈ {3,4,5,8,10} × offset ∈ {0,1,2,3,5} = **25개 서로 다른 샘플** (n=18~60).
- `gate2_stable_across_all: true`, **`gate2_flips: []`** — 25개 전부에서 base alias 중앙값이 **모든 행에서 2.0**, 단 한 번도 안 뒤집힘.
- `gate3_stable_across_all: true` — base tone이 샘플마다 0.0~0.575/1k자로 출렁여도 신규가 0.0(바닥)이라 항상 성립. base가 0.0인 샘플에서도 0.0≤0.0 동률로 통과.

### (3) legacy 비순응률 관찰 — 전수로 확정

- 보고서 (1)의 "sample 비순응률 76.7% > spec 추정 47~63%" 관찰을 전수로 확정: **전수 통과율 20.8% (104/500) → 비순응률 79.2%**.
- 즉 **샘플 과대포착이 아니다.** 30샘플(76.7%)은 오히려 모집단(79.2%)을 살짝 과소보고했다. **원래 전수 비순응률이 spec 추정(47~63%)보다 실제로 높다.**
- 위반 룰 분포(전수): ETYMOLOGY_LEN 313 / SUMMARY_LEN 233 / NAMING_LEN 226. 길이 룰 도입 전 생성물의 의도된 grandfather 상태이며 gate 아님 — 다만 legacy backfill 우선순위를 높게 잡을 근거.

## 다음 단계

- Phase 2A 통과 → **Phase 2B (chat↔API drift)** 진행 가능. 선행작업 `Scripts/db-expand/api_sample.py` 작성 필요 (사람 확인 대기: API 비용 발생).
- 2B도 통과 시 Phase 3 머지(`merge.py` → `terms.next.json` → iOS smoke test) 게이트로.
