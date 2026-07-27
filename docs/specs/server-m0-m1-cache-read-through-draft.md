# 서버 슬라이스 S1 (draft) — D1 스키마 + Worker read-through 캐시

> **성격: 비준 하네스 입력용 spec 슬라이스(`-draft.md`).** [`cache-delivery-milestones.md`](../cache-delivery-milestones.md)의 캐시 트랙 **M0(데이터 기반) + M1(Worker read-through)** 을 실제 코드베이스에 착지시킨 구현 스펙. 그 문서 §1 INV-1~13이 재론 금지 제약이고, 이 문서는 그 제약 아래의 **구현 결정**을 정한다.
>
> 이 문서는 **자율 구현 전 적대적 비준(완결성·모호성·테스트 가능성·정합성)의 대상**이다. 아래 §7 열린 질문은 비준이 판정할 항목이다.

- **트랙**: 서버(별도 repo `devetym-proxy`) — CMP 클라 마일스톤(M0~M9)과 번호 공간이 다르다. 혼동을 막으려 `S1`로 표기한다.
- **캐시 트랙 대응**: M0(term key 정규화·D1 스키마·entry 계약) + M1(Worker 조회 순서·write-back·멱등 write)
- **repo·브랜치**: `~/devetym-proxy`, `feat/s1-read-through-cache` (베이스 = **`main`에 `c7218db` 병합 후**의 main — §8-1 선행 조건)
- **범위 확정**: M0+M1만. **single-flight(DO)·품질 게이트·402 검색어 수집은 이 슬라이스 밖**(2026-07-27 사람 확정 — §0)
- **참조**: [ADR-0006](../adr/0006-server-cache-boundary.md)(계약 정본), [ADR-0004](../adr/0004-backend-proxy-boundary.md)(계승 계약), [ADR-0001(iOS)](https://github.com/data-sy/dev-etymology)(호스팅 선택), [INV-1~13](../cache-delivery-milestones.md), [ADR-0007](../adr/0007-ai-prompt-quality.md)(프롬프트 버전 형상)

---

## 0. 스코핑 판정

**S1 = M0 + M1.** single-flight(M2)·품질 게이트(M3)·승격 잡(M5)은 후속 슬라이스로 뗀다.

근거:
- **비용 절감 효과가 실제로 발생하는 최소 단위**가 M0+M1이다. M0만으로는 동작 변화가 0(저장소만 생기고 아무도 안 읽음)이라 슬라이스로서 무의미하고, M2까지 넣으면 Durable Objects라는 **새 바인딩·새 실행 모델·무료 플랜 가용성 확인**이 한 슬라이스에 겹쳐 비준 대상이 비대해진다.
- M2 없이도 **중복 저장은 이미 막힌다** — `term_key` unique + `INSERT ... ON CONFLICT DO NOTHING`(INV-4 저장 측). M2가 없어서 남는 손해는 "캐시 빈 상태 동시 요청 시 중복 **생성 비용**"뿐이고, 현 트래픽(전역 200/일 캡)에서 동일 용어 동시 충돌 확률은 낮다. **의도적으로 수용하고 후속 슬라이스로 뗀다.**
- 서버 트랙은 이번이 **첫 슬라이스**라 green 오라클(§5)이 아직 존재하지 않는다. 오라클을 세우는 비용이 이 슬라이스에 얹히므로 기능 범위는 좁게 잡는다.

**이 판정이 남기는 부채(명시)**: 동시 요청 중복 생성(M2), validator write-게이트(M3). 둘 다 §7·ROADMAP 백로그에 남긴다.

---

## 1. 목표 (이 슬라이스가 내는 것)

한 사용자가 생성시킨 어원 항목을 **다른 사용자가 재사용**해 Claude API 호출을 재과금하지 않는다.

- 현재: 번들 650개 미스 → **매 사용자마다** Anthropic 과금
- S1 이후: 번들 미스 → **D1 히트면 무과금**, 전역 최초 1회만 과금(INV-2 write-once)
- **클라이언트 무변경**(INV-1 투명성) — 앱 재배포 없이 서버 배포만으로 효과 발생

---

## 2. 스코프

### IN
- D1 스키마 + 마이그레이션 (`entries` / `entry_versions` / `aliases`)
- canonical term key 도출 규칙 (클라 `normalizeKeyword`와 동치)
- Worker 조회 순서: **D1 → 미스 시 Anthropic → 정규화 → write-back**
- 캐시 히트 시 **Anthropic 응답 shape 합성**(클라 계약 유지)
- INV-13 정규화-후-쓰기 (category 6집합 clamp)
- first-write-wins 멱등 write
- 캐시 경로 실패 시 **무해 폴백**(§3-7) + 킬 스위치
- `usage_log`에 `cache_hit` 구분 추가 (기존 마이그레이션 주석이 예고한 항목)

### OUT (후속 슬라이스)
- single-flight / Durable Objects (캐시 M2)
- validator·critic 품질 게이트 (캐시 M3)
- 무효화·재생성 트리거 (INV-5 gated — 스키마 컬럼만 확보, 실행 경로는 미구현)
- 번들 승격 잡 (캐시 M5) / 콘텐츠 팩 동기화 (캐시 M6)
- 402 놓친 검색어 수집 (ROADMAP 백로그 — 2026-07-27 별도 확정)
- "프로그래밍 용어처럼 보이는가" 휴리스틱 (INV-10 잔여)

---

## 3. 산출 명세

### 3-1. D1 스키마 (`migrations/0002_term_cache.sql`)

```sql
-- entries: 용어당 정본 1행. INV-2 write-once — read path 재생성 금지.
CREATE TABLE IF NOT EXISTS entries (
  term_key        TEXT PRIMARY KEY,         -- canonical key (§3-2). unique 제약 = INV-4 first-write-wins
  branch          TEXT NOT NULL,            -- 'term_entry' | 'not_dev_term' | 'possible_typo' (§7-2)
  payload         TEXT NOT NULL,            -- tool_use.input 원문 JSON (정규화 후 — INV-13)
  prompt_version  TEXT NOT NULL,            -- INV-9
  schema_version  INTEGER NOT NULL,         -- INV-9
  created_at      TEXT NOT NULL,            -- ISO 8601 UTC
  hit_count       INTEGER NOT NULL DEFAULT 0 -- INV-12 hot 선정 입력(승격 잡이 소비)
);

-- aliases: React ↔ react.js 접기 (INV-3). AI 응답 TermEntry.aliases에서 채운다.
CREATE TABLE IF NOT EXISTS aliases (
  alias_key  TEXT PRIMARY KEY,              -- canonical 처리된 별칭
  term_key   TEXT NOT NULL REFERENCES entries(term_key)
);

-- entry_versions: INV-5·INV-9 — 교체 시 구버전 보존. S1은 write만(읽기 경로 없음).
CREATE TABLE IF NOT EXISTS entry_versions (
  id             INTEGER PRIMARY KEY AUTOINCREMENT,
  term_key       TEXT NOT NULL,
  payload        TEXT NOT NULL,
  prompt_version TEXT NOT NULL,
  schema_version INTEGER NOT NULL,
  created_at     TEXT NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_aliases_term ON aliases (term_key);
CREATE INDEX IF NOT EXISTS idx_entries_hits ON entries (hit_count DESC);
```

`usage_log`에 캐시 구분 추가(같은 마이그레이션):

```sql
ALTER TABLE usage_log ADD COLUMN cache_hit INTEGER NOT NULL DEFAULT 0;
```

> **주의**: 기존 `migrations/0001_usage_log.sql` 주석이 *"향후 CMP M3의 D1 read-through 캐시가 붙으면 cache_hit 컬럼이 추가될 예정"*이라 이미 예고해 둔 항목이다. 이 슬라이스가 그 예고를 이행한다.

### 3-2. canonical term key 도출

요청 본문에서 키워드를 뽑아 정규화한다.

```js
// 클라는 원본 keyword를 messages[0].content에 담는다
// (devetym shared/.../ClaudePrompt.kt buildClaudeRequest — 대소문자 보존)
const raw = body?.messages?.[0]?.content;
const termKey = typeof raw === "string" ? raw.trim().toLowerCase() : null;
```

⚠️ **클라와 반드시 동치여야 한다.** 클라 정본은 `shared/src/commonMain/kotlin/com/robin/devetym/data/AppJson.kt:31`:

```kotlin
fun normalizeKeyword(s: String): String = s.trim().lowercase()
```

- `toLowerCase()`를 쓴다 — **`toLocaleLowerCase()` 금지**(로케일 의존 시 터키어 `I` 등에서 Kotlin `lowercase()`와 갈라진다).
- 클라가 이 함수를 바꾸면 **서버도 같이 바꿔야 한다**. 갈라지면 증상이 "조용한 영구 캐시 미스"라 눈에 안 띈다 → §6-3 동치 테스트로 고정하고, 클라 `AppJson.kt`에 동기화 지점 주석을 남긴다(devetym repo 측 변경 1줄).
- `termKey`가 `null`/빈 문자열이면 **캐시를 통째로 우회**하고 기존 경로로 통과시킨다(안전 실패 — §3-7).

### 3-3. Worker 조회 순서 (read path)

기존 `src/index.js` 요청 흐름의 **⑤ 본문 파싱·과금 파라미터 강제 직후, upstream fetch 직전**에 삽입한다.

```
① 메서드·기기ID 검사        (기존)
② 일일 한도 (기기·전역)      (기존)
③ 본문 파싱 + 과금 파라미터 강제 (기존)
─────────── 신규 ───────────
④ termKey 도출 (§3-2)
⑤ D1 조회: entries → 미스면 aliases → entries
   └─ 히트 → hit_count 증분(waitUntil) → 합성 응답 반환 (§3-4)   ★ Anthropic 미호출 = 무과금
─────────── 기존 ───────────
⑥ Anthropic 호출
⑦ 402 매핑 / 한도 카운터 가산 / usage 기록
─────────── 신규 ───────────
⑧ 성공 응답 정규화(INV-13) → write-back (waitUntil)
```

**한도 카운터와의 관계 (결정)**: 캐시 히트는 **한도를 소모하지 않는다**. 근거 — 한도(DAILY_LIMIT·GLOBAL_DAILY_LIMIT)의 목적은 *비용 방어*이고 히트는 비용이 0이다. 기존 코드도 이미 *"성공(=토큰 비용 발생)한 호출만 한도에 가산"* 원칙을 명시하고 있어 idiom이 일치한다. 부수 효과로 **사용자 체감 한도가 늘어난다**(캐시된 용어는 10회를 안 깎음) — 의도된 개선.

### 3-4. 캐시 히트 응답 합성 ★ 이 슬라이스의 핵심 계약

클라는 **Anthropic Messages API 응답 shape를 그대로 파싱**한다(`ClaudeResponse` → `toTermResult()` 3분기). 따라서 캐시 히트도 그 shape로 위장해야 INV-1 투명성이 성립한다.

```js
function synthesizeResponse(row) {
  return {
    content: [
      { type: "tool_use", name: row.branch === "term_entry" ? "return_term_entry"
                              : row.branch === "not_dev_term" ? "return_not_dev_term"
                              : "return_possible_typo",
        input: JSON.parse(row.payload) },
    ],
  };
}
```

성립 근거 (클라 코드 확인 완료):
- `ClaudeResponse`는 `content`만 선언하고 `AppJson.ignoreUnknownKeys`라 `id`/`usage`/`stop_reason` 등이 없어도 디코드된다.
- `toTermResult()`는 `content`에서 **첫 `tool_use` 블록**만 찾으므로 `thinking`/`text` 블록이 없어도 무방하다.
- 도구명 3종은 `Tools` 상수(`return_term_entry`/`return_not_dev_term`/`return_possible_typo`)와 정확히 일치해야 한다.
- HTTP 200 + `content-type: application/json`으로 반환한다(비2xx는 클라가 `InvalidResponse`로 봉인).

⚠️ **`usage` 필드가 없다** → 기존 `logUsage`가 `parsed?.usage`를 읽어 전부 0으로 기록한다. 히트는 실제로 토큰 0이라 값 자체는 정확하지만, **히트와 "0 토큰 실패"가 구분되지 않는다** → §3-1 `cache_hit` 컬럼으로 구분해 적재한다.

### 3-5. write-back + INV-13 정규화

미스 → Anthropic 성공(2xx) 시:

1. 응답에서 첫 `tool_use` 블록을 찾는다. 없으면 **write-back 스킵**(클라가 `InvalidResponse`로 처리할 응답을 캐시하지 않는다).
2. **branch 판정** — 도구명 3종 중 무엇인가. 알 수 없는 도구명이면 스킵.
3. **INV-13 정규화** — `term_entry` 분기면 `input.category`를 정본 6집합으로 clamp:
   `["동시성","자료구조","네트워크","DB","패턴","기타"]` (devetym `model/Category.kt` `Category.CANONICAL`). 집합 밖이면 `"기타"`로 치환한다(클라 M4 clamp와 동일 규칙).
4. **키워드 정본화** — `input.keyword`를 원본 그대로 둔다(대소문자 보존 — 클라가 표시에 쓴다). 접기는 `term_key`가 담당한다.
5. **버전 태깅(INV-9)** — 서버 상수 `PROMPT_VERSION`/`SCHEMA_VERSION`을 payload와 컬럼 양쪽에 기록.
6. **저장** — `INSERT INTO entries ... ON CONFLICT(term_key) DO NOTHING` (INV-4 first-write-wins). `aliases`도 `input.aliases[]`를 canonical 처리해 같은 방식으로 삽입(별칭 충돌 시에도 first-write-wins).
7. **`waitUntil`로 백그라운드 실행** — 응답을 막지 않는다(기존 `logUsage` idiom과 동일).

> **INV-13을 서버가 지는 이유(재확인)**: 클라도 M4에서 category를 clamp하므로 사용자 화면은 어느 쪽이든 안전하다. 그러나 **D1에 저장된 값은 INV-12 번들 승격 잡의 입력**이 되어 클라를 거치지 않고 `terms.json`으로 흘러간다. 그 경로에서 집합 밖 값이 조용히 새는 것을 막으려면 **저장 시점에** clamp되어 있어야 한다.

### 3-6. wrangler 설정

```toml
[[d1_databases]]
binding = "CACHE_DB"
database_name = "devetym-cache"
database_id = "…"   # wrangler d1 create devetym-cache 후 기입
```

별도 DB로 분리하는 근거는 §7-3(열린 질문)에서 판정.

### 3-7. 실패 격리 — "캐시는 절대 서비스를 깨지 않는다"

**이 슬라이스의 안전 요구 1순위.** 캐시는 최적화지 정확성 요건이 아니다.

| 실패 지점 | 처리 |
|---|---|
| `termKey` 도출 불가(본문 shape 예상 밖) | 캐시 우회, 기존 경로 그대로 |
| D1 조회 예외·타임아웃 | catch → 미스로 간주 → Anthropic 호출 |
| `payload` JSON 파싱 실패(손상 행) | 미스로 간주 → 재생성 경로 |
| write-back 실패 | `waitUntil` 안에서 swallow, 응답 무영향 |
| `CACHE_DB` 바인딩 없음 | 캐시 전 경로 no-op (기존 `USAGE_DB` idiom과 동일 — 배포 순서 자유) |

**킬 스위치**: 환경변수 `CACHE_DISABLED = "1"`이면 조회·쓰기를 모두 건너뛴다. 캐시가 이상 동작할 때 **코드 롤백 없이 `wrangler secret`/vars 변경 + 재배포로 즉시 무력화**한다.

---

## 4. 설계 불변식 (이 슬라이스가 반드시 지킬 것)

| INV | 이 슬라이스에서의 의미 |
|---|---|
| **INV-1** | 조회 순서 D1 → API. 클라 호출 형태·응답 shape 불변(§3-4). |
| **INV-2** | write-once. read path에서 재생성 금지 — 히트면 저장분을 그대로 준다. |
| **INV-3** | `term_key` canonical + `aliases` 테이블. 중복 항목 금지. |
| **INV-4** | `ON CONFLICT DO NOTHING` first-write-wins. (코얼레싱 측은 M2 — OUT) |
| **INV-9** | 모든 행에 `prompt_version`·`schema_version`. |
| **INV-11** | 서버는 SSOT가 아니다 — 캐시가 죽어도 앱은 번들·로컬로 살아있고, 서버는 API 경로로 폴백(§3-7). |
| **INV-13** | **정규화 후 write-back.** 원응답 캐시 금지(§3-5). |

---

## 5. 완료 조건 (DoD) — 서버 트랙 green 오라클 **신규 정의**

클라 트랙의 "4축"에 대응하는 서버측 오라클이 지금까지 없었다. 이 슬라이스가 세운다.

| 축 | 명령 | 무엇을 실측하나 |
|---|---|---|
| **S축1 · 정적** | `npx wrangler deploy --dry-run` | 설정·바인딩·문법 |
| **S축2 · 단위** | `npm test` (vitest + `@cloudflare/vitest-pool-workers`) | **실제 D1(miniflare)** 위에서 조회·write-back·멱등성. Anthropic만 목(fetch 스텁) |
| **S축3 · 마이그레이션** | `npx wrangler d1 migrations apply devetym-cache --local` | DDL 적용 가능성 |
| **S축4 · 무과금 라이브 스모크** | 배포 후 curl 4경로 | GET→405 · 비JSON→400 · 40KB→413 · 캐시 히트→200(Anthropic 미호출) |

**S축2가 이 슬라이스의 load-bearing 축이다** — D1을 실제로 돌려야 `ON CONFLICT`·alias 폴백·정규화 순서가 실측된다. `@cloudflare/vitest-pool-workers` 도입이 이 슬라이스 비용에 포함된다(§7-4).

**과금 게이트**: S축4의 캐시 히트 검증은 **선행 1회 실제 생성($0.03)** 이 필요하다. 이 1회는 승인된 지출로 간주하고, 그 외 어떤 축도 Anthropic을 호출하지 않는다.

---

## 6. 테스트 — 함수명 `test_[대상]_[조건]_[기대]`

### 6-1. read path
- `test_조회_캐시히트_Anthropic미호출` — fetch 스텁이 0회 호출됨을 실측 ★ 비용 절감의 실측
- `test_조회_캐시히트_클라파싱가능shape` — 합성 응답이 `content[0].type=="tool_use"` + 도구명 3종 중 하나
- `test_조회_별칭히트_정본반환` — `aliases`를 통한 2차 조회
- `test_조회_캐시미스_Anthropic호출후저장`
- `test_조회_D1예외_미스폴백` — D1이 던져도 200 응답

### 6-2. write path
- `test_저장_동일키2회_1행만` — INV-4 멱등
- `test_저장_집합밖category_기타로clamp` — INV-13
- `test_저장_tool_use없음_스킵`
- `test_저장_알수없는도구명_스킵`
- `test_저장_버전컬럼_기록됨` — INV-9

### 6-3. term key 동치 ★ 클라-서버 drift 방어
- `test_termKey_대문자입력_소문자키` (`React` → `react`)
- `test_termKey_공백패딩_트림`
- `test_termKey_비문자열content_null` → 캐시 우회
- 클라 `normalizeKeyword` 테이블과 **같은 입력 집합**을 쓴다(devetym `commonTest` 케이스 복제)

### 6-4. 격리
- `test_캐시_킬스위치켜짐_전경로우회`
- `test_캐시_바인딩없음_no_op`

---

## 7. 열린 질문 (비준이 판정할 항목)

### 7-1. 한도 카운터를 캐시 히트에 소모시킬 것인가
§3-3에서 **소모 안 함**으로 잠정 결정했다. 반론: 히트도 요청이므로 무한 요청 시 Worker 자원·KV 읽기는 소비된다(DoS 표면). 다만 Cloudflare 자체 보호가 앞단에 있고 비용은 0이라 수용 가능하다고 본다. **비준 판정 요청.**

### 7-2. `not_dev_term` / `possible_typo`도 캐시할 것인가 ★ 가장 중요한 판정
- **캐시 찬성**: INV-10의 "캐시 미스 남용"(무작위 문자열 반복 검색) 방어에 직접 기여한다. 같은 쓰레기 입력이 두 번 과금되지 않는다.
- **캐시 반대**: **오판을 동결**한다. 실제 개발 용어인데 `not_dev_term`으로 잘못 판정된 경우, 무효화 경로(INV-5)가 아직 미구현(OUT)이라 **정정 수단이 없다.**
- **잠정 제안**: 3분기 모두 캐시하되 `branch` 컬럼으로 구분(§3-1 스키마는 이미 이를 수용). 오판 정정은 후속 무효화 슬라이스가 담당하며, 그때까지는 `entries`에서 해당 행을 수동 DELETE하는 것이 유일한 정정 수단임을 문서에 남긴다.
- **대안**: S1에서는 `term_entry`만 캐시하고 나머지 2분기는 무효화 경로가 생긴 뒤로 미룬다(안전하지만 남용 방어 효과 포기).

### 7-3. 새 D1(`devetym-cache`) vs 기존 `devetym-usage`에 테이블 추가
- **분리 찬성**: 캐시는 **정본 데이터**(승격 잡의 입력), usage_log는 **폐기 가능한 텔레메트리**다. 수명주기가 다르고, usage_log를 통째로 비우고 싶을 때 캐시가 인질이 되지 않는다.
- **통합 찬성**: 마이그레이션 체인 1개, 바인딩 1개. 무료 플랜 D1 데이터베이스 개수 한도 소비 없음.
- **잠정 제안**: 분리(`CACHE_DB`). 단 **무료 플랜의 D1 DB 개수 한도를 착수 시 확인**할 것.

### 7-4. `@cloudflare/vitest-pool-workers` 도입 비용
현재 `package.json`에 테스트 프레임워크가 **전무**하다(스크립트는 dev/deploy/tail 3개뿐). S축2를 세우려면 vitest + pool-workers + 설정 파일이 새로 들어온다. 대안은 `wrangler dev --local` 수동 스모크인데 회귀 방어가 안 된다. **도입을 제안하되 비준이 비용 대비 타당성을 판정.**

### 7-5. `PROMPT_VERSION` 문자열을 무엇으로 할 것인가
ADR-0007이 현재 형상을 *"v1 5변경 + v2 Path A"*로 락했으나 **코드에 기계 판독 가능한 버전 문자열이 없다.** 제안: 서버 상수 `PROMPT_VERSION = "v2-pathA"`, `SCHEMA_VERSION = 1`. 클라 `TermEntry.promptVersion`(옵셔널)에 그대로 실려 왕복한다. **비준이 명명 확정.**

### 7-6. 프롬프트가 바뀌면 캐시를 어떻게 할 것인가
INV-5는 gated 무효화를 요구하지만 실행 경로는 이 슬라이스 OUT이다. S1 배포 후 프롬프트를 개정하면 **구버전 payload가 계속 서빙된다.** 이를 "알려진 상태"로 문서화하고 무효화 슬라이스를 백로그에 명시할 것을 제안. **비준 확인.**

---

## 8. 안전·규율

### 8-1. 선행 조건 (착수 전 반드시)
1. **`devetym-proxy`의 `c7218db`를 `main`에 병합** — 현재 `main`은 프로덕션(`c5cd809f`)보다 낡았고 `USAGE_DB` 바인딩이 없다. 이 상태에서 브랜치를 따면 D1 바인딩 없는 베이스 위에 캐시를 짓게 된다.
2. 무료 플랜 D1 데이터베이스 개수 한도 확인(§7-3).

### 8-2. 배포 규율
- 마이그레이션은 `--local` → `--remote` 순으로 적용. `--remote` 적용은 **되돌리기 어려우므로** 사람 승인 후 실행.
- 배포는 캐시 **킬 스위치 ON 상태로 먼저 올려** 기존 동작 무회귀를 확인한 뒤 OFF로 전환하는 2단 롤아웃을 제안한다.
- 브랜치는 병합 후에도 **삭제하지 않는다**(ROADMAP 브랜치 보존 규율).

### 8-3. 비용 규율
- 이 슬라이스 전 구간에서 Anthropic 실호출은 **S축4 스모크의 1회($0.03)** 만 허용한다.
- Console 월 $30 하드캡·알림($10/$20/$25)은 그대로 유효 — 변경하지 않는다.

### 8-4. 손대지 않는 것
- 시스템 프롬프트·도구 스키마(클라 `commonMain` 소유 — ADR-0004·0006 §6)
- 클라이언트 코드 — **단 §3-2 동기화 지점 주석 1줄은 예외**(devetym repo, 승인 대상)
- 한도 상수(`DAILY_LIMIT`·`GLOBAL_DAILY_LIMIT`)·과금 파라미터 강제(`FORCED_MODEL` 등)
- ADR·INV — 이 슬라이스는 제약을 **소비**하지 생성하지 않는다

---

## Open Questions

- §7-1 한도 카운터와 캐시 히트의 관계
- §7-2 3분기 중 캐시 대상 ★
- §7-3 D1 분리 vs 통합
- §7-4 테스트 프레임워크 도입 비용
- §7-5 버전 문자열 명명
- §7-6 프롬프트 개정 시 캐시 무효화 부재의 수용 여부
