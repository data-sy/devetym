package com.robin.devetym.di

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.robin.devetym.data.bundle.InMemoryBundleDbSource
import com.robin.devetym.db.DevEtymDatabase
import com.robin.devetym.repository.TermRepository
import com.robin.devetym.ui.BookmarkViewModel
import com.robin.devetym.ui.DetailViewModel
import com.robin.devetym.ui.HistoryViewModel
import com.robin.devetym.ui.SearchViewModel
import com.robin.devetym.ui.platform.AppActions
import com.robin.devetym.ui.platform.AppearanceStore
import com.robin.devetym.ui.platform.DeviceInfo
import com.robin.devetym.ui.platform.NoopAppActions
import com.robin.devetym.ui.platform.OnboardingStore
import com.robin.devetym.ui.platform.StubAppearanceStore
import com.robin.devetym.ui.platform.StubDeviceInfo
import com.robin.devetym.ui.platform.StubOnboardingStore
import org.koin.core.KoinApplication
import org.koin.dsl.koinApplication
import org.koin.dsl.module
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertSame

/**
 * M7 §6 — Koin 그래프 해석 + 단일-scope(DR-2 게이트). 테스트 Koin(격리 `koinApplication`, 전역 미오염) +
 * 테스트 플랫폼 모듈(in-memory JDBC + seam 스텁). ⚠️ 실 androidMain/iosMain 플랫폼 바인딩 완전성은 실기기 이월(§4).
 */
class KoinGraphTest {

    private var app: KoinApplication? = null

    @AfterTest fun teardown() { app?.close(); app = null }

    private fun testDatabase(): DevEtymDatabase {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        DevEtymDatabase.Schema.create(driver)
        return DevEtymDatabase(driver)
    }

    private fun koin() = koinApplication {
        modules(
            appModule(InMemoryBundleDbSource(emptyList())),   // Res 회피 — 테스트용 빈 번들 주입
            module {
                single<DevEtymDatabase> { testDatabase() }
                single<DeviceIdProvider> { object : DeviceIdProvider { override fun get() = "test" } }
                single<AppActions> { NoopAppActions() }
                single<AppearanceStore> { StubAppearanceStore() }
                single<OnboardingStore> { StubOnboardingStore() }
                single<DeviceInfo> { StubDeviceInfo() }
            },
        )
    }.also { app = it }

    @Test
    fun test_koin_그래프_해석_온보딩포함() {
        val k = koin().koin
        assertNotNull(k.get<TermRepository>())
        assertNotNull(k.get<SearchViewModel>())
        assertNotNull(k.get<BookmarkViewModel>())
        assertNotNull(k.get<HistoryViewModel>())
        assertNotNull(k.get<DetailViewModel>())   // factory

        // ⚠️ seam eager touch 필수 — KoinAppDependencies의 actions/appearance/device는 lazy getter라
        // 실제로 건드려야 바인딩 누락이 발화한다(§6). 모든 seam·VM·now를 touch.
        val deps = KoinAppDependencies(k)
        assertNotNull(deps.searchViewModel)
        assertNotNull(deps.bookmarkViewModel)
        assertNotNull(deps.historyViewModel)
        assertNotNull(deps.createDetailViewModel())
        assertNotNull(deps.actions)
        assertNotNull(deps.appearance)
        assertNotNull(deps.device)
        assertNotNull(deps.onboarding)   // M8 신규 seam — eager touch(§5 전 seam 대상)
        assertNotNull(deps.now())
    }

    @Test
    fun test_koin_repository_single_동일인스턴스() {
        val k = koin().koin
        assertSame(k.get<TermRepository>(), k.get<TermRepository>())   // DR-2 단일-scope 게이트
    }
}
