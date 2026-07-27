# 서버 슬라이스 S1 (draft) — D1 스키마 + Worker read-through 캐시

> **성격: 비준 하네스 입력용 spec 슬라이스(`-draft.md`).** [`cache-delivery-milestones.md`](../cache-delivery-milestones.md)의 캐시 트랙 **M0(데이터 기반) + M1(Worker read-through)** 을 실제 코드베이스에 착지시킨 구현 스펙. 그 문서 §1 INV-1~13이 재론 금지 제약이고, 이 문서는 그 제약 아래의 **구현 결정**을 정한다.
>
> **비준 이력**: 2026-07-27 적대적 비준 1회 완료(제네릭 엔진 + `profiles/devetym.md` 축1·축4 부분 적용 — 축2/3/5와 gradle 수렴 오라클은 서버 슬라이스에 무효라 미적용). **Blocker 4건 · Caution 9건 도출 → 이 문서에 반영 완료**, §7 열린 질문 6건 판정 완료. 판정 근거와 미해소 항목은 §7·§9 참조.
>
> ⚠️ **`ratify-spec` 정식 비준은 미실시** — `~/.claude/workflows/` 심링크 부재 + `profiles/devetym.md`에 서버 렌즈 없음. 서버 렌즈 신설은 별도 격리 세션(`personas/harness-engineer.md`) 소관이며 이 슬라이스의 선행 조건은 아니다.

- **트랙**: 서버(별도 repo `devetym-proxy`) — CMP 클라 마일스톤(M0~M9)과 번호 공간이 다르다. 혼동을 막으려 `S1`로 표기한다.
- **캐시 트랙 대응**: M0(term key 정규화·D1 스키마·entry 계약) + M1(Worker 조회 순서·write-back·멱등 write)
- **repo·브랜치**: `~/devetym-proxy`, `feat/s1-read-through-cache` (베이스 = **`main`에 `c7218db` 병합 후**의 main — §8-1 선행 조건)
- **범위 확정**: M0+M1만. **single-flight(DO)·품질 게이트·402 검색어 수집은 이 슬라이스 밖**(2026-07-27 사람 확정 — §0)
- **앱 심사 무관**: 이 슬라이스는 서버 배포만으로 완결된다. 클라 재빌드·스토어 심사가 필요 없고, 이미 설치된 앱이 다음 검색부터 효과를 받는다(INV-1 투명성). 앱 업데이트 사이클과 **병렬 진행 가능**.
- **참조**: [ADR-0006](../adr/0006-server-cache-boundary.md)(계약 정본), [ADR-0004](../adr/0004-backend-proxy-boundary.md)(계승 계약), [ADR-0001(iOS)](https://github.com/data-sy/dev-etymology)(호스팅 선택), [INV-1~13](../cache-delivery-milestones.md), [ADR-0007](../adr/0007-ai-prompt-quality.md)(프롬프트 버전 형상)

---

## 0. 스코핑 판정

**S1 = M0 + M1.** single-flight(M2)·품질 게이트(M3)·승격 잡(M5)은 후속 슬라이스로 뗀다.

근거:
- **비용 절감 효과가 실제로 발생하는 최소 단위**가 M0+M1이다. M0만으로는 동작 변화가 0(저장소만 생기고 아무도 안 읽음)이라 슬라이스로서 무의미하고, M2까지 넣으면 Durable Objects라는 **새 바인딩·새 실행 모델·무료 플랜 가용성 확인**이 한 슬라이스에 겹쳐 비준 대상이 비대해진다.
- M2 없이도 **중복 저장은 이미 막힌다** — `term_key` unique + `INSERT ... ON CONFLICT DO NOTHING`(INV-4 저장 측). M2가 없어서 남는 손해는 "캐시 빈 상태 동시 요청 시 중복 **생성 비용**"뿐이다.
- **동시 충돌 확률 정량화(비준 보완 — 정성 판단을 수치로 대체)**: 전역 캡 200회/일 = 평균 **0.0023 req/s**. Anthropic thinking 응답 지연 ~10–20s이므로, 임의 요청의 생성 창(20s) 안에 다른 요청이 들어올 확률 ≈ **4.5%**. 그중 *같은 신규 용어*일 확률은 검색 분포가 롱테일이라 ≪1%. 중복 생성이 실제로 발생해도 손해는 **건당 $0.02–0.04**이며 `ON CONFLICT DO NOTHING`이 중복 저장은 막는다. → **연간 수 건 수준의 낭비로 수용 가능. 의도적으로 수용하고 후속 슬라이스로 뗀다.**
- 서버 트랙은 이번이 **첫 슬라이스**라 green 오라클(§5)이 아직 존재하지 않는다. 오라클을 세우는 비용이 이 슬라이스에 얹히므로 기능 범위는 좁게 잡는다.

**이 판정이 남기는 부채(명시)**: 동시 요청 중복 생성(M2), validator write-게이트(M3), 정본 항목 무효화·재생성(INV-5). 셋 다 §7·ROADMAP 백로그에 남긴다.

---

## 1. 목표 (이 슬라이스가 내는 것)

한 사용자가 생성시킨 어원 항목을 **다른 사용자가 재사용**해 Claude API 호출을 재과금하지 않는다.

- 현재: 번들 650개 미스 → **매 사용자마다** Anthropic 과금
- S1 이후: 번들 미스 → **D1 히트면 무과금**, 전역 최초 1회만 과금(INV-2 write-once)
- **클라이언트 무변경**(INV-1 투명성) — 앱 재배포 없이 서버 배포만으로 효과 발생

---

## 2. 스코프

### IN
- D1 스키마 + 마이그레이션 (`entries` / `entry_versions` / `aliases`) — **캐시 전용 DB에 격리**(§3-1·§3-6)
- canonical term key 도출 규칙 — **AI 정본 키워드 기준**(§3-2, 비준 B1)
- Worker 조회 순서: **본문 파싱 → D1 조회 → (미스 시) 한도 검사 → Anthropic → 정규화 → write-back**(§3-3, 비준 B4)
- 캐시 히트 시 **Anthropic 응답 shape 합성**(클라 계약 유지)
- **write 전 최소 shape 게이트**(§3-5 2단계, 비준 B2) — 클라가 디코드 못 할 payload를 캐시에 넣지 않는다
- INV-13 정규화-후-쓰기 (category 6집합 clamp)
- first-write-wins 멱등 write
- **부정 분기(`not_dev_term`/`possible_typo`) 30일 soft TTL**(§3-5 7단계, §7-2 판정)
- 캐시 경로 실패 시 **무해 폴백**(§3-7) + 킬 스위치
- `usage_log`에 `cache_hit` 구분 추가 — **별도 마이그레이션으로 usage DB에 적용**(§3-1, 비준 B3)

### OUT (후속 슬라이스)
- single-flight / Durable Objects (캐시 M2)
- validator·critic 품질 게이트 (캐시 M3) — **단 §3-5의 최소 shape 게이트는 IN**(그 축소판)
- 정본 항목 무효화·재생성 트리거 (INV-5 gated — 스키마 컬럼만 확보, 실행 경로는 미구현). **부정 분기 TTL은 예외로 IN**(근거 §7-2)
- **생성 파라미터 강제(INV-8 · temperature 0–0.3)** — 별도 슬라이스로 분리 확정(§9-1). 이 슬라이스는 `FORCED_*` 상수를 건드리지 않는다
- 번들 승격 잡 (캐시 M5) / 콘텐츠 팩 동기화 (캐시 M6)
- 402 놓친 검색어 수집 (ROADMAP 백로그 — 2026-07-27 별도 확정)
- "프로그래밍 용어처럼 보이는가" 휴리스틱 (INV-10 잔여)

---

## 3. 산출 명세

### 3-1. D1 스키마

> ⚠️ **마이그레이션은 DB별로 디렉토리를 분리한다**(비준 B3). 캐시 테이블은 `devetym-cache`, `usage_log`는 `devetym-usage`에 있고 `wrangler d1 migrations apply`는 **DB 하나만** 대상으로 한다. 한 파일에 두 DB의 DDL을 섞으면 캐시 DB 적용 시 `no such table: usage_log`로 실패하고, usage DB 적용 시 캐시 테이블이 엉뚱한 곳에 생긴다. 기본 `migrations/` 디렉토리를 두 DB가 공유하는 현재 설정도 함께 고쳐야 한다(§3-6).

**`migrations/cache/0001_term_cache.sql`** (→ `devetym-cache`)

```sql
-- entries: 용어당 정본 1행. INV-2 write-once — read path 재생성 금지.
CREATE TABLE IF NOT EXISTS entries (
  term_key        TEXT PRIMARY KEY,         -- canonical key (§3-2). term_entry 분기는 AI 정본 keyword 기준.
                                            -- unique 제약 = INV-4 first-write-wins
  branch          TEXT NOT NULL,            -- 'term_entry' | 'not_dev_term' | 'possible_typo'
  payload         TEXT NOT NULL,            -- tool_use.input 원문 JSON (shape 게이트·정규화 통과 후 — §3-5)
  prompt_version  TEXT NOT NULL,            -- INV-9 (§3-5 5단계 — 프롬프트 해시 포함)
  schema_version  INTEGER NOT NULL,         -- INV-9
  created_at      TEXT NOT NULL,            -- ISO 8601 UTC. 부정 분기 soft TTL의 기준(§3-5 7단계)
  hit_count       INTEGER NOT NULL DEFAULT 0 -- INV-12 hot 선정 입력(승격 잡이 소비)
);

-- aliases: React ↔ react.js ↔ 리액트 접기 (INV-3).
-- AI 응답 TermEntry.aliases + 생성을 유발한 요청 키를 함께 채운다(§3-5 6단계).
CREATE TABLE IF NOT EXISTS aliases (
  alias_key  TEXT PRIMARY KEY,              -- canonical 처리된 별칭
  term_key   TEXT NOT NULL REFERENCES entries(term_key)
);

-- entry_versions: INV-5·INV-9 — 정본 교체 시 구버전 보존용 선확보.
-- ⚠️ S1에서는 읽지도 쓰지도 않는다. 정본 교체 경로(INV-5)가 이번 슬라이스 OUT이라 write 계기가 없다.
--    무효화 슬라이스가 채운다. DDL만 미리 확보해 이후 마이그레이션을 아낀다.
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

**`migrations/usage/0002_cache_hit.sql`** (→ `devetym-usage`, 기존 `0001_usage_log.sql`에 이어짐)

```sql
ALTER TABLE usage_log ADD COLUMN cache_hit INTEGER NOT NULL DEFAULT 0;
```

> **주의**: 기존 `migrations/0001_usage_log.sql` 주석이 *"향후 CMP M3의 D1 read-through 캐시가 붙으면 cache_hit 컬럼이 추가될 예정"*이라 이미 예고해 둔 항목이다. 이 슬라이스가 그 예고를 이행한다. 기존 `0001_usage_log.sql`은 `migrations/usage/`로 **이동**하고 파일명·번호는 유지한다(적용 이력 보존).

### 3-2. canonical term key 도출 ★ 비준 B1 반영

**요청 키(read)와 정본 키(write)를 분리한다.**

#### 요청 키 — 조회에 쓴다

```js
// 클라는 원본 keyword를 messages[0].content에 담는다
// (devetym shared/.../ClaudePrompt.kt buildClaudeRequest — 대소문자 보존)
const raw = body?.messages?.[0]?.content;
const requestKey = typeof raw === "string" ? normalizeTermKey(raw) : null;
```

#### 정본 키 — 저장에 쓴다 (분기별로 다르다)

| 분기 | `term_key` | 근거 |
|---|---|---|
| `term_entry` | **`normalizeTermKey(input.keyword)`** — AI가 돌려준 정본 키워드 | 클라 `BundleDbSource`도 `normalizeKeyword(entry.keyword)`로 색인한다. 번들·서버가 같은 키 공간을 써야 INV-12 승격이 성립 |
| `not_dev_term` · `possible_typo` | `requestKey` | AI 정본 키워드가 존재하지 않는다. 요청 문자열 자체가 판정 대상 |

**왜 요청 키를 그대로 쓰면 안 되는가 (비준 B1 — 깨지는 실행 경로)**:

1. A가 `리액트` 검색 → `term_key="리액트"`, payload `{keyword:"react", aliases:["리액트","React.js"]}`
2. B가 `React` 검색 → `requestKey="react"` → `entries` 미스 → `aliases`도 미스. **AI는 자기 자신을 `aliases`에 넣지 않는다**(프롬프트 §aliases 규칙 = "대체 표기"; 모범 답안 mutex도 `["뮤텍스","mutual exclusion"]`으로 자기 자신 없음)
3. → Anthropic 재호출 + `term_key="react"` **두 번째 행 생성**

INV-3 "중복 항목이 생기면 안 된다"를 직접 위반하고, **§1 목표(재과금 방지)가 한글·약어·표기변이 입력에서 미달성**된다. devetym은 한국 개발자 대상이라 이 경로가 롱테일의 상당 비중을 차지한다.

정본 키 방식에서는 위 시나리오가 이렇게 된다: A의 요청으로 `term_key="react"` 저장 + alias `리액트→react` 삽입 → B의 `React`는 **entries에서 직접 히트**, C의 `리액트`는 **alias 경유 히트**.

#### `normalizeTermKey` — 클라 `normalizeKeyword`와 동치여야 한다

클라 정본은 `shared/src/commonMain/kotlin/com/robin/devetym/data/AppJson.kt:31`:

```kotlin
fun normalizeKeyword(s: String): String = s.trim().lowercase()
```

⚠️ **`String.prototype.trim()`을 그대로 쓰면 동치가 깨진다**(비준 C3). Kotlin `trim()`은 `Char.isWhitespace()` 기준(U+001C–U+001F 포함, **NBSP U+00A0 제외**)이고, JS `trim()`은 ECMAScript WhiteSpace+LineTerminator 기준(**NBSP·U+FEFF 포함**, U+001C–U+001F 제외)이다.

- 증상: 서버는 자기 일관적이라 캐시 미스는 나지 않는다. 그러나 **INV-12 승격 잡이 서버 `term_key`를 `terms.json`으로 흘리면** 클라 `normalizeKeyword`가 만든 키와 어긋나 승격분이 영영 조회되지 않는다.
- 대응: 서버가 **Kotlin `Char.isWhitespace()` 집합을 정확히 복제**한다.

⚠️ **합집합을 쓰면 안 된다.** 두 규격의 합집합을 trim하면 서버가 클라보다 **더 많이** 자른다(NBSP 등). 클라는 `s.trim()` 그대로이고 §8-4가 클라 동작을 바꾸지 않으므로, 합집합은 동치를 만들기는커녕 반대 방향으로 깬다. 기준은 언제나 **클라 쪽 집합**이다.

```js
// Kotlin Char.isWhitespace() == java.lang.Character.isWhitespace() 의 정확한 복제.
// 포함: U+0009-U+000D, U+001C-U+001F, U+0020, U+1680,
//       U+2000-U+2006, U+2008-U+200A, U+2028, U+2029, U+205F, U+3000
// 제외(의도적): U+00A0 NBSP / U+2007 FIGURE SPACE / U+202F NNBSP / U+FEFF BOM / U+0085 NEL
//   위 5개는 JS trim() 또는 \s 에는 포함되지만 Kotlin trim() 은 자르지 않는다.
//   따라서 \s 를 쓰면 안 되고, 아래 집합을 이스케이프로 명시해야 한다.
const WS =
  "\\u0009-\\u000D\\u001C-\\u001F\\u0020\\u1680" +
  "\\u2000-\\u2006\\u2008-\\u200A\\u2028\\u2029\\u205F\\u3000";
const TRIM_CHARS = new RegExp(`^[${WS}]+|[${WS}]+$`, "g");

function normalizeTermKey(s) {
  return s.replace(TRIM_CHARS, "").toLowerCase();
}
```

- `toLowerCase()`를 쓴다 — **`toLocaleLowerCase()` 금지**(로케일 의존 시 터키어 `I` 등에서 Kotlin `lowercase()`와 갈라진다).
- 클라가 `normalizeKeyword`를 바꾸면 **서버도 같이 바꿔야 한다**. §6-3 동치 테스트로 고정하고, 클라 `AppJson.kt`에 동기화 지점 주석을 남긴다(devetym repo 측 변경 — §8-4 승인 대상).
- `requestKey`가 `null`/빈 문자열이면 **캐시를 통째로 우회**하고 기존 경로로 통과시킨다(안전 실패 — §3-7).

### 3-3. Worker 조회 순서 (read path) ★ 비준 B4 반영

```
① 메서드·기기ID 검사                      (기존, 순서 유지)
─────────── 순서 변경 ───────────
② 본문 크기 검사 + 파싱 + 과금 파라미터 강제  (기존 ⑤ — 한도 검사보다 앞으로 이동)
─────────── 신규 ───────────
③ requestKey 도출 (§3-2)
④ D1 조회: entries(requestKey) → 미스면 aliases(requestKey) → entries(term_key)
   └─ 히트 → hit_count 증분 + usage_log(cache_hit=1) (waitUntil)
            → 합성 응답 반환 (§3-4)   ★ Anthropic 미호출 = 무과금 · 한도 무소모 · 한도 검사 미통과
─────────── 기존 (상대 순서 유지, 캐시 미스일 때만 도달) ───────────
⑤ 일일 한도 검사 (기기·전역)
⑥ Anthropic 호출
⑦ 402 매핑 / 한도 카운터 가산 / usage 기록(cache_hit=0)
─────────── 신규 ───────────
⑧ shape 게이트 → 정규화(INV-13) → write-back (waitUntil)
```

**한도 카운터와의 관계 (판정 — §7-1)**: 캐시 히트는 한도를 **소모하지 않고, 한도 검사도 통과하지 않는다.**

- 근거: 한도(`DAILY_LIMIT`·`GLOBAL_DAILY_LIMIT`)의 목적은 *비용 방어*이고 히트는 비용이 0이다. 기존 코드도 *"성공(=토큰 비용 발생)한 호출만 한도에 가산"* 원칙을 명시하고 있어 idiom이 일치한다.
- **소모 안 함만으로는 부족했다(비준 B4)**: 검사를 앞에 두면 10/10을 소진한 기기는 *이미 캐시에 있는 용어조차* 429를 받는다. 전역 200회 소진 시에는 **전 사용자**가 무과금 응답조차 못 받는다 — §1 목표가 정작 가장 활발한 사용자에게 미달성이고, INV-1의 "서버 D1 캐시" 층이 통째로 비용 방어 게이트 뒤에 갇힌다. 그래서 캐시 조회를 한도 검사 **앞**으로 옮긴다.
- 부수 효과: **사용자 체감 한도가 늘어난다**(캐시된 용어는 10회를 안 깎고, 소진 후에도 계속 조회된다) — 의도된 개선.

⚠️ **오류 우선순위가 바뀐다(명시)**: `invalid_body`(400)·`body_too_large`(413)가 이제 `daily_limit_exceeded`(429)보다 **먼저** 반환된다. 클라 `ClaudeApi`는 429/402 외 non-2xx를 전부 `InvalidResponse`로 봉인하므로 계약 위반은 아니다. 단 **S축4 스모크 절차의 기대값이 달라지므로**(§5) 함께 갱신한다.

**남용 표면 재검토**: 본문 크기 상한(32KB)이 여전히 한도 검사보다 앞에 있고, 히트 경로 비용은 KV 읽기 0회 + D1 읽기 1~2회다. 무한 히트 요청은 Cloudflare 앞단 보호에 맡긴다. 히트 전용 카운터는 후속 슬라이스 백로그(§7-1).

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

성립 근거 (클라 코드 확인 완료 — 비준에서 재확인, **통과**):
- `ClaudeResponse`는 `content`만 선언하고 `AppJson.ignoreUnknownKeys`라 `id`/`usage`/`stop_reason` 등이 없어도 디코드된다.
- `toTermResult()`는 `content`에서 **첫 `tool_use` 블록**만 찾으므로 `thinking`/`text` 블록이 없어도 무방하다.
- 도구명 3종은 `Tools` 상수(`return_term_entry`/`return_not_dev_term`/`return_possible_typo`)와 정확히 일치해야 한다.
- `return_not_dev_term` 분기는 `input`을 읽지 않으므로 payload가 `{}`여도 무해하다.
- HTTP 200 + `content-type: application/json`으로 반환한다(비2xx는 클라가 `InvalidResponse`로 봉인).

> **비준 결과**: 합성 shape 자체는 깨지지 않는다. 깨지는 것은 **그 안에 담기는 payload의 무결성**이며, §3-5 shape 게이트가 그것을 막는다(비준 B2).

**usage 기록 (비준 C1 — 내부 모순 해소)**: 히트 경로는 ⑦에 도달하지 않으므로 기존 `logUsage`가 **호출되지 않는다**. 그대로 두면 `cache_hit=1` 행이 하나도 남지 않아 INV-12 hot 선정 입력과 비용 절감 실측 원자료가 비어버린다. 히트 경로에서 **별도로** 호출한다:

```js
ctx.waitUntil(logUsage(env, deviceId, { cacheHit: true }));   // 토큰 전부 0, cache_hit=1
```

`logUsage`는 `respBody` 대신 옵션 객체를 받도록 시그니처를 확장한다. 미스 경로는 `cache_hit=0`으로 기존 동작 유지.

### 3-5. write-back — shape 게이트 + INV-13 정규화

미스 → Anthropic 성공(2xx) 시, **`waitUntil` 안에서** 아래를 순서대로 수행한다. 이 시점에 사용자 응답은 이미 반환된 뒤이므로 **어떤 단계도 체감 지연에 영향을 주지 않는다.**

1. **tool_use 추출** — 응답에서 첫 `tool_use` 블록을 찾는다. 없으면 write-back 스킵(클라가 `InvalidResponse`로 처리할 응답을 캐시하지 않는다).
2. **branch 판정** — 도구명 3종 중 무엇인가. 알 수 없는 도구명이면 스킵.
3. **shape 게이트 ★ 비준 B2** — `input`을 클라가 실제로 디코드할 수 있는지 확인한다. 통과 못 하면 **저장만 스킵**하고 응답은 그대로 통과시킨다.

   ```js
   const input = toolUse.input ?? {};            // C8: input 부재·null 변이 방어
   if (branch === "term_entry") {
     const REQUIRED = ["keyword", "category", "summary", "etymology", "namingReason"];
     if (!REQUIRED.every((k) => typeof input[k] === "string" && input[k].length > 0)) return;
     if (input.aliases !== undefined && !Array.isArray(input.aliases)) return;
   } else if (branch === "possible_typo") {
     if (typeof input.suggestion !== "string") return;   // 클라는 없으면 ""로 폴백하나, 저장 가치 없음
   }
   ```

   **왜 필요한가 (깨지는 실행 경로)**: `TermEntry`는 `keyword`/`category`/`summary`/`etymology`/`namingReason`이 **required**이고 클라는 `AppJson.decodeFromJsonElement(TermEntry.serializer(), input)`로 strict 디코드한다. LLM이 한 필드라도 누락한 응답을 그대로 캐시하면, INV-2 write-once + 무효화 경로 OUT이므로 **그 용어는 모든 사용자에게 영구히 `InvalidResponse`**가 되고 히트라서 Anthropic도 안 불려 자가 치유되지 않는다. 복구는 수동 `DELETE`뿐인데 발견 수단이 없다.

   **왜 "일단 저장하고 나중에 점검"이 아닌가 (2026-07-27 사람 확정)**: 저장을 건너뛰면 다음 사용자의 요청이 캐시 미스가 되어 **AI가 다시 정상 응답을 만들 기회를 얻는다 — 자가 치유**. 저장하면 고장이 굳고, 나중 점검으로 고치려면 정본 교체 경로(INV-5)를 먼저 지어야 해서 검사 한 단계보다 훨씬 큰 작업이 된다. 품질 낮은(깨지지는 않은) 항목을 걸러내는 층은 별개이며 캐시 M3·M5가 담당한다.

4. **INV-13 정규화** — `term_entry` 분기면 `input.category`를 정본 6집합으로 clamp:
   `["동시성","자료구조","네트워크","DB","패턴","기타"]` (devetym `model/Category.kt` `Category.CANONICAL`). 집합 밖이면 `"기타"`로 치환한다(클라 M4 clamp와 동일 규칙).
5. **버전 태깅(INV-9)** — `SCHEMA_VERSION` 상수와 **프롬프트 해시 기반** `prompt_version`을 payload와 컬럼 양쪽에 기록. 명명은 §7-5 판정 참조.
6. **키 도출 + 저장** — §3-2의 분기별 규칙으로 `term_key`를 정한 뒤:

   ```sql
   INSERT INTO entries (...) VALUES (...) ON CONFLICT(term_key) DO NOTHING;   -- INV-4 first-write-wins
   ```

   **alias 삽입 규칙**:
   - 대상 = `input.aliases[]`(canonical 처리) **∪ `{requestKey}`**. 요청 키를 반드시 포함시킨다 — AI가 자기 자신이나 요청 표기를 aliases에 넣어 준다는 보장이 없다(§3-2).
   - **이미 `entries`에 존재하는 키는 alias로 등록하지 않는다**(비준 C9). 등록하면 조회는 entries 우선이라 서빙은 안전하지만, 잘못된 alias 행이 남아 무효화·승격 잡이 엉뚱한 정본을 가리킨다. `term_key` 자신도 이 규칙으로 자동 제외된다.
   - `ON CONFLICT(alias_key) DO NOTHING` — 별칭 충돌 시에도 first-write-wins.
   - `aliases`는 `entries`를 FK 참조하므로 **entries INSERT를 먼저** 수행한다(D1의 FK 강제 여부와 무관하게 순서를 고정 — 비준 N1).
7. **부정 분기 soft TTL (§7-2 판정)** — `not_dev_term`/`possible_typo`는 30일이 지나면 만료로 본다.

   ```js
   const NEGATIVE_TTL_DAYS = 30;
   ```

   - **조회 시(§3-3 ④)**: `branch !== 'term_entry' && created_at < now - 30d`면 **미스로 간주**한다.
   - **저장 시**: INSERT 전에 만료된 부정 분기 행을 지운다 —
     `DELETE FROM entries WHERE term_key = ? AND branch != 'term_entry' AND created_at < ?`
   - **근거**: INV-10의 남용 방어(같은 쓰레기 입력의 단기 반복 차단)는 30일이면 충분히 달성된다. 반면 **오판 동결**(실제 개발 용어를 `not_dev_term`으로 잘못 판정)의 손해는 무기한이고, 정정 수단이 수동 DELETE뿐이다(INV-5 OUT). TTL이 그 정정 수단 부재를 자동으로 치유한다. 추가 비용은 오판된 용어당 30일에 1회 재생성($0.02–0.04).
   - **INV-5와의 관계**: INV-5의 "raw LWW 금지"는 *검증된 정본*을 미검증 생성물이 덮어쓰는 것을 막는 규칙이다. 부정 판정은 정본이 아니며, `term_entry` 행은 이 경로가 건드리지 않는다(`branch != 'term_entry'` 조건). INV-2 write-once도 정본 분기에서 그대로 유지된다.
8. **실패 관측 (비준 C8)** — 위 전 과정은 `waitUntil` 안에서 swallow하되, 실패를 **삼키기만 하지 않는다**:

   ```js
   catch (err) { console.error("cache write-back failed", err?.message); }
   ```

   `wrangler tail`로 관측 가능해야 한다. 그렇지 않으면 캐시가 영영 안 채워지는데 아무도 모르는 상태가 된다.

> **INV-13을 서버가 지는 이유(재확인)**: 클라도 M4에서 category를 clamp하므로 사용자 화면은 어느 쪽이든 안전하다. 그러나 **D1에 저장된 값은 INV-12 번들 승격 잡의 입력**이 되어 클라를 거치지 않고 `terms.json`으로 흘러간다. 그 경로에서 집합 밖 값이 조용히 새는 것을 막으려면 **저장 시점에** clamp되어 있어야 한다.

### 3-6. wrangler 설정

```toml
[[d1_databases]]
binding = "USAGE_DB"
database_name = "devetym-usage"
database_id = "e76366e6-34e1-4a1a-8ed7-c771bd650580"
migrations_dir = "migrations/usage"     # ← 신규. 미지정 시 두 DB가 기본 migrations/를 공유해 체인이 교차 오염된다

[[d1_databases]]
binding = "CACHE_DB"
database_name = "devetym-cache"
database_id = "…"                       # wrangler d1 create devetym-cache 후 기입
migrations_dir = "migrations/cache"     # ← 신규
```

기존 `migrations/0001_usage_log.sql`은 `migrations/usage/0001_usage_log.sql`로 이동한다(이미 원격 적용된 이력이 있으므로 **파일명·번호를 바꾸지 않는다**).

별도 DB로 분리하는 근거는 §7-3 판정 참조.

### 3-7. 실패 격리 — "캐시는 절대 서비스를 깨지 않는다"

**이 슬라이스의 안전 요구 1순위.** 캐시는 최적화지 정확성 요건이 아니다.

| 실패 지점 | 처리 |
|---|---|
| `requestKey` 도출 불가(본문 shape 예상 밖) | 캐시 우회, 기존 경로 그대로 |
| D1 조회 예외·타임아웃 | catch → 미스로 간주 → 한도 검사 → Anthropic 호출 |
| `payload` JSON 파싱 실패(손상 행) | 미스로 간주 → 재생성 경로 |
| shape 게이트 탈락 | **저장만** 스킵, 응답은 정상 통과 (§3-5 3단계) |
| write-back 실패 | `waitUntil` 안에서 swallow + `console.error` (§3-5 8단계) |
| `CACHE_DB` 바인딩 없음 | 캐시 전 경로 no-op (기존 `USAGE_DB` idiom과 동일 — 배포 순서 자유) |

**킬 스위치 (비준 C7 — 기본값·활성 경로 확정)**:

- `wrangler.toml`의 `[vars]`에 `CACHE_DISABLED`를 둔다. **값이 문자열 `"1"`일 때만 캐시를 끈다.**
- **초기 커밋의 기본값은 `CACHE_DISABLED = "1"`(꺼짐)** — §8-2의 2단 롤아웃이 "킬 스위치 ON 상태로 먼저 배포"를 요구하므로, 기본값이 켜짐이면 첫 배포가 곧바로 그 규율을 위반한다.
- 전환은 `wrangler.toml` 수정 + `wrangler deploy`. 코드 롤백이 아니라 **한 줄 변경 + 재배포**로 즉시 무력화된다.
- `[vars]`를 쓰므로 `wrangler secret`은 사용하지 않는다(둘을 섞지 않는다).

---

## 4. 설계 불변식 (이 슬라이스가 반드시 지킬 것)

| INV | 이 슬라이스에서의 의미 |
|---|---|
| **INV-1** | 조회 순서 D1 → API. 클라 호출 형태·응답 shape 불변(§3-4). |
| **INV-2** | write-once. read path에서 재생성 금지 — 히트면 저장분을 그대로 준다. (부정 분기 TTL은 §3-5 7단계 근거로 예외) |
| **INV-3** | `term_key` = **AI 정본 키워드** canonical + `aliases` 테이블. 중복 항목 금지(§3-2). |
| **INV-4** | `ON CONFLICT DO NOTHING` first-write-wins. (코얼레싱 측은 M2 — OUT) |
| **INV-8** | ⚠️ **이 슬라이스에서 미달성 — 별도 슬라이스로 분리 확정(§9-1, 2026-07-27).** temperature는 Anthropic 기본값(1.0)으로 유지된다. |
| **INV-9** | 모든 행에 `prompt_version`·`schema_version`(§7-5 판정 반영). |
| **INV-10** | 부정 분기 캐시로 "캐시 미스 남용" 재과금을 30일 단위로 차단(§3-5 7단계). 휴리스틱 잔여는 OUT. |
| **INV-11** | 서버는 SSOT가 아니다 — 캐시가 죽어도 앱은 번들·로컬로 살아있고, 서버는 API 경로로 폴백(§3-7). |
| **INV-13** | **정규화 후 write-back.** 원응답 캐시 금지(§3-5). |

---

## 5. 완료 조건 (DoD) — 서버 트랙 green 오라클 **신규 정의**

클라 트랙의 "4축"에 대응하는 서버측 오라클이 지금까지 없었다. 이 슬라이스가 세운다.

| 축 | 명령 | 무엇을 실측하나 |
|---|---|---|
| **S축1 · 정적** | `npx wrangler deploy --dry-run` | 설정·바인딩·문법 |
| **S축2 · 단위** | `npm test` (vitest + `@cloudflare/vitest-pool-workers`) | **실제 D1(miniflare)** 위에서 조회·write-back·멱등성·shape 게이트·TTL. Anthropic만 목(fetch 스텁) |
| **S축3 · 마이그레이션** | `npx wrangler d1 migrations apply devetym-cache --local`<br>**및** `... devetym-usage --local` | DDL 적용 가능성. **두 DB 모두** 적용해야 B3 재발을 막는다 |
| **S축4 · 무과금 라이브 스모크** | 배포 후 curl 4경로 | §5-1 참조 |

**S축2가 이 슬라이스의 load-bearing 축이다** — D1을 실제로 돌려야 `ON CONFLICT`·alias 폴백·정규화 순서·TTL 경계가 실측된다. `@cloudflare/vitest-pool-workers` 도입이 이 슬라이스 비용에 포함된다(§7-4 판정: 승인).

### 5-1. S축4 스모크 절차 (§3-3 순서 변경 반영)

모든 요청에 유효한 `X-Device-Id`(8~128자)를 실어야 ①을 통과한다.

| # | 요청 | 기대 | 비고 |
|---|---|---|---|
| 1 | `GET /` | 405 `method_not_allowed` | ① — 변화 없음 |
| 2 | POST + 비JSON 본문 | 400 `invalid_body` | ②로 이동. **한도 소진 여부와 무관하게** 400 |
| 3 | POST + 40KB 본문 | 413 `body_too_large` | ②로 이동. 상한 32,000자 |
| 4 | POST + 캐시에 있는 용어 | 200 + `content[0].type=="tool_use"` | **Anthropic 미호출**. `wrangler tail`에 upstream fetch 없음을 확인 |

**과금 게이트**: 4번의 선행 조건으로 **실제 생성 1회($0.03)** 가 필요하다. 이 1회는 승인된 지출로 간주하고, 그 외 어떤 축도 Anthropic을 호출하지 않는다.

> 2·3번이 429보다 먼저 반환되는 것이 §3-3 순서 변경의 의도된 결과다. 기기 한도를 소진시킨 상태로 4번을 재실행해 **캐시 히트가 여전히 200을 반환하는지** 확인하면 B4 수정이 실측된다(추가 과금 없음).

---

## 6. 테스트 — 함수명 `test_[대상]_[조건]_[기대]`

### 6-1. read path
- `test_조회_캐시히트_Anthropic미호출` — fetch 스텁이 0회 호출됨을 실측 ★ 비용 절감의 실측
- `test_조회_캐시히트_클라파싱가능shape` — 합성 응답이 `content[0].type=="tool_use"` + 도구명 3종 중 하나
- `test_조회_별칭히트_정본반환` — `aliases`를 통한 2차 조회
- `test_조회_한글요청_영문정본히트` ★ **B1 회귀 방어** — `리액트` 저장 후 `React` 요청이 히트
- `test_조회_기기한도소진_캐시히트여전히200` ★ **B4 회귀 방어**
- `test_조회_전역한도소진_캐시히트여전히200` ★ **B4 회귀 방어**
- `test_조회_캐시미스_한도검사수행` — 미스일 때는 ⑤가 정상 동작
- `test_조회_캐시미스_Anthropic호출후저장`
- `test_조회_D1예외_미스폴백` — D1이 던져도 200 응답

### 6-2. write path
- `test_저장_동일키2회_1행만` — INV-4 멱등
- `test_저장_한글요청_정본키워드로저장` ★ **B1** — `term_key`가 `input.keyword` 기준
- `test_저장_요청키_alias로삽입` ★ **B1** — AI가 aliases에 안 넣어도 요청 키가 들어감
- `test_저장_요청키가정본키와동일_alias미삽입` — C9 자기참조 방지
- `test_저장_필수필드누락_스킵` ★ **B2** — `namingReason` 없는 응답이 저장되지 않음
- `test_저장_필수필드누락_응답은정상통과` ★ **B2** — 게이트 탈락이 사용자 응답을 깨지 않음
- `test_저장_input부재_스킵` — C8
- `test_저장_집합밖category_기타로clamp` — INV-13
- `test_저장_tool_use없음_스킵`
- `test_저장_알수없는도구명_스킵`
- `test_저장_버전컬럼_기록됨` — INV-9

### 6-3. term key 동치 ★ 클라-서버 drift 방어
- `test_termKey_대문자입력_소문자키` (`React` → `react`)
- `test_termKey_공백패딩_트림`
- `test_termKey_NBSP패딩_트림하지않음` ★ **C3** — Kotlin trim() 이 안 자르므로 서버도 자르면 안 된다
- `test_termKey_BOM패딩_트림하지않음` ★ **C3** — U+FEFF 동일
- `test_termKey_U001F패딩_트림` ★ **C3** — JS `\s` 로는 안 잘리는 구간
- `test_termKey_U3000패딩_트림` ★ **C3** — 전각 공백(한글 입력에서 실제로 발생)
- `test_termKey_비문자열content_null` → 캐시 우회

⚠️ **입력 집합의 정본 순서(비준 C4)**: 원안은 "devetym `commonTest` 케이스를 복제"였으나, **`shared/src/commonTest`에 `normalizeKeyword` 전용 테스트가 존재하지 않는다**(유일 언급은 `TermRepositoryTest.kt:342` 주석). 복제할 대상이 없으므로 순서를 뒤집는다: **클라 쪽에 정규화 케이스 테이블 테스트를 먼저 세우고**(§8-4 승인 대상), 서버가 같은 집합을 복제한다. 클라 테스트가 없는 상태에서 서버만 만들면 동치 방어가 무근거해진다.

### 6-4. 부정 분기 TTL
- `test_TTL_not_dev_term_29일_히트`
- `test_TTL_not_dev_term_31일_미스처리`
- `test_TTL_term_entry_31일_여전히히트` — 정본은 만료되지 않음(INV-2)
- `test_TTL_만료행_재생성시교체`

### 6-5. 격리
- `test_캐시_킬스위치켜짐_전경로우회`
- `test_캐시_킬스위치기본값_꺼짐` — C7 초기 배포 안전
- `test_캐시_바인딩없음_no_op`

---

## 7. 열린 질문 — **판정 완료** (2026-07-27 비준)

### 7-1. 한도 카운터를 캐시 히트에 소모시킬 것인가
**판정: 소모하지 않는다. 나아가 한도 검사도 통과하지 않는다.**
잠정안(소모 안 함)은 절반만 답한 것이었다 — 검사가 앞에 있으면 한도 소진 사용자에게 무과금 응답조차 못 준다(비준 B4). §3-3 순서를 재배열해 캐시 조회를 한도 검사 앞으로 옮겼다. 남용 표면(히트 무한 요청)은 32KB 본문 상한 + Cloudflare 앞단으로 수용하고, **히트 전용 카운터는 후속 슬라이스 백로그**로 남긴다.

### 7-2. `not_dev_term` / `possible_typo`도 캐시할 것인가 ★
**판정: 3분기 모두 캐시한다. `branch` 컬럼으로 구분하고, 부정 2분기에는 30일 soft TTL을 건다.**
- 캐시 찬성 논거(INV-10 남용 방어)는 유효하다 — 같은 쓰레기 입력이 반복 과금되지 않는다.
- 반대 논거(오판 동결)는 실재한다 — 무효화 경로(INV-5)가 OUT이라 정정 수단이 수동 DELETE뿐이다.
- **TTL이 두 논거를 동시에 만족시킨다**: 남용 방어의 실효는 단기 반복 차단이므로 30일이면 충분하고, 오판의 손해는 무기한이므로 만료가 곧 자동 정정이다. 구현 비용은 조회 조건 1개 + DELETE 1줄, 재과금은 오판 용어당 30일에 1회.
- 구현·INV-5 정합 근거는 §3-5 7단계.

### 7-3. 새 D1(`devetym-cache`) vs 기존 `devetym-usage`에 테이블 추가
**판정: 분리(`CACHE_DB`). 단 마이그레이션 디렉토리 분리가 필수 부속 조건.**
캐시는 정본 데이터(승격 잡의 입력), `usage_log`는 폐기 가능한 텔레메트리로 수명주기가 다르다는 논거를 채택한다. 다만 분리만 하고 `migrations_dir`를 나누지 않으면 **배포가 깨진다**(비준 B3) — §3-1·§3-6에 반영했다.
**✅ 개수 한도 확인 완료(2026-07-27)**: Workers Free 플랜 D1 한도 = **DB 10개 · DB당 500MB · 계정 합계 5GB**. 현재 사용 = **1개**(`devetym-usage`). `devetym-cache` 추가 시 2/10으로 여유가 충분하다. 용량도 문제되지 않는다 — entry payload가 항목당 ~1KB 수준이라 500MB면 수십만 항목이 들어간다.

### 7-4. `@cloudflare/vitest-pool-workers` 도입 비용
**판정: 도입한다.**
결정적 근거는 **write-once 특성상 수동 스모크가 재현 불가능**하다는 점이다 — 한 번 저장되면 같은 시나리오를 다시 돌리려면 매번 DELETE해야 한다. 이 슬라이스의 load-bearing 주장 3개(멱등 write·alias 폴백·정규화 순서)에 shape 게이트와 TTL 경계가 더해져 회귀 방어 없이는 B2형 독성 항목이 프로덕션에서 처음 발견된다. 비용은 devDependency 2개 + `vitest.config.js` 1개 + `package.json` 스크립트 1줄.

### 7-5. `PROMPT_VERSION` 문자열을 무엇으로 할 것인가
**판정: 하드코딩 상수 반려. system 프롬프트 해시를 채택한다.**

```js
const SCHEMA_VERSION = 1;
// prompt_version = "v2-pathA:" + sha256(system[0].text).slice(0, 12)
```

- **반려 근거**: 프롬프트 정본은 클라 `commonMain`이 소유한다(ADR-0004·0006 §6). 버전 문자열을 서버 상수로 두면 개발자가 `SYSTEM_PROMPT`를 고치고 앱을 재배포해도 서버 상수는 그대로라 **신·구 산출물이 같은 태그로 섞인다** → INV-9의 목적(선택적 무효화)이 무효화된다.
- 해시는 프록시가 이미 본문을 파싱하므로 추가 비용이 0이고, 클라 무변경(INV-1)을 지키면서 프롬프트 개정과 자동 연동된다. 사람이 읽을 라벨(`v2-pathA`)은 접두사로 보존한다.
- ⚠️ **해시를 `term_key`에는 넣지 않는다** — 넣으면 프롬프트 개정 시 전체 캐시가 무효화되어 비용이 급증한다. `prompt_version` 컬럼에만 기록한다.

### 7-6. 프롬프트가 바뀌면 캐시를 어떻게 할 것인가
**판정: 수용한다. 단 7-5의 해시 채택을 전제 조건으로 한다.**
S1 배포 후 프롬프트를 개정하면 구버전 payload가 계속 서빙되는 것은 그대로다. 그러나 해시 태깅이 있으면 **어느 행이 구버전인지 기계적으로 선별 가능**해져, 무효화 슬라이스가 왔을 때 실행 가능한 부채가 된다. 원안(하드코딩 상수)으로는 대상 선별 자체가 불가능해 **상환 불가 부채**가 되었을 것이다. "알려진 상태"로 문서화하고 무효화 슬라이스를 ROADMAP 백로그에 명시한다.

---

## 8. 안전·규율

### 8-1. 선행 조건 (착수 전 반드시)
1. **`devetym-proxy`의 `c7218db`를 `main`에 병합** — 현재 `main`은 프로덕션(`c5cd809f`)보다 낡았고 `USAGE_DB` 바인딩이 없다. 이 상태에서 브랜치를 따면 D1 바인딩 없는 베이스 위에 캐시를 짓게 된다. 실측 차이는 `wrangler.toml` 4줄(d1 블록 주석 해제)뿐. **PR 경유 vs 로컬 병합은 사람이 결정**하며, 하네스는 push/머지하지 않는다.
2. ✅ **완료(2026-07-27)** — 무료 플랜 D1 개수 한도 = 10, 현재 1개 사용. 상세 §7-3.
3. `@cloudflare/vitest-pool-workers`가 현재 `compatibility_date = "2025-06-01"`에서 동작하는지, `nodejs_compat` 플래그가 필요한지 확인(비준 N3).

**원격 `devetym-usage` 실측 상태(2026-07-27, read-only 조회)** — §3-1 마이그레이션 계획의 전제 확인:
- `0001_usage_log.sql` 적용됨(`d1_migrations` id=1, 2026-07-14 08:34:56)
- `usage_log` 8컬럼 존재, `cache_hit` **없음**(= `0002_cache_hit.sql`이 추가할 대상이 맞다)
- 11행 적재(2026-07-15 ~ 2026-07-27) — usage 텔레메트리가 실제로 흐르고 있다
- ⚠️ `wrangler d1 list`의 `num_tables` 필드는 **0으로 표시되나 실제와 다르다**(지연되는 캐시 통계). 이 필드로 마이그레이션 적용 여부를 판단하지 말 것
- → §3-1의 "`0001_usage_log.sql`을 `migrations/usage/`로 이동하되 **파일명·번호 유지**" 지시가 옳다. `d1_migrations`가 이름으로 적용 이력을 추적하므로 이름을 바꾸면 원격에서 재적용을 시도한다

### 8-2. 배포 규율
- 마이그레이션은 `--local` → `--remote` 순으로 적용. **두 DB 모두** 적용한다(§5 S축3). `--remote` 적용은 **되돌리기 어려우므로** 사람 승인 후 실행.
- 배포는 캐시 **킬 스위치 ON(`CACHE_DISABLED = "1"`) 상태로 먼저 올려** 기존 동작 무회귀를 확인한 뒤 OFF로 전환하는 2단 롤아웃. 초기 커밋의 기본값이 이미 ON이므로(§3-7) 별도 조작이 필요 없다.
- 브랜치는 병합 후에도 **삭제하지 않는다**(ROADMAP 브랜치 보존 규율).

### 8-3. 비용 규율
- 이 슬라이스 전 구간에서 Anthropic 실호출은 **S축4 스모크의 1회($0.03)** 만 허용한다.
- Console 월 $30 하드캡·알림($10/$20/$25)은 그대로 유효 — 변경하지 않는다.

### 8-4. 손대지 않는 것
- 시스템 프롬프트·도구 스키마(클라 `commonMain` 소유 — ADR-0004·0006 §6)
- 한도 상수(`DAILY_LIMIT`·`GLOBAL_DAILY_LIMIT`)·과금 파라미터 강제(`FORCED_MODEL` 등) — **§9 INV-8 판정 전까지 유지**
- ADR·INV — 이 슬라이스는 제약을 **소비**하지 생성하지 않는다
- **클라이언트 코드 — 단 아래 2건은 예외(devetym repo, 승인 대상)**:
  1. `AppJson.kt`에 서버 동기화 지점 주석 1줄(§3-2)
  2. `commonTest`에 `normalizeKeyword` 케이스 테이블 테스트 신설(§6-3, 비준 C4) — 서버 동치 테스트의 정본이 되므로 **서버 구현보다 먼저** 필요
  둘 다 출하 동작을 바꾸지 않으므로 앱 심사와 무관하다.

---

## 9. 사람 판정 — **완료** (2026-07-27)

### 9-1. INV-8(생성 파라미터) 미달성 — 스펙 범위와 충돌

`cache-delivery-milestones.md` M1은 지켜야 할 불변식으로 **INV-8(temperature 낮게, 0–0.3)** 을 명시하는데, 원 스펙 §4 표에 누락되어 있었다(비준 C5).

**실측 상태**:
- 클라 `buildClaudeRequest`는 `temperature`를 보내지 않는다 → Anthropic 기본값(1.0) 적용
- 프록시 `FORCED_*` 상수에도 `temperature`가 없다
- 현재 모델 `claude-sonnet-4-6` + extended thinking 조합에서 `temperature` 지정은 **허용된다**(API 제약 아님) → INV-8은 **달성 가능한데 그냥 미구현**

**충돌**: 고치려면 프록시에 `FORCED_TEMPERATURE`를 추가해야 하는데, 이는 §8-4가 "손대지 않는다"고 선을 그은 **과금·생성 파라미터 강제** 영역이다.

**선택지**:

| | 방법 | 장 | 단 |
|---|---|---|---|
| A | S1에 포함 — `body.temperature = 0.2` 서버 강제 | INV-8 즉시 달성. 정본 성격 강화(같은 용어 재생성 시 편차 감소) | §8-4 자기 규율 위반. 생성 품질 변화가 이 슬라이스 오라클로 검증 불가 |
| B | 별도 슬라이스로 분리 | 범위 규율 유지. 품질 영향을 따로 측정 가능 | INV-8 미달성 상태가 유지됨 |
| C | INV-8 자체를 재검토 | 정본이 실제로 필요한 값인지 확인 | INV 수정은 하네스 권한 밖 — 사람 소관 |

**✅ 판정: B (별도 슬라이스로 분리) — 2026-07-27 사람 확정.**

근거 — 이 슬라이스는 *비용 구조*를 바꾸는 것이고 INV-8은 *생성 품질*을 바꾼다. 한 슬라이스에 섞으면 배포 후 문제가 생겼을 때 원인 분리가 안 된다.

**이 판정이 남기는 부채(명시)**: INV-8은 S1 배포 후에도 **미달성 상태로 유지된다**(temperature = Anthropic 기본값 1.0). 즉 같은 용어를 재생성하면 편차가 큰 결과가 나올 수 있고, 이는 캐시가 "정본 1건을 동결"하는 성격과 긴장 관계에 있다 — 어떤 생성물이 정본이 되느냐가 운에 좌우된다. 다만 S1은 **재생성 자체를 없애는** 슬라이스라 노출 빈도는 오히려 줄어든다. ROADMAP 백로그에 별도 항목으로 남긴다.

### 9-2. 부정 분기 TTL 30일 — 값 확인

§7-2 판정이 채택한 `NEGATIVE_TTL_DAYS = 30`은 "단기 반복 남용은 막고 오판은 풀어준다"는 정성 기준에서 나온 값이며, 실측 근거가 없다.

**✅ 판정: 30일 그대로 채택 — 2026-07-27 사람 확정.** 출시 후 `usage_log`의 부정 분기 재생성 빈도를 보고 조정할 수 있도록 **상수 1개(`NEGATIVE_TTL_DAYS`)로 격리**한다. 재조정은 상수 변경 + 재배포로 끝나야 한다(다른 로직에 30이라는 값을 박지 않는다).

---

## Open Questions

**없음.** §7 6건(비준 판정)·§9 2건(사람 판정) 전부 종결 — 2026-07-27.

구현 착수의 남은 전제는 열린 질문이 아니라 **§8-1 선행 조건 3건**(프록시 `main` 병합 · D1 개수 한도 확인 · vitest-pool-workers 호환성)이다.
