# 🤖 서버 캐시 S1 — 구현 착수 핸드오프

> # ⛔ 역할 종료 — 이 문서로 세션을 시작하지 마시오
>
> **2026-07-28 구현·배포 완료.** 이 문서가 지시한 작업(§4 Phase 1~5)은 전부 끝났고 **캐시는 가동 중**이다.
> 아래 §1(환경 정합)·§4(구현 순서)·§6(완료 조건)은 **stale**이다 — 브랜치는 이미 만들어져 커밋·푸시·PR까지 됐고,
> 선행 조건 3건과 green 오라클 S축1~4가 모두 닫혔다.
>
> **§3(확정된 결정)의 10개 항목만 여전히 유효**하며, 그 정본은 이제 스펙과 구현 코드 자체다.
>
> **현재 상태를 알려면 → [`../../ROADMAP.md`](../../ROADMAP.md) 백로그 항목 I** (진행 상태 정본)
> **설계 근거를 보려면 → [`../specs/server-m0-m1-cache-read-through-draft.md`](../specs/server-m0-m1-cache-read-through-draft.md)**
> **서버 운영·검증 절차 → [`devetym-proxy`의 README](https://github.com/data-sy/devetym-proxy)**
>
> 〔이 문서가 남긴 결과: 프록시 커밋 4건(PR #3) · 클라 커밋 3건(PR #18) · 스펙 §3-2 정정 1건 ·
> 무과금 3축 green + 라이브 스모크 $0.0230. 세션 중 발견된 스펙 결함 1건은 보고·승인·정정 완료.〕
>
> 〔선행 핸드오프 [`🤖-26-07-27-server-cache-s1-handoff.md`](🤖-26-07-27-server-cache-s1-handoff.md)도 역할 종료 — 둘 다 이력 보존용.〕

---

<details>
<summary>이하 원문 보존 (2026-07-27 작성 시점 · 실행 완료)</summary>

> **성격: 콜드 세션 착수용 실행 문서.** 이 문서를 읽는 세션은 **이전 대화 맥락이 없다고 가정**한다.
> 이 문서를 링크로 받았다면 **§1을 그대로 실행하고 §4 순서대로 구현하면 된다.** 설계를 다시 논의하지 않는다.
>
> **작성**: 2026-07-27 (비준 세션). **선행 핸드오프**: [`🤖-26-07-27-server-cache-s1-handoff.md`](🤖-26-07-27-server-cache-s1-handoff.md) — 비준 착수용, **역할 종료**(비준 완료).
> **진행 상태 정본은 이 문서가 아니라 [`../../ROADMAP.md`](../../ROADMAP.md) 백로그 항목 I** — 충돌하면 ROADMAP을 신뢰한다.

---

## 0. 한 줄 요약

`devetym-proxy`(Cloudflare Worker)에 **D1 read-through 캐시**를 붙인다. 스펙은 **비준 완료·열린 질문 0건**이고, **코드는 아직 0줄**이다. 이 세션이 처음 코드를 쓴다.

**목표**: 한 사용자가 생성시킨 어원 항목을 다른 사용자가 재사용해 Anthropic 재과금을 없앤다. 비용이 *사용자 수*가 아니라 *새 용어 수*에 비례하게 만든다.

⚠️ **설계 결정은 전부 끝났다.** 스펙 §7(비준 판정 6건)·§9(사람 판정 2건)에 근거까지 기록돼 있다. **재론하지 말 것.** 구현 중 스펙이 틀렸다고 판단되면 고치지 말고 §7 규율대로 사람에게 보고한다.

---

## 1. 환경 정합 — 먼저 이것부터 실행한다

```bash
# ── proxy: main 최신화 후 구현 브랜치 분기 ──────────────────────────
git -C ~/devetym-proxy checkout main
git -C ~/devetym-proxy pull --ff-only origin main
git -C ~/devetym-proxy checkout -b feat/s1-read-through-cache

# ── devetym: 스펙 브랜치 위에 클라 변경 브랜치를 스택 ────────────────
#    (스펙이 main·origin 어디에도 없고 이 브랜치에만 있다. 스택해야 스펙을
#     작업트리에서 읽으면서 클라 변경을 얹을 수 있다.)
git -C ~/devetym checkout docs/server-cache-s1-spec
git -C ~/devetym checkout -b feat/s1-normalize-equivalence
```

**검증** — 아래가 전부 맞아야 착수한다.

```bash
git -C ~/devetym-proxy log --oneline -1        # 7dfbd7c 이상 (PR #2 병합분 포함)
git -C ~/devetym-proxy branch --show-current   # feat/s1-read-through-cache
grep -c 'binding = "USAGE_DB"' ~/devetym-proxy/wrangler.toml   # 1  (0이면 pull 실패)

git -C ~/devetym branch --show-current         # feat/s1-normalize-equivalence
ls ~/devetym/docs/specs/server-m0-m1-cache-read-through-draft.md   # 존재해야 함
```

> **왜 브랜치가 둘인가**: 이 슬라이스는 서버(`devetym-proxy`)가 주 무대지만, 스펙 §8-4가 승인한 **클라 변경 2건**이 devetym repo에 있고 그중 하나는 **서버 구현보다 먼저** 필요하다(§4 Phase 2). 두 repo를 오가며 작업한다.

---

## 2. 정본 (재론 금지 · 이 문서에 중복하지 않는다)

**구현 스펙 — 이것이 유일한 정본이다. 반드시 통독하고 시작한다.**
- [`~/devetym/docs/specs/server-m0-m1-cache-read-through-draft.md`](../specs/server-m0-m1-cache-read-through-draft.md)
  - §3 산출 명세 (스키마·키 도출·조회 순서·합성·write-back·설정·실패 격리)
  - §5 완료 조건 (green 오라클 S축1~4) · §6 테스트 목록
  - §7 비준 판정 6건 · §9 사람 판정 2건 — **결정과 근거**
  - §8 안전·규율

**제약 정본 (읽되 수정 금지)**
- `~/devetym/docs/cache-delivery-milestones.md` §1 — INV-1~13
- `~/devetym/docs/adr/0006-server-cache-boundary.md` — 계약 정본
- `~/devetym/docs/adr/0004-backend-proxy-boundary.md` — 계승 계약

**클라 계약 (서버가 맞춰야 할 상대)**
- `shared/src/commonMain/kotlin/com/robin/devetym/data/AppJson.kt:31` — `normalizeKeyword` ★ 동치 대상
- `.../data/remote/ClaudeDto.kt` — `ClaudeResponse`·`toTermResult()` 3분기·`Tools` 상수
- `.../data/remote/ClaudePrompt.kt` — `buildClaudeRequest`(keyword가 `messages[0].content`)
- `.../data/remote/ClaudeApi.kt` — 429/402 매핑·`InvalidResponse` 봉인 범위
- `.../model/Category.kt` — 정본 6집합 · `.../model/TermEntry.kt` — required 필드·wire 키

**서버 현행**
- `~/devetym-proxy/src/index.js` (204줄 바닐라 JS 단일 파일) · `wrangler.toml` · `migrations/0001_usage_log.sql`
- 테스트 프레임워크 **전무** — `package.json` 스크립트 = `dev`/`deploy`/`tail` 3개뿐. 이 슬라이스가 세운다.

---

## 3. 확정된 결정 요약 (근거는 스펙에)

재론 금지. 구현이 이 표와 어긋나면 구현이 틀린 것이다.

| # | 결정 | 스펙 |
|---|---|---|
| 1 | `term_key`는 **AI 정본 키워드** 기준(`term_entry`). 요청 키는 alias로 삽입. 부정 2분기는 요청 키 사용 | §3-2 |
| 2 | write 직전 **최소 shape 게이트**. 탈락 시 **저장만** 스킵(응답은 통과). `waitUntil` 안이라 체감 지연 0 | §3-5 3단계 |
| 3 | 마이그레이션 **DB별 디렉토리 분리**(`migrations/cache`·`migrations/usage`) + `migrations_dir` 지정 | §3-1·§3-6 |
| 4 | 조회 순서 = 본문 파싱 → **캐시 조회** → (미스만) 한도 검사 → Anthropic. 히트는 한도를 소모도 검사도 안 한다 | §3-3 |
| 5 | 3분기 모두 캐시. 부정 2분기는 **30일 soft TTL**(`NEGATIVE_TTL_DAYS` 상수 1개로 격리) | §7-2·§9-2 |
| 6 | `prompt_version` = `"v2-pathA:" + sha256(system[0].text).slice(0,12)`. **해시를 `term_key`에는 넣지 않는다** | §7-5 |
| 7 | 캐시 DB 분리(`CACHE_DB` = `devetym-cache`) | §7-3 |
| 8 | 테스트는 vitest + `@cloudflare/vitest-pool-workers` | §7-4 |
| 9 | 킬 스위치 `CACHE_DISABLED`, **초기 기본값 `"1"`(꺼짐)** | §3-7 |
| 10 | **INV-8(temperature 강제)은 이 슬라이스 밖.** `FORCED_*` 상수를 건드리지 않는다 | §9-1 |

---

## 4. 구현 순서

### Phase 1 · 선행 조건 1건 (§8-1 잔여)

1. ~~무료 플랜 D1 개수 한도 확인~~ → **✅ 완료(2026-07-27)**. 한도 10개 · 현재 1개(`devetym-usage`) · DB당 500MB. `devetym-cache` 추가에 제약 없음.
2. **`@cloudflare/vitest-pool-workers` 호환성** ⏳ — 현재 `compatibility_date = "2025-06-01"`에서 동작하는지, `nodejs_compat` 플래그가 필요한지. 필요하면 `wrangler.toml`에 추가하고 이유를 커밋 메시지에 남긴다.

**원격 `devetym-usage` 실측 상태(2026-07-27)** — 3a·3c 착수 전 알아둘 것:
- `0001_usage_log.sql`은 **원격에 이미 적용됨**(`d1_migrations` id=1). `migrations/usage/`로 옮길 때 **파일명·번호를 절대 바꾸지 말 것** — 바꾸면 원격에서 재적용을 시도한다
- `usage_log`에 `cache_hit` 컬럼 없음 = `0002_cache_hit.sql`의 `ALTER TABLE` 대상이 맞음
- 11행 적재 중(2026-07-15~) — 텔레메트리 정상 동작
- ⚠️ `wrangler d1 list`의 `num_tables`는 **0으로 나오지만 실제와 다르다**(지연 캐시 통계). 이걸로 마이그레이션 적용 여부를 판단하지 말 것

### Phase 2 · 클라 동치 테스트 ★ 서버보다 먼저

**repo: `~/devetym`, 브랜치 `feat/s1-normalize-equivalence`**

스펙 §6-3 경고대로, `shared/src/commonTest`에 `normalizeKeyword` 전용 테스트가 **없다**. 서버가 복제할 정본 집합이 존재하지 않으므로 **클라 쪽을 먼저 세운다.**

1. `commonTest`에 `normalizeKeyword` 케이스 테이블 테스트 신설 — 스펙 §6-3의 6개 케이스를 Kotlin 쪽에서 먼저 고정한다. ~~특히 **NBSP·BOM은 "자르지 않음"이 정답**이다(Kotlin `Char.isWhitespace()`가 제외).~~
   > ❌ **이 문장은 틀렸다(2026-07-28 실측 반증).** NBSP U+00A0은 **잘린다** — Kotlin/JVM `Char.isWhitespace()`는 `Character.isWhitespace(ch) || Character.isSpaceChar(ch)`라 NBSP·U+2007·U+202F를 포함한다. BOM U+FEFF만 "자르지 않음"이 맞다. 정본 집합은 스펙 §3-2 정정 박스와 `NormalizeKeywordTest`.
2. `AppJson.kt`에 서버 동기화 지점 주석 1줄 추가.
3. `./gradlew :shared:test` green 확인.
4. 커밋. **푸시하지 않는다.**

> 두 변경 모두 출하 동작을 바꾸지 않으므로 앱 심사와 무관하다.

### Phase 3 · 서버 구현

**repo: `~/devetym-proxy`, 브랜치 `feat/s1-read-through-cache`**

스펙 §3 순서대로. 권장 커밋 단위:

| 순서 | 내용 | 스펙 |
|---|---|---|
| 3a | 마이그레이션 디렉토리 분리 + `0001_usage_log.sql` 이동 + `wrangler.toml` `migrations_dir` | §3-1·§3-6 |
| 3b | `devetym-cache` D1 생성 + `migrations/cache/0001_term_cache.sql` + `CACHE_DB` 바인딩 | §3-1·§3-6 |
| 3c | `migrations/usage/0002_cache_hit.sql` + `logUsage` 시그니처 확장(`cache_hit`) | §3-1·§3-4 |
| 3d | `normalizeTermKey` — **Kotlin 집합 정확 복제**(합집합 아님, `\s` 금지) | §3-2 |
| 3e | 조회 순서 재배열 + 캐시 조회 + 합성 응답 + 히트 시 `hit_count`·usage 적재 | §3-3·§3-4 |
| 3f | shape 게이트 + INV-13 clamp + 버전 태깅 + write-back + alias 삽입 규칙 | §3-5 |
| 3g | 부정 분기 TTL(조회 만료 처리 + 저장 전 만료행 DELETE) | §3-5 7단계 |
| 3h | 킬 스위치 + 실패 격리 전 경로 | §3-7 |

### Phase 4 · 테스트 (S축2 — load-bearing)

스펙 §6의 테스트를 전부 작성한다. **회귀 방어 표식(★)이 붙은 것은 비준이 잡은 Blocker의 재발 방지용이므로 생략 금지**:
- `test_조회_한글요청_영문정본히트` (B1)
- `test_조회_기기한도소진_캐시히트여전히200` / `test_조회_전역한도소진_...` (B4)
- `test_저장_필수필드누락_스킵` / `test_저장_필수필드누락_응답은정상통과` (B2)
- `test_저장_요청키_alias로삽입` (B1)

`package.json`에 `"test": "vitest run"` 추가.

### Phase 5 · green 오라클 4축 (§5)

| 축 | 명령 |
|---|---|
| S축1 | `npx wrangler deploy --dry-run` |
| S축2 | `npm test` |
| S축3 | `npx wrangler d1 migrations apply devetym-cache --local` **및** `... devetym-usage --local` |
| S축4 | 배포 후 스모크 — §5-1 절차 (**사람 승인 후**) |

**S축1~3이 전부 green이 될 때까지 배포하지 않는다.**

---

## 5. 안전 규율 — 이 세션이 **하지 않을 것**

- **push·머지 금지** — 커밋까지만. 푸시·PR·머지는 전부 사람이 한다
- **`--draft` 접미사 제거 금지** — 스펙 파일명은 그대로 둔다
- **D1 `--remote` 마이그레이션 금지** — `--local`만. `--remote`는 되돌리기 어려우므로 **사람 승인 후에만**
- **배포 금지** — `wrangler deploy`는 사람 승인 후. `--dry-run`은 자유
- **Anthropic 실호출 금지** — S축4 스모크의 **1회($0.03)** 만 허용이고 그것도 사람 승인 후. S축1~3은 전부 무과금
- **ADR·INV·`cache-delivery-milestones.md` 수정 금지** — 이 슬라이스는 제약을 **소비**하지 생성하지 않는다. 제약 자체에 결함이 보이면 **고치지 말고 보고**
- **스펙 수정 금지(원칙)** — 구현하다 스펙이 실제와 어긋나면 보고 후 승인받아 고친다. 임의 수정 금지
- **하네스(`~/dev/agent-harnesses/`) 수정 금지** — 격리 세션 소관
- **손대지 않는 것**: 시스템 프롬프트·도구 스키마·한도 상수(`DAILY_LIMIT`·`GLOBAL_DAILY_LIMIT`)·과금 파라미터 강제(`FORCED_MODEL` 등, **INV-8 포함** — §9-1)
- **브랜치 삭제 금지** — 병합 후에도 보존

---

## 6. 완료 조건

이 세션이 끝났다고 말할 수 있는 조건:

1. S축1·S축2·S축3 **전부 green** (무과금)
2. 스펙 §6 테스트 전건 작성 + 통과, ★ 회귀 방어 테스트 포함
3. 두 repo 각각 커밋 완료 (푸시 안 함)
4. `~/devetym/ROADMAP.md` 백로그 항목 I에 진행 상태 갱신 — **진행 상태 정본은 디스크 로드맵**
5. 사람에게 남길 것 정리: ① S축4 스모크 실행 승인 요청($0.03) ② `--remote` 마이그레이션 승인 요청 ③ 배포 승인 요청 ④ 두 repo PR 생성 여부

**구현 중 발견한 스펙 결함은 고치지 말고 목록으로 남겨 보고한다.**

---

## 7. 착수 전 5분 체크

- [ ] §1 명령 실행 + 검증 4줄 통과
- [ ] 스펙 통독 (특히 §3 전체 · §7 판정 · §9 판정)
- [ ] `src/index.js` 204줄 통독 — 어디에 무엇을 끼워 넣는지 파악
- [ ] 클라 `ClaudeDto.kt`·`TermEntry.kt` 확인 — shape 게이트가 방어할 대상
- [ ] §5 안전 규율 숙지 — 특히 push·`--remote`·실호출 3종

</details>
