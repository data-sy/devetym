# spec.md — DevEtym 구현 명세서 (Compose Multiplatform · Claude Code 전용)

> 이 문서는 Claude Code가 참조하는 **구현 명세**다. 무엇을 어떤 순서로 코드로 만드는지 규정한다.
> - 상위 설계(레이어·기술 선택)는 [`../architecture.md`](../architecture.md), 결정 근거는 [`../adr/`](../adr/), 제품의 *왜*는 [`../product/prd.md`](../product/prd.md).
> - **[ADR-0002](../adr/0002-code-idiom-principle.md) 준수**: iOS 흐름을 계승하되 **코틀린 관용으로** 쓴다. Swift/SwiftData 제약을 우회하던 패턴(수동 재조회·`@MainActor` 도배·EnvironmentKey 우회)은 **옮기지 않고 삭제**한다.
> - 서명·스토어·인프라 등 인간 작업은 로드맵/런치 문서 소관(작성 예정).
> - 버전·모델 ID·라이브러리 좌표는 착수 시점에 공식 문서로 재확인한다.

거의 전부 `commonMain`에 둔다. 플랫폼별로 갈리는 건 네트워크 엔진·DB 드라이버·기기 식별자뿐(`expect`/`actual`).

---

## Phase 1 — 모델 및 기반

### 1-1. 도메인 모델 (`commonMain/model/`)

**TermEntry** — 번들 DB + AI 응답 공통 DTO
```kotlin
@Serializable
data class TermEntry(
    val keyword: String,
    val aliases: List<String> = emptyList(),
    val category: String,
    val summary: String,
    val etymology: String,
    val namingReason: String,
    // 버전 태깅 (INV-9, 서버 캐시·딜리버리 트랙 선반영) — 옵셔널·default로 기존 번들 DB/AI 응답과 호환.
    // 서버 배달 항목의 선택적 무효화·재생성을 위한 프롬프트/스키마 버전. null = 버전 이전(pre-versioning) 항목.
    val schemaVersion: Int? = null,
    val promptVersion: String? = null,
)
```

> **버전 필드는 옵셔널이다**: 현재 번들 DB·AI 응답 어디에도 없으므로 역직렬화 시 default(`null`)로 채워진다. 서버 캐시 트랙(→ [`../cache-delivery-milestones.md`](../cache-delivery-milestones.md)) 착수 전까지는 채우지 않는다. 지금 필드만 미리 확보해 이후 서버 통합 시 `@Serializable` DTO·DB 스키마 마이그레이션을 회피한다.

**Source / TermResult** — 결과 출처는 문자열이 아니라 타입으로(컴파일 타임 분기 강제)
```kotlin
enum class Source { BUNDLE, AI }

sealed interface TermResult {
    data class Found(val entry: TermEntry, val source: Source) : TermResult
    data object NotDevTerm : TermResult
    data class PossibleTypo(val suggestion: String) : TermResult
}
```

> **DTO ↔ 엔티티 변환 시 `aliases`·`category`를 반드시 보존한다**(설계 불변식). 변환은 지정된 매퍼(`TermEntry.toEntity()` / `TermEntity.toDto()`)만 사용한다.

**카테고리 값 (번들 DB·AI 응답 공통 고정 집합, 6개):**
`동시성` · `자료구조` · `네트워크` · `DB` · `패턴` · `기타` — 이외 값 불허(AI 응답 포함).

### 1-2. 로컬 저장 스키마 (`commonMain/data/local/`)

로컬 DB는 **히스토리·북마크·AI 캐시**를 담는다. 라이브러리는 [ADR-0003](../adr/0003-local-storage.md)(SQLDelight 우선, 착수 시 확정). 아래는 SQLDelight 기준 스케치 — Room이면 `@Entity`/`@Dao`로 등가 표현.

```sql
CREATE TABLE term (
  keyword      TEXT PRIMARY KEY NOT NULL,     -- 정규화된 용어(영문 소문자)
  aliases      TEXT NOT NULL,                 -- JSON 인코딩 (aliases 보존 불변식)
  category     TEXT NOT NULL,                 -- 6개 고정 집합
  summary      TEXT NOT NULL,
  etymology    TEXT NOT NULL,
  namingReason TEXT NOT NULL,
  source       TEXT NOT NULL,                 -- 'BUNDLE' | 'AI'
  isBookmarked INTEGER NOT NULL DEFAULT 0,
  createdAt    INTEGER NOT NULL
);

CREATE TABLE searchHistory (
  keyword    TEXT PRIMARY KEY NOT NULL,
  searchedAt INTEGER NOT NULL
);

-- 반응형 쿼리(Flow로 관찰) — 목록 UI 자동 갱신
bookmarked:
SELECT * FROM term WHERE isBookmarked = 1 ORDER BY createdAt DESC;

recent:
SELECT * FROM searchHistory ORDER BY searchedAt DESC LIMIT :limit;
```

- **드라이버만 플랫폼별**: `AndroidSqliteDriver`(androidMain) / `NativeSqliteDriver`(iosMain), `expect`/`actual`로 주입.
- 저장 정책(**설계 불변식**): lazy 저장(AI 응답 시 `source=AI`, 북마크 시 번들 용어를 `source=BUNDLE`으로), upsert(동일 keyword 갱신·`isBookmarked`/`source` 보존), `searchHistory`는 검색 성공 시에만.

### 1-3. 상수 (`commonMain`)
```kotlin
object Constants {
    const val claudeModel = "…"        // Anthropic 공식 모델 ID — 착수 시 docs.anthropic.com 확인
    const val apiTimeoutMs = 30_000L
    const val autocompleteDebounceMs = 300L
    const val recentSearchLimit = 5
    const val proxyBaseUrl = "…"       // devetym-proxy 엔드포인트
    // 지원/제보 이메일 등 외부 접점은 AppConfig에서 관리
}
```

### 1-4. DI 골격 — Koin (`commonMain/di/`)

> **EnvironmentKey 우회는 옮기지 않는다([ADR-0002](../adr/0002-code-idiom-principle.md)).** Koin으로 일반 주입한다.

```kotlin
val appModule = module {
    single { bundleDbSource(get()) }
    single { termDatabase(get()) }                 // 드라이버는 플랫폼 모듈에서
    single { httpClient(get()) }                   // 엔진은 플랫폼 모듈에서
    single { ClaudeApi(get(), deviceId = get()) }
    single<TermRepository> { TermRepositoryImpl(get(), get(), get(), get()) }
    single<AnalyticsService> { PlaceholderAnalyticsService() }
    viewModel { SearchViewModel(get()) }
    viewModel { DetailViewModel(get()) }
    viewModel { BookmarkViewModel(get()) }
    viewModel { HistoryViewModel(get()) }
}
// expect val platformModule: Module  (androidMain/iosMain에서 actual: 엔진·드라이버·deviceId)
```

### 1-5. 번들 DB (`commonMain/composeResources` 또는 `data/bundle/`)
- `terms.json`을 앱 시작 시 1회 로드해 메모리 캐시(공유 리소스).
- 스키마: `keyword`(필수)·`aliases`(필수, 최소 1개)·`category`(6개 중 하나)·`summary`·`etymology`·`namingReason`.
- 초기엔 소량으로 시작, Phase 4에서 650개까지 확장(iOS 번들 자산 재사용).

### 1-6. 앱 진입점 (셸)
- **androidApp**: `MainActivity`가 `setContent { App() }`. `startKoin { modules(appModule, platformModule) }`은 `Application`에서 1회.
- **iosApp**: SwiftUI 진입점이 `ComposeUIViewController { App() }`를 호스팅. Koin 초기화는 진입 시 1회.
- `App()`은 `commonMain/ui`의 루트 Composable(테마 + 네비게이션 스캐폴드).

✅ **Phase 1 완료 조건**: 모델·스키마·Koin 배선·번들 로더·양 셸 진입점이 컴파일되고, 빈 앱이 Android·iOS 양쪽에서 뜬다.

---

## Phase 2 — 데이터 소스 · 오케스트레이터

### 2-1. BundleDbSource (`data/bundle/`)
```kotlin
interface BundleDbSource {
    fun search(keyword: String): TermEntry?      // keyword + aliases, 대소문자 무시 완전 매칭
    fun autocomplete(prefix: String): List<TermEntry>  // keyword prefix 매칭
}
```
- `search`: 정규화(trim+lowercase) 후 `keyword` 또는 `aliases` 완전 일치.
- `autocomplete`: 빈 prefix면 빈 목록, 아니면 `keyword.startsWith(prefix)`.

### 2-2. ClaudeApi (`data/remote/`) — Ktor

> **백엔드 계약 계승([ADR-0004](../adr/0004-backend-proxy-boundary.md)).** 앱에 키 없음. 프록시가 키 주입 + 기기당 일일 한도.

```kotlin
class ClaudeApi(private val client: HttpClient, private val deviceId: () -> String) {
    suspend fun generate(keyword: String): TermResult {
        val res = client.post(Constants.proxyBaseUrl) {
            header("X-Device-Id", deviceId())         // 프록시 한도 카운터 키
            contentType(ContentType.Application.Json)
            setBody(buildClaudeRequest(keyword))       // system 프롬프트 + tools를 여기서 구성
        }
        if (res.status.value == 429) throw DailyLimitException()   // 일일 한도 초과
        return res.body<ClaudeResponse>().toTermResult()           // tool_use 3분기
    }
}
```

**요청 본문 (Anthropic Messages API 형태 그대로 구성):**
- `model`·`max_tokens`(4096)·`thinking`(enabled)·`system`(프롬프트)·`tools`(3개)·`tool_choice`(auto)·`messages`.
- **시스템 프롬프트·도구 스키마는 `commonMain`에 산다** — iOS에서 검증된 프롬프트를 그대로 계승. 프롬프트 원문은 AI 품질 문서(`docs/ai-quality/`, 작성 예정)를 정본으로 두고 여기서 참조.

**응답 파싱 — tool_use 3분기(설계 불변식):** `content`에서 `tool_use` 블록을 찾아 도구 이름으로 분기.

| 도구 | 결과 |
|---|---|
| `return_term_entry` | `input` → `TermEntry` 디코드 → `Found(source = AI)` |
| `return_not_dev_term` | `NotDevTerm` |
| `return_possible_typo` | `input.suggestion` → `PossibleTypo` |

**오류 타입:**
```kotlin
sealed class ClaudeException : Exception() {
    data object DailyLimitExceeded : ClaudeException()   // HTTP 429
    data object Timeout : ClaudeException()
    data class Network(val cause: Throwable) : ClaudeException()
    data object InvalidResponse : ClaudeException()      // tool_use 없음/디코드 실패
}
```
- 도구가 `not_dev_term`/`possible_typo`인 경우는 **예외가 아니라 `TermResult`로** 반환(정상 분기).

### 2-3. TermRepository (`repository/`) — 오케스트레이터

> ViewModel이 의존하는 **유일한 인터페이스**. 목록 조회는 **`Flow`로 반응형** 노출(수동 재조회 없음).

```kotlin
interface TermRepository {
    suspend fun fetch(keyword: String): TermResult
    fun autocomplete(prefix: String): List<TermEntry>
    suspend fun toggleBookmark(entry: TermEntry): Boolean
    fun bookmarkedTerms(): Flow<List<Term>>
    fun recentSearches(limit: Int): Flow<List<SearchHistory>>
    suspend fun deleteSearchHistory(keyword: String)
    suspend fun clearAllSearchHistory()
}
```

**`fetch` 오케스트레이션 순서 (설계 불변식 — 그대로):**
1. 입력 정규화(trim + lowercase). 빈 문자열이면 즉시 `NotDevTerm`(네트워크 호출 없음).
2. **BundleDbSource.search** → 히트 시 히스토리 upsert 후 `Found(BUNDLE)`.
3. **로컬 캐시** 조회 — **`source == AI`인 `Term`만** 캐시로 취급(북마크용 번들 항목 제외). 히트 시 히스토리 upsert 후 `Found(AI)`.
4. **ClaudeApi.generate**:
   - `Found(AI)` → `Term` upsert(`source=AI`, `aliases` 포함) + 히스토리 upsert 후 반환.
   - `NotDevTerm` / `PossibleTypo` → 그대로 반환(**히스토리 저장 안 함**).
   - `ClaudeException` → 전파(**히스토리 저장 안 함**). Analytics에 오류 로깅.

**upsert 정책:** `Term`은 동일 keyword 존재 시 필드 갱신(`isBookmarked`·`source` 보존). `searchHistory`는 존재 시 `searchedAt`만 갱신.

**toggleBookmark:** 로컬에 `Term` 존재 시 `isBookmarked` 토글 후 값 반환. 미존재(번들 용어)면 `Term(source=BUNDLE, isBookmarked=true)` 저장 후 `true`.

**Analytics:** `AnalyticsService` 인터페이스로 검색 결과 유형·오류를 로깅(현재 Placeholder 구현). 인터페이스 추상화 유지.

### 2-4. 테스트 (`commonTest/`)
Fake `TermRepository` 협력자(Fake BundleDbSource / Fake ClaudeApi / in-memory DB)로 검증. 함수명 `test_[대상]_[조건]_[기대]`.

- Repository: 빈입력→NotDevTerm / 번들히트 즉답 / alias 히트 / 번들미스→AI호출 / 캐시히트→API스킵 / API오류 전파 / NotDevTerm·PossibleTypo 반환 / 성공시 히스토리저장 / 실패시 미저장 / 기존Term 필드갱신·북마크보존 / 북마크토글(기존·번들) / bookmarked·recent 정렬 / 히스토리 삭제·전체삭제.
- BundleDbSource: 정확매칭 / alias매칭 / 대소문자무시 / 미발견 / prefix자동완성 / 빈prefix.
- ClaudeApi: 정상 tool_use→Found / not_dev_term→NotDevTerm / possible_typo→PossibleTypo / 429→DailyLimitExceeded / 타임아웃 / tool_use없음→InvalidResponse.

✅ **Phase 2 완료 조건**: 모든 테스트 통과, Fake로 네트워크·DB 무의존 검증.

---

## Phase 3 — Compose UI (`commonMain/ui/`)

### 3-0. 공통 규칙
- 모든 **ViewModel은 `TermRepository`만** 주입(Koin `koinViewModel()`). DB·Ktor·번들 로더 직접 참조 금지.
- 화면 상태는 **`sealed` + `StateFlow<UiState>`**로 노출. `collectAsStateWithLifecycle()`로 관찰.
- **목록은 반응형 `Flow`** — 데이터 변경 후 **수동 재조회 코드를 두지 않는다**([ADR-0002](../adr/0002-code-idiom-principle.md)). 북마크 토글·히스토리 삭제 시 DB 변경이 `Flow`로 자동 반영된다.
- 상태는 **불변**, 변경은 `copy`. 네비게이션은 **Compose Navigation**(`NavHost`).
- **다크모드**는 시스템 설정 자동 대응.

### 3-0-1. 디자인 시스템
- **Material3 테마 + 커스텀 토큰**(색·타이포·간격)을 `commonMain/ui/theme`에 둔다. **다크 우선**.
- 폰트·팔레트 등 실제 토큰 값은 **디자인 문서(`docs/design/`, 작성 예정) 정본**을 따른다. iOS의 dark-first 팔레트·DM 서체 방향을 출발점으로 계승하되, **값은 플랫폼 관례에 맞춰 달라질 수 있다**(PRD: 시각 결과물은 달라도 됨).
- 폰트는 `composeResources`에 번들, `FontFamily`로 등록해 타이포 스케일과 연계.

### 3-1. 스캐폴드 · 하단 탭
- 루트 `App()` = 테마 + `Scaffold` + 하단 `NavigationBar`.
- 탭: **검색 / 북마크 / 히스토리 / 설정**. (PRD의 핵심 3탭 + 설정.)
- 결과 상세는 검색 그래프 내에서 push, 하단 탭 유지.

### 3-2. Search
- **검색창은 화면 하단**(한 손 도달성). 엔터/검색 → 상세로 이동.
- 타이핑 중 자동완성: `repository.autocomplete(prefix)` → 드롭다운. **디바운스 300ms**(coroutine `delay` + 이전 job 취소), 최소 1자.
- 최근 검색 칩: `recentSearches(5)`를 **`Flow`로 관찰**(자동 갱신). 칩 탭 → 상세 이동.
- 검색 job은 재검색 시 이전 job 취소(레이스 방지).

### 3-3. Detail
`DetailViewModel`이 `StateFlow<DetailUiState>` 노출:
```kotlin
sealed interface DetailUiState {
    data object Loading : DetailUiState               // AI 생성 중 (번들 히트는 로딩 없음)
    data class Result(val result: TermResult) : DetailUiState
    data class Error(val kind: ErrorKind) : DetailUiState
}
```
- **`Found`**: 용어명(large) · **카테고리 태그** · **AI 생성 뱃지(`source == AI`일 때만)** · 한 줄 요약 · 어원 블록 · 작명 이유(스크롤) · 액션(북마크 토글 · 공유) · 하단 오류 제보.
- **`NotDevTerm`**: "개발 용어를 검색해주세요" 안내.
- **`PossibleTypo`**: "{suggestion}을(를) 찾으셨나요?" — 추천 탭 시 **같은 상세를 replace**(back stack 교체, push 아님).
- **`Error`**: `ErrorKind`별 문구 — 타임아웃 / 네트워크(오프라인 구분) / 응답처리불가 / **일일 한도 초과**("오늘 사용량을 다 썼어요") / 기타(제보 유도). 예외는 `TermResult`에 넣지 않고 ViewModel `catch` → `Error` 상태.
- 화면 이탈 시 진행 중 job 취소.

### 3-4. 오류 제보 (mailto)
- 위치: Detail 하단 고정. 문구: "이 설명이 잘못됐나요? 오류 제보하기".
- `mailto:` 구성 — 수신=지원 이메일, 제목=`[오류제보] {keyword}`, 본문에 keyword/source/summary/etymology/namingReason + 제보 유도. 플랫폼별 인텐트(`expect`/`actual` 또는 멀티플랫폼 URL 핸들러)로 메일 앱 실행.

### 3-5. Bookmark
- `bookmarkedTerms()`를 **`Flow`로 관찰** → 목록 자동 갱신(`.onAppear` 재조회 불필요).
- 항목: keyword + 한 줄 미리보기(summary). 탭 → 상세. 스와이프 삭제 → `toggleBookmark` (목록은 Flow로 자동 반영).
- 빈 상태 안내.

### 3-6. History
- `recentSearches(limit)`를 **`Flow`로 관찰**.
- 항목의 `searchedAt`은 **상대 시간**("방금 전"·"1시간 전"·"어제"·"3일 전"), 한국어 로케일(멀티플랫폼 시간 포맷터 또는 `expect`/`actual`).
- 탭 → 상세. 스와이프 삭제 → `deleteSearchHistory`. 상단 "전체 삭제" → `clearAllSearchHistory`. (모두 Flow로 자동 반영.)

### 3-7. Onboarding
- 첫 실행 1회(플래그를 **멀티플랫폼 Settings / DataStore**에 저장).
- 내용: 앱 소개 1~2문장 + "이 앱의 모든 설명은 AI가 생성합니다. 오류가 있을 수 있으니 제보해 주세요." + 시작 버튼.

### 3-8. Settings
- 로직 없음(순수 UI + 시스템 API). 구성:
  - **외관**: 시스템/라이트/다크(멀티플랫폼 Settings 저장 → 루트 테마에 반영).
  - **앱 정보**: 버전·빌드 번호(`expect`/`actual`로 플랫폼 값 조회).
  - **지원**: 문의·앱 평가·오류 제보.
  - **법적 고지**: 오픈소스 라이선스(폰트 OFL) · AI 생성 고지 · 개인정보 처리방침(외부 URL).
- 앱 평가: Android=In-App Review, iOS=StoreKit `requestReview`(플랫폼별 `expect`/`actual`).

✅ **Phase 3 완료 조건**: 양 플랫폼에서 모든 탭 렌더링, 검색→결과 플로우 동작, 오류 상태 분기(한도 초과 포함), 제보 mailto 동작, 외관 전환 동작. **수동 재조회 코드 없음**(반응형으로 갱신 확인).

---

## Phase 4 — 통합 및 마무리

### 4-1. 오류 처리 통합
- Phase 3-3의 오류 분기가 실네트워크에서 정상 동작 확인. 오프라인/타임아웃/429 각각 재현.

### 4-2. 접근성
- 아이콘·이미지에 contentDescription. Dynamic Type/글꼴 크기 스케일 대응. 다크·라이트 전 화면 렌더링 검증(양 플랫폼).

### 4-3. 번들 DB 확장
- 초기 소량 → 650개로 확장(iOS `terms.json` 자산 재사용, keyword/aliases 변경 금지).
- 각 용어 `category` 필수(6개 집합), `aliases` 최소 1개. 확장 후 JSON 유효성 + `aliases`/`category` 검증(생성·검증 파이프라인은 `docs/db-expand/`, 작성 예정).

### 4-4. 앱 아이콘
- **Android**: adaptive icon(전경/배경 레이어), `mipmap` + `ic_launcher` 배선.
- **iOS**: `AppIcon.appiconset`(1024 single-size, 풀블리드·투명 금지).
- 디자인 원본(딥 그린/크림 방향)은 `docs/design/icon/`(작성 예정) 정본. 양 플랫폼에서 소형 사이즈 가독성 검증.

### 4-5. 스플래시 / 런치
- **Android**: Splash Screen API(브랜드 배경 + 로고). "Loading" 텍스트·스피너 금지.
- **iOS**: Launch Screen(브랜드 배경 풀블리드 + 중앙 로고, safe area 존중).
- 라이트·다크 동일 브랜드 색(런치는 단일 컬러). 첫 화면 전환 시 깜빡임 없음.

✅ **Phase 4 완료 조건**: Phase 1–3 기능이 Android·iOS 통합 동작, 오류 처리 완비, 아이콘·스플래시 적용 및 가독성 검증 완료.
