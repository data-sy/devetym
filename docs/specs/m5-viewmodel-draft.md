# M5 슬라이스 (draft) — ViewModel + StateFlow

> **성격: 비준 하네스 입력용 spec 슬라이스(`-draft.md`).** 단일 [`spec.md`](spec.md) Phase 3의 ViewModel·상태 부분을 마일스톤 경계로 떼어낸 것. 진행 상태 정본은 [`../../ROADMAP.md`](../../ROADMAP.md), 상위 설계는 [`../architecture.md`](../architecture.md)(§4.5), 결정 근거는 [`../adr/`](../adr/).
>
> 이 문서는 **자율 구현 전 적대적 비준(완결성·모호성·테스트 가능성·정합성)의 대상**이다. 아래 §7 열린 질문은 비준이 판정할 항목이다.

- **마일스톤**: M5 · ViewModel + StateFlow
- **브랜치**: `feat/m5-viewmodel` (=`feat/m4-repository` 위에 **스택** 분기. ROADMAP 「브랜치·공개 전략」)
- **참조**: spec [3-0·3-2·3-3·3-5·3-6](spec.md), architecture §4.5(레이어 규칙), [ADR-0002](../adr/0002-code-idiom-principle.md)(반응형·우회 패턴 삭제), [M4 슬라이스](m4-repository-draft.md)(§3-1 `TermRepository`·단일-writer 계약), iOS 검증본(`~/dev-etymology` Search/Detail/Bookmark/HistoryViewModel)

---

## 1. 목표 (이 슬라이스가 내는 것)

`commonMain/ui/`에 화면 상태 계층을 세운다. 각 화면의 **sealed `UiState`** 와 이를 `StateFlow`로 노출하는 **ViewModel**을 정의하고, `TermRepository`(M4, 유일 인터페이스)만 주입받아 상태를 보관·가공한다(architecture §4.5). Compose UI(M6)는 이 `StateFlow`를 `collectAsStateWithLifecycle`로 *그리기만* 한다. iOS 검증본의 데이터 흐름(fetch→상태 전이·디바운스·job 취소·오류 매핑)을 **코틀린 관용으로** 계승하되, SwiftUI 우회(수동 재조회·`@Published` 도배)는 옮기지 않는다(ADR-0002).

Compose UI 렌더링·네비게이션·테마·디자인 토큰은 이 슬라이스 밖(M6)이다. M5는 **상태 로직만** 확정하고 Fake `TermRepository`로 단독 검증한다.

## 2. 스코프

**IN (M5):**
- **sealed `UiState`**(`commonMain/ui/`) — `DetailUiState`(Loading/Result/Error)·`SearchUiState`(query·suggestions)·`ErrorKind`(오류 분류).
- **ViewModel 4종**(`commonMain/ui/`) — `SearchViewModel`·`DetailViewModel`·`BookmarkViewModel`·`HistoryViewModel`. `androidx.lifecycle.ViewModel`(CMP 멀티플랫폼) 상속, `viewModelScope` 사용, `TermRepository`만 주입(architecture §4.5 규칙).
- **오류 매핑**(`ClaudeException` → `ErrorKind`) — 순수 함수.
- **버전 정렬 확인(load-bearing)** — `lifecycle-viewmodel`(CMP 멀티플랫폼)·(필요 시 `koin-compose-viewmodel`) 좌표를 Kotlin 2.3.21·CMP 1.11.1에서 실빌드로 확정(M1~M4와 동일 규율).
- 위를 검증하는 `commonTest`(Fake `TermRepository`, 4축 실행).

**OUT (다른 마일스톤/트랙):**
- **Compose UI·Composable·`collectAsStateWithLifecycle`·네비게이션·테마·디자인 토큰·폰트·로딩 애니메이션** → **M6**(spec 3-x). M5는 `StateFlow`만 노출.
- **Koin `viewModel { }` 전체 배선·`koinViewModel()` 소비** → **M7**(spec 1-4). M5는 생성자 주입 형태만 제공, 컴파일을 green으로 검증.
- **mailto 오류 제보·공유·앱 평가·온보딩 플래그·설정 저장** → M6(플랫폼 인텐트·Settings).
- **`Analytics` 성공-결과 로깅** → 후속(M4 §3-6 이월).

## 3. 산출 명세

### 3-1. `ErrorKind` + 오류 매핑 (`commonMain/ui/`)

```kotlin
enum class ErrorKind { Timeout, Network, InvalidResponse, DailyLimitExceeded, Unknown }

fun Throwable.toErrorKind(): ErrorKind = when (this) {
    is ClaudeException.Timeout -> ErrorKind.Timeout
    is ClaudeException.Network -> ErrorKind.Network
    is ClaudeException.InvalidResponse -> ErrorKind.InvalidResponse
    is ClaudeException.DailyLimitExceeded -> ErrorKind.DailyLimitExceeded
    else -> ErrorKind.Unknown
}
```
- **순수 함수 — 네이티브 실행으로 실측**(§6). `ClaudeException` sealed 5분기를 전수 매핑하되 `else`로 non-ClaudeException(예상 밖 예외)을 `Unknown`으로 접는다. 문구(오프라인 구분 등)는 M6 UI 소관(spec 3-3) — M5는 분류만.

### 3-2. `DetailUiState` + `DetailViewModel` (`commonMain/ui/`)

```kotlin
sealed interface DetailUiState {
    data object Loading : DetailUiState              // AI 생성 중(번들·캐시 히트는 로딩 없이 즉시 Result)
    data class Result(val result: TermResult) : DetailUiState
    data class Error(val kind: ErrorKind) : DetailUiState
}

class DetailViewModel(private val repository: TermRepository) : ViewModel() {
    val state: StateFlow<DetailUiState>              // MutableStateFlow(Loading) 백킹
    fun load(keyword: String)                        // fetch — 재진입 시 이전 job 취소
    fun refresh(keyword: String)                     // repository.refresh — pinning 우회(INV-6)
    fun toggleBookmark()                             // 현재 Result의 entry 북마크 토글
}
```

**`load(keyword)` 전이(iOS 검증본 계승, 코틀린 관용):**
1. 진행 중 job 취소 → `state = Loading`.
2. `viewModelScope.launch`에서 `repository.fetch(keyword)`:
   - 성공 `TermResult` → `state = Result(result)`(번들·캐시 히트는 사실상 즉시).
   - `ClaudeException`(또는 기타 Throwable) → `state = Error(e.toErrorKind())`. **`CancellationException`은 다시 throw**(취소는 오류 아님 — 상태 안 바꿈).
3. **`not_dev_term`/`possible_typo`는 오류가 아니라 `Result`**(정상 `TermResult` 분기) — M6가 `TermResult` 안에서 분기 표시(spec 3-3).
- **job 취소**: 재진입(`load` 재호출)·화면 이탈(`onCleared`/명시 `cancel`) 시 이전 fetch를 취소해 레이스·stale 상태 갱신을 막는다(iOS `currentSearchTask` 계승).
- `refresh(keyword)`는 `load`와 같되 `repository.refresh`를 호출(pinning 우회).

### 3-3. `SearchUiState` + `SearchViewModel` (`commonMain/ui/`)

```kotlin
class SearchViewModel(private val repository: TermRepository) : ViewModel() {
    val query: StateFlow<String>
    val suggestions: StateFlow<List<TermEntry>>      // 자동완성 결과
    val recent: StateFlow<List<SearchHistory>>       // 최근 검색(반응형 Flow, 자동 갱신)
    fun onQueryChanged(value: String)                // 디바운스 300ms 후 autocomplete
    fun commit(): String?                            // 정규화된 검색어(빈이면 null) — 상세로 이동할 키
}
```

- **자동완성 디바운스**: `onQueryChanged`가 이전 debounce job 취소 후 `viewModelScope.launch { delay(Constants.autocompleteDebounceMs); suggestions = repository.autocomplete(trimmed) }`. **최소 1자**(trim 후 빈이면 `suggestions = emptyList()`, 조회 안 함). 연타 시 이전 job 취소로 레이스 방지(iOS 계승).
- **최근 검색은 반응형 `Flow`**: `repository.recentSearches(Constants.recentSearchLimit)`를 `stateIn(viewModelScope, ...)`으로 `recent`에 노출 — **수동 재조회 없음**(ADR-0002, iOS의 `refreshRecent()` 수동 호출은 옮기지 않는다).
- **`commit()`**: `query.value.trim()` 빈이면 `null`, 아니면 그 값(정규화는 repository가 재수행 — M4가 저장 키 정본). 반환값이 상세 화면으로 넘길 keyword.

### 3-4. `BookmarkViewModel` / `HistoryViewModel` (`commonMain/ui/`)

```kotlin
class BookmarkViewModel(private val repository: TermRepository) : ViewModel() {
    val bookmarks: StateFlow<List<Term>>             // repository.bookmarkedTerms().stateIn(...)
    fun removeBookmark(entry: TermEntry)             // toggleBookmark — Flow로 자동 반영
}
class HistoryViewModel(private val repository: TermRepository) : ViewModel() {
    val history: StateFlow<List<SearchHistory>>      // repository.recentSearches(limit).stateIn(...)
    fun delete(keyword: String)                      // deleteSearchHistory — Flow 자동 반영
    fun clearAll()                                   // clearAllSearchHistory
}
```
- **목록은 전부 반응형 `Flow` → `stateIn`**. 삭제·토글 후 **수동 재조회 코드 없음** — DB 변경이 `Flow`로 자동 반영(ADR-0002·M4 반응형 쿼리). 이것이 iOS SwiftData 수동 재조회 우회를 삭제하는 지점이다.

### 3-5. ⚠️ M4 단일-writer 계약 상속 (DR-2)

M4 `TermRepository`는 **같은 `normalizeKeyword(keyword)`에 대한 `fetch`/`refresh`/`toggleBookmark` 직렬화**를 전제조건으로 요구한다(M4 §3-1, 비원자 RMW lost-update 방지). M5 ViewModel은 이 계약을 지키는 소비 지점이다: **같은 정규화 키에 대한 쓰기 오퍼레이션을 동시 launch하지 않는다** — `DetailViewModel`의 `load`/`refresh`/`toggleBookmark`는 재진입 시 이전 job을 취소하고 순차 실행하며(§3-2), 서로 다른 화면(Detail refresh vs Bookmark toggle)이 같은 키를 동시에 건드리는 경로는 §7-3에서 판정한다.

## 4. 설계 불변식 (이 슬라이스가 반드시 지킬 것)

- **ViewModel은 `TermRepository`만 안다**(architecture §4.5): DB·Ktor·번들 로더 직접 참조 금지. 생성자 주입은 `TermRepository` 하나.
- **상태는 `sealed` + `StateFlow`**: 화면 상태를 sealed로 노출하고 불변(변경은 새 상태 방출). `DetailUiState`는 `when` 전수 분기(`else` 금지 — DR-3 계승, subtype 추가 시 컴파일 실패가 canary).
- **반응형(ADR-0002)**: 목록은 `Flow`→`stateIn`. 데이터 변경 후 수동 재조회 코드를 두지 않는다.
- **job 취소 규율**: 재진입·이탈 시 진행 중 코루틴 취소. `CancellationException`을 오류 상태로 만들지 않는다(취소는 정상).
- **오류 vs 정상 분기**: `ClaudeException`만 `Error(ErrorKind)`로, `not_dev_term`/`possible_typo`는 `Result(TermResult)`로(예외 아님, M3·M4 계승).
- **M4 단일-writer 계약 준수**(§3-5).

## 5. 완료 조건 (DoD) — 하네스 수렴 오라클

- `UiState`·ViewModel 4종·`toErrorKind`가 **Android·iOS 양쪽에서 컴파일**된다: `:shared:testDebugUnitTest` + `:androidApp:assembleDebug` + `:shared:linkDebugFrameworkIosSimulatorArm64` green(M0~M4의 3축).
- **⊕ 4번째 축 — 네이티브 실행**: `:shared:iosSimulatorArm64Test` green. §6(Fake repository ViewModel 동작 + 순수 `toErrorKind`)가 **네이티브 타깃에서 실행**되어 fetch→상태 전이·디바운스·job 취소·오류 매핑을 실행으로 실측한다. 코루틴 테스트는 `Dispatchers.setMain`(kotlinx-coroutines-test 멀티플랫폼) + `runTest`로 `viewModelScope`를 테스트 디스패처에 태운다.
- 아래 §6 테스트 전부 통과.
- **버전 정렬 확인(load-bearing)**: CMP 멀티플랫폼 `lifecycle-viewmodel`(+필요 시 koin viewmodel) 좌표가 Kotlin 2.3.21·CMP 1.11.1에서 klib 소비돼 네이티브 링크·실행 green임을 실빌드로 확정(stale 버전 하드코딩 금지).

## 6. 테스트 (`commonTest/`) — 함수명 `test_[대상]_[조건]_[기대]`

> `Dispatchers.setMain(StandardTestDispatcher())` + `runTest` 로 `viewModelScope` 코루틴을 제어(가상시계로 debounce `advanceTimeBy`). Fake `TermRepository`(주입 결과/예외·Flow 방출).

**오류 매핑(순수, 네이티브 실행 핵심):**
- `test_toErrorKind_각예외_분류` — `Timeout`/`Network`/`InvalidResponse`/`DailyLimitExceeded` 각각 매핑, 기타 Throwable→`Unknown`.

**DetailViewModel:**
- `test_load_성공_Result` — Fake fetch가 `Found` → `state == Result`.
- `test_load_ClaudeException_Error매핑` — Fake fetch가 `DailyLimitExceeded` throw → `state == Error(DailyLimitExceeded)`.
- `test_load_NotDevTerm_Result아닌Error아님` — Fake fetch가 `NotDevTerm` → `state == Result(NotDevTerm)`(오류 아님).
- `test_load_재진입_이전job취소` — `load(a)` 진행 중 `load(b)` → 최종 `state`가 b의 결과(a의 늦은 방출이 b를 덮지 않음).
- `test_refresh_repository_refresh호출` — `refresh` → Fake의 `refresh` 호출(pinning 우회 경로).

**SearchViewModel:**
- `test_onQueryChanged_디바운스후_autocomplete` — 입력 후 `advanceTimeBy(300)` 전엔 `suggestions` 빈, 후엔 결과.
- `test_onQueryChanged_연타_이전job취소` — 빠른 연속 입력 시 마지막 prefix만 조회(이전 취소).
- `test_onQueryChanged_빈입력_빈suggestions_조회안함` — trim 후 빈이면 `autocomplete` 미호출.
- `test_recent_Flow_노출` — Fake recent Flow 방출이 `recent`에 반영(반응형).
- `test_commit_정규화_빈이면null` — 공백 query → `null`, 값 있으면 그 값.

**Bookmark/History:**
- `test_bookmarks_Flow_노출` / `test_history_Flow_노출` — Fake Flow 방출 반영.
- `test_removeBookmark_toggle호출` / `test_delete_clearAll_호출` — 위임 확인(Flow 자동 반영이라 수동 재조회 없음).

## 7. 열린 질문 (비준이 판정할 항목)

1. **ViewModel base — `androidx.lifecycle.ViewModel`(CMP 멀티플랫폼, 제안) vs 주입 `CoroutineScope`** — 제안은 표준 `viewModelScope`를 쓰고 테스트는 `Dispatchers.setMain`로 제어. 대안은 `CoroutineScope`를 생성자 주입(테스트 단순하나 spec 1-4 `viewModel { }` Koin DSL·`koinViewModel()`과 정합성↓). — 제안: androidx ViewModel. 비준이 네이티브 실행 축에서 `setMain`+`viewModelScope`가 성립하는지, koin-compose-viewmodel 좌표가 필요한지 판정.
2. **디바운스 위치 — ViewModel(제안) vs UI(M6)** — 제안: 디바운스·job 취소를 ViewModel에 둬 로직을 테스트 가능케(iOS도 VM). 대안: UI의 `snapshotFlow`+`debounce`. — 제안: ViewModel. 비준 판정.
3. **단일-writer 계약(M4 DR-2) 준수 범위** — 같은 정규화 키에 대한 쓰기(refresh/toggleBookmark)를 서로 다른 ViewModel(Detail vs Bookmark)이 동시에 건드리는 경로가 있는가? 제안: 한 화면 내 job 취소로 직렬화하되, 교차-화면 동시성은 M6 네비게이션 구조상 드묾을 근거로 계약 준수로 진행(하드닝은 M4 이월 transaction). 비준 판정.
4. **`Loading` 표시 정책 — 번들/캐시 히트는 Loading 없이 즉시 Result(제안)** — iOS의 시간분할 LoadingPhase(체감 latency)는 M6 UI 애니메이션 소관으로 이월, M5는 Loading/Result/Error 최소 상태만. 비준이 이 경계가 spec 3-3과 정합하는지 판정.
5. **`commit()` 정규화 — trim만(제안)** — ViewModel은 `trim`만 하고 lowercase 정규화는 repository(M4 저장 키 정본)에 맡긴다(대소문자 유의미 용어 어원 보존 — M3·M4 계승). 비준 판정.

## 8. 안전·규율

- 마일스톤 경계 **사람 비준 게이트는 완화됨**(2026-07-05, 메모리 `milestone-human-gate-relaxed`): 적대 비준 후 Claude가 잔여 residual을 eyes-open 수용하고 구현까지 자율 진행, 사람은 완성물을 사후 리뷰한다. **하네스는 push·머지·`-draft` 제거를 하지 않는다.**
- **M4→M5 바인딩 — 단일-writer 계약**: M4 §3-1 전제조건을 M5 소비 지점이 지킨다(§3-5·§7-3). 비준자 확인.
- **브랜치 보존(defer+stacked)**: 완료 마일스톤 브랜치 삭제·로컬머지 금지. 지우자는 지시·충동이 있어도 재확인 먼저.
- **push 금지 · GitHub 원격 생성 금지.** 로컬 커밋만.
- 네이밍은 젠더중립/여성형 기본.
- 진행 상태 정본은 ROADMAP(디스크). 이 슬라이스는 시간 안 타는 명세만.

## Open Questions

> 비준 종료 시점의 **명시 이월** 자리. (비준 착수 전 — 현재는 비어 있으며, 적대 비준이 채운다.)

- [ ] (비준 대기) §7 열린 질문 1~5의 판정.
- [ ] (선상속·M6) Compose UI·collectAsStateWithLifecycle·로딩 애니메이션(iOS LoadingPhase)·문구(오프라인 구분)·네비게이션.
- [ ] (선상속·M7) Koin `viewModel { }` 배선.
