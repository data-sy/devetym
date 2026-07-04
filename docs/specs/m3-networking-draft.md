# M3 슬라이스 (draft) — 네트워킹 + 번들 로더 (클라측)

> **성격: 비준 하네스 입력용 spec 슬라이스(`-draft.md`).** 단일 [`spec.md`](spec.md) Phase 2의 데이터 소스(2-1 `BundleDbSource`·2-2 `ClaudeApi`) 부분을 마일스톤 경계로 떼어낸 것. 진행 상태 정본은 [`../../ROADMAP.md`](../../ROADMAP.md), 상위 설계는 [`../architecture.md`](../architecture.md), 결정 근거는 [`../adr/`](../adr/).
>
> 이 문서는 **자율 구현 전 적대적 비준(완결성·모호성·테스트 가능성·정합성)의 대상**이다. 아래 §7 열린 질문은 비준이 판정할 항목이다.

- **마일스톤**: M3 · 네트워킹 + 번들 로더 (**클라측만** — §0 스코핑 판정)
- **브랜치**: `feat/m3-networking` (=`feat/m2-local-db` 위에 **스택** 분기 — main엔 아직 M1·M2가 없으므로 M3가 M1·M2 코드를 상속해야 빌드됨. ROADMAP 「브랜치·공개 전략」)
- **참조**: spec [2-1·2-2](spec.md), [ADR-0006](../adr/0006-server-cache-boundary.md)(read-through 캐시 경계, ADR-0004 대체), [ADR-0002](../adr/0002-code-idiom-principle.md)(코틀린 관용), [M1 슬라이스](m1-model-serialization-draft.md)(§3-1 wire 키 계약·§7-3 Json 정책 이월·§7-4 로더 실측 이월), [M2 슬라이스](m2-local-db-draft.md)(§3-4 매퍼·`aliasesJson`), 캐시 트랙 [INV-1·2·9·13](../cache-delivery-milestones.md)

---

## 0. 스코핑 판정 (슬라이스 첫머리 — 핸드오프 §2 요구)

**M3 = 클라측(CMP `commonMain`)만. 서버(`devetym-proxy`)는 별도 트랙으로 뗀다.** (2026-07-05 사람 확인.)

근거:
- 서버는 **별도 repo·TS/Cloudflare Worker·D1·Durable Objects**로, 언어도 green 오라클도 CMP와 이질적이다(KMP 4축 vs Worker 자체 테스트). 한 슬라이스에 묶으면 비준 대상이 비대해지고 4축 수렴 오라클과 정합이 흐려진다.
- **ADR-0006상 read-through는 클라에 투명**하다: 클라 `Source`는 여전히 `BUNDLE` vs 네트워크(→`AI`)이고, 서버 D1 히트 여부는 서버 내부 사정이라 **호출 형태가 캐시 유무와 무관하게 같다**. 따라서 서버 repo가 없어도 클라를 계약(ADR-0006)에 맞춰 짓고 `MockEngine`으로 실측할 수 있다 — 나중에 서버가 서도 클라 리팩토링이 없다(투명성이 곧 리팩토링-0 보증).
- M1·M2 리듬(응집된 `commonMain` 슬라이스 1개 + 4축 green)을 보존한다.

**이 판정이 ROADMAP·핸드오프의 "M3 = 클라+서버"를 클라측으로 좁힌다.** 서버측 항목(D1 스키마·Worker read-through·write-back·first-write-wins·single-flight(DO)·validator write-게이트·rate-limit/남용/무효화, 그리고 **INV-13 정규화-후-캐시쓰기**)은 전부 **서버 트랙**으로 이관한다(§2 OUT, §4 INV-13 이관, §7-6). ROADMAP M3 ⚠️의 두 상속 항목 중 **INV-A 로더 실측은 클라(BundleDbSource)라 이 슬라이스가 소유**하고(§4·§6-B), **INV-13(서버 category 소유)은 서버 트랙으로 이관**한다.

---

## 1. 목표 (이 슬라이스가 내는 것)

`commonMain`에 두 데이터 소스를 세운다:
- **`BundleDbSource`** — 앱 시작 1회 로드한 번들 `terms.json`(650)을 메모리 캐시해 `search`(keyword+aliases 완전 매칭)·`autocomplete`(keyword prefix)를 제공. 로컬 "head" 계층(INV-12).
- **`ClaudeApi`** — Ktor로 read-through 프록시(ADR-0006)를 호출하고 Anthropic 응답의 `tool_use`를 3분기해 `TermResult`로 변환. 시스템 프롬프트·도구 스키마는 `commonMain` 소유(iOS 검증본 계승, ADR-0006 §6).

이 두 소스는 M4 `TermRepository`가 3계층 read-through로 오케스트레이션할 **메커니즘**을 제공한다 — fetch 3단 순서·pinning 스킵·upsert 정책·**AI 응답 category 정규화**는 M4 소관이고, M3는 그 위에 얹힐 **로더·네트워크 클라이언트**만 확정한다. M1이 세운 도메인 모델(`TermEntry`/`TermResult`/`Source`)과 M2가 세운 저장 계층(`Term`·매퍼)을 소비한다.

Ktor 도입은 **버전 정렬을 빌드로 실측**하는 지점이다(M1 serialization·M2 SQLDelight와 동일 규율): Ktor 좌표가 Kotlin 2.3.21에서 klib 소비돼 `linkDebugFrameworkIosSimulatorArm64`·`iosSimulatorArm64Test` green이 되는지 실빌드로 확인한다(§5). UI·오케스트레이터(M4)·Koin 전체 조립·셸은 이 슬라이스 밖이다.

## 2. 스코프

**IN (M3, 클라측):**
- **`BundleDbSource`**(`commonMain/data/bundle/`) — 인터페이스(spec 2-1) + 파싱·인덱싱·매칭 구현. 번들 바이트 획득(`Res.readBytes`)과 **파싱·인덱스·매칭을 분리**해(§3-1) INV-A 로더 실측(§6-B)이 실제 배포 `terms.json`을 파서·인덱스에 태울 수 있게 한다.
- **`ClaudeApi`**(`commonMain/data/remote/`) — Ktor 클라이언트 + 요청 빌더(`buildClaudeRequest`) + 응답 DTO + `tool_use` 3분기 파서(`toTermResult`) + `ClaudeException` 오류 타입(spec 2-2).
- **시스템 프롬프트·도구 스키마**(`commonMain/data/remote/`) — iOS 검증본 계승, 3도구(`return_term_entry`/`return_not_dev_term`/`return_possible_typo`) JSON 스키마 + 시스템 프롬프트 원문(ADR-0006 §6, §3-3).
- **HttpClient 팩토리 + 엔진 `expect`/`actual`**(`commonMain` + `androidMain`/`iosMain`) — ContentNegotiation(Json)·타임아웃 설정은 `commonMain`, 엔진(Darwin/OkHttp)만 플랫폼별(§3-4).
- **공유 `Json` wire 정책 확정**(M1 §7-3 이월) — 디코드 회복성(`ignoreUnknownKeys`) 정책을 이 슬라이스가 못박는다(§3-4).
- 위를 검증하는 `commonTest`(§6-A, JVM+네이티브 양쪽 실행) + `androidUnitTest`(§6-B, 실 번들 로더 회귀 가드).

**OUT (다른 마일스톤/트랙):**
- **서버 `devetym-proxy` 전체**(D1 스키마·Worker read-through·write-back·first-write-wins·single-flight(DO)·validator write-게이트·rate-limit/남용/무효화·**INV-13 정규화-후-캐시쓰기**) → **서버 트랙**(별도 repo·TS/Worker, 캐시 트랙 M0/M1/M2/M3/M7). §0 판정.
- **`TermRepository`**(오케스트레이터·fetch 3단·로컬 AI 캐시 조회·upsert 정책·pinning 스킵·**AI 응답 category 정규화**·Analytics 오류 로깅) → **M4**(spec 2-3). M3는 `BundleDbSource.search`/`ClaudeApi.generate`를 *제공*할 뿐, 이들을 순서대로 엮는 오케스트레이션과 결과의 로컬 저장은 M4다.
- **`Constants`**(proxyBaseUrl·claudeModel·타임아웃 값)·**Koin 전체 조립**(엔진·클라이언트·deviceId 주입) → 상수는 M3가 참조하되 값 확정은 착수 시(§3-2), Koin 런타임 주입 배선은 **M7**(spec 1-4). M3는 팩토리·`expect`/`actual`를 제공하고 그 컴파일을 green으로 검증한다.
- **기기 식별자(`deviceId`) 실제 구현**(`expect`/`actual` 플랫폼 ID) → M7 배선(M3는 `deviceId: () -> String` 주입 시그니처만 소비, 테스트는 상수 람다).
- **UI·ViewModel·오류 표시 분기**(`ErrorKind`) → M5·M6(spec 3-x).

## 3. 산출 명세

### 3-1. `BundleDbSource` (`commonMain/data/bundle/`)

```kotlin
interface BundleDbSource {
    fun search(keyword: String): TermEntry?             // keyword + aliases, 대소문자 무시 완전 매칭
    fun autocomplete(prefix: String): List<TermEntry>   // keyword prefix 매칭
}
```

- **`search`**: 입력을 **공유 정규화 함수** `normalizeKeyword`로 정규화한 뒤 `keyword`(정규화 비교) **또는 `aliases` 중 하나**(정규화 비교)와 완전 일치하는 항목. 미발견 시 `null`. 빈/공백 입력은 `null`.
  - **정규화 키 다대일 충돌 시 계약(조용한 비결정 제거)**: `normalizeKeyword`로 접힌 정규화 키가 서로 다른 두 엔트리에 걸쳐 중복될 수 있다 — 실 배포 `terms.json`(650)에 현재 **3건 실재**(`집계`→{aggregate, aggregation}·`분기`→{branch, fork}·`샤딩`→{shard, sharding}). 이때 `search`는 **번들(리스트) 순서상 첫 매칭을 결정적으로 반환**한다(아래 인덱스 first-wins). 뒤로 밀린 매칭은 `search`로 가려지고 `autocomplete`도 alias 비대상(§3-1 아래)이라 대체 발견 경로가 없다 — 그러나 이 반환은 **결정적**이며 리스트 순서에 고정돼 "조용히 비결정적으로 틀림"은 아니다. **alias 정규화-유일성 강제(번들 린트 de-dup)와 가려진 엔트리 발견성 회복은 M3에서 미봉이며 Open Questions로 명시 이월**한다 — M3 `search`는 최소 계약으로 **결정적 반환만** 보증한다.
  - **`normalizeKeyword`는 term-key(캐시 키·로컬 매칭) 정규화의 단일 정본이다**(`commonMain/data/`): `fun normalizeKeyword(s: String): String = s.trim().lowercase()`. `BundleDbSource.search`가 이 함수로 로컬 매칭 키를 만들고, **서버 캐시 키잉도 같은 정규화**(§7-3 서버 파생 또는 `X-Term-Key` 헤더)를 써 `React`/`react`가 같은 term-key로 접혀 캐시 파편화·M4 중복 upsert가 방지된다. **단, 이 정규화는 키잉 전용이다 — AI에 보여줄 질의 content에는 적용하지 않는다**(§3-2): lowercase가 대소문자 유의미 용어(`NaN`/`Go`/`REST`/`C`)의 의미를 뭉개 어원 오답을 유발하므로, AI user 메시지에는 원본 keyword를 대소문자 보존해 싣는다(iOS 검증본 계승). 키잉과 프롬프트 입력은 다른 요구라 한 함수로 합치지 않는다.
- **`autocomplete`**: 빈/공백 prefix면 `emptyList()`. 아니면 정규화 prefix로 `keyword.lowercase().startsWith(prefix)`인 항목들. (aliases는 autocomplete 대상 아님 — spec 2-1.)

**로드 경로와 파싱·인덱스를 분리한다(§6-B 실측·§7-1 판정 대상):**
```kotlin
// 파싱·인덱스·매칭 — 바이트 획득과 무관. 테스트가 실 번들 entries를 직접 주입할 수 있다.
class InMemoryBundleDbSource(entries: List<TermEntry>) : BundleDbSource {
    // init에서 검색 인덱스 구성: keyword·aliases를 정규화 키로 접어 매칭 집합에 넣는다.
    // 정규화 키 충돌 시 번들(리스트) 순서 first-wins(putIfAbsent) — §3-1 결정적 반환 계약과 일치(last-wins 덮어쓰기 금지).
    // ⚠️ INV-A: aliases를 인덱스에서 누락하면 search(alias)가 조용히 miss한다(§4·§6-B의 sharp 오라클).
}

// 실제 바이트 획득(compose-resources) — 이 suspend 경로가 앱 런타임의 로드다.
suspend fun loadBundleDbSource(json: Json): BundleDbSource {
    val text = Res.readBytes("files/terms.json").decodeToString()
    return InMemoryBundleDbSource(json.decodeFromString<List<TermEntry>>(text))
}
```
- **분리 이유**: `Res.readBytes`(compose-resources 런타임)는 **plain JVM 단위테스트에서 미실행**이다(M1 fixture가 `MissingResourceException`으로 실측 — `TermsBundleFixtureTest` 주석). 따라서 INV-A 로더 실측(§6-B)은 실 배포 `terms.json`을 `File`로 읽어 **`InMemoryBundleDbSource`의 파서·인덱스·매칭 경로**에 태운다(M1 fixture와 동일 배치 이유). `Res.readBytes` **바이트 획득 자체**의 런타임 정확성(경로·인코딩)은 컴파일/링크 + M8 실기기 로드로 실측(§4 이월).
- 앱 시작 1회 로드·메모리 캐시(공유 리소스). 로드는 suspend(리소스 IO) — 호출자(M4/M7)가 초기화 시 1회 await.

### 3-2. `ClaudeApi` (`commonMain/data/remote/`) — Ktor

> **백엔드 계약([ADR-0006](../adr/0006-server-cache-boundary.md), ADR-0004 대체).** 앱에 키 없음 — 프록시가 키 주입 + 기기당 일일 한도. **프록시는 read-through 캐시**(서버가 D1 조회→미스 시 생성·write-back, INV-1·2)나 **클라엔 투명**하다(§0). 이 호출 형태는 캐시 유무와 무관하게 같다. 응답의 `schemaVersion`/`promptVersion`(INV-9)만 `TermEntry`로 왕복 디코드.

```kotlin
class ClaudeApi(
    private val client: HttpClient,
    private val deviceId: () -> String,
) {
    suspend fun generate(keyword: String): TermResult {
        val res = try {
            client.post(Constants.proxyBaseUrl) {
                header("X-Device-Id", deviceId())          // 프록시 한도 카운터 키
                contentType(ContentType.Application.Json)
                setBody(buildClaudeRequest(keyword))        // system 프롬프트 + tools 구성(§3-3)
            }
        } catch (e: HttpRequestTimeoutException) {
            throw ClaudeException.Timeout
        } catch (e: IOException) {                          // 연결 실패 등(Ktor 멀티플랫폼 IOException — §3-4·DR-2)
            throw ClaudeException.Network(e)
        }
        // status를 body 디코드 전에 먼저 검사한다(DR-1). 429는 한도, 그 밖 non-2xx(529 Overloaded·
        // 프록시(Cloudflare) 앞단 502/503/524 HTML 오류페이지)는 ClaudeResponse가 아니므로 디코드에
        // 태우지 않고 매핑한다 — 디코드에 태우면 try 밖 SerializationException으로 uncaught 크래시.
        if (res.status.value == 429) throw ClaudeException.DailyLimitExceeded
        if (!res.status.isSuccess()) throw ClaudeException.InvalidResponse   // non-2xx non-429 → 매핑(크래시 금지)
        return try {
            res.body<ClaudeResponse>().toTermResult()       // tool_use 3분기
        } catch (e: HttpRequestTimeoutException) {           // 바디 수신 중 타임아웃
            throw ClaudeException.Timeout
        } catch (e: IOException) {                            // 바디 수신 중 연결끊김
            throw ClaudeException.Network(e)
        } catch (e: SerializationException) {                // 2xx인데 비JSON/스키마 불일치 바디
            throw ClaudeException.InvalidResponse
        }
    }
}
```

**요청 본문**(Anthropic Messages API 형태 — 프록시가 그대로 forward):
- `model`(`Constants.claudeModel` — 착수 시 docs.anthropic.com에서 최신 ID 확인)·`max_tokens`(4096)·`thinking`(enabled)·`system`(§3-3 프롬프트)·`tools`(§3-3 3개)·`tool_choice`(auto)·`messages`(user 메시지에 **원본 `keyword`** — 대소문자 보존, iOS 검증본 계승).
- **키잉 vs 프롬프트 입력 분리**: `buildClaudeRequest`는 **원본 `keyword`를 대소문자 보존해** user 메시지에 싣는다(iOS 검증본 계승) — lowercase하면 `NaN`→`nan`·`Go`→`go`·`REST`→`rest`·`C`→`c`처럼 대소문자 유의미 용어가 뭉개져 어원이 조용히 오답이 된다. 캐시 파편화 방지(`React`/`react` 접기)는 **AI 질의 content가 아니라 캐시 키**에서 한다(§3-1 `normalizeKeyword` = `trim().lowercase()`를 서버가 본문에서 파생하거나 §7-3 `X-Term-Key` 헤더로). 키잉과 프롬프트 입력은 다른 요구라 한 함수 소비로 합치지 않는다.

**응답 파싱 — `tool_use` 3분기(설계 불변식):** `content` 배열에서 첫 `tool_use` 블록을 찾아 도구 이름으로 분기.

- **content-block DTO는 `tool_use` 아닌 블록에 관용적이어야 한다(네이티브 디코드 필수 조건).** 요청이 `thinking`(enabled)이므로 **실 Anthropic 응답의 `content` 배열은 항상 `thinking`(및 흔히 `text`) 블록이 `tool_use` 앞에 온다** — `tool_use` 단일 블록 응답은 프로덕션에서 오지 않는다. 따라서 content 원소 DTO는 **비다형 flat DTO**(`type: String` + `tool_use` 필드만 nullable, thinking/text 등의 필드는 미선언 → `ignoreUnknownKeys`가 무시)로 두거나, 다형(`@JsonClassDiscriminator("type")` sealed)으로 갈 경우 **미지 변이를 흡수하는 polymorphic-default(catch-all)를 필수로 둔다** — 개별 변이(thinking/text/redacted_thinking …)를 열거 등록만 하는 것은 금지다(default 없는 다형 금지). 개별 등록은 catch-all default 위에 선택적으로만 얹는다. ⚠️ 이 한정(default 필수)이 없으면: 구현이 다형+개별등록을 택해 자주 오는 `thinking`/`text`만 등록하고 **드문 `redacted_thinking`(thinking이 플래그될 때만 프로덕션에 드물게 등장) 변이를 누락**해도, 그 변이를 태우는 canned 응답이 없어 4축 green이 전부 통과한다 — 그런데 실기기 첫 호출에서 미지의 discriminator *값*이 나타나면 `ignoreUnknownKeys`가 구제하지 못하고(그건 *키* 무시일 뿐) 네이티브에서 디코드 예외를 던진다 — DR-1 fix로 이제 그 예외는 §3-2 `generate`의 body 디코드 `try`가 `SerializationException`으로 잡아 크래시 대신 `InvalidResponse`로 매핑되나, **자주 오는 프로덕션 shape(정상 `Found`)가 미등록 변이 하나 때문에 통째로 `InvalidResponse` 오류로 격하**된다(정상 응답의 거짓 오류화). catch-all default를 필수로 못박으면 미등록 변이가 default로 흡수돼 이 격하 경로가 데이터 변이(새 canned 픽스처)에 의존하지 않고 닫힌다. **비다형 flat DTO를 우대**(단언·경로 최소).

| 도구 | 결과 |
|---|---|
| `return_term_entry` | `input` → `TermEntry` 디코드 → `Found(entry, source = AI)` |
| `return_not_dev_term` | `NotDevTerm` |
| `return_possible_typo` | `input.suggestion` → `PossibleTypo(suggestion)` |

- **`tool_use` 블록 없음/알 수 없는 도구명/`input` 디코드 실패** → `ClaudeException.InvalidResponse`.
- **non-2xx(429 제외)·비JSON/스키마 불일치 바디** → `ClaudeException.InvalidResponse`(DR-1). 새 오류 변이를 더하지 않고 기존 `InvalidResponse`로 접는다(단언·경로 최소) — 5xx/HTML 오류페이지는 클라 관점에서 "쓸 수 없는 응답"이라 이 버킷이 정합이고, 재시도 정책은 downstream(M4/M5) 소관이라 M3는 오류 세분을 얹지 않는다.
- `not_dev_term`/`possible_typo`는 **예외가 아니라 `TermResult`로** 반환(정상 분기, spec 2-2).
- **category는 pass-through**(M1·M2 상속): `return_term_entry.input`의 `category`가 6집합 밖이라도 M3는 거부·정규화하지 않고 그대로 `TermEntry`로 디코드한다. **AI 응답 경로의 category 정규화/clamp는 M4**(오케스트레이터가 upsert 직전, INV-13이 서버에서 하는 것과 대칭 지점)로 이월(§4·§7-4).

**오류 타입:**
```kotlin
sealed class ClaudeException : Exception() {
    data object DailyLimitExceeded : ClaudeException()   // HTTP 429
    data object Timeout : ClaudeException()
    data class Network(val cause: Throwable) : ClaudeException()
    data object InvalidResponse : ClaudeException()      // tool_use 없음/디코드 실패·non-2xx(429 제외)·비JSON 바디(DR-1)
}
```

### 3-3. 시스템 프롬프트·도구 스키마 (`commonMain/data/remote/`)

- **위치는 `commonMain`**(ADR-0006 §6·ADR-0004 계승) — 두 클라이언트(iOS·CMP)가 같은 프록시를 공유하며 프롬프트/도구 스키마를 클라가 소유한다(서버 이전은 유보).
- **iOS 검증본을 그대로 계승**한다(리터럴 포팅이되 프롬프트 텍스트는 검증된 자산이라 변형 금지 — ADR-0002의 "우회 패턴 삭제"는 코드 관용에 적용, 프롬프트 원문은 계승 대상). 원문 출처는 iOS 구현(`~/dev-etymology`)이며, **착수 시 그 정본에서 프롬프트·3도구 JSON 스키마를 실측 복사**한다(버전 하드코딩 금지 규율과 동형 — stale 사본 금지). `docs/ai-quality/` 정본화는 ROADMAP Later 백로그.
- 3도구 스키마: `return_term_entry`(입력 = `TermEntry` 6필드 shape)·`return_not_dev_term`(입력 없음/빈)·`return_possible_typo`(입력 = `{suggestion: String}`).

### 3-4. HttpClient·엔진 `expect`/`actual` + 공유 `Json` 정책

**엔진만 플랫폼별**(M2 드라이버와 동형 — 교체 비용 최소화):
```kotlin
// commonMain — 클라이언트 설정(ContentNegotiation·타임아웃)은 공통, 엔진만 주입
expect fun httpEngine(): HttpClientEngine
fun createHttpClient(json: Json): HttpClient = HttpClient(httpEngine()) {
    install(ContentNegotiation) { json(json) }
    install(HttpTimeout) { requestTimeoutMillis = Constants.apiTimeoutMs }
}
// androidMain: actual fun httpEngine() = OkHttp.create() (또는 Android 엔진)
// iosMain:     actual fun httpEngine() = Darwin.create()
```
- **actual의 검증은 컴파일/링크**(M1·M2 규율): iOS actual은 `linkDebugFrameworkIosSimulatorArm64`, Android actual은 `assembleDebug`. 단위테스트는 이 actual 엔진을 실행하지 않고 **`MockEngine`**(common)을 주입한다(§6-A). 실엔진(Darwin/OkHttp) 연결실패·타임아웃 예외가 `ClaudeApi.generate`의 `catch` 분기(Network/Timeout)로 실제 발화하는지의 **분기 매핑 검증**은 §5 링크 green + M8 실기기가 커버(§4 이월, DR-2).
- **`generate`가 `catch`할 `IOException` 타입 확정(DR-2)**: `commonMain`의 `catch (e: IOException)`는 **`java.io.IOException`이 아니라 Ktor의 멀티플랫폼 `IOException`**이다 — `java.io.IOException` import는 iOS 네이티브에서 미컴파일(프로파일 D-2 KMP 최대 실패원)이라 4축이 loud로 잡는다. 착수 시 확정할 Ktor 버전의 멀티플랫폼 타입을 명시 import한다(**Ktor 3.x = `kotlinx.io.IOException`, 2.x = `io.ktor.utils.io.errors.IOException`** — §5에서 Ktor 버전 실빌드 확정과 동시에 고정). **단 `MockEngine` 오라클은 이 `catch`가 실엔진 예외타입을 실제로 서브타입하는지 검증하지 못한다**(무측정 클래스) — MockEngine은 예외를 임의 주입할 뿐 실 소켓 예외 타입을 재현하지 않으므로, 이 매핑 정합은 위 M8 이월로 정직히 넘긴다.

**공유 `Json` wire 정책 확정(M1 §7-3 이월 결착):**
```kotlin
// commonMain/data/ — 번들 로더·ClaudeApi 응답 디코드 공용
val AppJson = Json {
    ignoreUnknownKeys = true   // 진화하는 서버/Anthropic 응답·번들 필드 추가에 하위호환(디코드 회복성)
}
```
- `ignoreUnknownKeys = true`가 핵심 정책이다: 서버 read-through 응답·Anthropic content 블록·미래 번들 필드가 늘어도 디코드가 깨지지 않는다. **주의(INV-A와의 상호작용)**: `ignoreUnknownKeys`는 *모르는 키를 무시*할 뿐 **알려진 `aliases` 키가 다른 이름/생략되면 여전히 default `emptyList()`로 조용히 떨어진다**(M1 §3-1 wire 계약) — 이 silent 소실을 막는 것이 §6-B 로더 실측이다(성공 디코드는 무효 오라클).
- M2 매퍼의 `aliasesJson`(저장-내부 List<String> 인코딩)은 **이 wire 정책과 독립**이며 M2대로 유지(M2 §3-4). M3는 wire 디코드 경로만 확정한다.

## 4. 설계 불변식 (이 슬라이스가 반드시 지킬 것)

- **INV-A 로더측 실측 (DR-1 바인딩 상속 — M3 필수 실측)**: **번들 로더(`BundleDbSource`)의 실제 파싱·인덱스·매칭 경로**가 aliases *내용*을 보존하고 **alias로 검색이 성립**함을 실 배포 `terms.json`으로 실측한다(§6-B). **이것이 M1 §7-4 carry-forward의 폐쇄 지점**이다 — M1 fixture(`TermsBundleFixtureTest`)는 실 파일을 *디코드*해 aliases 내용을 단언했으나(리스트 수준), **로더의 인덱스가 aliases를 검색 집합에 편입하는지**는 무측정이었다. `search(alias)`가 항목을 찾지 못하면(인덱스를 keyword로만 구성) 디코드는 여전히 aliases를 보이나 검색은 silent miss — 이 손실은 §6-B의 **alias 검색 오라클**만 잡는다. **성공 디코드·리스트 단언은 오라클로 삼지 않는다**(M1 §7-4 무효 오라클 규정 계승): 반드시 실 번들을 로더에 태워 (a) `search("aa-tree")`의 aliases 내용·category, (b) `search("Arne Andersson tree")`(alias) → `aa-tree` 매칭을 단언한다.
- **INV-1·2 read-through 투명성**: 클라 `ClaudeApi.generate` 호출 형태는 서버 D1 캐시 히트/미스와 무관하게 동일하다(§0). 클라는 캐시 계층을 알지 못하며 `Source`를 늘리지 않는다(`BUNDLE` vs 네트워크→`AI`).
- **category pass-through 유지 (M1·M2 상속)**: `ClaudeApi`는 `return_term_entry`의 6집합 밖 category도 거부·정규화 없이 디코드한다(§3-2). **AI 응답 category 정규화/clamp는 M4**로 이월(§7-4) — M3는 순수 파싱 계층이라 정책을 얹지 않는다(M2 매퍼가 pass-through였던 것과 동형).
- **INV-13(서버 정규화-후-캐시쓰기)은 M3(클라) 소관이 아니다 — 서버 트랙으로 이관**: ROADMAP M3 ⚠️의 "서버 read-through category 소유"는 §0 스코핑상 클라 M3 밖이다. 서버가 정규화 이전 원응답을 캐시-히트로 되돌려 클라 정규화를 우회하지 않도록 하는 **정규화-후-캐시쓰기 순서 고정**(validator write-게이트 지점의 category clamp)은 **서버 트랙 DoD**로 상속한다(캐시 트랙 INV-13, cache M1 write-back·M3 write-게이트). 클라측 방어(M4의 수신 category 정규화)와 서버측 소유(INV-13)는 상보적이며, M3는 어느 쪽도 얹지 않고 두 이월을 명시한다(§7-4·§7-6).
- **ADR-0002 코틀린 관용**: iOS(SwiftData/URLSession) 우회 패턴을 옮기지 않는다. 순수 `suspend` + Ktor 관용, 결과는 예외 아닌 `TermResult` 분기(정상 3분기)와 `ClaudeException`(오류) 분리.

## 5. 완료 조건 (DoD) — 하네스 수렴 오라클

- `BundleDbSource`·`ClaudeApi`·프롬프트/도구·HttpClient 엔진 `expect`/`actual`·`AppJson`이 **Android·iOS 양쪽에서 컴파일**된다: `:shared:testDebugUnitTest` + `:androidApp:assembleDebug` + `:shared:linkDebugFrameworkIosSimulatorArm64` green(M0~M2의 3축).
- **⊕ 4번째 축 — 네이티브 실행(M2 B1 선례 계승, 핸드오프 §2 선제 폐쇄)**: `:shared:iosSimulatorArm64Test` green. §6-A(엔진 무관 `commonTest`: `BundleDbSource` 매칭 + `ClaudeApi`×`MockEngine` 3분기)가 **네이티브 타깃에서 실행**되어 (a) Native `kotlinx.serialization`의 **Anthropic 응답 shape 디코드**(**프로덕션 shape의 중첩 content 배열 = `thinking`(및 `text`) 선행 블록 + `tool_use`**·`tool_use.input`→`TermEntry`)와 (b) Native **문자열 정규화**(공유 `normalizeKeyword` = `trim().lowercase()` 매칭·prefix)를 **링크가 아니라 실행으로** 실측한다. (a)의 측정 타당성은 §6-A canned 응답이 `tool_use` 앞에 선행 thinking/text 블록을 실은 실 shape여야 성립한다 — tool_use 단일 블록이면 선행 블록 디코드를 안 태워 무효 오라클(§6-A 주석). 이로써 네이티브 실행 갭(M1·M2에서 비준 blocker였던 것)을 **M3는 선제 폐쇄**한다.
  - **네이티브 잔여 이월(명시)**: `MockEngine`은 엔진을 대체하므로 **실엔진(Darwin) 연결실패·타임아웃 예외의 `ClaudeException` 분기(Network/Timeout) 매핑 검증**(DR-2 — 진짜 무측정 클래스는 '소켓 IO'가 아니라 '실엔진 예외타입→분기 매핑'이다: MockEngine은 실 소켓 예외 타입을 재현하지 않아 `catch` 서브타입 정합을 원리상 검증 못 함)과 **`Res.readBytes` 바이트 획득 런타임**은 §6-A로 무측정 — 컴파일/링크 + **M8 통합/실기기 DoD**로 상속(M2가 `NativeSqliteDriver` 실행을 M8로 이월한 것과 동형).
- 아래 §6 테스트가 전부 통과. **§6-B(실 번들 로더 INV-A 실측)는 DoD 필수 항목** — 이것이 없으면 DR-1 로더측이 무측정으로 남는다.
- **버전 정렬을 사실로 확인(load-bearing, 핸드오프 §2)**: Ktor 좌표(엔진·`content-negotiation`·`serialization-kotlinx-json`)가 **Kotlin 2.3.21 × kotlinx.serialization 1.9.0에서 klib 소비된다는 것을 실빌드로 확인**한다 — 특히 `linkDebugFrameworkIosSimulatorArm64`·`iosSimulatorArm64Test`가 Ktor 네이티브(Darwin/MockEngine) klib를 소비해 green이어야 한다. **Ktor 버전은 하드코딩하지 말고 착수 시 Maven에서 2.3.21 호환 최신을 실빌드로 확정**(M1 serialization·M2 SQLDelight와 동일 규율). 버전 카탈로그 헤더의 '빌드 확인' 표기를 이 확인의 대체물로 삼지 말 것.

## 6. 테스트 — 함수명 `test_[대상]_[조건]_[기대]`

### 6-A. 엔진 무관 순수 실측 (`commonTest/`) — **JVM+네이티브 양쪽 실행(4축)**

**BundleDbSource 매칭**(인메모리 `InMemoryBundleDbSource(fixtureEntries)` — 실 파일 IO 없음, §3-1 분리):
- `test_search_정확매칭_반환` — keyword 완전 일치 → 해당 `TermEntry`.
- `test_search_alias매칭_반환` — 입력이 어떤 항목의 `aliases` 중 하나와 일치 → 그 항목(alias가 검색 집합에 편입됐음을 인메모리 수준에서 확인 — §6-B가 실 번들로 재확인).
- `test_search_대소문자무시_반환` — `"REACT"`/`"React"`/`"react"` 동일 매칭(Native `lowercase` 실측).
- `test_search_미발견_null` — 어떤 keyword·alias와도 불일치 → `null`.
- `test_search_정규화충돌_리스트앞선엔트리_반환` — `fixtureEntries`에 **정규화 키가 충돌하는 최소 쌍**(두 엔트리가 같은 정규화 alias를 공유, 예: 엔트리A `aliases`와 엔트리B `keyword`가 `normalizeKeyword` 후 동일)을 리스트 순서로 넣고, 그 충돌 키로 `search`하면 **리스트 앞선(먼저 삽입된) 엔트리**를 반환함을 단언 — §3-1 first-wins(putIfAbsent) 결정적-반환 계약을 **실행 오라클로 고정**한다(구현이 last-wins 덮어쓰기로 인덱스를 지으면 이 테스트만 실패해 계약 이탈을 잡는다). 네이티브 실행 축에서도 인덱스 구성 순서가 결정적임을 재확인.
- `test_search_빈입력_null` — `""`/공백 → `null`(네트워크·정책 판단 없음, 순수).
- `test_autocomplete_prefix매칭_목록` — `keyword.startsWith(prefix)` 항목들.
- `test_autocomplete_빈prefix_빈목록` — `""`/공백 → `emptyList()`.

**ClaudeApi × MockEngine**(Ktor `MockEngine`으로 canned 응답 주입 — 네트워크 무의존, Native Ktor 파이프라인+직렬화 실측):

> **canned 응답은 실 프로덕션 shape로 고정한다(무효 오라클 방지).** 요청이 `thinking`(enabled)이라 실 응답은 `tool_use` 앞에 `thinking`(가능하면 `text`도) 블록이 온다 — 3분기 canned 응답(`return_term_entry`/`return_not_dev_term`/`return_possible_typo`)은 모두 **`tool_use` 앞에 최소 1개 `thinking` 블록(및 `text`)을 선행**시켜, §5가 주장하는 '중첩 content 블록 네이티브 디코드'가 **선행 thinking 블록을 실제로 태우는지** 실측하게 한다. `tool_use` 단일 블록 canned는 프로덕션 shape를 안 태워 성공 디코드가 증명 대상을 비껴가는 무효 오라클이 된다.

- `test_generate_정상toolUse_Found` — canned content = `[thinking 블록, (text 블록), return_term_entry tool_use]`(프로덕션 shape) → 첫 `tool_use`를 찾아 `Found(entry, AI)`, entry의 `aliases`·`category`·버전필드(있으면) 왕복 보존. **선행 thinking/text 블록이 있어도** DTO가 관용적으로 디코드하고(§3-2 flat DTO) `tool_use`를 정확히 집어냄을 네이티브 실행으로 단언.
- `test_generate_notDevTerm_NotDevTerm` — `return_not_dev_term` → `NotDevTerm`(예외 아님).
- `test_generate_possibleTypo_PossibleTypo` — `return_possible_typo`(`input.suggestion`) → `PossibleTypo(suggestion)`.
- `test_generate_429_DailyLimitExceeded` — MockEngine이 HTTP 429 → `ClaudeException.DailyLimitExceeded`.
- `test_generate_5xx_InvalidResponse` — MockEngine이 HTTP 503(또는 529 Overloaded) + **비JSON HTML 오류 바디**(프록시 앞단 오류페이지 shape) → `ClaudeException.InvalidResponse`. body 디코드 *전* status 매핑이라 크래시가 아님을 네이티브 실행으로 실측(DR-1 — status 검사가 try 밖 SerializationException 크래시를 선제 차단).
- `test_generate_비JSON바디_InvalidResponse` — MockEngine이 HTTP 200 + **비JSON/스키마 불일치 바디** → `ClaudeException.InvalidResponse`. 2xx인데 `ClaudeResponse`로 디코드 불가한 경우 body 디코드 `try`가 `SerializationException`을 잡아 매핑함을 실측(무매핑 크래시 금지).
- `test_generate_toolUse없음_InvalidResponse` — `tool_use` 블록 없는 응답 → `ClaudeException.InvalidResponse`.
- `test_generate_집합밖category_passThrough` — `return_term_entry.input.category="네트웤"`(오타)가 **거부·정규화 없이** `TermEntry`로 디코드(정규화는 M4, §4). 
- `test_generate_XDeviceId헤더_전송` — MockEngine이 수신 요청의 `X-Device-Id` 헤더 = 주입된 deviceId 값임을 단언(프록시 한도 키잉 계약).
- (타임아웃) `test_generate_타임아웃_Timeout` — MockEngine이 `HttpRequestTimeoutException` 유발(또는 지연) → `ClaudeException.Timeout`. *네이티브에서 타임아웃 유발이 불안정하면 §7-5대로 이 케이스만 JVM(`androidUnitTest`)로 격리하고 나머지 3분기·429는 commonTest 유지 — 비준 판정.*

### 6-B. 실 번들 로더 INV-A 실측 (`androidUnitTest/`, `File` 읽기) — **DR-1 로더측 폐쇄, 필수**

> M1 `TermsBundleFixtureTest`와 동일 배치 이유: `Res.readBytes`(compose-resources 런타임)는 plain JVM 단위테스트에서 미실행(`MissingResourceException`)이라, `:shared:testDebugUnitTest`(=androidUnitTest 실행)에서 실 배포 `terms.json`을 `java.io.File("src/commonMain/composeResources/files/terms.json")`로 직접 읽어 **`InMemoryBundleDbSource`의 파서·인덱스·매칭 경로에 태운다**(classpath 사본 아닌 실 배포 파일).

- `test_실번들로더_aliases내용보존_및_alias검색` — 실 `terms.json`(650)을 `AppJson`으로 디코드해 `InMemoryBundleDbSource`를 만든 뒤:
  - (a) `search("aa-tree")!!` 의 `aliases == listOf("AA 트리", "Arne Andersson tree")`(순서 포함)·`category == "자료구조"`.
  - (b) **`search("Arne Andersson tree")?.keyword == "aa-tree"`** — alias가 로더 인덱스의 검색 집합에 편입됐음을 실 번들로 실측(**이 alias 검색이 M1 fixture 대비 증분 폐쇄점** — 인덱스를 keyword로만 구성하면 여기서 silent miss).
  - (c) 항목 수 650(번들 무결성) + aliases 보유 항목이 다수 존재(전부 empty면 wire 키 계약 위반 신호, M1 fixture 계승).
- **성공 디코드·리스트 존재를 오라클로 삼지 않는다**(§4·M1 §7-4): 반드시 (a) aliases *내용*과 (b) *alias 검색 성립*을 단언한다.

## 7. 열린 질문 (비준이 판정할 항목)

1. **`BundleDbSource` 로드 경로 분리 (제안: 파싱·인덱스와 `Res.readBytes` 획득 분리)** — `InMemoryBundleDbSource(entries)` + `suspend loadBundleDbSource(json)`로 나눠, INV-A 로더 실측(§6-B)이 실 배포 `terms.json`을 파서·인덱스·매칭에 태우게 한다(`Res.readBytes`는 JVM 단위테스트 미실행 — M1 fixture 실측). **대안**: 로더를 단일 suspend 함수로 두고 `Res` 로드까지 한 덩어리 → INV-A 실측이 `Res` 런타임에 묶여 JVM 단위테스트에서 불가(계측 공백). — 제안: 분리. 비준이 §6-B 배치(androidUnitTest File)와 잔여(`Res.readBytes` 런타임 M8 이월)가 DR-1 로더측을 충분히 폐쇄하는지 판정.

2. **`ClaudeApi` 테스트 대체 지점 — `MockEngine`(제안) vs 순수 파서 분리** — 제안은 `HttpClient(MockEngine)`으로 `generate`를 end-to-end 실측해 **Native Ktor 파이프라인(ContentNegotiation·body 디코드)+429 처리**까지 네이티브 실행 축에 태운다. **대안**: `toTermResult()`를 순수 함수로 떼어 Ktor 없이 3분기만 테스트(더 얇으나 파이프라인·429·헤더 무측정). — 제안: MockEngine(파이프라인 포함). 비준이 `MockEngine`이 실 엔진(Darwin/OkHttp) IO를 대체함을 근거로 §5 잔여 이월(M8 실기기)이 정직한지 판정.

3. **서버 캐시 키잉 계약 seam(클라↔서버 트랙 경계)** — read-through 서버가 D1을 term key로 조회하려면 요청에서 키를 추출해야 한다. **AI user 메시지 본문은 원본 keyword(대소문자 보존, §3-2)라 그대로 캐시 키로 쓰면 `React`/`react`가 파편화**하므로, 캐시 키는 정규화값(`normalizeKeyword` = `trim().lowercase()`)이어야 한다. 이를 (i) 서버가 본문 keyword를 받아 **서버측에서 정규화**해 파생할지, (ii) 클라가 명시 헤더(`X-Term-Key: <정규화 keyword>`)로 실을지. **정규화 함수 자체는 M3 확정**(§3-1 `normalizeKeyword` — 로컬 매칭 정본)이나 **AI 질의 content엔 적용하지 않는다**(키잉 전용). 두 옵션 모두 서버 트랙과 조율해 나중에 무리팩토링으로 확정 가능(투명성상 클라 재작성 없음)하므로 이 슬라이스에서 헤더를 강제하지 않는다. — 비준이 헤더(ii)를 M3에서 확정할지, 서버 트랙 착수 시로 미룰지 판정. 미룰 시 §8에 명시 이월.

4. **AI 응답 category 정규화 지점 — M3(파서) vs M4(오케스트레이터)** — M1 §7-2는 "AI 응답 경로(M3·M4)가 집합 밖 category를 강제/정규화"라 했다. **제안**: M3 `ClaudeApi`는 pass-through(순수 파서 유지, M2 매퍼와 동형)하고, **정규화/clamp는 M4**가 upsert 직전(서버 INV-13이 write-게이트에서 하는 것과 대칭 클라 지점)에 수행한다. — 비준이 이 배치(M3 pass-through + M4 정규화)가 downstream 버킷팅 누락을 막는 데 충분한지, 아니면 M3 파서가 즉시 clamp해야 하는지 판정. **미월 시 명시**: M3는 category를 pass-through로 두고, 클라측 정규화 상속을 M4 DoD로, 서버측 소유(INV-13)를 서버 트랙으로 각각 이월(§4).

5. **타임아웃 케이스 배치 — commonTest(네이티브 포함) vs androidUnitTest** — `test_generate_타임아웃_Timeout`은 `MockEngine`에서 지연·예외 유발이 네이티브 런루프에서 불안정할 수 있다. **제안**: 3분기·429·헤더·pass-through는 commonTest(4축), 타임아웃 유발 1건만 불안정하면 `androidUnitTest`(JVM)로 격리. — 비준이 타임아웃의 네이티브 실측 필요성(§5 네이티브 실행 갭 폐쇄 취지)과 안정성을 저울질해 판정. 격리 시 네이티브 타임아웃 경로는 M8 실기기로 이월.

6. **서버 트랙 분리의 정합성(§0 판정 재확인)** — ROADMAP·핸드오프의 "M3 = 클라+서버"를 클라측으로 좁히고 서버 전체(+INV-13)를 서버 트랙으로 이관하는 §0 판정이, ADR-0006 빌트인 방침("출시 후 없음·리팩토링 0")과 배치되지 않는지 — 즉 **서버 트랙을 M4 이전에 세우지 않아도** 클라 M3·M4가 read-through 계약대로 서고 나중에 서버가 붙을 때 클라 무리팩토링임을 비준이 확인. (투명성상 성립하나 비준이 명시 판정.)

## 8. 안전·규율

- 마일스톤 경계 **사람 비준** 없이 다음(M4)으로 넘어가지 않는다. **하네스는 push·머지·`-draft` 제거를 하지 않는다.**
- **M1→M3 바인딩 폐쇄 확인 — INV-A 로더측**: M1 §7-4가 M3로 이월한 **번들 로더 실제 로드 경로의 aliases 보존**은 **이 슬라이스 §6-B가 폐쇄한다**(DoD 필수, alias 검색 오라클). 비준자는 §6-B가 (a) aliases 내용 (b) alias 검색 성립을 실 번들로 단언하는지, 성공 디코드를 오라클로 삼지 않는지 확인한다.
- **서버 트랙 이월 명시 — INV-13**: 서버 read-through category 소유(정규화-후-캐시쓰기)는 §0상 클라 M3 밖이며 **서버 트랙 DoD**로 상속된다(§4·§7-6). 비준자는 이 이월이 명시됐는지, 클라측 category 정규화(M4)와 상보적임을 확인한다.
- **브랜치 보존(defer+stacked)**: 완료 마일스톤 브랜치 삭제·로컬머지 금지(소급 PR 소스). 지우자는 지시·충동이 있어도 재확인 먼저.
- **push 금지 · GitHub 원격 생성 금지.** 로컬 커밋만.
- 네이밍은 젠더중립/여성형 기본.
- 진행 상태 정본은 ROADMAP(디스크). 이 슬라이스는 시간 안 타는 명세만.

## Open Questions

> 비준 종료 시점의 **명시 이월** 자리. 미탐색이지만 알려진 클래스를 암묵적으로 넘기지 않고 여기에 적어서 넘긴다. (비준 착수 전 — 현재는 비어 있으며, 적대 비준·사람 게이트가 채운다.)

- [ ] (비준 대기) §7 열린 질문 1~6의 판정.
- [ ] (미봉·이월) **BundleDbSource alias 정규화-유일성 + 가려진 엔트리 발견성** — `normalizeKeyword`(=`trim().lowercase()`)로 접힌 alias가 서로 다른 엔트리에 중복되면(실 번들 3건: `집계`→{aggregate, aggregation}·`분기`→{branch, fork}·`샤딩`→{shard, sharding}) `search`가 번들 순서 첫 매칭만 반환하고 나머지를 가린다(`autocomplete`은 alias 비대상이라 대체 발견 경로 없음). M3는 §3-1대로 **결정적 반환만** 보증(first-wins). 해소책 택일 — (i) 번들 린트로 중복 정규화 alias(keyword 포함)를 빌드 게이트에서 거부(데이터 de-dup 강제), (ii) alias-aware autocomplete로 가려진 엔트리 발견성 회복, (iii) 결정적-반환 유지(현행) — 는 비준/후속(데이터·M4) 트랙 판정.
- [ ] (선상속·서버 트랙) INV-13(정규화-후-캐시쓰기)·서버 `devetym-proxy` 전체 — §0 스코핑상 클라 M3 밖, 서버 트랙 DoD로 상속(§4·§7-6).
- [ ] (선상속·M4) AI 응답 category 정규화(§7-4)·fetch 3단 오케스트레이션·로컬 AI 캐시 조회·upsert 정책·pinning 스킵 — M4 DoD로 상속.
- [ ] (선상속·M8) 실엔진(Darwin/OkHttp) 연결실패·타임아웃 예외의 `ClaudeException` 분기(Network/Timeout) 매핑 검증(DR-2 — MockEngine은 실 소켓 예외타입 미재현이라 이 매핑 원리상 무측정)·`Res.readBytes` 바이트 획득 런타임 — §6-A `MockEngine`·§6-B `File`로 무측정, M8 통합/실기기 DoD로 상속(§5).
- [ ] (비준 종료·ESCALATE·2026-07-05) **잔여 Blocker AD-1 — 2xx 비JSON/빈바디의 `NoTransformationFoundException` 미포착** — 적대 비준이 cap(6) 도달 후에도 닫지 못한 Blocker를 암묵적으로 넘기지 않고 명시 이월한다. Ktor `ContentNegotiation`의 `res.body<ClaudeResponse>()`는 응답 `Content-Type`이 `application/json`이 아니거나(예: Cloudflare 200 챌린지/인터스티셜 HTML, 프록시 앞단 `text/plain` 빈 200 바디) 디코드 실패를 `SerializationException`이 아니라 `NoTransformationFoundException`(`UnsupportedOperationException` 계열)으로 던진다 — §3-2 body-디코드 `try`의 `catch (e: SerializationException)`는 이를 못 잡아 try 밖 uncaught 크래시(DR-1이 막으려던 바로 그 크래시)로 샌다. §6-A `test_generate_비JSON바디`가 `application/json`+garbage 바디만 태워 `content-type` 불일치/빈바디 경로를 오라클로 안 덮으므로 4축 green이 조용히 통과하는 거짓 수렴이다. **해소책**: (i) body 디코드 `try`의 catch를 `SerializationException`에서 상위(예: `Exception`/`Throwable` 또는 `NoTransformationFoundException` 포함)로 넓혀 `InvalidResponse`로 매핑, (ii) `res.body` 호출 전 응답 `Content-Type`을 검사해 non-JSON을 선제 `InvalidResponse`로 접기, (iii) §6-A에 `Content-Type` 불일치·빈 200 바디 canned 2건 추가. **사람 게이트(`-draft` 제거 승인) 판정 대상.**
- [ ] (비준 종료·ESCALATE·2026-07-05) **비준 미완 — 사람 게이트로 인계** — 적대 비준 6라운드(cap=6) 종료, 전이 라운드 없음, 잔여 Caution 0. ESCALATE 사유=cap 도달·Blocker 1 잔존(AD-1, 위 항목). verdict 로그 정본: `~/dev/agent-harnesses/runs/m3-networking-draft-verdict.json`. **다음 = 사람 게이트: 위 AD-1 해소책 판정 후 `-draft` 제거 승인 여부 결정.** 하네스는 `-draft` 제거·머지·push 하지 않는다.
