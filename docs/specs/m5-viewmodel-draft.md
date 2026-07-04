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
    is ClaudeException -> when (this) {              // sealed 전수 분기 — 내부 when엔 else 없음(DR-3 canary)
        is ClaudeException.Timeout -> ErrorKind.Timeout
        is ClaudeException.Network -> ErrorKind.Network
        is ClaudeException.InvalidResponse -> ErrorKind.InvalidResponse
        is ClaudeException.DailyLimitExceeded -> ErrorKind.DailyLimitExceeded
    }
    else -> ErrorKind.Unknown                        // 비-ClaudeException(예상 밖 예외)만 접는다
}
```
- **순수 함수 — 네이티브 실행으로 실측**(§6). 바깥 `when`은 `ClaudeException`(sealed, 현재 4분기)만 안쪽 `when`으로 전수 매핑하고 — **안쪽 `when`엔 `else`가 없어** 하위타입 추가 시 컴파일 canary가 발화한다(DR-3·§4 「`else` 금지」). `else`는 비-`ClaudeException`(예상 밖 예외)만 `Unknown`으로 접는다 — 장래 추가되는 `ClaudeException` 하위타입이 `else`로 조용히 `Unknown` 강등되지 않는다. 문구(오프라인 구분 등)는 M6 UI 소관(spec 3-3) — M5는 분류만.

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
- **최근 검색은 반응형 `Flow`**: `repository.recentSearches(Constants.recentSearchLimit)`를 `stateIn(viewModelScope, started = <이월>, initialValue = emptyList())`으로 `recent`에 노출 — **수동 재조회 없음**(ADR-0002, iOS의 `refreshRecent()` 수동 호출은 옮기지 않는다). `SharingStarted` 확정은 이월(§6 반응형 관측 규약 — 오라클은 그 선택과 독립하게 discriminating).
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

### 3-5. ⚠️ M4 단일-writer 계약 상속 (DR-2) — 정규화 키 Mutex(제안 메커니즘) · 강제는 이월

M4 `TermRepository`는 **같은 `normalizeKeyword(keyword)`에 대한 `fetch`/`refresh`/`toggleBookmark` 직렬화**를 전제조건으로 요구한다(M4 §3-1·§3-4, 비원자 RMW lost-update 방지). 한 ViewModel 내부의 job 취소(§3-2)는 **그 VM 안에서만** 순차성을 준다 — 서로 다른 화면의 두 살아있는 ViewModel은 각자 독립 `viewModelScope`로 launch해 **공유 직렬화 지점이 없다**. 교차-화면 경로가 실재한다: `DetailViewModel.refresh("react")`가 `api.generate` 네트워크 왕복(수 초) 동안 RMW 창을 연 사이 `BookmarkViewModel.removeBookmark(entry(keyword="react"))`가 동기 RMW로 `isBookmarked=0`을 기록하면, refresh가 반환되며 토글 이전 스냅샷을 upsert에 재주입해 **방금 해제한 북마크·`seenAt`을 조용히 되돌린다**(에러 없음). M4 §3-4는 refresh RMW 창이 네트워크 왕복 전체라 좁지 않다며 'rareness' 정당화를 **명시 철회**하고 방어를 계약(직렬화)으로 못박았다.

**M5는 M4 단일-writer 계약을 계승하고, 그 하드닝의 제안 메커니즘으로 정규화 키 `Mutex`를 명세한다 — 다만 이 마일스톤에서 그 강제를 실측·증명한다고 자칭하지 않는다.** M4 Open Questions에 이월돼 있던 단일-writer 하드닝의 **구체 메커니즘 후보**는 이렇다: `TermRepositoryImpl`의 세 쓰기 오퍼레이션(`fetch`/`refresh`/`toggleBookmark`)은 각자 맨 앞에서 계산하는 `val key = normalizeKeyword(keyword)`로 **키잉된 `Mutex`를 오퍼레이션 전 구간(refresh의 네트워크 왕복 포함) 동안 보유**한 뒤 RMW한다. 같은 정규화 키의 두 번째 쓰기는 첫 쓰기가 끝날 때까지 suspend된다. 직렬화 단위는 raw 입력이 아니라 **정규화 저장 키**다(M4 AD-1 — `"React"`/`"react"`/`"REACT"`가 같은 로우에 RMW하므로 raw 뮤텍스는 lost-update를 놓친다). 잠금 맵은 정규화 키→`Mutex`이며, 서로 다른 키는 병렬을 유지한다(전역 잠금 아님). 맵 자체 접근의 원자성은 짧은 동기 잠금(맵 조회·삽입만 감싸는 별도 잠금 또는 동시성 맵)으로 확보한다.

**⚠️ 이 메커니즘의 강제는 M5에서 검증되지 않는 두 전제에 의존하므로, DR-2는 '해소'가 아니라 이월된다(§7-3·Open Questions):**
- **(전제 a) 공유 인스턴스 — 미검증·미게이트.** 잠금 맵은 `TermRepositoryImpl` 인스턴스 필드이므로 **직렬화는 모든 ViewModel이 같은 repository 인스턴스를 공유할 때만 성립**한다. 이는 M7 Koin 배선이 `TermRepository`를 `single`로 제공함을 요구하나(M5는 생성자 주입 형태만 제공 — §2 OUT), M5에는 이를 강제·검증하는 게이트가 없다. M7이 factory/화면-scoped로 배선하면 각 VM이 별개 impl·별개 잠금 맵을 받아 Mutex가 아무것도 직렬화하지 않는다. 이 single-scope 전제는 **게이트 없는 미검증 이월 전제**다.
- **(전제 b) 실제 동시성 노출 — 단일스레드 오라클로는 실측 불가.** §6 교차-VM 오라클은 단일스레드 test dispatcher(`Dispatchers.setMain`) 위에서 도는데, 그 위에선 `toggleBookmark`의 동기 RMW가 refresh 연속(continuation)에 대해 원자적으로 완료되고 M4 impl이 보존 필드를 네트워크 前/後 어느 쪽에서 재조회하든 최종값이 correct하게 수렴한다 → **Mutex 유무를 구분하지 못한다**(§6 참조). 따라서 그 오라클의 green은 Mutex의 작동을 증명하지 못한다.

> 이 메커니즘은 M4 read-side 순서(3단 캐시)를 바꾸지 않고 write-side RMW만 감싼다. ViewModel은 여전히 `TermRepository` 하나만 주입받으므로(§4·architecture §4.5) 직렬화가 인터페이스 뒤에 숨어 VM은 알 필요가 없다. §6의 교차-VM 테스트는 실제 `TermRepositoryImpl`(Fake 협력자 + 지연 가능한 generator)을 두 VM에 **손으로 배선해 공유**시키는 배선/무크래시 **스모크**다 — 실제 DI 스코프 결정(전제 a)과 무관하게 공유를 강제하고, 단일스레드 dispatcher(전제 b)라 Mutex 유무를 구분하지 못하므로 **직렬화 강제의 증명이 아니다.** DR-2 강제의 실측은 이월된다.

## 4. 설계 불변식 (이 슬라이스가 반드시 지킬 것)

- **ViewModel은 `TermRepository`만 안다**(architecture §4.5): DB·Ktor·번들 로더 직접 참조 금지. 생성자 주입은 `TermRepository` 하나.
- **상태는 `sealed` + `StateFlow`**: 화면 상태를 sealed로 노출하고 불변(변경은 새 상태 방출). `DetailUiState`는 `when` 전수 분기(`else` 금지 — DR-3 계승, subtype 추가 시 컴파일 실패가 canary).
- **반응형(ADR-0002)**: 목록은 `Flow`→`stateIn`. 데이터 변경 후 수동 재조회 코드를 두지 않는다.
- **job 취소 규율**: 재진입·이탈 시 진행 중 코루틴 취소. `CancellationException`을 오류 상태로 만들지 않는다(취소는 정상).
- **오류 vs 정상 분기**: `ClaudeException`만 `Error(ErrorKind)`로, `not_dev_term`/`possible_typo`는 `Result(TermResult)`로(예외 아님, M3·M4 계승).
- **M4 단일-writer 계약 계승**(§3-5): 정규화 키 키잉 `Mutex`를 교차-VM 동시 쓰기 직렬화의 **제안 메커니즘**으로 둔다('rareness' 근거는 폐기). 다만 그 강제는 M7 single-scope 배선(미게이트)과 실제 동시성 노출에 의존해 **M5에서 증명되지 않으며 이월**된다(§7-3·Open Questions) — M5는 계약을 '강제 완료'로 자칭하지 않는다.

## 5. 완료 조건 (DoD) — 하네스 수렴 오라클

- `UiState`·ViewModel 4종·`toErrorKind`가 **Android·iOS 양쪽에서 컴파일**된다: `:shared:testDebugUnitTest` + `:androidApp:assembleDebug` + `:shared:linkDebugFrameworkIosSimulatorArm64` green(M0~M4의 3축).
- **⊕ 4번째 축 — 네이티브 실행**: `:shared:iosSimulatorArm64Test` green. §6(Fake repository ViewModel 동작 + 순수 `toErrorKind`)가 **네이티브 타깃에서 실행**되어 fetch→상태 전이·디바운스·job 취소·오류 매핑을 실행으로 실측한다. 코루틴 테스트는 `Dispatchers.setMain`(kotlinx-coroutines-test 멀티플랫폼) + `runTest`로 `viewModelScope`를 테스트 디스패처에 태운다.
- 아래 §6 테스트 전부 통과.
- **버전 정렬 확인(load-bearing)**: CMP 멀티플랫폼 `lifecycle-viewmodel`(+필요 시 koin viewmodel) 좌표가 Kotlin 2.3.21·CMP 1.11.1에서 klib 소비돼 네이티브 링크·실행 green임을 실빌드로 확정(stale 버전 하드코딩 금지).

## 6. 테스트 (`commonTest/`) — 함수명 `test_[대상]_[조건]_[기대]`

> `Dispatchers.setMain(StandardTestDispatcher())` + `runTest` 로 `viewModelScope` 코루틴을 제어(가상시계로 debounce `advanceTimeBy`). Fake `TermRepository`(주입 결과/예외·Flow 방출).
>
> **⚠️ 반응형 관측 규약(거짓 green 방지).** `stateIn`으로 노출된 목록(`recent`/`bookmarks`/`history`)의 반응형 테스트는 **`.value`를 직접 읽어 단정하지 않는다** — `SharingStarted`가 lazy(`WhileSubscribed`)면 활성 구독자가 없을 때 upstream이 수집되지 않아, upstream 배선이 틀려도(잘못된 Flow 연결·`map` 누락) 테스트가 `initialValue`(빈 리스트)를 읽고 조용히 green이 된다. 그러므로 반응형 테스트는 **`backgroundScope.launch { vm.recent.collect { latest = it } }`로 활성 collector를 건 뒤 Fake upstream을 방출하고 `advanceUntilIdle()`로 스케줄러를 소진한 다음** 수집된 값이 방출값과 같은지 단정한다(활성 구독이 있으면 `SharingStarted` 선택과 무관하게 upstream이 실제로 수집된다). `initialValue`는 `emptyList()`로 두고, `SharingStarted`(Eagerly vs WhileSubscribed) 확정은 배터리·M6 구독 수명 소관으로 이월하되 — 위 규약이 그 선택과 독립하게 오라클을 discriminating으로 만든다.

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
- `test_recent_Flow_노출` — Fake recent Flow 방출이 `recent`에 반영(반응형). **활성 collector 필수**(아래 반응형 관측 규약).
- `test_commit_정규화_빈이면null` — 공백 query → `null`, 값 있으면 그 값.

**Bookmark/History:**
- `test_bookmarks_Flow_노출` / `test_history_Flow_노출` — Fake Flow 방출 반영. **활성 collector 필수**(아래 반응형 관측 규약).
- `test_removeBookmark_toggle호출` / `test_delete_clearAll_호출` — 위임 확인(Flow 자동 반영이라 수동 재조회 없음).

**교차-VM 배선 스모크(§3-5, 정규화 키 Mutex — 비-discriminating):**
> ⚠️ **이 스모크는 DR-2 직렬화 강제를 증명하지 않는다.** Fake 인터페이스로는 Mutex를 실측할 수 없어 실 `TermRepositoryImpl`(Fake `LocalTermStore`·지연 가능한 Fake generator 주입)을 두 ViewModel에 **손으로 배선해 공유**시키지만, (a) 실제 DI 스코프(M7 single 여부)와 무관하게 공유를 강제하므로 프로덕션 공유 전제를 검증하지 못하고, (b) 단일스레드 test dispatcher에선 최종값이 Mutex 유무와 무관하게 correct로 수렴해 Mutex 유무를 구분하지 못한다(§3-5 전제 a·b). 따라서 아래 케이스는 **공유-impl 경로가 크래시 없이 일관된 최종 상태로 수렴함을 확인하는 스모크**일 뿐, 'Mutex 없으면 실패'하는 discriminating 오라클이 아니다. DR-2 강제의 실측은 이월(Open Questions).
- `test_교차VM_refresh중_removeBookmark_최종일관` — 공유 `TermRepositoryImpl`(generator의 `generate`를 suspend로 지연) 위에 `DetailViewModel`·`BookmarkViewModel`. `detail.refresh("react")` 뒤 `bookmark.removeBookmark(entry(keyword="React"))`(원형 대소문자 — 같은 정규화 로우) 호출 → 지연 해제 → **최종 저장 `term[react].isBookmarked == false`**·`seenAt` 동형. (스모크: 단일스레드에선 Mutex 없이도 green이므로 이 green은 직렬화 강제의 증거가 아니다.)
- `test_교차VM_다른키_비간섭` — `refresh("react")` 진행 중 `refresh("kotlin")`이 진행돼 두 키가 서로의 최종 상태를 오염시키지 않음을 확인(키별 독립. 병렬성 자체는 단일스레드 dispatcher에서 실측 대상이 아님).

## 7. 열린 질문 (비준이 판정할 항목)

1. **ViewModel base — `androidx.lifecycle.ViewModel`(CMP 멀티플랫폼, 제안) vs 주입 `CoroutineScope`** — 제안은 표준 `viewModelScope`를 쓰고 테스트는 `Dispatchers.setMain`로 제어. 대안은 `CoroutineScope`를 생성자 주입(테스트 단순하나 spec 1-4 `viewModel { }` Koin DSL·`koinViewModel()`과 정합성↓). — 제안: androidx ViewModel. 비준이 네이티브 실행 축에서 `setMain`+`viewModelScope`가 성립하는지, koin-compose-viewmodel 좌표가 필요한지 판정.
2. **디바운스 위치 — ViewModel(제안) vs UI(M6)** — 제안: 디바운스·job 취소를 ViewModel에 둬 로직을 테스트 가능케(iOS도 VM). 대안: UI의 `snapshotFlow`+`debounce`. — 제안: ViewModel. 비준 판정.
3. **단일-writer 계약(M4 DR-2) 준수 범위 — 미해소·이월**: 같은 정규화 키에 대한 쓰기(refresh/toggleBookmark)를 서로 다른 ViewModel(Detail vs Bookmark)이 동시에 건드리는 교차-화면 경로가 **실재**한다(두 VM이 독립 `viewModelScope`라 VM 내 job 취소로는 못 막음). **'M6 네비게이션 구조상 드묾' 근거는 폐기한다** — M4 §3-4가 rareness 정당화를 이미 명시 철회했으므로, 계약을 '준수'로 자칭할 수 없다. §3-5는 하드닝의 **제안 메커니즘**(정규화 키 `Mutex`)을 명세하되, 그 강제는 (a) M7이 `TermRepository`를 `single`로 배선하는 미검증·미게이트 전제와 (b) 단일스레드 오라클이 Mutex 유무를 구분 못 하는 한계에 걸려 **M5에서 증명되지 않는다** → 라운드 2의 '해소' 자칭은 철회하고 강제 실측을 이월한다(Open Questions). 비준이 이 이월 경계가 정직한지 판정.
4. **`Loading` 표시 정책 — 번들/캐시 히트는 Loading 없이 즉시 Result(제안)** — iOS의 시간분할 LoadingPhase(체감 latency)는 M6 UI 애니메이션 소관으로 이월, M5는 Loading/Result/Error 최소 상태만. 비준이 이 경계가 spec 3-3과 정합하는지 판정.
5. **`commit()` 정규화 — trim만(제안)** — ViewModel은 `trim`만 하고 lowercase 정규화는 repository(M4 저장 키 정본)에 맡긴다(대소문자 유의미 용어 어원 보존 — M3·M4 계승). 비준 판정.

## 8. 안전·규율

- 마일스톤 경계 **사람 비준 게이트는 완화됨**(2026-07-05, 메모리 `milestone-human-gate-relaxed`): 적대 비준 후 Claude가 잔여 residual을 eyes-open 수용하고 구현까지 자율 진행, 사람은 완성물을 사후 리뷰한다. **하네스는 push·머지·`-draft` 제거를 하지 않는다.**
- **M4→M5 바인딩 — 단일-writer 계약**: M4 §3-1 전제조건을 M5가 계승하고 제안 메커니즘(§3-5 정규화 키 Mutex)을 명세하되, 강제 실측은 이월한다(§3-5 전제 a·b·§7-3·Open Questions). 비준자 확인.
- **브랜치 보존(defer+stacked)**: 완료 마일스톤 브랜치 삭제·로컬머지 금지. 지우자는 지시·충동이 있어도 재확인 먼저.
- **push 금지 · GitHub 원격 생성 금지.** 로컬 커밋만.
- 네이밍은 젠더중립/여성형 기본.
- 진행 상태 정본은 ROADMAP(디스크). 이 슬라이스는 시간 안 타는 명세만.

## Open Questions

> 비준 종료 시점의 **명시 이월** 자리. (비준 착수 전 — 현재는 비어 있으며, 적대 비준이 채운다.)

- [ ] (비준 대기) §7 열린 질문 1~5의 판정.
- [ ] (선상속·DR-2 강제 미검증·M7 게이트) 정규화 키 `Mutex`(§3-5)는 하드닝의 제안 메커니즘일 뿐, 강제는 **M5에서 증명되지 않는다**: (a) `TermRepository`=`single` 배선은 M7 미검증·미게이트 전제이고(factory/화면-scoped 배선 시 Mutex가 아무것도 직렬화 못 함), (b) §6 교차-VM 테스트는 단일스레드·손배선 공유라 Mutex 유무를 구분 못 하는 비-discriminating 스모크다. 실제 강제 실측은 (i) M7에 두 VM이 동일 인스턴스를 받는지 확인하는 배선 게이트를 두거나 (ii) 다중스레드 실측 또는 SQLDelight `transaction` 원자화로 이월.
- [ ] (선상속·M6) Compose UI·collectAsStateWithLifecycle·로딩 애니메이션(iOS LoadingPhase)·문구(오프라인 구분)·네비게이션.
- [ ] (선상속·M7) Koin `viewModel { }` 배선.
