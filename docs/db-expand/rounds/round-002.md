# 라운드 002 — 발주 (Phase 6 본 확장, 오케스트레이터 작성)

> **상태: 종결 (2026-06-20). 머지 완료 (510→550), 게이트 전부 PASS.** 발주(2026-06-19) → 실행·머지(2026-06-20).
> 결과·측정·판정은 아래 "라운드 결과"·"오케스트레이터 결정 필요/판정" 섹션 참조.
> 진행 상태 정본은 디스크 — 충돌 시 `ROADMAP.md` "Now" · `spec.md` · 이 문서를 신뢰.

## 성격 (왜 이 라운드인가)

- **마지막 수동 라운드**(handoff 전제). 목표는 "650 수동 도달"이 아니라:
  1. **Phase 5 Done 충족** — 새 흐름 `Generator → validator → critic(v2) → 재생성 → scope_diff → 머지`를 실제로 통과시킨다.
  2. **Phase 7 판단 데이터 확보** — 통과율·평균 재시도·사람 손 시간·2B 샘플 API 비용/latency·critic-v2 고유 검출 추이.
- 나머지 +100(550→650)은 Phase 7 자동화 loop로 흡수.

## 발주 범위 (사람 확정 2026-06-19)

- **batch size: 40** (510 → 550)
- **카테고리 배분 — 결손(→100) 비례, 기타 0:**

| 카테고리 | 현재 | 신규 | 도달 |
|---|---|---|---|
| 자료구조 (long pole) | 64 | **+11** | 75 |
| 동시성 | 71 | **+9** | 80 |
| 네트워크 | 79 | **+7** | 86 |
| 패턴 | 79 | **+7** | 86 |
| DB | 80 | **+6** | 86 |
| 기타 | 137 | 0 | 137 |
| **합** | 510 | **+40** | **550** |

- 배분 근거: 결손 합 +127(자료구조 36·동시성 29·네트워크 21·패턴 21·DB 20)에 비례. 자료구조·동시성 가중으로 long pole 우선 축소.

## 수용 게이트 (spec 고정 + 이 라운드 적용)

1. **validator 100%** — 길이(summary 20~30 / etymology 60~120 / namingReason 150~270) · 카테고리 enum · null guard · keyword 형식 · alias 최소1 + 한글 alias 최소1 · keyword 유니크. 신규 batch 전용(머지 산출물엔 미적용, legacy grandfather).
2. **critic-v2 통과** — nuanced 4종(ALIAS_STRICT 의미판단 · ETYMOLOGY_FACT · NAMING_COHERENCE · NAMING_CLOSING). 정량 룰은 critic에서 제거됨(validator 단일 정본).
3. **scope_diff: scope_leak 0** — 재생성이 있었으면 `python scope_diff.py before.json after.json <failed_keywords>` 로 확인. clean exit 0.
4. **dedup** — 기존 `terms.json`(510)의 `{keyword}` ∪ `{모든 alias}` 차집합. array 내 keyword 중복 0.
5. **분포 보정** — 신규 기타 0, 코어 동등화 방향 일치(위 표대로).
6. **비가역 게이트** — `terms.next.json` swap·머지 커밋은 iOS smoke 통과 후 **사람 승인**.

## 실행 흐름 (handoff-phase6.md 착수 순서)

1. [AI] `Scripts/db-expand/keywords-round-002.txt` 큐레이션 — 위 배분대로 40개, 코어 위주(자료구조 보강), 기타 0. ✅ **완료 (2026-06-19)**.
2. [AI] dedup (게이트 4). ✅ **완료** — 기존 510의 `{keyword} ∪ {모든 alias}` 정규화(소문자·기호제거) 차집합으로 **충돌 0건**. 후보 내부 중복 0. 부분일치 17건은 별개 복합어(토큰 공유)라 게이트 아님.
3. [사람] 2탭 실행 — A=`prompts/v2-batch.md`, B=`prompts/critic-v2.md`. Generator → validator(정량 1차) → critic(v2) → 재생성(최대 3회) → 통과. ◀ **현재 대기 (사람만 가능: claude.ai 탭)**
4. [AI] scope_diff (게이트 3).
5. [AI] merge + iOS smoke → 사람 승인 후 swap·커밋 (게이트 6).
6. [AI] 이 문서 "라운드 결과" 채움 + "오케스트레이터 결정 필요" 섹션.

## 측정 (라운드별 누적 — Phase 7 입력)

- 최종 통과율 / 평균 재시도 횟수 / 라운드당 사람 손 시간 / 2B 샘플 항목당 API 비용·latency.
- critic-v2 고유 검출 추이(round-001 = 0). 계속 0이면 critic 추가 축소/제거 신호.
- 길이 카운팅 오차 누적(round-001 표본 작아 신호 약함).

## 미결/이월 (오케스트레이터 추적)

- **Phase 2B drift 게이트 결정**: round-001.md는 "게이트 결정 미결"로 남았으나 실제로는 결정 (a)(원인 식별 = Done 인정)로 머지·커밋(`e11cf15`)됨. round-001은 종결이라 회귀 사유 아님 — 기록 일관성 차원의 이월만. Phase 7 loop 설계 시 "API 단발 길이 비순응 → validator→재생성 필수"가 정본 근거.

---

## 오케스트레이터 사전 검증 — 큐레이션·dedup (2026-06-19)

> 2탭 생성 전 단계 게이트. validator/critic/scope_diff는 생성 산출물이 없어 아직 판정 대상 아님.

- **PASS.** dedup "충돌 0" 주장을 오케스트레이터가 독립 재현(terms.json 510의 `{keyword}∪{모든 alias}` 정규화 차집합) → **정확 충돌 0 / 후보 내부 중복 0** 확인.
- batch 40 / 배분(자11·동9·네7·패7·DB6, 기타0) 발주와 정확히 일치. terms.json 베이스 510·코어 분포 일치.
- 부분일치: 오케스트레이터 집계 22건(실행 세션 17건). 차이는 카운팅 방식뿐, 전부 토큰 공유 별개 개념(`abstract-factory↔factory` 등) — 게이트 아님.
- 판단 메모(비블로킹): `consistent-hashing` DB 배정은 분산/네트워크 경계 용어. enum 위반 아니라 게이트는 아님. 2탭에서 어색하면 네트워크 재배정 여지.
- **결정: 3단계(claude.ai 2탭) 진행 승인.** 이후 게이트(validator 100% / critic-v2 / scope_leak 0)는 생성 산출물 도착 시 판정.

## 라운드 결과 (2026-06-20, 통과·머지 완료)

- batch size: 40 → **terms.json 510 → 550** (무손실 swap, 기존 510 변경 0·소실 0)
- Generator 라운드 수: **2** (cycle1 생성 → cycle2 재생성)
- 최종 통과율: **40/40**
- 라운드당 사람 손 시간: **≈10분** (round-001과 비슷한 수준. round-001.md의 "≈60분"은 임시 어림값이었고 실제론 두 라운드 손 시간이 유사 — 2026-06-20 사람 정정). 트리거 >5분은 충족이나 아래 "수동 유지" 결정 참조.
  - critic 격리 삽질(탭 B가 .md 주석부까지 paste되어 메모리 오염 → 임시챗 재실행)에 추가 시간 소요. 아래 관찰③.
- API 비용·latency (Phase 2B 샘플): 미측정(생성 전량 claude.ai 정액). **수동 유지 결정으로 API 전환 보류** → 비용 수집 불요(아래 판정).

### 검증 결과
- **validator (결정론, 최종 게이트)**: cycle1 **16/40**(실패 24건 전부 길이) → cycle2 24개 재생성 **24/24** → 합본 **40/40**.
  - cycle1 실패 24건 분류: SUMMARY_LEN(<20자) 19건 / NAMING_LEN(<150자) 4건 / ETYMOLOGY_LEN 2건. (pairing-heap 2룰 중복)
- **critic-v2 (격리 탭, nuanced 4종)**: **40 passed / 0 failed.** passed∪failed = 입력 40 정확 일치(무결성 OK).
- **scope_diff**: cycle1→합본 재생성 `clean=true`, scope_leak 0, missing_change 0 — 깨진 24개만 변경, 통과 16개 무변경.

### critic 고유 검출 = 0 (Phase 5 축소 신호 누적)
- critic-v2가 validator 외 유니크하게 잡은 nuanced 문제: **0건** (round-001도 0 → **2라운드 연속 0**).
- → critic 추가 축소/제거 검토 신호 누적. 단 critic은 cross-batch가 아닌 batch-내 의미판단 담당이라, 표본 더 쌓고 결정(Phase 6 누적).

### 길이 카운팅 오차 (critic)
- critic이 길이를 오판한 사례 0 (정량 룰을 아예 안 봄 — validator 단일 정본 설계대로). cycle1 길이 실패는 전부 validator가 잡음.

### Scope leak
- 0건 (scope_diff 도구 검증). 30~50 batch라 눈 검수 불가 → 도구가 정상 작동 확인(Phase 5 scope_diff 실통과).

### dedup / 충돌 (코드 게이트)
- HARD(신규 keyword == 기존 keyword): 0.
- SOFT-A(신규 alias == 기존 keyword/alias, 정규화 비교): 2건 검출 → 처리:
  - `interval-tree`의 `구간 트리` ↔ 기존 `segment-tree`의 `구간 트리` — **완전매칭상 실제 충돌**(동일 문자열). interval-tree에서 제거(인터벌 트리 유지).
  - `cache-aside`의 `lazy loading` ↔ 기존 `lazy-loading`(keyword) — **false positive**(공백≠하이픈, 앱은 완전매칭이라 안 부딪힘). 제거했다가 smoke에서 `lazy loading`(공백) 무결과 회귀 발견 → **복원**.

### smoke (코드 레벨 결정론, BundleDBService 복제)
- 디코딩: 550 전부 6필드·타입·카테고리 OK (Codable 디코딩 실패 0).
- 검색 결정론: 신규 keyword·alias(한+영 풀네임)·autocomplete prefix·정렬·유니크 전부 PASS. 모호성 해소 확인(`구간 트리`→segment-tree, `lazy loading`→cache-aside, `lazy-loading`→기존 일반).
- (워크트리 비밀파일 부재로 풀 Xcode 빌드 대신 디코딩·검색 경로 결정론 검증 — handoff 규칙 준수.)

### 분포
- 카테고리(누적): 자료구조 **76**(+12) / 동시성 **80**(+9) / 네트워크 **86**(+7) / 패턴 **86**(+7) / DB **85**(+5) / 기타 137(불변). 합 550.
  - long pole 자료구조(64) 최대 보강. 기타 비중 26.9%→24.9%.
- 길이 평균: validator 범위 내(별도 측정 생략 — 전수 통과로 갈음).

### 관찰
- **①** generator가 **summary 20자 하한을 상습 미달**(cycle1 실패 24건 중 19건이 16~19자). round-001엔 없던 신규 패턴 — 표본 작아 단정은 이르나 누적 관찰 대상.
- **②** 코드 dedup이 **정규화(공백·하이픈 제거) 비교**라 앱의 **완전매칭**보다 공격적 → false-positive 1건. dedup을 검색 의미와 맞춰야 함(다음 라운드 변경 후보).
- **③** critic **격리 운용 함정**: critic-v2.md를 통째로(─── 위 주석부 포함) paste하면 탭이 파이프라인 메타·dev-etym 맥락을 들고 와 **격리 위반** → 판정 대신 메타 응답. 해소: **임시챗 + ─── 사이 본문만** paste. 런북에 명문화 필요.

### 다음 라운드 변경 후보
1. **dedup을 완전매칭 기준으로** (정규화 비교는 informational로 분리). 관찰② 근거.
2. **v2-batch 프롬프트에 summary 하한 여유** 문구("최소 22자 이상 권장") 검토 — 관찰① 누적 시. 지금은 기록만.
3. **critic 격리 런북 항목 추가** — "임시챗 + 본문만 paste, JSON 외 메타 응답 시 오염 신호". 관찰③.
4. **수동 round-003** — 자동화(API) 대신 claude.ai 정액 수동 1~2라운드 더로 550→650 마감(아래 판정).

## 오케스트레이터 결정 필요 / 판정 (2026-06-20)

- **머지 승인(비가역)**: 사람 승인 완료 → swap·커밋 진행.
- **consistent-hashing 카테고리**: 발주안 DB → generator 자료구조. **자료구조 유지로 확정**(사람) — long pole 보강에 유리, 분산/자료구조 경계 용어로 타당.
- **alias 충돌 2건**: 위 처리대로 확정(사람) — 구간 트리 제거(실충돌) / lazy loading 복원(false-positive).
- **게이트 종합 판정: PASS.** validator·critic-v2·scope_diff·dedup·smoke 전부 통과. **Phase 5 Done(새 흐름 실통과) 충족.**
- **Phase 7 방향 결정 (사람, 2026-06-20): 자동화(API 전환) 보류 → claude.ai 정액 수동 유지.** 근거: claude.ai는 정액제라 한계비용 0, API는 종량제. 출시 전 잔여 +100(550→650)은 수동 1~2라운드로 흡수 — 라운드당 손 시간 ≈10분으로 시간 부담 작음. 자동화가 사는 건 비용이 아니라 사람 시간인데 잔여 규모가 작아 이득이 비용(API 종량)을 못 넘음. **재검토 조건**: 출시 후 analytics 기반 대량 확장(수백 개)으로 사람 시간이 병목이 될 때만. (spec Phase 7 "claude.ai 유지 vs API 전환"에서 **유지** 채택.)
