# M4 슬라이스 (draft) — Repository 오케스트레이터

> **성격: 비준 하네스 입력용 spec 슬라이스(`-draft.md`).** 단일 [`spec.md`](spec.md) Phase 2의 오케스트레이터(2-3·2-4) 부분을 마일스톤 경계로 떼어낸 것. 진행 상태 정본은 [`../../ROADMAP.md`](../../ROADMAP.md), 상위 설계는 [`../architecture.md`](../architecture.md), 결정 근거는 [`../adr/`](../adr/).
>
> 이 문서는 **자율 구현 전 적대적 비준(완결성·모호성·테스트 가능성·정합성)의 대상**이다. 아래 §7 열린 질문은 비준이 판정할 항목이다.

- **마일스톤**: M4 · Repository 오케스트레이터
- **브랜치**: `feat/m4-repository` (=`feat/m3-networking` 위에 **스택** 분기 — main엔 아직 M1~M3가 없으므로 M4가 그 코드를 상속해야 빌드됨. ROADMAP 「브랜치·공개 전략」)
- **참조**: spec [2-3·2-4](spec.md), [ADR-0006](../adr/0006-server-cache-boundary.md)(3계층 read-through·pinning), [ADR-0002](../adr/0002-code-idiom-principle.md)(반응형으로 수동 재조회 제거), [M2 슬라이스](m2-local-db-draft.md)(§3-2 쿼리·§3-4 매퍼·DR-M2-2 createdAt 보존·DR-M2-3 schemaVersion Int범위), [M3 슬라이스](m3-networking-draft.md)(§3-1 `BundleDbSource`·§3-2 `ClaudeApi`·§4·§7-4 AI category 정규화 이월), 캐시 트랙 [INV-1·2·6·9·11·13](../cache-delivery-milestones.md)

---

## 1. 목표 (이 슬라이스가 내는 것)

`commonMain`에 **3계층 read-through 오케스트레이터** `TermRepository`를 세운다. M3가 준 두 데이터 소스(`BundleDbSource`·`ClaudeApi`)와 M2가 준 로컬 저장(쿼리·매퍼)을 **fetch 3단 순서**(로컬 번들 → 로컬 AI 캐시 → 네트워크[서버 D1 캐시→API])로 엮고, 결과를 lazy 저장(upsert)하며, 목록을 **반응형 `Flow`**로 노출한다(ADR-0002 — 수동 재조회 없음). 이 계층이 M5 ViewModel이 의존할 **유일한 인터페이스**다.

M4는 M2·M3가 준 메커니즘 위에 **정책**을 얹는다: fetch 순서·pinning 스킵(INV-6)·upsert 보존 목록(createdAt·isBookmarked·source·seenAt)·AI 응답 category 정규화. UI·ViewModel·Koin 전체 조립·셸은 이 슬라이스 밖이다.

## 2. 스코프

**IN (M4):**
- **`TermRepository` 인터페이스**(`commonMain/repository/`) — spec 2-3의 8메서드.
- **`TermRepositoryImpl`** — fetch 3단 오케스트레이션·refresh·toggleBookmark·히스토리 조작. 협력자(`BundleDbSource`·`ClaudeApi`·로컬 저장·`AnalyticsService`·시계)를 생성자 주입.
- **`LocalTermStore` 추상화**(`commonMain/data/local/`) — M2 SQLDelight 쿼리(`selectTermByKeyword`·`insertOrReplaceTerm`·`bookmarked`·`recent`·`insertOrReplaceSearch`·`deleteSearch`·`clearAllSearch`)를 감싸는 인터페이스 + `SqlDelightTermStore`(생성 `DevEtymDatabase` 위임) 구현. Fake로 대체 가능케 해 오케스트레이션 테스트가 드라이버 없이 성립(§6·§7-1).
- **`AnalyticsService` 인터페이스 + `PlaceholderAnalyticsService`**(`commonMain/analytics/`) — 결과 유형·오류 로깅용 인터페이스(spec 2-3). **M4 fetch는 오류만 배선**(§3-6·DR-1).
- **AI 응답 category 정규화**(M3 §7-4 이월 — 클라측 소유) — 집합 밖 category를 `기타`로 clamp 후 저장·반환.
- 위를 검증하는 `commonTest`(Fake 협력자, 4축 실행) + `androidUnitTest`(실 DB 통합, JVM JDBC).

**OUT (다른 마일스톤/트랙):**
- **UI·ViewModel·`UiState`·디바운스·네비게이션** → M5·M6(spec 3-x). M4는 `Flow`·`suspend`만 노출하고 소비는 상위.
- **Koin 전체 조립**(협력자 바인딩·드라이버·엔진·deviceId 주입) → **M7**(spec 1-4). M4는 생성자 주입 형태만 제공, 컴파일을 green으로 검증.
- **서버 `devetym-proxy`**(D1·read-through·INV-13 정규화-후-캐시쓰기) → **서버 트랙**(M3 §0). M4는 클라 순서만 짓고 서버 D1 계층은 `ClaudeApi.generate` 안에서 서버가 처리(클라 투명).
- **`.asFlow()` 실제 재방출을 넘는 UI 반영** → M5/M6. M4는 `Flow` 노출 + Fake로 재방출 1건 스모크(§6).

## 3. 산출 명세

### 3-1. `TermRepository` 인터페이스 (`commonMain/repository/`)

```kotlin
interface TermRepository {
    suspend fun fetch(keyword: String): TermResult
    suspend fun refresh(keyword: String): TermResult   // 명시적 새로고침 — pinning 우회, 서버 최신본 강제(INV-6)
    fun autocomplete(prefix: String): List<TermEntry>
    suspend fun toggleBookmark(entry: TermEntry): Boolean
    fun bookmarkedTerms(): Flow<List<Term>>
    fun recentSearches(limit: Int): Flow<List<SearchHistory>>
    suspend fun deleteSearchHistory(keyword: String)
    suspend fun clearAllSearchHistory()
}
```
- `Term`·`SearchHistory`는 SQLDelight 생성 타입(M2). ViewModel(M5)이 의존하는 유일 인터페이스.
- **⚠️ 전제조건 — 단일-writer 직렬화(DR-2)**: 소비자(M5)는 **같은 `normalizeKeyword(keyword)`에 대한 `fetch`/`refresh`/`toggleBookmark`를 직렬화**해 동시 실행하지 않는다. 충돌 도메인은 raw 입력 문자열이 아니라 **정규화된 저장 키**(모든 쓰기가 `term.keyword=normalizeKeyword(input)` 단일 로우에 RMW — §3-4/AD-1)다: 서로 다른 raw 표기(`"React"`·`"react"`·`"REACT"`)라도 같은 정규화 로우를 건드리므로 직렬화 단위를 정규화 키로 잡아야 lost-update가 막힌다. `refresh`는 네트워크 왕복(수 초) 동안 RMW 창이 열려 있어, 같은 정규화 키의 동시 쓰기는 lost-update(북마크·`seenAt` 조용한 되돌림)를 낳는다(§3-4). M4 내부 가정이 아니라 **유일 인터페이스인 이 지점에 명시해 M5로 전파하는 계약**이다.

### 3-2. `LocalTermStore` 추상화 (`commonMain/data/local/`, §7-1 판정 대상)

M2 쿼리를 감싸 오케스트레이션 테스트가 **드라이버 없이** Fake로 성립하게 한다(§6-A 네이티브 실행). SQLDelight 생성 `devEtymQueries`를 직접 repository에 주입하면 in-memory JDBC(JVM)로만 테스트 가능해 네이티브 실행 축이 오케스트레이션을 못 태운다.

```kotlin
interface LocalTermStore {
    fun selectByKeyword(keyword: String): Term?
    fun upsertTerm(term: Term)
    fun bookmarked(): Flow<List<Term>>
    fun recent(limit: Long): Flow<List<SearchHistory>>
    fun upsertSearch(keyword: String, searchedAt: Long)
    fun deleteSearch(keyword: String)
    fun clearAllSearch()
}

class SqlDelightTermStore(private val db: DevEtymDatabase) : LocalTermStore {
    // 각 메서드를 db.devEtymQueries.* + .asFlow().mapToList(Dispatchers.Default)로 위임(M2 §3-2).
}
```
- **actual DB 실행 정확성은 `androidUnitTest`(JVM JDBC) 통합 테스트가 커버**(§6-B), 네이티브 `NativeSqliteDriver` 실행은 M8 이월(M2 DR-1 잔여). `SqlDelightTermStore` 자체는 얇은 위임이라 로직이 없다 — 정책 로직은 전부 `TermRepositoryImpl`에 있어 Fake로 실측된다.

### 3-3. `TermRepositoryImpl` — fetch 3단 오케스트레이션 (설계 불변식 — spec 2-3 그대로)

```kotlin
class TermRepositoryImpl(
    private val bundle: BundleDbSource,
    private val api: ClaudeApi,
    private val store: LocalTermStore,
    private val analytics: AnalyticsService,
    private val clock: () -> Long,          // createdAt/seenAt/searchedAt 주입(매퍼에 Clock 없음 — M2 §3-4)
) : TermRepository
```

**`fetch(keyword)` 순서:**
1. **정규화**: `val key = normalizeKeyword(keyword)`(M3 §3-1 공유 정본 = `trim().lowercase()`). `key.isEmpty()`면 즉시 `NotDevTerm`(네트워크·저장 없음).
2. **번들**: `bundle.search(key)` 히트 → `store.upsertSearch(key, clock())` 후 `Found(entry, BUNDLE)`. (번들 히트는 `term` 테이블에 쓰지 않는다 — 북마크 시에만 저장, spec 2-3 `toggleBookmark`.)
3. **로컬 AI 캐시**: `store.selectByKeyword(key)`가 **`source == "AI"`인 로우만** 캐시로 취급(북마크용 번들 항목 제외). 히트 → `upsertSearch` 후 `Found(row.toDto(), AI)`. **`seenAt != null`(pinned) 로우도 여기서 그대로 반환**(INV-6) — `refresh()`만 이 단계를 건너뛴다.
4. **네트워크**: `api.generate(keyword)`(**원본 keyword** — 대소문자 보존, M3 §3-2. 프록시 read-through, 클라 투명):
   - `Found(entry, AI)` → **category 정규화**(§3-5) + **keyword 정규화**(`entry.copy(keyword=key)` — 저장 키를 조회 키와 단일 정본으로 고정, §3-4) 후 **upsert**(§3-4, `source=AI`, 처음 본 항목이면 `seenAt=clock()`) + `upsertSearch` 후 `Found(정규화 entry, AI)` 반환.
   - `NotDevTerm` / `PossibleTypo` → 그대로 반환(**히스토리·term 저장 안 함**).
   - `ClaudeException` → `analytics.logError(...)` 후 **전파**(**저장 안 함**).

**`refresh(keyword)`**: fetch와 같되 **3단(로컬 AI 캐시)을 건너뛰어** 항상 네트워크로 가 서버 최신본을 강제하고, upsert 시 pinned 로우의 `seenAt`을 **갱신**한다(fetch는 pinned를 덮지 않음 — INV-6, refresh만 예외). 번들 히트(2단)는 refresh에서도 동일(번들은 로컬 head).

**`autocomplete(prefix)`**: `bundle.autocomplete(prefix)` 위임(정규화는 `BundleDbSource` 내부).

**`toggleBookmark(entry)`**: `val key = normalizeKeyword(entry.keyword)`로 조회·저장 키를 단일 정본화한다. `store.selectByKeyword(key)` 존재 시 `isBookmarked` 토글(§3-4 read-modify-write로 createdAt·source·seenAt 보존) 후 새 값 반환. 미존재(번들 용어)면 `entry.copy(keyword=key).toEntity(source=BUNDLE, createdAt=clock(), isBookmarked=true, seenAt=null)` 저장 후 `true`(저장 keyword=key로 고정 — §3-4. 원형으로 저장하면 un-bookmark가 기존 로우를 못 찾아 중복 삽입).

**`bookmarkedTerms()` / `recentSearches(limit)`**: `store.bookmarked()` / `store.recent(limit.toLong())` 위임(반응형 `Flow`). **수동 재조회 코드 없음**(ADR-0002).

**`deleteSearchHistory(keyword)` / `clearAllSearchHistory`**: `store.deleteSearch(normalizeKeyword(keyword))` / `store.clearAllSearch` 위임 — 삭제도 `normalizeKeyword`로 write 경로(`upsertSearch(key=normalizeKeyword(input))`)와 **대칭화**한다(§3-4 「모든 키 경로 정본화」를 삭제 경로에서도 지킴). 정규화 없이 위임하면 M5가 원형 입력(`"React"`)으로 삭제 시 저장된 정규화 로우(`"react"`)와 불일치해 조용히 no-op으로 stale 최근검색이 잔존한다. `clearAllSearch`는 키 인자가 없어 정규화 불요.

### 3-4. upsert 정책 — 보존 목록 (⚠️ M2 DR-M2-2 상속 — M4 필수)

`store.upsertTerm`은 M2 `insertOrReplaceTerm`(=`INSERT OR REPLACE`=DELETE+INSERT)이라 **모든 컬럼을 새 값으로 덮는다**. 따라서 기존 로우 갱신 시 **read-modify-write**로 보존-임계 필드를 재주입한다:

- **⚠️ 저장 keyword = 조회 key 단일 정본 (M2가 M4로 위임한 정규화 책임)**: M2 스키마는 `term.keyword` PK를 **정규화된 용어**로 못박았고(정규화 책임은 M4) `selectByKeyword`·로컬 AI 캐시 조회(3단)는 **항상** `normalizeKeyword`된 `key`로 조회한다. 그러나 `api.generate`는 **원본 keyword**를 받고 Claude가 `entry.keyword`를 대소문자 유의미 원형(`"React"`·`"REST"`)으로 반환할 수 있다. 따라서 **모든 저장 경로**(§3-3 4단 network upsert·아래 신규/기존 로우·`toggleBookmark` 번들 저장)는 `toEntity` 대상 entry의 `keyword`를 `key`(=`normalizeKeyword(input)`)로 **고정**한다(`entry.copy(keyword=key)`). 이를 어기면 term.keyword가 비정규화 원형으로 저장돼 다음 fetch의 3단 캐시가 **영구 miss**(INV-1·2 위반·pinning 무력화·`createdAt` 매 fetch 재설정으로 재정렬·중복 로우 누적)한다. §6-A가 대소문자 유의미 입력으로 실측.

- **기존 로우가 있으면**(`store.selectByKeyword(key) != null`): 옛 `Term`을 읽어 **`createdAt`·`isBookmarked`·`source`·`seenAt`을 보존**하고, 갱신 대상(내용 필드 + 버전)만 새 값으로 `toEntity(...)`에 넣어 upsert한다. `toEntity`의 네 DB 전용 필드가 **기본값 없는 필수 인자**라(M2 §3-4·DR-1) 재주입 누락은 **컴파일 에러**다 — silent 북마크 소실·unpin·재정렬을 컴파일이 막는다.
  - **`createdAt` 보존이 정렬 안정성의 락(DR-M2-2)**: `bookmarked`가 `ORDER BY createdAt DESC`라, refresh/fetch가 `createdAt`을 새 시각으로 덮으면 북마크 목록이 **새로고침마다 조용히 재정렬**된다. 옛 `createdAt`을 그대로 재주입해 이를 막는다. §6-A가 실측.
  - **`source` 보존**: AI 캐시 로우를 refresh해도 `source=AI` 유지. 번들 북마크 로우(`source=BUNDLE`)를 fetch가 `AI`로 바꾸지 않는다. ⚠️ **부작용(DR-4)**: 번들에서 빠진 `source=BUNDLE` 북마크 용어를 이후 fetch하면 `source`·`isBookmarked`·`createdAt`은 보존되나 **내용 필드(summary·etymology·namingReason·category)는 매 fetch마다 AI값으로 조용히 덮인다** — 사용자가 북마크한 큐레이션 정의가 AI 생성본으로 대체된다(캐시 우회뿐 아니라 북마크 내용 정체성이 흔들림). 승격/내용-동결 정책은 데이터 트랙으로 이월(Open Questions).
  - **`seenAt` 보존/갱신**: `fetch`는 pinned(`seenAt!=null`) 로우를 3단에서 반환하고 network 단계로 안 가므로 덮을 일이 없다. 네트워크로 처음 저장 시 `seenAt=clock()`(INV-6 pin). **`refresh`만** pinned를 network로 갱신하며 이때 `seenAt`을 새로 찍는다.
- **신규 로우**(미존재, AI 네트워크 경로): `toEntity(source=AI, createdAt=clock(), isBookmarked=false, seenAt=clock())` 명시 — 처음 본 AI 로우를 pin한다(INV-6). ⚠️ **정본 우선순위(AD-1)**: 상속 M2 `TermMapper.kt`(line 29) 도크 주석의 `seenAt=null` 예시는 **번들 북마크 저장 경로**(§3-3 `toggleBookmark` 미존재 분기, `seenAt=null`)만 가리키는 좁은 예시이지 AI 신규 저장 경로가 아니다. AI 신규 로우의 `seenAt`은 **이 스펙 §3-4가 정본(`clock()` = pin)이며 상속 주석보다 우선**한다. 매퍼는 `seenAt`을 필수 `Long?` 인자로만 강제할 뿐(`null`도 유효 값) 컴파일이 잘못된 값을 못 잡으므로, 구현자는 상속 주석의 좁은 예시가 아니라 이 정책을 따른다(§6-A `test_fetch_캐시미스_..._upsert`의 `seenAt!=null` 단언이 DoD 게이트에서 방어).

> **⚠️ RMW 원자성 — 단일-writer 계약(DR-2)**: 위 read-modify-write(`store.selectByKeyword` → `store.upsertTerm`)는 트랜잭션으로 감싸지 않는다 — `LocalTermStore`는 얇은 위임 추상화라 트랜잭션 경계를 두지 않는다. 그래서 **같은 `normalizeKeyword(keyword)`에 대한 `fetch`/`refresh`/`toggleBookmark`의 단일-writer 직렬화(동일 정규화 키 동시 쓰기 없음)를 `TermRepository` 인터페이스의 명시 전제조건으로 둔다**(§3-1) — M4 내부 가정이 아니라 유일 소비자 M5가 지켜야 하는 계약으로 전파한다. **직렬화 단위는 raw 입력이 아니라 정규화 키다**(AD-1이 모든 쓰기를 `term.keyword=normalizeKeyword(input)` 단일 로우로 정본화했으므로): raw 뮤텍스로 잡으면 `refresh("React")`와 `"react"`/`"REACT"` 표기의 `toggleBookmark`가 서로 다른 raw 키라 겹치지 않으면서도 같은 정규화 로우에 RMW해 lost-update가 새어 나간다. `refresh`의 RMW 창은 **네트워크 왕복 전체(수 초)라 좁지 않다**: 같은 정규화 키의 `refresh`(옛 로우를 읽고 네트워크 대기)와 `toggleBookmark`(동기 RMW)가 겹치면 refresh가 옛 값으로 덮어 북마크·`seenAt`이 조용히 되돌려진다. lost-update가 실재하므로 **'창이 좁다'는 이전 정당화는 refresh에 대해 철회한다** — 방어는 계약(직렬화)으로 하고, 계약 대신 SQLDelight `transaction`으로 원자화할지는 후속 하드닝으로 이월한다(Open Questions).

### 3-5. AI 응답 category 정규화 (⚠️ M3 §7-4 이월 — 클라측 소유)

`ClaudeApi`는 category를 pass-through로 뒀다(M3 §4). M4가 **AI 응답 경로에서** 집합 밖 category를 정규화한다 — upsert **직전**(서버 INV-13이 write-게이트에서 하는 것과 대칭 클라 지점):

```kotlin
private fun clampCategory(c: String): String = if (c in Category.CANONICAL) c else Category.ETC
```
- `api.generate`가 준 `Found(entry, AI)`의 `entry.category`가 6집합 밖(오타 `네트웤`·영문 `Database`)이면 `기타`로 clamp한 `TermEntry`를 저장·반환한다. **번들(2단)·로컬 캐시(3단) 경로는 clamp하지 않는다**(캐시는 이미 clamp된 AI 값이 저장됨). 서버측 소유(INV-13)와 상보 — M4는 클라 수신분만 방어(§4).
- ⚠️ **번들 무-clamp 근거의 정직한 재서술(DR-3)**: 번들 경로를 clamp하지 않는 근거는 **강제 빌드 게이트인 '저작 린트'가 아니다** — 그런 게이트는 현재 **미존재**(M3 Open Questions에 번들 de-dup/린트가 미해소로 이월). 현재 배포 `terms.json`(650건)은 category가 전부 in-set이지만 이는 **경험적 클린일 뿐 강제 게이트 없음**이 정직한 상태다. 번들 저작 오타(영문 `Database`·오타 `네트웤`)가 유입되면 fetch 번들 경로가 un-clamped로 반환·`toggleBookmark`로 저장하고 downstream category 버킷팅(M5/M6)에서 조용히 누락될 수 있다. 번들 category in-set를 강제하는 빌드/테스트 게이트는 **데이터 트랙에 명시 이월**한다(Open Questions).

### 3-6. `AnalyticsService` (`commonMain/analytics/`)

```kotlin
interface AnalyticsService {
    fun logSearchResult(keyword: String, result: TermResult)
    fun logError(keyword: String, error: Throwable)
}
class PlaceholderAnalyticsService : AnalyticsService { /* no-op */ }
```
- 인터페이스 추상화 유지(실제 구현은 후속). **M4 `fetch`는 오류 경로만 `logError`로 로깅한다**(§3-3 `ClaudeException` 분기). `logSearchResult`는 인터페이스에 선언해 두되 **M4 `fetch`는 호출하지 않는다** — 결과-유형 성공 로깅 배선은 후속(M5+)으로 이월한다(DR-1). §3-3 성공 경로(번들/캐시/네트워크 Found·NotDevTerm·PossibleTypo)에 `logSearchResult` 호출 지점이 없고 §6-A도 `logError`만 오라클로 단언하는 것과 정합 — M4 스코프에서 성공-로깅 산출물을 주장하지 않는다.

## 4. 설계 불변식 (이 슬라이스가 반드시 지킬 것)

- **INV-1·2 3계층 read-through 순서**: fetch가 번들 → 로컬 AI 캐시 → 네트워크 순서를 지키고, 캐시 히트 시 네트워크를 호출하지 않는다(§6-A 실측: 캐시 히트→`api` 미호출).
- **INV-6 local-first pinning**: `seenAt != null` 로우는 `fetch`가 그대로 반환하고 네트워크·덮어쓰기를 하지 않는다. `refresh`만 pinned를 우회해 갱신한다. §6-A가 pinned fetch 불변·refresh 갱신을 실측.
- **⚠️ createdAt 보존 (M2 DR-M2-2 상속 — M4 필수 실측)**: 기존 로우 upsert 시 `createdAt`을 `isBookmarked`/`source`/`seenAt`과 함께 보존해 `bookmarked`(`createdAt DESC`) 정렬이 새로고침마다 재정렬되지 않는다. §6-A가 refresh 후 정렬 안정성을 실측(이것이 M2 §7 carry-forward의 폐쇄 지점).
- **⚠️ schemaVersion Int 범위 (M2 DR-M2-3 상속)**: `term`에 쓰이는 모든 `schemaVersion`은 `TermEntry.schemaVersion`(`Int?`) 출처를 거쳐 `toEntity`의 `Int?.toLong()`로 저장되므로 **항상 Int 범위**다 — `toDto`의 `Long?→Int?`가 무손실이다(DB에 Int범위 밖 Long이 들어갈 경로 없음). M4는 임의 `Long` schemaVersion을 컬럼에 직접 쓰지 않아 이 불변식을 **구성으로** 보장한다(§6-A 버전 왕복 실측).
- **AI category 정규화 (M3 §7-4 상속)**: AI 응답 경로의 집합 밖 category를 `기타`로 clamp 후 저장·반환(§3-5). 서버측 INV-13(정규화-후-캐시쓰기)은 서버 트랙 소관 — M4는 클라 수신분만 방어.
- **ADR-0002 반응형**: 목록은 `Flow`로만 노출. 데이터 변경 후 수동 재조회 코드를 두지 않는다(북마크 토글·히스토리 삭제가 `Flow`로 자동 반영).
- **lazy 저장 (spec 2-3)**: 번들 히트는 `term`에 쓰지 않음(북마크 시에만). `NotDevTerm`/`PossibleTypo`/오류는 히스토리·term 저장 안 함.

## 5. 완료 조건 (DoD) — 하네스 수렴 오라클

- `TermRepository`·`TermRepositoryImpl`·`LocalTermStore`·`SqlDelightTermStore`·`AnalyticsService`가 **Android·iOS 양쪽에서 컴파일**된다: `:shared:testDebugUnitTest` + `:androidApp:assembleDebug` + `:shared:linkDebugFrameworkIosSimulatorArm64` green(M0~M3의 3축).
- **⊕ 4번째 축 — 네이티브 실행**: `:shared:iosSimulatorArm64Test` green. §6-A(Fake 협력자 오케스트레이션)가 **네이티브 타깃에서 실행**되어 fetch 3단 분기·pinning 스킵·category clamp·createdAt 보존 정책을 **드라이버 없이 실행으로** 실측한다(순수 Kotlin 오케스트레이션 로직의 네이티브 실행).
- 아래 §6 테스트 전부 통과. **§6-A(Fake 협력자 오케스트레이션)는 DoD 필수** — fetch 순서·pinning·보존 목록 정책이 여기서 실측된다. **§6-B(실 DB 통합, JVM JDBC)**는 `SqlDelightTermStore`가 M2 쿼리를 올바로 위임함을 실측.
- 신규 라이브러리 좌표 없음(M2 `sqldelight-coroutines-extensions`·M3 협력자 재사용). `.asFlow().mapToList` 배선이 컴파일·실행됨을 확인.

## 6. 테스트 — 함수명 `test_[대상]_[조건]_[기대]`

### 6-A. Fake 협력자 오케스트레이션 (`commonTest/`) — **JVM+네이티브 양쪽 실행(4축), DoD 필수**

Fake `BundleDbSource`·Fake `ClaudeApi`(주입된 `TermResult`/예외 반환·호출 여부 기록)·Fake `LocalTermStore`(in-memory Map + Flow)·Fake `AnalyticsService`·고정 `clock`.

- `test_fetch_빈입력_NotDevTerm_네트워크없음` — `""`·공백 → `NotDevTerm`, `api` 미호출.
- `test_fetch_번들히트_FoundBUNDLE_히스토리저장_term미저장` — 번들 매칭 → `Found(BUNDLE)`, `upsertSearch` 호출·`upsertTerm` 미호출·`api` 미호출.
- `test_fetch_alias히트_FoundBUNDLE` — 입력이 번들 alias → `Found(BUNDLE)`.
- `test_fetch_번들미스_로컬AI캐시히트_FoundAI_API스킵` — `store`에 `source=AI` 로우 → `Found(AI)`, `api` 미호출.
- `test_fetch_로컬번들북마크로우는캐시아님_API호출` — `store`에 `source=BUNDLE` 로우만 → 캐시로 안 침, `api` 호출.
- `test_fetch_캐시미스_API호출_FoundAI_upsert_히스토리저장` — `api`가 `Found(AI)` → `upsertTerm(source=AI, seenAt!=null)` + `upsertSearch` 호출.
- `test_fetch_pinned항목_그대로반환_API스킵` — `store` 로우 `seenAt!=null` → `Found(AI)` 그대로, `api` 미호출.
- `test_refresh_pinned우회_API호출_seenAt갱신` — 같은 pinned 로우에 `refresh` → `api` 호출, upsert 시 `seenAt` 새 값·`createdAt`/`isBookmarked` 보존.
- `test_fetch_NotDevTerm_PossibleTypo_저장안함` — `api`가 `NotDevTerm`/`PossibleTypo` → 그대로 반환, `upsertTerm`·`upsertSearch` 미호출.
- `test_fetch_ClaudeException_전파_저장안함_analytics로깅` — `api`가 `DailyLimitExceeded` → 전파, 저장 없음, `analytics.logError` 호출.
- `test_fetch_기존AI로우refresh_createdAt보존_정렬안정` — 옛 `createdAt=100`인 AI 로우를 refresh(새 clock=200) → upsert된 로우의 `createdAt==100`(DR-M2-2, 정렬 안정성).
- `test_fetch_기존북마크로우_source와isBookmarked보존` — `source=BUNDLE,isBookmarked=true` 로우를 fetch 경로가 갱신해도 둘 보존.
- `test_fetch_AI응답_집합밖category_기타로clamp` — `api`가 category=`네트웤` → 저장·반환 category==`기타`(§3-5, M3 §7-4).
- `test_fetch_대소문자유의미입력_저장keyword정규화` — 입력 `"React"`, `api`가 `entry.keyword="React"`(원형)로 `Found(AI)` 반환 → **저장된 term.keyword == `normalizeKeyword("React")`(=`"react"`)** 단언(§3-4 저장 키 정본). 원형 저장이면 실패하는 오라클.
- `test_fetch_대소문자유의미입력_재fetch캐시히트_API미호출` — `fetch("React")` 1회 저장 후 `api` 호출 카운트 리셋 → `fetch("React")` 재호출 시 3단 캐시 히트로 `api` **미호출**·`Found(AI)`(INV-1·2). 저장 키가 원형이면 영구 miss로 `api` 재호출→실패.
- `test_fetch_대소문자유의미입력_재fetch_createdAt보존` — `fetch("REST")`(clock=100) 저장 후 `fetch("REST")`(clock=200) → 저장 로우 `createdAt==100`(캐시 히트로 재설정 없음, DR-M2-2). 원형 저장이면 매번 신규 로우 경로로 `createdAt` 재설정→실패.
- `test_toggleBookmark_대소문자유의미입력_unbookmark_기존로우재사용` — `toggleBookmark(entry(keyword="React"))`로 저장(true) 후 다시 `toggleBookmark(entry(keyword="React"))` → 같은 로우 토글로 `false` 반환·중복 로우 없음(저장 키=`normalizeKeyword`). 원형 저장이면 두 번째가 기존 로우 미발견→중복 삽입으로 실패.
- `test_fetch_AI응답_버전필드_왕복보존` — `schemaVersion=2,promptVersion="2026-07"` → upsert·조회 왕복 무손실(Int 범위).
- `test_toggleBookmark_기존로우_토글_보존` — 기존 로우 토글 시 `createdAt`/`source`/`seenAt` 보존, 값 반환.
- `test_toggleBookmark_번들용어_BUNDLE저장_true` — 미존재 → `source=BUNDLE,isBookmarked=true` 저장 후 `true`.
- `test_bookmarked_recent_Flow_노출` — Fake Flow가 방출하는 목록을 그대로 노출(반응형 스모크).
- `test_deleteSearch_clearAll_위임` — `upsertSearch`로 정규화 로우(`"react"`) 저장 후 **원형-대소문자 입력** `deleteSearchHistory("React")`가 그 정규화 로우를 지움을 실측(§3-4 삭제 경로 정본화 — 정규화 없으면 불일치로 no-op·잔존→실패). `clearAllSearchHistory` 위임도 확인.

### 6-B. 실 DB 통합 (`androidUnitTest/`, in-memory JDBC 드라이버) — `SqlDelightTermStore` 위임 실측

> M2 §6-B와 동일 배치: `JdbcSqliteDriver(IN_MEMORY)` + `DevEtymDatabase.Schema.create(driver)`로 실 DB를 띄워 `SqlDelightTermStore`가 M2 쿼리를 올바로 위임함을 실측(네이티브 드라이버 실행은 M8 이월).

- `test_store_upsert_select_왕복` — `upsertTerm` 후 `selectByKeyword`로 되읽어 필드 일치.
- `test_store_bookmarked_Flow_createdAt내림차순` — 북마크 로우 삽입 후 `bookmarked()` 첫 방출이 `createdAt DESC`.
- `test_store_recent_Flow_searchedAt내림차순_limit` — `upsertSearch` 다건 후 `recent(limit)` 첫 방출 정렬·limit.
- `test_store_deleteSearch_clearAll` — 삭제 후 잔여 확인.

## 7. 열린 질문 (비준이 판정할 항목)

1. **`LocalTermStore` 추상화 도입 (제안) vs `DevEtymDatabase` 직접 주입** — 제안은 저장을 인터페이스로 감싸 오케스트레이션 정책(fetch 순서·pinning·보존 목록)을 **Fake로 드라이버 없이 4축(네이티브 포함) 실측**하게 한다. **대안**: repository가 `DevEtymDatabase`(또는 `devEtymQueries`)를 직접 받고 테스트는 in-memory JDBC(JVM)만 → 오케스트레이션이 네이티브 실행 축에서 무측정. — 제안: 추상화. spec 1-4 Koin이 `TermRepositoryImpl(get(),get(),get(),get())`로 협력자 4개를 주입하므로 `store`를 4번째로 두는 것과 정합. 비준 판정.
2. **`refresh`의 번들 처리 — 번들 히트도 반환(제안) vs 번들도 건너뛰고 네트워크 강제** — 제안: `refresh`는 로컬 AI 캐시(3단)만 건너뛰고 번들(2단)은 그대로(번들=로컬 head, 서버보다 신뢰). 대안: refresh가 번들도 건너뜀. — 제안: 번들 유지. 비준이 INV-6·INV-11(오프라인 우선)과 정합하는지 판정.
3. **category clamp 지점 — M4 upsert 직전(제안)** — AI 응답 경로만 clamp, 번들·캐시 경로는 안 함(§3-5). 비준이 이 위치가 downstream 버킷팅 누락을 막기 충분한지, 서버 INV-13(서버 트랙)과 중복·누락 없이 상보적인지 판정.
4. **`clock` 주입 형태 — `() -> Long`(제안) vs `kotlinx.datetime.Clock`** — 제안: 얇은 `() -> Long`(epoch millis, 테스트 결정성·의존성 최소). M2 매퍼가 `createdAt: Long`을 호출자 주입으로 뒀으므로 정합. 비준 판정(대안: `kotlinx-datetime` 도입).
5. **Analytics 로깅 시점·내용** — M4는 **오류 경로만** `logError`로 로깅한다(§3-3·§3-6). `logSearchResult` 성공-결과 로깅은 M4 스코프에서 배선하지 않고 후속(M5+)으로 이월(DR-1). 후속 배선 시점·PII(keyword 원문) 로깅 문제는 그때 판정.

## 8. 안전·규율

- 마일스톤 경계 **사람 비준 게이트는 완화됨**(2026-07-05, 메모리 `milestone-human-gate-relaxed`): 적대 비준 수렴/ESCALATE 후 Claude가 잔여 residual을 eyes-open 수용하고 구현까지 자율 진행, 사람은 완성물을 사후 리뷰한다. **하네스는 push·머지·`-draft` 제거를 하지 않는다.**
- **M2→M4 바인딩 폐쇄 — createdAt 보존(DR-M2-2)·schemaVersion Int범위(DR-M2-3)**: §6-A가 refresh 후 createdAt 보존·정렬 안정성과 버전 왕복을 실측한다. 비준자는 이 두 상속이 DoD에 걸려 있는지 확인.
- **M3→M4 바인딩 — AI category 정규화(§7-4)**: 클라측 clamp를 M4가 소유(§3-5). 서버측 INV-13은 서버 트랙. 비준자는 상보성 확인.
- **브랜치 보존(defer+stacked)**: 완료 마일스톤 브랜치 삭제·로컬머지 금지. 지우자는 지시·충동이 있어도 재확인 먼저.
- **push 금지 · GitHub 원격 생성 금지.** 로컬 커밋만.
- 네이밍은 젠더중립/여성형 기본.
- 진행 상태 정본은 ROADMAP(디스크). 이 슬라이스는 시간 안 타는 명세만.

## Open Questions

> 비준 종료 시점의 **명시 이월** 자리. (비준 착수 전 — 현재는 비어 있으며, 적대 비준이 채운다.)

- [ ] (비준 대기) §7 열린 질문 1~5의 판정.
- [ ] (선상속·M8) 네이티브 `NativeSqliteDriver` 실행 정확성(M2 DR-1 잔여) — §6-B는 JVM JDBC 전용, `SqlDelightTermStore`의 네이티브 실행은 M8 통합/실기기 DoD로 상속.
- [ ] (선상속·서버 트랙) INV-13 정규화-후-캐시쓰기 — 서버 소관. M4는 클라측 category clamp만 소유.
- [ ] (선상속·DR-2) RMW 원자성 — `fetch`/`refresh`/`toggleBookmark`의 비원자 read-modify-write(`selectByKeyword`→`upsertTerm`, 트랜잭션 없음). M4는 **같은 `normalizeKeyword(keyword)`에 대한 단일-writer 직렬화를 `TermRepository` 인터페이스 전제조건으로 명시 전파**(§3-1·§3-4)해 진행한다(M4 내부 가정 아님 — 유일 소비자 M5가 지킬 계약). 직렬화 단위는 raw 입력이 아니라 정규화 저장 키다(AD-1이 모든 쓰기를 `term.keyword=normalizeKeyword(input)` 단일 로우로 정본화 — raw 키 뮤텍스는 대소문자 다른 표기가 같은 로우에 RMW하는 lost-update를 놓친다). `refresh`의 RMW 창은 네트워크 왕복 전체라 좁지 않으므로 이전 '창이 좁다' 정당화는 철회했다. 계약 대신 SQLDelight `transaction` 원자화(계약 없이도 안전)로 대체할지는 하드닝으로 이월.
- [ ] (선상속·데이터 트랙·DR-3) 번들 category in-set 강제 게이트 — 현재 번들 무-clamp 근거는 경험적 클린일 뿐 강제 빌드/테스트 게이트가 미존재(§3-5). 번들 저작 오타 유입 시 downstream 버킷팅 누락 방지용 in-set 강제 게이트를 데이터 트랙에 이월.
- [ ] (선상속·드문 엣지·DR-4) 크로스버전 번들 제거 시 `source=BUNDLE` 북마크 로우의 AI 승격/내용-동결 정책 — 번들 북마크(`source=BUNDLE`, `seenAt=null`) 용어가 다음 버전 번들에서 빠지면 network `Found(AI)` upsert가 `source=BUNDLE` 보존(§3-4)이라 이후 매 fetch마다 3단 캐시 불인정→네트워크 재호출(캐시 우회). **게다가 그 fetch마다 내용 필드(summary·etymology·namingReason·category)가 AI값으로 조용히 덮여, 사용자가 북마크한 큐레이션 정의가 AI 생성본으로 대체된다** — 성능(캐시 우회)만이 아니라 북마크 내용 정체성 문제다. network Found upsert에서 `source=BUNDLE`→`AI` 승격할지, 혹은 번들-제거 로우 내용을 동결할지 정책 결정을 데이터 트랙으로 이월.
- [x] (비준 종료·ESCALATE → **구현에서 해소** 2026-07-05) **잔여 Blocker AD-2 — 상속 M2 `TermMapper.kt`(line 29) 신규-저장 `seenAt=null` 도크 주석 vs §3-4 AI 신규 로우 `seenAt=clock()`(INV-6 pin) 정면 충돌**. **해소(해소책 (i) 채택)**: M2 `TermMapper.kt` line 29 주석을 **경로별로 정정** — AI 네트워크 신규 로우 `seenAt=clock()`(pin) / 번들 북마크 신규 로우 `seenAt=null`(unpinned)로 명시해 "신규 저장=seenAt=null" 일반화 랜드마인 제거. 구현은 `buildAiRow`가 신규 AI 로우에 `seenAt=clock()` 명시, §6-A `test_fetch_캐시미스_..._upsert`의 `seenAt!=null`이 4축 green으로 방어. 실제 주석은 "신규 저장은 호출부에서 `isBookmarked = false, seenAt = null`을 명시한다"로 **`isBookmarked=false`인 일반 신규 저장 지침**이다. 그런데 §3-4가 이 주석을 "번들 북마크 저장 경로(§3-3 `toggleBookmark`, `isBookmarked=true`)만 가리키는 좁은 예시"로 재규정한 in-spec 디스앰비규에이션은 주석의 실제 범위(값 `true` vs `false` 불일치)를 **오독**한다 — 상속 주석은 특정 번들 경로가 아니라 신규 저장 전반에 `seenAt=null`을 지시한다. 도크 주석이라 컴파일·§6-A 오라클(`test_fetch_캐시미스_upsert`의 `seenAt!=null`은 AI 경로만 방어)로 잡히지 않아 **랜드마인 잔존**: 구현자가 주석의 넓은 지시(`seenAt=null`)를 따르면 AI 신규 로우가 unpin돼 INV-6이 조용히 무력화된다. 해소책 택일 — (i) 상속 `TermMapper.kt` 주석을 신규-저장 경로별(AI=`clock()` pin / 번들 북마크=`null`)로 정정, (ii) §3-4 디스앰비규에이션 문언을 주석 실범위(`isBookmarked=false` 일반 신규 저장)에 맞게 교정, (iii) `test_fetch_캐시미스_upsert`(`seenAt!=null`) 오라클 유지로 AI 경로만 방어(주석 랜드마인은 잔존 인정) — 는 사람 게이트/구현 판정.
- [x] (비준 종료·ESCALATE → **eyes-open 수용·구현 완료** 2026-07-05) **비준 6라운드(cap=6) 종료, ESCALATE**(잔여 Blocker 1=AD-2 위에서 구현 해소, 잔여 Caution 2 처리): **AD-1**(저장 키 정규화가 반환 DTO keyword까지 소문자화) → **수용**: 번들 keyword가 이미 소문자(`aa-tree`·`mutex`·`jpa`)라 정규화 저장이 **일관적**이다(원형 casing은 stored 관심사 아님, display는 M5/M6 소관). **AD-3**(`ClaudeApi` final이라 Fake seam 부재) → **해소**: `TermGenerator` 인터페이스 추출(`ClaudeApi`가 구현), `TermRepositoryImpl`이 인터페이스 의존 → §6-A가 `FakeTermGenerator` 주입·호출 카운트로 캐시 히트→API 미호출 실측. verdict 로그: `~/dev/agent-harnesses/runs/`(task `wqgjbx9lh`). 게이트 완화 하 잔여 residual 처리 완료, downstream 이월(DR-2 RMW 원자성·DR-3 번들 category 게이트·DR-4 크로스버전 승격)은 각 트랙 상속. `-draft` 유지.
