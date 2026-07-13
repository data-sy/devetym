# 번들 DB 확장 (db-expand) — 인덱스

개발 어원 사전의 번들 DB(`shared/src/commonMain/composeResources/files/terms.json`)를 출시 전 확장한 작업의 문서함.

## 상태: ✅ 종결 (2026-06-20)

- **목표 N=650 도달.** 500 → 510 → 550 → 590 → **650** (round-001~004).
- 코어 5개 카테고리 균등화 완료: 자료구조 103 · 동시성 103 · 패턴 103 · DB 102 · 네트워크 102 · 기타 137.
- 방식: claude.ai 2탭(Generator/Critic) 수동 batch → 코드 게이트(validator·critic-v2·scope_diff·merge·smoke) → 무손실 swap.
- 진행 상태 정본: [`../../ROADMAP.md`](../../ROADMAP.md) "Now"(Done 이관 대상).
- 추가 확장은 출시 후 Firebase Analytics 검색 빈도 기반(ROADMAP "Later")으로 이관 — 새 핸드오프 불필요.

## 디렉토리 지도

### 정본 (이 폴더)
- [`spec.md`](spec.md) — 실행 스펙(목표·게이트·길이 규약·확정 결정). **단일 정본.**
- [`runbook-manual-round.md`](runbook-manual-round.md) — 수동 라운드 실행 런북.
- [`rounds/`](rounds/) — 라운드별 산출물·기록. **불변 히스토리.**
  - `round-00N.json` — 그 라운드 신규 batch(검증 통과본).
  - `round-00N.md` — 발주·결과·게이트·관찰 기록.
  - `round-001-consistency-{A,B}.*` — Phase 2 일관성 점검(베이스라인·chat↔API drift).

### 활성 도구 (`Scripts/db-expand/`)
출시 후 추가 확장 시 **재사용**. 제자리 유지.
- `validator.py` — 결정론 정량 게이트(길이·카테고리·null·alias·keyword 형식/유니크).
- `merge.py` — 기존 terms.json + 신규 batch 머지(충돌 assert).
- `scope_diff.py` — 재생성 scope_leak 검출(before/after/failed).
- `prompts/` — `v2-batch.paste.md`·`critic-v2.paste.md`(복붙 운용 정본) + `*.md`(참조 원본).
- `keywords-round-00N.txt` — 라운드별 큐레이션 입력(아카이브).
- `.env.local` — API 키(gitignore, 로컬 전용).

### 아카이브 ([`archive/`](archive/))
작업 완료된 세션 스캐폴딩. 히스토리 보존용, 재실행 안 함.
- `handoff-phase6.md` · `handoff-round-003.md` · `handoff-round-004.md` — 라운드별 세션 핸드오프.
- `session-orchestrator.md` · `session-phase6-executor.md` — 세션 역할 정의(2세션 분리 운용기).

## 흐름 한눈에

```
keyword 큐레이션+dedup(완전매칭) → Generator(탭A) → validator(100%까지 재생성)
  → critic-v2(탭B 임시챗 격리) → scope_diff → merge → smoke → 사람 승인 → swap → 기록
```
