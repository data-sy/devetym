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
- **`AnalyticsService` 인터페이스 + `PlaceholderAnalyticsService`**(`commonMain/analytics/`) — 검색 결과 유형·오류 로깅(spec 2-3).
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
   - `Found(entry, AI)` → **category 정규화**(§3-5) 후 **upsert**(§3-4, `source=AI`, 처음 본 항목이면 `seenAt=clock()`) + `upsertSearch` 후 `Found(정규화 entry, AI)` 반환.
   - `NotDevTerm` / `PossibleTypo` → 그대로 반환(**히스토리·term 저장 안 함**).
   - `ClaudeException` → `analytics.logError(...)` 후 **전파**(**저장 안 함**).

**`refresh(keyword)`**: fetch와 같되 **3단(로컬 AI 캐시)을 건너뛰어** 항상 네트워크로 가 서버 최신본을 강제하고, upsert 시 pinned 로우의 `seenAt`을 **갱신**한다(fetch는 pinned를 덮지 않음 — INV-6, refresh만 예외). 번들 히트(2단)는 refresh에서도 동일(번들은 로컬 head).

**`autocomplete(prefix)`**: `bundle.autocomplete(prefix)` 위임(정규화는 `BundleDbSource` 내부).

**`toggleBookmark(entry)`**: `store.selectByKeyword(normalizeKeyword(entry.keyword))` 존재 시 `isBookmarked` 토글(§3-4 read-modify-write로 createdAt·source·seenAt 보존) 후 새 값 반환. 미존재(번들 용어)면 `entry.toEntity(source=BUNDLE, createdAt=clock(), isBookmarked=true, seenAt=null)` 저장 후 `true`.

**`bookmarkedTerms()` / `recentSearches(limit)`**: `store.bookmarked()` / `store.recent(limit.toLong())` 위임(반응형 `Flow`). **수동 재조회 코드 없음**(ADR-0002).

**`deleteSearchHistory` / `clearAllSearchHistory`**: `store.deleteSearch` / `store.clearAllSearch` 위임.

### 3-4. upsert 정책 — 보존 목록 (⚠️ M2 DR-M2-2 상속 — M4 필수)

`store.upsertTerm`은 M2 `insertOrReplaceTerm`(=`INSERT OR REPLACE`=DELETE+INSERT)이라 **모든 컬럼을 새 값으로 덮는다**. 따라서 기존 로우 갱신 시 **read-modify-write**로 보존-임계 필드를 재주입한다:

- **기존 로우가 있으면**(`store.selectByKeyword(key) != null`): 옛 `Term`을 읽어 **`createdAt`·`isBookmarked`·`source`·`seenAt`을 보존**하고, 갱신 대상(내용 필드 + 버전)만 새 값으로 `toEntity(...)`에 넣어 upsert한다. `toEntity`의 네 DB 전용 필드가 **기본값 없는 필수 인자**라(M2 §3-4·DR-1) 재주입 누락은 **컴파일 에러**다 — silent 북마크 소실·unpin·재정렬을 컴파일이 막는다.
  - **`createdAt` 보존이 정렬 안정성의 락(DR-M2-2)**: `bookmarked`가 `ORDER BY createdAt DESC`라, refresh/fetch가 `createdAt`을 새 시각으로 덮으면 북마크 목록이 **새로고침마다 조용히 재정렬**된다. 옛 `createdAt`을 그대로 재주입해 이를 막는다. §6-A가 실측.
  - **`source` 보존**: AI 캐시 로우를 refresh해도 `source=AI` 유지. 번들 북마크 로우(`source=BUNDLE`)를 fetch가 `AI`로 바꾸지 않는다.
  - **`seenAt` 보존/갱신**: `fetch`는 pinned(`seenAt!=null`) 로우를 3단에서 반환하고 network 단계로 안 가므로 덮을 일이 없다. 네트워크로 처음 저장 시 `seenAt=clock()`(INV-6 pin). **`refresh`만** pinned를 network로 갱신하며 이때 `seenAt`을 새로 찍는다.
- **신규 로우**(미존재): `toEntity(source=AI, createdAt=clock(), isBookmarked=false, seenAt=clock())` 명시.

### 3-5. AI 응답 category 정규화 (⚠️ M3 §7-4 이월 — 클라측 소유)

`ClaudeApi`는 category를 pass-through로 뒀다(M3 §4). M4가 **AI 응답 경로에서** 집합 밖 category를 정규화한다 — upsert **직전**(서버 INV-13이 write-게이트에서 하는 것과 대칭 클라 지점):

```kotlin
private fun clampCategory(c: String): String = if (c in Category.CANONICAL) c else Category.ETC
```
- `api.generate`가 준 `Found(entry, AI)`의 `entry.category`가 6집합 밖(오타 `네트웤`·영문 `Database`)이면 `기타`로 clamp한 `TermEntry`를 저장·반환한다. **번들(2단)·로컬 캐시(3단) 경로는 clamp하지 않는다**(번들은 저작 린트가 in-set 보장, 캐시는 이미 clamp된 값이 저장됨). 서버측 소유(INV-13)와 상보 — M4는 클라 수신분만 방어(§4).

### 3-6. `AnalyticsService` (`commonMain/analytics/`)

```kotlin
interface AnalyticsService {
    fun logSearchResult(keyword: String, result: TermResult)
    fun logError(keyword: String, error: Throwable)
}
class PlaceholderAnalyticsService : AnalyticsService { /* no-op */ }
```
- 인터페이스 추상화 유지(실제 구현은 후속). `fetch`가 결과 유형·오류를 로깅.

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
- `test_fetch_AI응답_버전필드_왕복보존` — `schemaVersion=2,promptVersion="2026-07"` → upsert·조회 왕복 무손실(Int 범위).
- `test_toggleBookmark_기존로우_토글_보존` — 기존 로우 토글 시 `createdAt`/`source`/`seenAt` 보존, 값 반환.
- `test_toggleBookmark_번들용어_BUNDLE저장_true` — 미존재 → `source=BUNDLE,isBookmarked=true` 저장 후 `true`.
- `test_bookmarked_recent_Flow_노출` — Fake Flow가 방출하는 목록을 그대로 노출(반응형 스모크).
- `test_deleteSearch_clearAll_위임` — 위임 호출 확인.

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
5. **Analytics 로깅 시점·내용** — `fetch` 성공/오류에 `logSearchResult`/`logError`. 세부(어떤 결과를 언제)를 §3-3에 고정했는지, PII(keyword 원문) 로깅이 문제인지 비준 판정.

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
