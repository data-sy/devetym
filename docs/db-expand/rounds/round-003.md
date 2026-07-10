# 라운드 003 — 발주·실행 (수동 라운드, 오케스트레이터+밸리데이터 세션)

> **상태: 종결 (2026-06-20). 머지 완료 (550→590), 게이트 전부 PASS.**
> 진행 상태 정본은 디스크 — 충돌 시 `ROADMAP.md` "Now" · `spec.md` · 이 문서를 신뢰.
> 핸드오프: `docs/db-expand/archive/handoff-round-003.md`. 흐름: `Generator(claude.ai 탭A) → validator → critic-v2(탭B 격리) → 재생성 → scope_diff → merge → swap`.

## 성격 (왜 이 라운드인가)

- **claude.ai 정액 수동 유지 결정(round-002 판정) 이후 첫 라운드.** 목표 N=650, 잔여 +100을 수동 라운드로 흡수하는 1/N.
- round-002 개선 3종 실적용 검증: ① **dedup 완전매칭 기준** ② **critic 임시챗 격리 런북**(`*.paste.md` 복붙 전용 파일 신설) ③ **summary 하한 여유**(프롬프트에 "23~27자 한가운데" 명시).

## 발주 범위 (사람 확정 2026-06-20)

- **batch size: 40** (550 → 590)
- **카테고리 배분 — 목표 N=650 코어 균등화 결손(→103) 비례, 기타 0:**

| 카테고리 | 현재 | 발주 | 도달(실제) |
|---|---|---|---|
| 자료구조 (long pole) | 76 | **+11** | 87 |
| 동시성 | 80 | **+9** | 90 |
| DB | 85 | **+7** | 92 |
| 네트워크 | 86 | **+7** | 93 |
| 패턴 | 86 | **+6** | 91 |
| 기타 | 137 | 0 | 137 |
| **합** | 550 | **+40** | **590** |

- 배분 근거: 목표 103 대비 결손(자료구조 27·동시성 23·DB 18·네트워크 17·패턴 17, 합 102)에 비례. 자료구조 최대 가중(long pole 우선).
- 핸드오프 본문은 "DB 위주 보강"이라 적혔으나 균등화 수치상 DB는 max 근접(결손 작음)이라 자료구조 최우선으로 발주(사람 확정). round-002 균등화 원칙과 일관.

## 수용 게이트 (spec 고정 + 이 라운드 적용)

1. **validator 100%** — 길이/카테고리/null/keyword 형식/alias 최소1+한글1/keyword 유니크. 신규 batch 전용.
2. **critic-v2 통과** — nuanced 4종(ALIAS_STRICT·ETYMOLOGY_FACT·NAMING_COHERENCE·NAMING_CLOSING). 임시챗 격리.
3. **scope_diff: scope_leak 0** — 재생성 있었으면 before/after/failed로 확인.
4. **dedup — 완전매칭 기준** (round-002 관찰② 반영). 정규화 비교는 informational만.
5. **분포 보정** — 신규 기타 0, 코어 균등화 방향 일치.
6. **비가역 게이트** — swap·머지 커밋은 smoke 통과 후 **사람 승인**.

## 라운드 결과 (2026-06-20, 통과·머지 완료)

- batch size: 40 → **terms.json 550 → 590** (무손실 swap, 기존 550 변경 0·소실 0)
- Generator 라운드 수: cycle1 생성(40) → cycle2 HAMT 재생성(1, validator) → cycle3 split-horizon 재생성(1, critic)
- 최종 통과율: **40/40**

### 검증 결과
- **validator (결정론, 최종 게이트)**: cycle1 **39/40**(실패 1건: `hash-array-mapped-trie` ETYMOLOGY_LEN 125자, trie 설명 중복) → 재생성 1/1 → 합본 **40/40**.
  - **round-002 개선 효과 확인**: summary 하한 미달 **0건** (round-002 cycle1은 16/40, 실패 24건 중 19건이 summary <20자였음). 프롬프트 "23~27자 한가운데" 명시가 왕복을 크게 줄임 — cycle1 39/40.
- **critic-v2 (격리 탭, nuanced 4종)**: cycle1 **39 passed / 1 failed** → 재생성 후 **40 passed / 0 failed.** passed∪failed = 입력 일치(무결성 OK).
- **scope_diff**: `clean=true`, scope_leak 0, missing_change 0 — 재생성 2건(HAMT·split-horizon)만 변경, 나머지 38 무변경.

### ⭐ critic 고유 검출 = 1 (round-001·002 연속 0 깨짐)
- **critic-v2가 validator 외 유니크하게 잡은 nuanced 문제 1건** — 3라운드 만의 첫 고유 검출.
  - `split-horizon` alias **`수평 분할`**: RULE_ALIAS_STRICT 위반. horizon(지평선·시야)을 horizontal(수평)로 오역 → 실제 "수평 분할"은 별개 개념(horizontal partitioning/샤딩)을 가리킴. entry 자신의 etymology가 horizon을 "지평선·시야 범위"로 풀이하는 것과 모순.
  - 처리: '수평 분할' 제거, 음차 '스플릿 호라이즌'만 유지(한글 alias 최소1 충족). 재생성 → validator·critic 재통과.
- **의미**: critic 추가 축소/제거 신호(2연속 0)를 **뒤집는 데이터**. 의미 오역은 validator(정량)가 원리적으로 못 잡는 영역 — critic 유지 근거 확보. Phase 6 누적 판단에 입력.

### dedup / 충돌 (코드 게이트, 완전매칭 기준)
- 큐레이션 단계: 기존 550의 `{keyword} ∪ {모든 alias}` **완전매칭** 차집합 → 후보 40 전부 충돌 0. 정규화(공백·기호 제거) 유사도도 0(informational). keyword 형식 위반 0.
- 머지 직전 alias 전수 재검: 신규 keyword/alias vs 기존 완전매칭 **0**, 신규 내부 alias 중복 **0**.
- **round-002 관찰② 반영 확인**: round-002의 `lazy loading` false-positive 같은 정규화 과잉 검출 없음(완전매칭 기준 적용).

### smoke (코드 레벨 결정론, BundleDBService 복제)
- 디코딩: 590 전부 TermEntry 6필드·타입 OK (Codable 디코딩 실패 0).
- 검색 결정론: 신규 keyword(표본10)·alias(HAMT/정족수/DLX/STP/쓰기 증폭/보킹)·autocomplete prefix(`scat`→scatter-gather, `read-`→read-repair 포함) 전부 PASS. keyword 유니크(검색 결정론) OK.
- (워크트리 비밀파일 부재로 풀 Xcode 빌드 대신 디코딩·검색 경로 결정론 검증 — handoff 규칙 준수.)

### 분포
- 카테고리(누적): 자료구조 **87**(+11) / 동시성 **90**(+10) / DB **92**(+7) / 네트워크 **93**(+7) / 패턴 **91**(+5) / 기타 137(불변). 합 590.
  - **balking 분류 이동**: 큐레이션은 패턴(+6 발주)이었으나 generator가 `동시성`으로 분류 → 동시성 디자인 패턴(POSA/Java)으로 타당해 유지. 결과 동시성 +10·패턴 +5.
  - 기타 비중 24.9%→23.2%. long pole 자료구조 87로 보강했으나 목표 103 대비 여전히 최소(다음 라운드도 자료구조 우선).

### 관찰
- **①** round-002 관찰①(summary 하한 상습 미달)이 **프롬프트 보정으로 해소**됨 — cycle1 길이 실패가 24건→1건(그 1건도 summary 아닌 etymology 상한 초과). 개선 후보②(summary 하한 여유) **유효 확인**.
- **②** `*.paste.md` 복붙 전용 파일(격리 지시 단락 + `---` 본문, 주석부 제외) 신설로 critic 격리 운용 안정화 — round-002 관찰③(주석부 통째 paste → 메모리 오염) 재발 없음. 개선 후보③ **유효 확인**.
- **③** critic 고유 검출 1건(split-horizon)은 generator가 한국어 alias를 만들 때 영단어 형태(horizon↔horizontal)를 의미가 아닌 철자 유사로 옮기는 오류 유형. 향후 alias 사실성 점검의 대표 케이스.

### 다음 라운드 변경 후보
1. **자료구조 계속 long pole** — round-004도 자료구조 최대 가중(87 → 103 목표, +16 잔여).
2. **alias 오역 점검** — 관찰③ 유형(영단어 형태 유사로 인한 한국어 alias 오역). critic ALIAS_STRICT가 유효하게 작동 중이므로 흐름 유지.
3. **잔여 +60** (590→650): 자료구조 16·패턴 12·동시성 13·DB 11·네트워크 10 안팎. 수동 1~2라운드.

## 오케스트레이터 결정 / 판정 (2026-06-20)

- **머지 승인(비가역)**: 사람 승인 완료 → swap·커밋 진행.
- **balking 카테고리**: generator 동시성 분류 유지(동시성 디자인 패턴으로 타당).
- **split-horizon alias**: '수평 분할' 제거 확정(오역, critic 검출) — 음차만 유지.
- **게이트 종합 판정: PASS.** validator 40/40 · critic-v2 0 fail · scope_leak 0 · dedup(완전매칭) 0 · smoke 전부 통과.
- **critic 유지 판정 갱신**: round-003에서 critic 고유 검출 1건 발생(2연속 0 깨짐). 의미 오역은 validator 미커버 영역 — **critic 유지**(축소/제거 보류). 수동 유지 결정(round-002)과 정합.
