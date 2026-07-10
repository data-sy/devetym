# DevEtym 아키텍처 설계서 (Compose Multiplatform)

> **성격: 기술 설계 정본(*어떻게*의 정본).** 이 앱을 어떤 레이어로, 어떤 기술로, 어떤 데이터 흐름으로 만드는지 규정한다.
> **제품의 *왜*는 [`product/prd.md`](product/prd.md)**, **개별 결정의 근거는 [`adr/`](adr/)**, **화면·동작 상세는 `specs/`**(작성 예정)에 있다. 충돌 시 각 정본을 신뢰할 것.
> 버전·도구 사실(Kotlin·CMP·플러그인 호환)은 빠르게 바뀐다 — 착수 시점에 공식 문서(kotlinlang.org / klibs.io)로 재확인하는 것을 원칙으로 한다.

---

## 1. 설계 목표

- **한 벌의 코드로 Android·iOS를 모두** — 로직뿐 아니라 UI(Composable)까지 `commonMain`에 둔다.
- **레이어를 독립적으로 테스트·교체** — 각 층은 바로 아래 층의 *인터페이스*에만 의존한다.
- **재사용 가능한 골격** — 이 구조가 이후 다른 앱에도 그대로 쓰일 수 있는 CMP 스타터가 되도록 설계한다.
- **백엔드 계약 불변** — 클라이언트가 Cloudflare Worker 프록시를 거쳐 Claude에 닿는 계약은 플랫폼과 무관하게 유지한다.

---

## 2. 아키텍처 개요 — 단방향 레이어링

의존은 **한 방향으로만** 흐른다. 위 층은 바로 아래 층만 알고, 아래 층은 위 층을 모른다. 그리고 이 거의 전부가 `commonMain`에 있으며, 플랫폼별로 갈리는 건 네트워크 엔진과 DB 드라이버 같은 얇은 조각뿐이다.

```
Compose UI            # @Composable · 상태를 그리기만 함
│  관찰 (collectAsState)
▼
ViewModel             # StateFlow<UiState> 노출 · 상태 보관/가공
│  호출
▼
Repository            # 소스 조율 · 캐시 정책 (오케스트레이터)
│            ╲
▼             ▼
Ktor(원격)        DB(로컬)     # 엔진·드라이버만 플랫폼별 (expect/actual)
        ╲        ╱
         Claude 프록시 · SQLDelight/Room · 번들 JSON
```

### 의존성 규칙 (예외 없음)
- **UI(Composable)는 ViewModel만** 안다.
- **ViewModel은 Repository 인터페이스만** 안다 — Ktor 클라이언트, DB, 번들 로더를 직접 참조하지 않는다.
- **Repository가 모든 소스를 소유하고 조율**한다 (번들 DB, 로컬 DB, 네트워크).
- 검색·자동완성·북마크·히스토리 모두 **단일 Repository 인터페이스**를 통해 호출한다.

> 이 규칙 덕분에 UI는 상태를 *그리기만* 하고, ViewModel은 상태를 *보관·가공*하며, Repository가 소스를 *조율*한다. 테스트에서는 Repository를 Fake로 갈아끼워 ViewModel을 단독 검증한다.

---

## 3. 모듈 · 소스셋 구조

```
shared/                         # KMP 모듈 — 거의 전부 여기
├── commonMain/
│   ├── model/                  # data class · sealed (플랫폼 무관)
│   ├── data/
│   │   ├── remote/             # Ktor 클라이언트 · Claude 요청/응답
│   │   ├── local/              # DB 스키마 · DAO/쿼리
│   │   └── bundle/             # 번들 terms.json 로더
│   ├── repository/             # 오케스트레이터
│   ├── ui/                     # Compose 화면 · ViewModel (공유)
│   └── di/                     # Koin 모듈 (공통 배선)
├── androidMain/                # actual: OkHttp 엔진 · Android DB 드라이버 · 플랫폼 Koin 모듈
└── iosMain/                    # actual: Darwin 엔진 · Native DB 드라이버 · 플랫폼 Koin 모듈

androidApp/                     # 셸: MainActivity · Manifest · 서명 · Play
iosApp/                         # 셸: SwiftUI 진입점(ComposeView 호스팅) · Info.plist · 서명 · App Store
```

- **`expect`/`actual`**로 갈리는 건 실질적으로 **네트워크 엔진**과 **DB 드라이버**, 그리고 기기 식별자·플랫폼 Koin 모듈 정도다.
- 셸(`androidApp`/`iosApp`)은 얇다 — 진입점·권한·서명·심사만 담당하고 화면/로직은 `shared`가 갖는다.

---

## 4. 레이어별 설계

### 4.1 모델 (`model/`)

- **DTO ↔ 엔티티 분리 유지.** 번들 DB·AI 응답 공통 DTO(`TermEntry`)와, 로컬 DB에 저장되는 엔티티(`Term`)를 나눈다. 변환 시 **`aliases`를 반드시 포함**한다(데이터 소실 방지 — 설계 불변식).
- **직렬화**: `@Serializable data class`. JSON은 `kotlinx.serialization`으로 자동 변환한다.
- **검색 결과 분기는 `sealed`로.** 문자열 플래그가 아니라 타입으로 강제한다.

```kotlin
// commonMain/model — 스케치
@Serializable
data class TermEntry(
    val keyword: String,
    val aliases: List<String> = emptyList(),
    val category: String,
    val summary: String,
    val etymology: String,
    val namingReason: String,
)

// 검색 결과 — UI 분기의 원천
sealed interface TermResult {
    data class Found(val entry: TermEntry, val source: Source) : TermResult
    data object NotDevTerm : TermResult
    data class PossibleTypo(val suggestion: String) : TermResult
}

enum class Source { BUNDLE, AI }   // source 문자열 대신 타입으로
```

> `source`를 문자열("bundle"/"ai")이 아니라 `enum`으로 두는 게 CMP에서의 개선점이다 — 분기 실수를 컴파일 타임에 잡는다.

### 4.2 로컬 저장 (`data/local/`)

로컬 DB는 히스토리·북마크·AI 캐시를 담는다. **후보는 SQLDelight(.sq에 SQL 먼저, 타입세이프 API 생성)와 Room KMP(@Entity/@Dao 어노테이션).** 드라이버만 플랫폼별(`AndroidSqliteDriver` / `NativeSqliteDriver`)로 갈린다.

- **결정은 [ADR로 분리](adr/)** — 이 문서는 "무엇을 저장하나"까지만 규정한다.
- 저장 대상 엔티티 두 개: **`Term`**(캐시 + 북마크, `aliases` 포함), **`SearchHistory`**(검색 성공 시).
- **반응형 쿼리로 UI 자동 갱신.** 로컬 DB 변경을 `Flow`로 관찰해 ViewModel이 자동으로 최신 상태를 받는다. (수동 재조회 없이 목록이 갱신된다.)

**저장 정책 (설계 불변식 — 그대로 유지)**
- **lazy 저장**: AI 응답 수신 시 `Term`으로 저장(`source = AI`, `aliases` 포함). 북마크 시 번들 용어도 이때 `Term`으로 저장(`source = BUNDLE`). **번들 용어는 검색만으로는 저장하지 않는다.**
- **upsert**: 동일 `keyword`의 `Term`이 있으면 새로 만들지 않고 필드만 갱신(`isBookmarked`, `source` 보존). `SearchHistory`는 동일 `keyword` 존재 시 `searchedAt`만 갱신.
- **`SearchHistory`는 검색 성공 시에만** 저장한다.

```sql
-- SQLDelight 예 (Room이면 @Entity로 등가 표현)
CREATE TABLE term (
  keyword      TEXT PRIMARY KEY NOT NULL,
  aliases      TEXT NOT NULL,        -- JSON 인코딩 (aliases 보존 불변식)
  category     TEXT NOT NULL,
  summary      TEXT NOT NULL,
  etymology    TEXT NOT NULL,
  namingReason TEXT NOT NULL,
  source       TEXT NOT NULL,        -- 'BUNDLE' | 'AI'
  isBookmarked INTEGER NOT NULL DEFAULT 0,
  createdAt    INTEGER NOT NULL
);
```

### 4.3 네트워킹 (`data/remote/`)

**Ktor Client**가 공유 네트워킹의 표준이다. `HttpClient`에 **엔진을 플랫폼별로** 꽂고(Android=OkHttp, iOS=Darwin), JSON은 `kotlinx.serialization`으로 변환한다. URLSession 자리를 대체한다.

**백엔드 계약 (불변)**
- 클라이언트는 **Anthropic Messages API 요청 본문을 그대로 구성**해(모델·`max_tokens`·`thinking`·`system` 프롬프트·`tools`·`tool_choice`) 프록시로 POST한다.
- 프록시(Cloudflare Worker)는 **API 키 주입 + 기기당 일일 한도 강제**만 하는 얇은 통로다. **앱에 키는 없다.**
- 요청 헤더에 익명 **`X-Device-Id`**를 실어 보낸다 — 프록시의 기기당 한도 카운터 키.
- 프록시가 한도 초과를 **`429`**로 알리면 `DailyLimitExceeded`로 분기한다.

> **시스템 프롬프트·도구 스키마는 `commonMain`에 산다.** 프롬프트 엔지니어링이 클라이언트 책임이라는 현재 계약을 그대로 옮긴다(→ 향후 서버로 옮길지는 [ADR 후보](adr/)). 프롬프트 버전 형상(v1 5변경+v2 Path A)·3분기·품질 게이트·미채택(closing/selfcheck v3 보류) 정본 = [ADR-0007](adr/0007-ai-prompt-quality.md), 근거 문서 = [`docs/ai-quality/`](ai-quality/).

**응답 파싱 — tool_use 3분기 (설계 불변식)**
모델은 반드시 세 도구 중 하나를 호출한다. `content`에서 `tool_use` 블록을 찾아 이름으로 분기한다:

| 도구 이름 | 결과 |
|---|---|
| `return_term_entry` | `input`을 `TermEntry`로 디코드 → 정상 결과 |
| `return_not_dev_term` | `TermResult.NotDevTerm` |
| `return_possible_typo` | `input.suggestion` → `TermResult.PossibleTypo` |

```kotlin
// commonMain/data/remote — 스케치
class ClaudeApi(private val client: HttpClient, private val deviceId: () -> String) {
    suspend fun generate(keyword: String): TermResult {
        val res = client.post(PROXY_URL) {
            header("X-Device-Id", deviceId())
            contentType(ContentType.Application.Json)
            setBody(buildClaudeRequest(keyword))   // system+tools를 여기서 구성
        }
        if (res.status.value == 429) throw DailyLimitException()
        return res.body<ClaudeResponse>().toTermResult()   // tool_use 3분기
    }
}
```

> **스트리밍은 지금 범위 밖.** 현재 계약은 단발 요청/응답이다. 토큰 스트리밍(`Flow<String>`)은 이후 선택지로 남긴다 — 도입 시 Ktor + `Flow`로 흘린다.

### 4.4 Repository — 오케스트레이터 (`repository/`)

Repository가 **번들 DB → 로컬 캐시 → AI**를 조율한다. ViewModel이 의존하는 유일한 인터페이스다.

**`fetch` 흐름 (설계 불변식 — 순서 그대로)**
1. 입력 **정규화**(trim + lowercase). 빈 입력은 `NotDevTerm`.
2. **번들 DB** 조회 → 있으면 히스토리 upsert 후 `Found(source = BUNDLE)`.
3. **로컬 캐시** 조회 — **`source == AI`인 항목만** 캐시로 취급(북마크용으로 저장된 번들 항목은 캐시에서 제외). 있으면 히스토리 upsert 후 `Found(source = AI)`.
4. **Claude 호출** → 성공 시 `Term` upsert + 히스토리 upsert 후 `Found(source = AI)`. 실패는 tool_use 분기(`NotDevTerm`/`PossibleTypo`) 또는 오류(타임아웃·네트워크·429) 전파.

```kotlin
// commonMain/repository — 인터페이스 (ViewModel이 의존하는 유일한 계약)
interface TermRepository {
    suspend fun fetch(keyword: String): TermResult
    fun autocomplete(prefix: String): List<TermEntry>
    // 북마크
    suspend fun toggleBookmark(entry: TermEntry): Boolean
    fun bookmarkedTerms(): Flow<List<Term>>       // 반응형
    // 히스토리
    fun recentSearches(limit: Int): Flow<List<SearchHistory>>  // 반응형
    suspend fun deleteSearchHistory(keyword: String)
    suspend fun clearAllSearchHistory()
}
```

> 목록 조회(`bookmarkedTerms`, `recentSearches`)를 `Flow`로 노출하는 게 CMP에서의 개선점이다 — DB 변경이 UI에 자동 반영되어, 데이터 변경 직후 수동 재조회하던 절차가 사라진다.

### 4.5 ViewModel + StateFlow (`ui/`)

- 화면 상태를 **`sealed`로 모델링**해 `StateFlow<UiState>`로 노출한다. `@Published`/`ObservableObject` 자리를 대체한다.
- ViewModel은 **Repository 인터페이스만** 주입받는다.
- 상태는 **불변**으로 다룬다 — 변경은 `copy`로(가변 변경은 재구성 누락 위험).

```kotlin
sealed interface SearchUiState {
    data object Idle : SearchUiState
    data object Loading : SearchUiState
    data class Result(val result: TermResult) : SearchUiState
    data class Error(val kind: ErrorKind) : SearchUiState
}
```

### 4.6 Compose UI (`ui/`)

- SwiftUI 화면을 Compose로 그린다. `collectAsState()`로 ViewModel 상태를 관찰해 **상태를 그리기만** 한다.
- **탭 3개**(검색/북마크/히스토리) 하단 고정, **검색창 하단**(한 손 도달성), 결과 화면은 push 이동하되 탭 유지.
- **오타 추천 탭 시 같은 상세 화면을 replace**(push 아님).
- **다크모드**는 시스템 설정에 자동 대응. 색·폰트·간격 등 시각 토큰은 [디자인 문서](design/) 소관(작성 예정)이며 플랫폼별로 달라질 수 있다.

### 4.7 DI — Koin (`di/`)

`module { }`에 "무엇을 어떻게 만들지"를 선언한다. `EnvironmentKey` 기반 주입 자리를 대체한다. 플랫폼별 구현(엔진·드라이버·기기ID)은 플랫폼 모듈에서 채운다.

```kotlin
val appModule = module {
    single { httpClient(get()) }                 // 엔진은 플랫폼 모듈에서 주입
    single<TermRepository> { TermRepositoryImpl(get(), get(), get()) }
    single<AnalyticsService> { PlaceholderAnalyticsService() }
    viewModel { SearchViewModel(get()) }
}
// startKoin { modules(appModule, platformModule) }
```

- 테스트/Preview에서는 `TermRepository`를 Fake로 교체한다(EnvironmentKey에서 Mock 주입하던 것과 등가).
- **Analytics는 인터페이스로 추상화**해 유지한다(현재는 Placeholder 구현).

### 4.8 동시성 규칙

- SwiftData의 `mainContext`가 메인 스레드 전용이라 `@MainActor`가 강제되던 제약은, CMP에서는 **코루틴 디스패처**로 다룬다. ViewModel은 메인 디스패처에서 상태를 갱신하고, 네트워크·DB IO는 `suspend` + 적절한 디스패처로 오프로드한다.
- 취소는 **협조적**이다 — 오래 도는 작업은 구조적 동시성(코루틴 스코프)으로 취소를 전파한다.

---

## 5. 이행/구축 순서 (코어 먼저, UI 마지막)

위험이 낮은 코어부터 세우고 UI를 마지막에 얹는다.

1. **KMP 골격** — `shared + androidApp + iosApp` 생성, 빈 앱이 양쪽에서 뜨는지 확인.
2. **모델·직렬화** — 어원 항목을 `@Serializable data class`로(버전 태깅 포함, INV-9).
3. **로컬 DB** — 스키마·마이그레이션 정리(SQLDelight/Room 확정 후). local-first pinning 컬럼 빌트인(ADR-0006).
4. **네트워킹** — Ktor 클라이언트 + Claude 요청/응답(tool_use 파싱). **프록시 = read-through 캐시**(서버 D1→API·write-back, 클라엔 투명; ADR-0006). 서버 신규 구축은 `devetym-proxy`.
5. **Repository** — `fetch` 3계층 read-through + local-first pinning/새로고침 + 북마크/히스토리.
6. **ViewModel + StateFlow** — 화면 상태를 sealed로.
7. **Compose UI** — 화면 이식(가장 기계적).
8. **배선·셸** — Koin 조립, 서명·배포·권한은 각 셸에.

> 세부 일정·완료 상태는 [로드맵](../ROADMAP.md)(작성 예정)이 정본이다. 이 문서는 *순서의 원칙*만 규정한다.

---

## 6. 빌드 · 배포

공유 코드가 많아도 **출시는 여전히 플랫폼별**이다. CMP가 지워주는 건 UI·로직 이중 구현이지 배포 파이프라인이 아니다.

| 단계 | Android | iOS |
|---|---|---|
| 빌드 | Gradle → AAB/APK | Xcode (`shared.xcframework` 소비) |
| 서명 | keystore | 인증서·프로비저닝 프로파일 |
| 배포 | Play Console | App Store Connect |
| 권한·설정 | Manifest | Info.plist·entitlements |

CI(GitHub Actions 등)로 양쪽 빌드를 자동화하면 "한 소스, 두 배포"의 관리 비용이 준다.

---

## 7. 버전 정렬 · 함정 체크리스트

착수 시 한 번 훑는다 (사실은 빠르게 바뀌므로 공식 문서로 재확인):

- **버전 정렬** — Kotlin ↔ CMP ↔ 컴파일러 플러그인(`kotlinx.serialization` 등) 호환 조합. 하나만 어긋나도 빌드가 깨진다.
- **라이브러리 지원** — 새 라이브러리는 `klibs.io`에서 iOS·멀티플랫폼 지원과 호환 버전 사전 확인.
- **기본 `final`** — 상속하려면 `open` 명시.
- **불변 상태** — 가변 변경은 재구성 누락 위험, `copy` 사용.
- **협조적 취소** — CPU 루프는 `ensureActive` 없으면 안 멈춘다.
- **Native 빌드 느림** — iOS는 AOT라 빌드 시간이 길다.
- **interop 도구** — iOS 노출용 SKIE vs Swift Export 결정 필요.

---

## 8. 열린 결정 (→ ADR)

| 결정 | 상태 | 비고 |
|---|---|---|
| CMP 선택 자체 | ✅ [ADR-0001](adr/0001-cross-platform-framework.md) | Flutter/RN/네이티브 대비 근거 명문화 |
| 코드 관용구: 리터럴 포팅 금지 | ✅ [ADR-0002](adr/0002-code-idiom-principle.md) | 구조=설계 / 미시=구현 / 우회=삭제 |
| 로컬 DB: SQLDelight vs Room KMP | 🟡 [ADR-0003](adr/0003-local-storage.md) (제안) | SQLDelight 우선, 착수 시 재확인 |
| 프록시 경계·프롬프트 위치 | ⤳ [ADR-0004](adr/0004-backend-proxy-boundary.md) (대체됨) | 프롬프트는 클라(`commonMain`)·`X-Device-Id`·`429`·tool_use 3분기 계약 형태만 유효 |
| 서버 read-through 캐시 경계 | ✅ [ADR-0006](adr/0006-server-cache-boundary.md) | 프록시→캐시 확장, 3계층·pinning을 M1~M8 빌트인(ADR-0004 대체) |
| AI 프롬프트·품질 정본 | ✅ [ADR-0007](adr/0007-ai-prompt-quality.md) | 시스템 프롬프트 버전(v1+v2 Path A)·3분기·품질 게이트·미채택 근거 락, iOS 검증본 commonMain 계승 |
| iOS interop: SKIE vs Swift Export | ✅ [ADR-0005](adr/0005-ios-interop.md) | SKIE 확정(버전 민감) |
| AI 스트리밍 도입 여부 | ⏳ 검토 | 현재 단발 응답, `Flow` 스트리밍은 이후 |

---

## 부록 · 설계 계보 (prior art)

이 아키텍처는 동일 제품의 iOS(SwiftUI) 구현에서 **검증된 데이터 흐름과 설계 불변식을 의도적으로 계승**한다. 새 결정을 처음부터 내리되, 이미 실전에서 검증된 규칙(단방향 의존, `fetch` 3단 흐름, lazy 저장·upsert 정책, `aliases` 보존, tool_use 3분기, 프록시 계약)은 그대로 가져와 리스크를 낮췄다. 개념 대응은 아래와 같다:

| 개념/역할 | iOS (SwiftUI) | CMP (Kotlin) |
|---|---|---|
| UI | SwiftUI View | Compose `@Composable` |
| 상태 | `@Published` / `ObservableObject` | `StateFlow<UiState>` (sealed) |
| 오케스트레이터 | `TermService : TermServiceProtocol` | `TermRepository` |
| 번들 검색 | `BundleDBService` | `data/bundle` 로더 |
| 네트워크 | `ClaudeAPIService`(URLSession) | `ClaudeApi`(Ktor) |
| 로컬 저장 | SwiftData(`@Model`) | SQLDelight / Room KMP |
| 결과 분기 | `enum TermResult` | `sealed interface TermResult` |
| DTO/엔티티 | `TermEntry` / `Term` | 동일 분리 유지 |
| DI | EnvironmentKey 주입 | Koin `module` |
| 동시성 | `@MainActor` | 코루틴 디스패처 |
| 분석 | `AnalyticsServiceProtocol` | `AnalyticsService` 인터페이스 |

*결과물(색·폰트·간격)은 플랫폼 관례에 따라 달라질 수 있으나, 위 흐름과 불변식은 동일하게 유지한다.*
