# M7 슬라이스 (draft) — Koin 배선 + 앱 셸 통합

> **성격: 비준 하네스 입력용 spec 슬라이스(`-draft.md`).** 단일 [`spec.md`](spec.md) Phase 1(배선)의 마일스톤 경계. 진행 상태 정본 [`../../ROADMAP.md`](../../ROADMAP.md), 상위 설계 [`../architecture.md`](../architecture.md)(§4.7 DI). 자율 구현 전 적대 비준 대상.
>
> **⚠️ 검증 천장(M6 계승·지속).** 셸을 `AppRoot`에 연결해도 화면 **시각·상호작용·런타임 동작**은 이 마일스톤에서 검증되지 않는다(실기기 필요). M7의 green은 **컴파일·조립·링크 + Koin 그래프 해석(테스트 Koin) + DR-2/DR5-2 순수·구조 테스트 네이티브 실행**까지다. 「코드 완료·실기기 검증 필요」 라벨 유지.

- **마일스톤**: M7 · Koin 배선 + 앱 셸
- **브랜치**: `feat/m7-koin-wiring` (=`feat/m6-compose-ui` 위에 **스택** 분기)
- **참조**: spec [1-4](spec.md), architecture §4.7, [M4 슬라이스](m4-repository-draft.md)(DR-2 단일-writer), [M5 슬라이스](m5-viewmodel-draft.md)(OQ-3 Mutex 이월), [M6 슬라이스](m6-compose-ui-draft.md)(DR5-2 쓰기 유실·`AppDependencies`·seam·`AppRoot`)

---

## 1. 목표

지금까지 각 계층은 인터페이스·생성자 주입 형태로만 서 있고 **연결돼 있지 않다**(앱 셸은 여전히 M0 `App()`을 그린다 — `App()`은 commonMain `ui/App.kt`에서 Koin으로 `Greeting`을 해석해 텍스트만 렌더). M7은 **Koin 그래프**로 전 계층(번들·네트워크·로컬·오케스트레이터·분석·seam·ViewModel)을 조립하고, **앱 셸**(`MainActivity`/iOS `MainViewController`)이 현재의 `App()` 호출 대신 `AppRoot(deps)`를 그리도록 연결한다. 이로써 앱이 **처음으로 실행 가능한 전체**가 된다(런타임 시각 검증은 천장).

동시에 **M4/M5/M6가 이월한 두 항목을 다룬다**: **DR-2 단일-writer**(정규화 키 Mutex + `TermRepository`를 `single`로 배선하는 게이트)는 **구조로만 담보**하고 진짜 병렬 강제(맵-가드 원자성의 실행-검증)는 실기기까지 미검증으로 남긴다(OQ-2 잔여, §3-5·§7-2 — '닫음' 자칭 금지), **DR5-2 쓰기 내구성**(`toggleBookmark`가 화면 이탈에도 유실되지 않게 앱 스코프 쓰기)은 판별 네이티브 테스트(§6)로 닫는다.

## 2. 스코프

**IN (M7):**
- **공통 Koin 모듈**(`di/`): `BundleDbSource`(terms.json 로드)·`TermGenerator`=`ClaudeApi`(HttpClient+deviceId)·`LocalTermStore`=`SqlDelightTermStore`(DevEtymDatabase)·`AnalyticsService`=`PlaceholderAnalyticsService`·**`TermRepository`=`single`**(DR-2 단일-scope 게이트)·`clock`·앱 쓰기 스코프(`appWriteScope`)·ViewModel(`SearchViewModel`/`BookmarkViewModel`/`HistoryViewModel` `single`, `DetailViewModel` `factory`).
- **플랫폼 Koin 모듈**(`androidMain`/`iosMain`): `DriverFactory`(Android `Context`)·`httpEngine()`·`deviceId` 제공·seam 구현 바인딩.
- **`deviceId` seam**: `X-Device-Id`용 안정 문자열 제공(expect/actual 또는 seam). **영속 고유 ID는 M8 이월** — M7은 안정 스텁(고정/세션 UUID) 바인딩(프록시는 별도 트랙이라 값 자체는 비임계).
- **`AppDependencies` 실구현**(`KoinAppDependencies`): Koin에서 VM·seam 해석. `AppRoot(deps)` 소비.
- **앱 셸 연결**: `MainActivity.setContent { AppRoot(deps) }` + iOS `MainViewController = ComposeUIViewController { AppRoot(deps) }`. `initKoin`에 플랫폼 모듈 추가.
- **온보딩 게이트**: `AppRoot`의 `onboarded`를 seam(`AppearanceStore`류의 온보딩 플래그 저장)으로 승격하거나 M8까지 in-memory 유지 — 최소 게이트만(§7).
- **DR-2 구조 담보**: `TermRepositoryImpl`에 정규화 키 Mutex(fetch/refresh/toggleBookmark 전 구간 직렬화) + Koin `single` 배선으로 **모든 소비자 동일 인스턴스**. 게이트 테스트(같은 인스턴스 해석)·동시성 스모크. 진짜 병렬 강제는 자칭 안 함(OQ-2 잔여).
- **DR5-2 마감**: `DetailViewModel`이 선택적 `writeScope`(앱 스코프)로 `toggleBookmark`를 launch — 화면 이탈(`onCleared`)에도 쓰기가 취소되지 않는다. 기본값은 M5 동작 보존(테스트 무영향).
- **그래프 검증 테스트**(`androidUnitTest`): 테스트 Koin(실 공통 모듈 + 테스트 플랫폼 모듈: in-memory JDBC 드라이버·MockEngine)으로 `TermRepository`·VM·`AppDependencies` 해석 + **단일-scope 실측**(동일 `TermRepository` 인스턴스). + DR-2 동시성·DR5-2 내구성 테스트(네이티브 포함 가능한 것은 commonTest).

**OUT (M8/트랙):**
- **seam actual 실구현·런타임 검증**(외관모드 영속·mailto·공유·앱평가·클립보드·Firebase ID)·**deviceId 영속 고유화** → M8. M7은 바인딩 지점만(스텁 or 최소 actual).
- **아이콘/스플래시 자산·폰트 라이선스 고지·접근성 감사·에러 통합 실측·실기기 시각/상호작용 검증** → M8/실기기.
- **서버 `devetym-proxy`** → 별도 트랙(M3 §0).

## 3. 산출 명세

### 3-1. 공통 Koin 모듈 (`di/AppModule.kt` 확장)

```kotlin
fun appModule(readyBundle: BundleDbSource) = module {            // preload한 값을 파라미터로 주입(top-level `val appModule` 금지 — DR-1)
    single<Json> { AppJson }
    single { appWriteScope() }                                   // CoroutineScope(SupervisorJob()+Dispatchers.Default) — DR5-2
    single<BundleDbSource> { readyBundle }                       // 파라미터 readyBundle 바인딩(preload 초기값 — 아래 주의)
    single<LocalTermStore> { SqlDelightTermStore(get()) }        // DevEtymDatabase 주입
    single { createHttpClient(get()) }
    single<TermGenerator> { ClaudeApi(get(), deviceId = get<DeviceIdProvider>()::get) }
    single<AnalyticsService> { PlaceholderAnalyticsService() }
    single<TermRepository> { TermRepositoryImpl(get(), get(), get(), get(), clock = ::epochMillis) }  // DR-2 단일 인스턴스
    single { SearchViewModel(get()) }
    single { BookmarkViewModel(get()) }
    single { HistoryViewModel(get()) }
    factory { DetailViewModel(get(), writeScope = get()) }        // 화면별 신규(DR5-2 앱 스코프 주입)
}
```
- `DevEtymDatabase`는 플랫폼 모듈이 `single { createDatabase(get()) }`로(드라이버 주입). `clock`은 `epochMillis()`(플랫폼 현재시각 — 기존 없으면 expect/actual 신규, §7).
- **번들 로드(정정)**: `di/AppModule.kt`는 commonMain이고 commonMain은 `runBlocking`을 못 쓴다 — `runBlocking`은 kotlinx-coroutines의 concurrent(jvm+native) 소스셋에만 있어 consumer commonMain API 표면에 없다(commonMain에서 미해결 → 3축 전부 컴파일 실패). 따라서 **`startKoin` *이전에* `loadBundleDbSource()`(suspend)를 await해 `readyBundle` 값을 만든 뒤 그 값을 `appModule(readyBundle)` 파라미터로 주입**(top-level `val appModule`은 런타임 심볼 `readyBundle`를 컴파일 시점에 미해결하므로 금지 — DR-1)하되, **이 preload+`initKoin`을 플랫폼 진입점의 `runBlocking`으로 동기 완료**한다(작은 JSON·시작 1회, androidMain/iosMain은 `runBlocking` 사용 가능 — commonMain 금지). 첫 프레임/`getKoin()`은 이 동기 초기화 이후에만 도달하므로 async-init 레이스가 원천 차단된다(§3-4 순서 불변식). commonMain 모듈 정의에는 블로킹 호출을 두지 않는다.

### 3-2. 플랫폼 Koin 모듈 (`androidMain`/`iosMain` `di/PlatformModule.*.kt`)

- **androidMain**: 플랫폼 모듈을 **팩토리 함수 `androidPlatformModule(context: Context): Module`**로 노출(`android.content.Context`는 androidMain 소스셋에서만 참조 — commonMain 미유입)·`single { DriverFactory(context) }`·`single { createDatabase(get()) }`·`single<DeviceIdProvider> { ... }`·seam actual 바인딩(M8까지 스텁 허용). koin-android `androidContext()` 미사용 → shared androidMain은 koin-core만.
- **iosMain**: 팩토리 함수 **`iosPlatformModule(): Module`**로 노출·`single { DriverFactory() }`·`single { createDatabase(get()) }`·`single<DeviceIdProvider> { ... }`·seam 바인딩.
- **commonMain `initKoin`은 `Context`를 모른다(DR-2)**: `suspend fun initKoin(platformModule: Module)`가 preload(`loadBundleDbSource()`)로 `readyBundle`를 만든 뒤 `startKoin { modules(appModule(readyBundle), platformModule) }`로 공통+플랫폼 모듈을 조립한다. 플랫폼 모듈은 **각 플랫폼 소스셋의 팩토리 함수가 빌드**해 넘긴다(Android=`androidPlatformModule(context)`, iOS=`iosPlatformModule()`). `Context` 타입을 commonMain `initKoin` 시그니처에 **넣지 않는다** — 넣으면 `iosSimulatorArm64` 네이티브 컴파일에서 `Context` 미해결로 링크 실패. `Module`은 koin-core commonMain 타입이라 shared는 koin-core만으로 성립.

### 3-3. `AppDependencies` 실구현 (`di/KoinAppDependencies.kt`)

```kotlin
class KoinAppDependencies(private val koin: Koin) : AppDependencies {
    override val searchViewModel get() = koin.get<SearchViewModel>()
    override val bookmarkViewModel get() = koin.get<BookmarkViewModel>()
    override val historyViewModel get() = koin.get<HistoryViewModel>()
    override fun createDetailViewModel() = koin.get<DetailViewModel>()   // factory
    override val actions get() = koin.get<AppActions>()
    override val appearance get() = koin.get<AppearanceStore>()
    override val device get() = koin.get<DeviceInfo>()
    override fun now() = epochMillis()
}
```

### 3-4. 앱 셸 연결

- **현재 셸 상태(정정)**: 두 셸은 `Greeting`을 직접 그리지 않는다 — androidApp `MainActivity`는 `setContent { App() }`, iOS `MainViewController`는 `ComposeUIViewController { App() }`로 **commonMain `App()`을 호출**한다(`App()`이 내부에서 Koin으로 `Greeting`을 해석). M7 편집 대상은 이 **`App()` 호출**을 아래 `AppRoot(deps)`로 교체하는 것이다(셸에서 `Greeting` 심볼을 찾지 말 것).
- **순서 불변식(DR-2)**: 셸은 **preload+`initKoin` 동기 완료 뒤에만** `AppRoot`를 렌더하고 `getKoin()`을 호출한다. 초기화를 async(코루틴 launch)로 뒤로 밀면 첫 프레임이 `getKoin()`을 기동 전에 불러 크래시하므로, 초기화는 플랫폼 진입점에서 `runBlocking`으로 **동기 완료**한다(작은 JSON·시작 1회, androidMain/iosMain 허용 — commonMain 금지). 이로써 async-init/first-frame 레이스가 코드 경로 자체에서 제거된다.
- **androidApp `MainActivity`**: `setContent { AppRoot(KoinAppDependencies(KoinPlatform.getKoin())) }`. `Application.onCreate`에서 `runBlocking { initKoin(androidPlatformModule(context = this@App)) }`로 동기 완료(preload 포함) — `Context`는 **androidMain의 `androidPlatformModule(context)` 팩토리가 받아** 플랫폼 모듈을 빌드하고, commonMain `initKoin`엔 완성된 `Module`만 넘어간다(Context 타입 commonMain 미유입 — DR-2, koin-android `androidContext` 미사용, `getKoin()`도 koin-core `KoinPlatform.getKoin()` 사용). **shared는 koin-core만** 의존하고 셸도 koin-android의 `androidContext`/koin-android API를 쓰지 않는다 — 다만 `androidApp/build.gradle.kts`는 여전히 `implementation(libs.koin.android)`를 선언하며, koin-android가 koin-core를 **transitively 제공**한다(셸 gradle 좌표 교체는 §8 셸 편집 범위 밖 — M8/정리 트랙). onCreate 반환 시 그래프 기동이 끝나 있어 `getKoin()`이 안전하다.
- **iOS `MainViewController`**: `ComposeUIViewController { AppRoot(KoinAppDependencies(KoinPlatform.getKoin())) }`. iOS 앱 시작(iOSApp.swift)이 부르는 `doInitKoin()`(Kotlin/iosMain)이 `fun doInitKoin() = runBlocking { initKoin(iosPlatformModule()) }`로 **동기 완료 후 반환** → 이후 ComposeUIViewController/AppRoot의 `getKoin()`이 안전(Swift async/시퀀싱 가드 불필요).
- **Obj-C 파사드명 고정(AD-2)**: `doInitKoin`을 담는 iosMain 파일은 반드시 **`AppModule.kt`로 명명**한다 — Kotlin/Native가 파일명에서 Obj-C 파사드 클래스명을 만들고(commonMain `AppModule.kt`와 **같은 파일명**이면 같은 iOS 컴파일에서 `AppModuleKt` 파사드로 병합), 그 결과 기존 Swift 호출부 `AppModuleKt.doInitKoin()`(iOSApp.swift:8)이 **수정 없이 유지**된다. iosMain 파일을 다른 이름(예: `PlatformModule.ios.kt`)으로 두면 파사드명이 새 `…Kt`로 바뀌어 Swift가 클래스명 불일치로 컴파일 실패하는데, 이 Swift 앱 빌드는 M7 3축 green(`:shared:*`)에 없어 Xcode/실기기에서만 드러난다(3축이 못 잡는 Swift 커플링을 파일명 제약으로 봉인 — Swift 편집 불요).
- **남겨질 `App()`(commonMain) 처리**: 셸이 더는 `App()`을 호출하지 않으므로 M0 `App()`은 (a) 제거하거나 (b) 향후 미사용 데드코드 경고를 피하려면 삭제한다 — M7은 `App()`을 **제거**한다(셸이 `AppRoot`로 직행하므로 M0 Greeting 렌더 경로는 폐기). `Greeting` 바인딩 자체는 그래프에서 더는 소비되지 않으면 함께 정리(잔존 시 §6 그래프 테스트 미영향).
- 두 셸이 `App()` 호출 대신 `AppRoot`를 그린다 — 이 연결 자체가 **조립/링크 green으로 검증**(런타임 시각은 천장).

### 3-5. DR-2 — 정규화 키 단일-writer 구조 담보 (`TermRepositoryImpl`)

- `fetch`/`refresh`/`toggleBookmark`가 각자 맨 앞 `val key = normalizeKeyword(...)`로 **키잉된 `Mutex`를 오퍼레이션 전 구간(refresh 네트워크 왕복 포함) 보유**한 뒤 RMW. 같은 정규화 키의 두 번째 쓰기는 첫 쓰기 완료까지 suspend. 서로 다른 키는 병렬(전역 잠금 아님).
- **잠금 맵 접근 원자성(정정 — commonMain·네이티브 필수)**: 키→`Mutex` 맵을 plain `HashMap.getOrPut`으로 열면 Kotlin/Native(iosSimulatorArm64)에서 두 코루틴이 서로 다른 스레드에서 같은 키를 열 때 데이터 레이스로 **같은 키에 `Mutex` 인스턴스가 둘 생겨 상호배제가 무너진다**(isBookmarked/seenAt lost-update). commonMain엔 JVM `synchronized`가 없으므로 '동기 잠금'을 쓸 수 없다. 맵 접근은 **commonMain 가능한 프리미티브**로 못박는다: 전용 `kotlinx.coroutines.sync.Mutex`(맵 가드용)로 감싼 `getOrPut`(전부 suspend·common 호환) — 또는 atomicfu 도입. DR-2를 **오직 구조로 담보**하므로(§7-2 진짜 병렬 미검증) 이 맵-접근 원자성 지점이 실제 프리미티브로 정확히 실현돼야 담보가 void되지 않는다.
- **Mutex 비재진입 주의**: op 최상단 1회 획득, 락 보유 중 같은 키 재획득 금지(데드락 — `orchestrate`가 `buildAiRow`를 부르되 후자는 락을 다시 잡지 않음).
- **single-scope 게이트**: Koin이 `TermRepository`를 `single`로 제공 → 모든 VM이 동일 인스턴스·동일 잠금 맵 공유(M5 OQ-3 전제 a 충족). §6 게이트 테스트가 실측.

### 3-6. DR5-2 마감 — 쓰기 내구성 (`DetailViewModel`)

- `DetailViewModel(repository, writeScope: CoroutineScope? = null)`: `toggleBookmark()`가 `(writeScope ?: viewModelScope).launch { repository.toggleBookmark(entry) }`. **M7이 앱 스코프(`SupervisorJob`+`Dispatchers.Default`, `onCleared`와 무관) 주입** → 별표 탭 직후 화면 이탈해도 토글 launch가 취소되지 않아 DB에 반영된다. 기본값 `null`=viewModelScope(M5 동작·테스트 보존).
- **주의(정직)**: 앱 스코프 쓰기는 유실을 막지만 **확정 UI 피드백**(성공/실패 표시)은 상세 이탈 후엔 표시할 화면이 없어 별도다 — 리스트 화면 재진입 시 반응형 Flow로 반영된다(STAY는 M6가 이미 즉시 반영). M7은 **내구성**을 닫고, 이탈-후-피드백의 완결(예: 스낵바)은 필요 시 M8.

## 4. 설계 불변식

- **셸은 얇다**(architecture §3): 진입점은 `initKoin` + `AppRoot`만. 화면·로직은 `shared`.
- **Koin 그래프 완전성(범위 정직)**: §6 그래프 테스트는 **공통 모듈 + 테스트 플랫폼 모듈**(in-memory JDBC·MockEngine) 바인딩만 **해석-실측**한다 — 모든 seam·VM·`now()` 프로퍼티를 eager로 touch해(§6 참조, `AppDependencies` seam 접근자는 lazy getter라 eager touch 없이는 lazy 경로 바인딩 누락이 green을 통과) 이 그래프 범위 안에서 '모든 `get()`이 바인딩을 가진다'를 실측한다. **단, 실 androidMain/iosMain 플랫폼 모듈**(DriverFactory·createDatabase·DeviceIdProvider·seam actual)은 조립(assembleDebug)/링크(linkFramework)로 **컴파일-only**이며 Koin '해석'은 어떤 M7 테스트도 실행하지 않는다 — 이 바인딩들의 미해결/시그니처 불일치는 컴파일이 아닌 **런타임 사건**이라 실기기로 이월된다(M7 천장). 따라서 완전성 실측은 **공통+테스트-플랫폼 그래프에 한정**되며, 실 플랫폼 바인딩 완전성은 **자칭하지 않는다**(거짓 green 금지).
- **DR-2 단일-writer**: `TermRepository`=`single` + 키 Mutex. 소비자(VM)는 계약을 신뢰(§3-5).
- **DR5-2 내구성**: 쓰기는 화면 수명과 분리된 스코프.
- **검증 천장 정직**: 런타임 시각·상호작용은 green 자칭 금지.

## 5. 완료 조건 (DoD)

- **컴파일·조립·링크 green(3축)**: `:shared:testDebugUnitTest` + `:androidApp:assembleDebug`(셸이 `AppRoot`+Koin 조립) + `:shared:linkDebugFrameworkIosSimulatorArm64`(iOS 셸·플랫폼 모듈 링크). **shared는 koin-core만** 의존(`Context`는 `initKoin` 파라미터로 전달 — koin-android `androidContext` 불필요), koin 좌표가 Kotlin 2.3.21에서 소비됨을 실빌드로 확정.
- **⊕ 그래프·결착 네이티브/JVM 실행**: `:shared:iosSimulatorArm64Test` green(DR-2 Mutex 직렬화·DR5-2 내구성 등 commonTest 네이티브 실행) + `androidUnitTest` 그래프 테스트(테스트 Koin 해석·단일-scope).
- §6 테스트 통과.
- **명시적 비-보증**: 런타임 화면·상호작용·seam actual 동작은 이 DoD가 보증하지 않음 → 「코드 완료·실기기 검증 필요」.

## 6. 테스트

**그래프(`androidUnitTest` — 테스트 Koin + JDBC/MockEngine):**
- `test_koin_그래프_해석` — 테스트 모듈로 `startKoin` 후 `TermRepository`·`SearchViewModel`·`DetailViewModel`·`AppDependencies` 해석 성공(미해결 바인딩 없음). ⚠️ **seam eager touch 필수**: `KoinAppDependencies`의 `actions`/`appearance`/`device`는 **lazy getter**(`get() = koin.get<…>()`)라 해석만 하고 프로퍼티를 안 건드리면 그 바인딩은 resolve되지 않아, 테스트 플랫폼 모듈이 예: `DeviceInfo` 바인딩을 빠뜨려도 green이 나고 실기기 Settings 진입에서 `NoBeanDefFoundException`이 터진다(런타임은 M7 천장이라 false-green이 아침 리뷰를 통과). 따라서 그래프 테스트는 `deps.actions`/`deps.appearance`/`deps.device`/`deps.searchViewModel`/`deps.bookmarkViewModel`/`deps.historyViewModel`·`deps.createDetailViewModel()`·`deps.now()`를 **모두 eager로 접근해 `assertNotNull`** 해야 한다(모든 seam·VM·now 프로퍼티를 실제로 touch). 이렇게 해야 §4 '미해결 바인딩 실측' 주장이 lazy seam 경로에도 참이 된다.
- `test_koin_repository_single_동일인스턴스` (DR-2 게이트) — `get<TermRepository>() === get<TermRepository>()` 그리고 두 VM(`get<BookmarkViewModel>` 경유 파생 불가라 직접) 해석이 **동일 repository 인스턴스**를 참조(single-scope 실측).

**DR-2 단일-writer (`commonTest` — 네이티브 실행):**
- `test_mutex_동일키_직렬화` — 실 `TermRepositoryImpl`(지연 generator·in-memory store), **다중스레드 실측 불가 시 결정적 대체**: 같은 키 `refresh`+`toggleBookmark`를 인터리브해도 최종 상태 일관(스모크) + Mutex 보유 중 재진입이 데드락 없이 완료(비재진입 경로 실측). ⚠️ 단일스레드 오라클 한계는 M5 기록대로 — 이 테스트는 데드락 부재·계약 준수를 보증하고 진짜 병렬 강제는 구조(single+Mutex)로 담보.

**DR5-2 내구성 (`commonTest` — 네이티브 실행):**
- `test_toggleBookmark_외부스코프_이탈해도_반영` — `DetailViewModel(repo, writeScope=외부스코프)`로 `load`→`toggleBookmark()` 직후 `viewModelScope`를 취소(이탈 모사)해도, 외부 writeScope가 살아 있어 `repository.toggleBookmark`가 완료돼 store에 반영(기본 `null`=viewModelScope 경로면 취소로 미반영 — discriminating).

## 7. 열린 질문 (비준이 판정할 항목)

1. **번들 로드 — 플랫폼 진입점에서 preload+initKoin을 `runBlocking` 동기 완료(제안)** — terms.json을 `startKoin` 전에 `loadBundleDbSource()`(suspend)로 await해 `readyBundle`를 모듈에 주입하되, 이 preload+`initKoin`을 androidMain/iosMain 진입점의 `runBlocking`으로 **동기 완료**한다(작은 JSON·시작 1회). 첫 프레임/`getKoin()`이 초기화 완료 이후에만 도달해 async-init 레이스가 원천 차단됨(§3-4 순서 불변식). ⚠️ **폐기된 대안**: (a) commonMain `single { runBlocking { … } }`는 `runBlocking`이 commonMain에 없어 컴파일 불가; (b) '지연 suspend init'은 `BundleDbSource.search(keyword)`가 **non-suspend**라 첫 검색 시점 지연-로드 불가; (c) **suspend 진입점+코루틴 launch**(비동기 preload 후 initKoin)는 initKoin 완료 전 첫 프레임이 `getKoin()`을 불러 기동 크래시(racy) — commonMain 금지의 `runBlocking`을 플랫폼 진입점으로 내려 동기화하는 경로만 안전. 비준이 동기 `runBlocking` 경로가 적절한지 판정.
2. **DR-2 Mutex 실측 — 단일스레드 스모크+구조 담보(제안) vs 다중스레드 스트레스 테스트** — 제안: 데드락 부재·계약 준수를 결정적으로, 진짜 병렬 강제는 single+Mutex 구조로. 다중스레드 테스트는 flaky. 비준이 이 담보가 정직한지(강제를 자칭 안 하는지) 판정. **전제**: 구조 담보가 성립하려면 §3-5의 잠금 맵 접근이 commonMain·네이티브 가능한 프리미티브(맵 가드 `kotlinx.coroutines.sync.Mutex` 또는 atomicfu)로 실현돼야 한다 — JVM `synchronized`류를 암시하면 네이티브에서 맵-접근 레이스로 담보가 조용히 void됨.
3. **DR5-2 앱 스코프 — 선택적 `writeScope` 주입(제안) vs `toggleBookmark`를 repository 스코프로 이관** — 제안: VM 선택 주입(M5 기본 보존). 비준 판정.
4. **seam·deviceId 스텁 — M7 스텁 바인딩(제안) vs 최소 actual 당김** — 제안: 스텁 바인딩(조립 green), 실 actual M8. 비준이 스텁이 거짓 green(런타임 작동한 척)인지 판정 — M7은 seam '동작'을 보증 안 함을 명시.
5. **온보딩 게이트 영속 — in-memory(제안, M8 영속) vs M7 seam 저장** — 제안: 최소 in-memory(재시작 시 재노출), 영속은 M8. 비준 판정.

## 8. 안전·규율

- **검증 천장 정직**(§0·§5): 런타임·시각·seam 동작 green 자칭 금지. **거짓 green 금지.**
- **셸 편집 범위**: M7은 `androidApp`/`iosApp` 셸의 진입점만 편집(`AppRoot` 연결·`initKoin`). 서명·심사·매니페스트 확장은 M8.
- **M4/M5/M6 이월 처리**: DR5-2(§3-6)는 이 슬라이스가 판별 테스트로 닫음. DR-2(§3-5)는 **구조(single+키 Mutex)로만 담보**하며 맵-가드 원자성의 진짜 병렬 강제는 실기기까지 미검증(OQ-2 잔여) — '닫음/마감' 자칭 금지. 비준자 확인.
- 마일스톤 경계 **사람 게이트 완화**(메모리 `milestone-human-gate-relaxed`). 하네스는 push·머지·`-draft` 제거 안 함.
- **브랜치 보존·push 금지·젠더중립 네이밍·진행상태는 ROADMAP(디스크)**.

## Open Questions

> 비준 착수 전 — 비어 있으며 적대 비준이 채운다.

- [ ] (비준 대기) §7 열린 질문 1~5 판정.
- [ ] (선상속·M8) seam actual 실구현·deviceId 영속 고유화·온보딩 영속·아이콘/스플래시·폰트 라이선스 고지·접근성·에러 통합·실기기 검증.
- [ ] (이월·M8/실기기) **실 androidMain/iosMain 플랫폼 Koin 그래프 해석**(DeviceIdProvider·DriverFactory·createDatabase·seam actual 바인딩 완전성) 실기기 스모크 — M7 그래프 테스트는 테스트-플랫폼 스텁으로 대체 해석하므로 실 플랫폼 바인딩 누락/시그니처 불일치는 런타임 사건(§4)으로 이월. 이 잔여를 실기기 첫 검색·Settings 진입 스모크로 닫는다.
