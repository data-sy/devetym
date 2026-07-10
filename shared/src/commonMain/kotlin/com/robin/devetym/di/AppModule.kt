package com.robin.devetym.di

import com.robin.devetym.analytics.AnalyticsService
import com.robin.devetym.analytics.PlaceholderAnalyticsService
import com.robin.devetym.data.AppJson
import com.robin.devetym.data.bundle.BundleDbSource
import com.robin.devetym.data.bundle.loadBundleDbSource
import com.robin.devetym.data.local.LocalTermStore
import com.robin.devetym.data.local.SqlDelightTermStore
import com.robin.devetym.data.remote.ClaudeApi
import com.robin.devetym.data.remote.TermGenerator
import com.robin.devetym.data.remote.createHttpClient
import com.robin.devetym.repository.TermRepository
import com.robin.devetym.repository.TermRepositoryImpl
import com.robin.devetym.ui.BookmarkViewModel
import com.robin.devetym.ui.DetailViewModel
import com.robin.devetym.ui.HistoryViewModel
import com.robin.devetym.ui.SearchViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.serialization.json.Json
import org.koin.core.context.startKoin
import org.koin.core.module.Module
import org.koin.dsl.module

/**
 * 프록시 한도 카운터 키(`X-Device-Id`) 제공 (M7 §3-1). **영속 고유 ID는 M8 이월** — 플랫폼 스텁 바인딩.
 */
interface DeviceIdProvider {
    fun get(): String
}

/** 화면 수명과 분리된 앱 쓰기 스코프 (M7 §3-6 DR5-2 취소 내성). */
fun appWriteScope(): CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

/** 플랫폼 현재 시각(ms). clock 주입 소스 — expect/actual(commonMain엔 시각 소스 부재). */
expect fun epochMillis(): Long

/**
 * 공통 Koin 모듈 (M7 §3-1). **번들은 preload한 `readyBundle` 값을 파라미터로 주입**한다(commonMain은
 * `runBlocking` 불가 — top-level `val appModule` 금지). `TermRepository`=`single`(DR-2 단일-scope
 * 게이트 — 모든 소비자 동일 인스턴스·잠금 맵 공유).
 */
fun appModule(readyBundle: BundleDbSource): Module = module {
    single<Json> { AppJson }
    single { appWriteScope() }
    single<BundleDbSource> { readyBundle }
    single<LocalTermStore> { SqlDelightTermStore(get()) }
    single { createHttpClient(get()) }
    single<TermGenerator> { ClaudeApi(get(), deviceId = get<DeviceIdProvider>()::get) }
    single<AnalyticsService> { PlaceholderAnalyticsService() }
    single<TermRepository> { TermRepositoryImpl(get(), get(), get(), get(), clock = ::epochMillis) }
    single { SearchViewModel(get()) }
    single { BookmarkViewModel(get()) }
    single { HistoryViewModel(get()) }
    factory { DetailViewModel(get(), writeScope = get()) }
}

/**
 * 공통 진입 (M7 §3-2). preload(`loadBundleDbSource`)로 `readyBundle`를 만든 뒤 공통+플랫폼 모듈 조립.
 * `Context`는 이 시그니처에 넣지 않는다(iOS 네이티브 컴파일 미해결) — 플랫폼 팩토리가 완성된 `Module`을 넘긴다.
 * **플랫폼 진입점의 `runBlocking`으로 동기 완료**(첫 프레임/`getKoin()` 이전 — async-init 레이스 차단).
 */
suspend fun initKoin(platformModule: Module) {
    val readyBundle = loadBundleDbSource(AppJson)
    startKoin { modules(appModule(readyBundle), platformModule) }
}
