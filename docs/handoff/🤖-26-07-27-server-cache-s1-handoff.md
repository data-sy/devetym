# 🤖 서버 캐시 S1 — 비준 착수 핸드오프

> # ⛔ 역할 종료 — 이 문서로 세션을 시작하지 마시오
>
> **2026-07-27 비준 완료.** 이 문서가 지시한 작업(스펙 비준)은 끝났고, §5 열린 질문 6건은 전부 판정됐다.
> 아래 §3(현재 상태)·§4(블로커)는 **stale**이다 — untracked 2건은 커밋됐고, 프록시 `main` 블로커는 PR #2 병합으로 해소됐다.
>
> **구현을 시작하려면 → [`🤖-26-07-27-server-cache-s1-impl-handoff.md`](🤖-26-07-27-server-cache-s1-impl-handoff.md)**
>
> 이 문서는 비준 당시의 판단 근거를 남기기 위해 **이력으로만** 보존한다.

---

> **성격: 콜드 세션 착수용 실행 문서.** 이 문서를 읽는 세션은 **이전 대화 맥락이 없다고 가정**한다. 필요한 배경은 아래에 전부 있고, 더 필요하면 §7 참조 경로를 직접 읽는다.
>
> **작성**: 2026-07-27 (조사·스펙 작성 세션). **다음 세션이 할 일 = 스펙 비준.**
> **진행 상태 정본은 이 문서가 아니라 [`../../ROADMAP.md`](../../ROADMAP.md)** — 충돌하면 ROADMAP을 신뢰한다.

---

## 1. 한 줄 요약

`devetym-proxy`(Cloudflare Worker)에 **D1 read-through 캐시**를 붙이는 첫 슬라이스(S1)의 **스펙이 작성됐고, 비준 전 단계**다. 코드는 한 줄도 안 짰다.

**이번 세션의 산출 목표**: 스펙을 적대적으로 비준해 §5 열린 질문 6건을 판정하고, 결함을 스펙에 반영한다. **구현은 비준 이후 별도 세션.**

---

## 2. 왜 하는가 (배경 최소 세트)

devetym 앱은 검색어를 3단으로 찾는다: **번들 650개 → 기기 로컬 캐시 → Claude API(프록시 경유)**.

문제는 **로컬 캐시가 기기 안에만 산다**는 것. A가 생성시킨 어원 항목은 A 폰에만 저장되고 서버엔 안 남는다. 그래서 **B가 같은 용어를 검색하면 Anthropic을 다시 호출해 재과금**된다(건당 $0.02~0.04).

- Console 월 하드캡 **$30**, 프록시 전역 캡 **200회/일**(하루 최대 ~$8)
- 즉 캡을 채우면 **닷새면 월 예산 소진** → 남은 기간 전 사용자가 402를 본다
- 서버 캐시가 들어가면 비용이 "사용자 수"가 아니라 **"새 용어 수"에 비례**하게 바뀐다 (INV-12 플라이휠)

설계는 이미 **ADR-0006에서 Accepted**이고 불변식 INV-1~13도 락돼 있다. **미구현된 건 서버 코드뿐**이다. 클라이언트는 이미 이 계약에 맞춰 지어져 있어서 **앱 재배포 없이 서버 배포만으로 효과가 난다**(INV-1 투명성).

---

## 3. 현재 상태 (2026-07-27 기준 실측)

### 3-1. 저장소·브랜치

| repo | 브랜치 | 작업트리 | 비고 |
|---|---|---|---|
| `~/devetym` (앱·문서) | `main` | 깨끗 + **untracked 2건**(§3-3) | ⚠️ origin보다 **2 커밋 앞섬**(미푸시): `b4d6b3e`, `9673362` |
| `~/devetym-proxy` (서버) | `feat/enable-usage-d1` | 깨끗 | ⚠️ **`c7218db`가 `main`에 미병합**(§4-1) |

### 3-2. 서버(`devetym-proxy`) 실제 상태

- `src/index.js` **204줄 바닐라 JS 단일 파일**. 프레임워크 없음
- 하는 일: 키 주입(`ANTHROPIC_API_KEY` 시크릿) → 기기/전역 일일 한도(KV) → 과금 파라미터 서버 강제 → Anthropic 중계 → 402/429 매핑 → usage 기록
- 바인딩: `RATE_LIMIT`(KV) · `USAGE_DB`(D1 `devetym-usage`)
- **term 캐시는 없다.** 마이그레이션은 `0001_usage_log.sql` 하나뿐이고 그건 비용 관측용 로그다
- 테스트 프레임워크 **전무** (`package.json` 스크립트 = dev/deploy/tail 3개)
- 프로덕션 배포본 = `c5cd809f` (2026-07-14)

### 3-3. 이번 작업으로 생긴 파일 (둘 다 **untracked**, 커밋 위치 미정)

1. **`docs/specs/server-m0-m1-cache-read-through-draft.md`** ← **비준 대상**
2. `docs/handoff/🤖-26-07-27-server-cache-s1-handoff.md` (이 문서)

---

## 4. 착수 전 블로커 3건 ⚠️

### 4-1. `devetym-proxy` main이 프로덕션보다 낡음 — **사람 승인 필요**

`c7218db`(usage D1 바인딩 기입)가 `feat/enable-usage-d1`에만 있고 `main`엔 없다. 그런데 **프로덕션엔 배포돼 있다**. 이 상태에서 `main`에서 브랜치를 따면 **`USAGE_DB` 바인딩 없는 베이스** 위에 캐시를 짓게 된다.

- 기존 패턴은 PR(프록시 PR #1이 그랬다) — PR #2로 갈지 로컬 병합만 할지 **사람이 결정**
- **하네스·AI가 push/머지하지 않는다** (안전 규율 §6)

### 4-2. 비준 프로파일 렌즈 미스매치 — **이번 세션의 핵심 판단 지점**

비준 정본 도구는 `~/dev/agent-harnesses/engine/ratify-spec.workflow.js`이고, 도메인 렌즈는 `profiles/devetym.md`다. 그런데 그 프로파일은 **KMP 클라이언트 전용**이다:

> **입력 단위**: 마일스톤별 슬라이스 `docs/specs/mN-...-draft.md`
> **수렴 오라클**: `./gradlew :shared:test` + Android assemble + iOS 빌드가 모두 green … *비준 단계의 리뷰어는 이 오라클로 검증 가능하게 스펙이 쓰였는지도 본다 — 스펙이 양 플랫폼 빌드로 확인 불가능한 주장을 하면 **측정 불가 결함***

S1은 **Cloudflare Worker/JS/D1** 슬라이스고 오라클도 gradle이 아니라 **wrangler/vitest**(스펙 §5 S축1~4)다. 이 렌즈로 그대로 돌리면 리뷰어가 "gradle 삼중 게이트로 측정 불가"를 **거짓 결함으로 대량 리포트**한다.

**선택지**:

| | 방법 | 장 | 단 |
|---|---|---|---|
| **A** | `profiles/devetym.md`에 서버 렌즈 추가 후 ratify-spec | 정석. 서버 슬라이스가 앞으로 더 나오므로(M2 DO·M3 게이트) 어차피 필요 | **하네스 수정은 격리 세션 + `personas/harness-engineer.md` 규율** → 세션이 하나 더 늘어남 |
| **B** | 프로파일 없이 `/design-review`(제네릭)로 1차 검증 | 렌즈 미스매치 회피, 즉시 가능 | 비준 강도가 ratify-spec보다 낮음 |
| **C** | ratify-spec에 오라클 차이를 인자로 주입 | 세션 1개로 끝 | 워크플로가 그런 인자를 받는지 **미확인** |

**직전 세션 권고**: **B로 1차 검증**해 §5 열린 질문과 명백한 구멍을 걷어내고 → **A(별도 격리 세션)로 서버 렌즈를 만든 뒤 ratify-spec 정식 비준**. 다만 이건 권고일 뿐 **이번 세션이 재판정해도 된다.**

### 4-3. `~/.claude/workflows/` 심링크 부재

`agent-harnesses/README.md`는 `engine/*.workflow.js`를 `~/.claude/workflows/`에 심링크하면 `Workflow({name:'ratify-spec', ...})`로 호출 가능하다고 안내하는데, **해당 디렉토리가 없다**(`ls` 결과 없음). ratify-spec을 쓰려면 이것부터 확인·복구해야 한다. (`~/.claude/commands/`의 `design-review.md`·`audit-doc.md` 심링크는 **정상**.)

---

## 5. 비준이 판정해야 할 열린 질문 6건

스펙 §7 원문 참조. 요지만:

| # | 질문 | 잠정안 |
|---|---|---|
| **7-1** | 캐시 히트가 일일 한도를 소모해야 하나 | **소모 안 함** — 히트는 비용 0이고 한도의 목적은 비용 방어. 기존 코드도 "성공(=토큰 비용 발생)한 호출만 가산" |
| **7-2** ★ | `not_dev_term`/`possible_typo`도 캐시할 것인가 | **가장 무거운 판정.** 캐시하면 무작위 문자열 남용 방어(INV-10)에 직접 기여하나 **오판을 동결**한다. 무효화 경로(INV-5)가 이번 슬라이스 OUT이라 정정 수단이 수동 DELETE뿐. 잠정 = 3분기 다 캐시 + `branch` 컬럼 구분 |
| **7-3** | 새 D1(`devetym-cache`) vs 기존 `devetym-usage`에 테이블 추가 | **분리** 제안(캐시=정본, usage_log=폐기 가능 텔레메트리). 단 무료 플랜 D1 DB 개수 한도 확인 필요 |
| **7-4** | `@cloudflare/vitest-pool-workers` 도입 비용 | 서버 트랙에 green 오라클이 없어 신규 정의가 필요. 실제 D1을 돌리는 S축2가 load-bearing. 도입 제안하되 비용 대비 타당성 판정 요청 |
| **7-5** | `PROMPT_VERSION` 문자열 명명 | ADR-0007이 형상을 *"v1 5변경 + v2 Path A"*로 락했으나 **기계 판독 가능한 버전 문자열이 코드에 없다**. 제안 = `"v2-pathA"`, `SCHEMA_VERSION = 1` |
| **7-6** | 프롬프트 개정 시 캐시 무효화 부재를 수용할 것인가 | INV-5 gated 무효화는 이번 OUT. 개정하면 **구버전 payload가 계속 서빙됨**. "알려진 상태"로 문서화 + 백로그 명시 제안 |

### 비준이 특히 봐야 할 곳 (직전 세션의 자기 신고)

스펙 저자(직전 세션)가 **스스로 불확실하다고 느낀 지점**을 정직하게 남긴다:

- **§3-4 캐시 히트 응답 합성** — 이 슬라이스의 핵심 계약. 클라가 Anthropic 응답 shape를 그대로 파싱하므로 히트 시 서버가 그 shape를 **합성**해야 한다. 클라 코드(`ClaudeResponse`·`toTermResult()`)를 읽고 성립을 확인했으나, **실제 왕복 실측은 아직 없다.** 여기가 틀리면 슬라이스 전체가 무너진다
- **§3-2 term key 동치** — 클라 `normalizeKeyword`(`AppJson.kt:31` = `trim().lowercase()`)와 서버 JS가 갈라지면 증상이 **"조용한 영구 캐시 미스"**라 발견이 어렵다. 동치 테스트로 고정했으나 커버가 충분한지 의심스럽다
- **§5 green 오라클** — 서버 트랙 첫 정의라 **선례가 없다.** S축1~4가 실제로 수렴 오라클로 기능하는지 검증 안 됨
- **§0 M2(single-flight) 제외 결정** — "현 트래픽에선 동시 충돌 확률이 낮아 수용"이 근거인데, **정량 근거 없이 정성 판단**이다

---

## 6. 안전 규율 (이번 세션이 **하지 않을 것**)

- **push·머지·`-draft` 제거 금지** — 전부 사람 게이트 (`agent-harnesses/README.md` 계승)
- **`devetym-proxy` 코드 작성 금지** — 이번 세션은 비준까지. 구현은 비준 통과 후 별도 세션
- **D1 `--remote` 마이그레이션 금지** — 되돌리기 어렵다. 사람 승인 후에만
- **하네스 수정 금지** — `agent-harnesses/` 변경은 격리 세션 + `personas/harness-engineer.md` (제어면/규범 문서 분리)
- **ADR·INV 수정 금지** — 이 슬라이스는 제약을 **소비**하지 생성하지 않는다. 비준에서 INV 자체에 결함이 발견되면 **수정하지 말고 사람에게 보고**
- **Anthropic 실호출 금지** — 이번 세션 비용 0. (구현 세션의 스모크 1회 $0.03만 승인됨)
- 시스템 프롬프트·도구 스키마·한도 상수는 손대지 않는다

---

## 7. 참조 경로

**비준 대상**
- `docs/specs/server-m0-m1-cache-read-through-draft.md` ← 이것

**제약 정본 (재론 금지)**
- `docs/cache-delivery-milestones.md` §1 — INV-1~13
- `docs/adr/0006-server-cache-boundary.md` — 계약 정본(ADR-0004 대체)
- `docs/adr/0004-backend-proxy-boundary.md` — 계승 계약(프롬프트 위치·429·tool_use 3분기)
- `docs/adr/0007-ai-prompt-quality.md` — 프롬프트 버전 형상

**클라 계약 (서버가 맞춰야 할 상대)**
- `shared/src/commonMain/kotlin/com/robin/devetym/data/remote/ClaudeApi.kt` — 호출·오류 매핑
- `.../data/remote/ClaudeDto.kt` — `ClaudeResponse`·`toTermResult()` 3분기
- `.../data/remote/ClaudePrompt.kt` — `buildClaudeRequest`(keyword가 `messages[0].content`)
- `.../data/AppJson.kt:31` — `normalizeKeyword` ★ 동치 대상
- `.../model/Category.kt` — 정본 6집합(INV-13 clamp 기준)
- `.../model/TermEntry.kt` — wire 키 계약·버전 필드

**서버 현행**
- `~/devetym-proxy/src/index.js` · `wrangler.toml` · `migrations/0001_usage_log.sql`

**비용 맥락**
- `docs/cost/cost-management-decision.md` — 비용 지도·3층 방어
- `docs/cost/console-settings-log.md` — Console 설정 스냅샷(append-only)

**하네스**
- `~/dev/agent-harnesses/README.md` · `profiles/devetym.md` · `engine/ratify-spec.workflow.js` · `reviews/design-review.md`

---

## 8. 이번 세션 종료 시 남길 것

1. **비준 verdict** — §5 6건 판정 + 신규 발견 결함
2. **스펙 갱신** — 판정 결과를 `-draft.md`에 반영(`-draft` 접미사는 **제거하지 않는다**)
3. **ROADMAP 갱신** — 백로그 항목 `I · devetym-proxy read-through 캐시 확장`에 진행 상태 기록 (**진행 상태 정본은 디스크 로드맵** — 메모리에 status를 복제하지 않는다)
4. **커밋 위치 결정** — untracked 2건(§3-3)을 `main` 직접 vs 새 브랜치 중 어디에 올릴지 사람과 확정
