# 라운드 004 — 발주·실행 (수동 라운드, 오케스트레이터+밸리데이터 세션) · **목표 N=650 도달, 마지막 확장 라운드**

> **상태: 종결 (2026-06-20). 머지 완료 (590→650), 게이트 전부 PASS.**
> 진행 상태 정본은 디스크 — 충돌 시 `ROADMAP.md` "Now" · `spec.md` · 이 문서를 신뢰.
> 핸드오프: `docs/db-expand/archive/handoff-round-004.md`. 흐름: `Generator(claude.ai 탭A) → validator → critic-v2(탭B 격리) → 재생성 → scope_diff → merge → swap`.

## 성격 (왜 이 라운드인가)

- **잔여 +60을 한 라운드로 흡수하는 마지막 확장 라운드(A안).** 목표 N=650 도달 → 코어 5개 카테고리 102~103 동등선 안착.
- **batch 60 (round-003의 40보다 큼) 출력 품질 관리:** generator를 탭A 한 방에서 **30+30 두 턴**으로 paste(자료구조16+동시성13 / 패턴12+DB10+네트워크9). 한 응답에 60개 entry를 뱉을 때의 뒤쪽 품질 저하·절단 회피. 머지는 1회.

## 발주 범위 (사람 확정 2026-06-20)

- **batch size: 60** (590 → 650), 기타 0.
- **카테고리 배분 — 목표 N=650 코어 균등화 완료(잔여 결손 비례, 자료구조 long pole 최대 가중):**

| 카테고리 | 현재 | 발주 | 도달(실제) |
|---|---|---|---|
| 자료구조 (long pole) | 87 | **+16** | 103 |
| 동시성 | 90 | **+13** | 103 |
| 패턴 | 91 | **+12** | 103 |
| DB | 92 | **+10** | 102 |
| 네트워크 | 93 | **+9** | 102 |
| 기타 | 137 | 0 | 137 |
| **합** | 590 | **+60** | **650** |

- 배분 근거: 코어 5개를 ~102.6 동등선으로 끌어올리는 워터필링(5L−453=60 → L=102.6). 자료구조 최대 가중(long pole, round-003 다음 후보 1번).

## 수용 게이트 (spec 고정 + 이 라운드 적용)

1. **validator 100%** — 길이/카테고리/null/keyword 형식/alias 최소1+한글1/keyword 유니크. 신규 batch 전용.
2. **critic-v2 통과** — nuanced 4종(ALIAS_STRICT·ETYMOLOGY_FACT·NAMING_COHERENCE·NAMING_CLOSING). 임시챗 격리.
3. **scope_diff: scope_leak 0** — 재생성 있었으면 before/after/failed로 확인.
4. **dedup — 완전매칭 기준**. 정규화 비교는 informational만.
5. **분포 보정** — 신규 기타 0, 코어 균등화 완료.
6. **비가역 게이트** — swap·머지 커밋은 smoke 통과 후 **사람 승인**.

## 라운드 결과 (2026-06-20, 통과·머지 완료)

- batch size: 60 → **terms.json 590 → 650** (무손실 swap, 기존 590 변경 0·소실 0)
- Generator 라운드 수: cycle1 생성(30+30=60) → cycle2 길이경계 9개 재생성(validator)
- 최종 통과율: **60/60**

### 검증 결과
- **validator (결정론, 최종 게이트)**: cycle1 **51/60** → 길이경계 9개 재생성 → 합본 **60/60**.
  - 실패 9건 전부 **길이 경계 미세 위반**(내용·사실 문제 0): namingReason 하한(142~149자, 150 미달) 6건 + etymology 상한(123~140자, 120 초과) 5건(tcp-bbr·alpn은 둘 다). 실패 keyword: min-max-heap·adjacency-list·transaction-script·lazy-initialization·delegation-pattern·command-query-separation·raii·tcp-bbr·alpn.
  - **round-003 개선 효과 지속**: summary 하한 미달 **0건**(round-002 cycle1 16/40에서 19건이 summary 미달이었음). "23~27자 한가운데" 명시 효과 유지. 이번 실패는 summary가 아닌 namingReason 하한·etymology 상한 경계.
- **critic-v2 (격리 탭, nuanced 4종)**: **60 passed / 0 failed.** passed∪failed = 입력 60 일치(무결성 OK).
- **scope_diff**: `clean=true`, scope_leak 0, missing_change 0 — 재생성 9건만 변경, 나머지 51 무변경.

### critic 고유 검출 = 0 (round-003의 1건과 대비)
- 이번 라운드 critic-v2 고유 검출 **0건**. 누적 추이: **round-001=0 · 002=0 · 003=1 · 004=0**.
- **해석**: round-003의 split-horizon 오역 검출(1건)은 round별 편차. critic은 평시 0이지만 의미 오역이 들어오면 잡는 안전망 — validator(정량)가 원리적으로 못 보는 영역이라 **유지 판정 불변**(round-003 근거 유효). 4라운드 누적 고유검출 1건/총 150 entry.

### dedup / 충돌 (코드 게이트, 완전매칭 기준)
- 큐레이션 단계: 기존 590의 `{keyword} ∪ {모든 alias}` **완전매칭** 차집합 → 후보 60 전부 충돌 0. 정규화 유사도도 0(informational). keyword 형식 위반 0.
- 머지 직전 alias 전수 재검(재생성 반영): 신규 keyword/alias vs 기존 완전매칭 **0**, 신규 내부 중복 **0**.
  - **acronym 대문자 alias 5건**(raii↔RAII·alpn↔ALPN·mtls↔mTLS·phaser↔Phaser·exchanger↔Exchanger): keyword와 자기 alias가 대소문자만 다른 경우. **기존 DB 선례 60건**(acid/ACID·api/API·dns/DNS·cqrs/CQRS …)의 확립된 관례라 유지. 검색은 `.lowercased()`라 무해.

### smoke (코드 레벨 결정론, BundleDBService 복제)
- 디코딩: 650 전부 TermEntry 6필드·타입 OK (Codable 디코딩 실패 0).
- keyword 유니크(검색 결정론) OK, 카테고리 enum 위반 0.
- 검색 결정론: 신규 keyword(disruptor·y-fast-trie·raii·hash-join·sliding-window·quotient-filter)·alias(LCT·쿠쿠 필터·순환 배리어·병합 조인·토큰 버킷·mTLS·CQS·인접 리스트)·autocomplete prefix(`link`·`two-phase`·`go-`·`mono`) 전부 단일 결정론 매칭 PASS.
- (워크트리 비밀파일 부재로 풀 Xcode 빌드 대신 디코딩·검색 경로 결정론 검증 — handoff 규칙 준수.)

### 분포 (코어 균등화 완료)
- 카테고리(누적): 자료구조 **103**(+16) / 동시성 **103**(+13) / 패턴 **103**(+12) / DB **102**(+10) / 네트워크 **102**(+9) / 기타 137(불변). 합 650.
  - **카테고리 재분류 없음**: round-003의 balking(패턴→동시성) 같은 generator 재분류 0. 발주 분류대로 생성됨(disruptor→동시성, raii→패턴 등 큐레이션 의도 유지).
  - 기타 비중 23.2%→**21.1%**. long pole이던 자료구조가 103으로 코어 최상위와 동률 도달.

### 관찰
- **①** batch 60(30+30 분할 생성)이 품질 저하 없이 통과 — 분할 paste 전략 유효. cycle1 51/60(85%)로 round-003 cycle1 39/40(98%)보다 통과율은 낮았으나, 실패가 전부 길이 경계 ±10자 미세 위반이라 재생성 1회로 100% 도달(왕복 1 cycle 추가로 흡수).
- **②** 실패 유형이 round-002(summary 하한)·round-003(etymology 상한 1건)과 달리 **namingReason 하한**에 몰림(6/9). generator가 batch가 커지면 후반 entry의 namingReason을 짧게 끝내는 경향. 분할 생성에도 각 chunk 내 후반부에서 나타남 → 향후 대량 batch 시 namingReason 하한도 프롬프트에서 "180자 이상" 강조 여지(이번엔 재생성으로 충분).
- **③** critic 고유검출 0 — round-003 1건은 편차로 확인. critic은 안전망으로 유지하되 평시 비용 대비 검출률은 낮음(누적 1/150).

## 오케스트레이터 결정 / 판정 (2026-06-20)

- **머지 승인(비가역)**: 사람 승인 완료 → swap·커밋 진행.
- **acronym 대문자 alias**: 기존 선례 60건 관례로 유지(무변경).
- **카테고리**: generator 분류 = 발주 분류 일치, 재분류 없음.
- **게이트 종합 판정: PASS.** validator 60/60 · critic-v2 0 fail · scope_leak 0 · dedup(완전매칭) 0 · smoke 전부 통과 · 무손실(기존 590 무변경).
- **목표 N=650 도달.** Phase 6 확장 종결. 추가 확장은 출시 후 analytics 기반(Later 백로그)으로 이관 — round-005 핸드오프 불필요.

## 측정 누적 (Phase 6 종합)

| 라운드 | batch | 도달 | validator cycle1 | critic 고유검출 |
|---|---|---|---|---|
| 001 (POC) | 10 | 510 | — | 0 |
| 002 | 40 | 550 | 16/40 (summary 하한) | 0 |
| 003 | 40 | 590 | 39/40 | 1 (split-horizon 오역) |
| 004 | 60 | **650** | 51/60 (namingReason 하한) | 0 |

- critic 누적 고유검출: 1건 / 150 entry. validator 미커버(의미 오역) 영역 안전망으로 유지.
- 수동 claude.ai 정액 흐름으로 목표 N=650 완주. Phase 7 자동화 미진입(잔여 규모상 API 종량 < 정액 한계비용 0 판단, 유지).
