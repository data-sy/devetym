package com.robin.devetym.di

import android.content.Context
import com.robin.devetym.data.bundle.InMemoryBundleDbSource
import com.robin.devetym.db.DevEtymDatabase
import com.robin.devetym.ui.platform.AppActions
import com.robin.devetym.ui.platform.AppearanceStore
import com.robin.devetym.ui.platform.ConsentStore
import com.robin.devetym.ui.platform.DeviceInfo
import com.robin.devetym.ui.platform.OnboardingStore
import org.junit.runner.RunWith
import org.koin.core.KoinApplication
import org.koin.dsl.koinApplication
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * M9 §3-1 — 실 플랫폼 Koin 그래프 완전성(M7 DR-1 Android 절반 끌어내리기).
 *
 * `KoinGraphTest`(테스트-스텁 모듈)와 달리 **실 `androidPlatformModule(context)`**를 Robolectric 실 Context로
 * 조립하고, `KoinAppDependencies`가 eager-touch하는 전 seam을 `koin.get()`으로 강제 해석해 실 바인딩 누락
 * (`NoDefinitionFound`)을 JVM 테스트로 잡는다.
 *
 * 🔒 **부분 스텁 금지(spec §3-1·§4)**: 7개 플랫폼 바인딩 전부 **실 모듈에서** 해석한다. 특히 `DevEtymDatabase`는
 * DR-1이 겨눈 하중 바인딩 — in-memory JDBC 스텁(`KoinGraphTest.testDatabase()`)으로 되돌리지 않고 실
 * `AndroidSqliteDriver`(Robolectric SQLite)로 열어 **실제 쿼리 왕복**까지 실행해 "정말 열렸음"을 강제한다.
 * 각 바인딩의 실/스텁 여부를 concrete 타입 단언으로 표기한다(표기 없는 green 금지).
 *
 * @Config sdk=34 — android-all 가용 API 핀(compileSdk 36 android-all 미배포 회피). DB/seam은 안정 API만 사용.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class RealAndroidGraphTest {

    private var app: KoinApplication? = null

    @AfterTest fun teardown() { app?.close(); app = null }

    private fun realGraph(context: Context) = koinApplication {
        modules(
            appModule(InMemoryBundleDbSource(emptyList())),   // Res 회피(빈 번들) — 플랫폼 바인딩은 스텁 아님
            androidPlatformModule(context),                    // ⚠️ 실 모듈(스텁 아님) — 7 바인딩 실 해석
        )
    }.also { app = it }

    @Test
    fun test_androidPlatformModule_실그래프_전seam_해석() {
        val context: Context = RuntimeEnvironment.getApplication()   // 실 Android Context(Robolectric)
        val koin = realGraph(context).koin

        // ── 7개 실 플랫폼 바인딩 전부 실 concrete 타입으로 해석(부분 스텁 금지 단언) ──
        // (1) DevEtymDatabase — 실[하중 바인딩]. AndroidSqliteDriver로 실 DB. 아래서 쿼리 왕복으로 열림 강제.
        val db = koin.get<DevEtymDatabase>()
        assertNotNull(db)
        // (2) DeviceIdProvider — 실: PrefsDeviceIdProvider(SharedPreferences)
        assertTrue(koin.get<DeviceIdProvider>() is PrefsDeviceIdProvider, "DeviceIdProvider가 실 바인딩 아님")
        // (3) AppActions — 실: AndroidAppActions(Context)
        assertTrue(koin.get<AppActions>() is AndroidAppActions, "AppActions가 실 바인딩 아님")
        // (4) AppearanceStore — 실: PrefsAppearanceStore(SharedPreferences)
        assertTrue(koin.get<AppearanceStore>() is PrefsAppearanceStore, "AppearanceStore가 실 바인딩 아님")
        // (5) OnboardingStore — 실: PrefsOnboardingStore(SharedPreferences)
        assertTrue(koin.get<OnboardingStore>() is PrefsOnboardingStore, "OnboardingStore가 실 바인딩 아님")
        // (6) ConsentStore — 실: PrefsConsentStore(SharedPreferences, M9-후속 §2-F)
        assertTrue(koin.get<ConsentStore>() is PrefsConsentStore, "ConsentStore가 실 바인딩 아님")
        // (7) DeviceInfo — 실: AndroidDeviceInfo(Context)
        assertTrue(koin.get<DeviceInfo>() is AndroidDeviceInfo, "DeviceInfo가 실 바인딩 아님")

        // ── 하중 바인딩 강제: 실 DB를 실제로 열어 스키마 create + 쿼리 왕복(구성만이 아닌 실 오픈 증명) ──
        db.devEtymQueries.insertOrReplaceTerm(
            keyword = "graph-probe", aliases = "[]", category = "기타", summary = "s", etymology = "e",
            namingReason = "n", source = "BUNDLE", isBookmarked = 0, createdAt = 1L,
            seenAt = null, schemaVersion = null, promptVersion = null,
        )
        assertTrue(
            db.devEtymQueries.selectAllTerms().executeAsList().any { it.keyword == "graph-probe" },
            "실 AndroidSqliteDriver DB 왕복 실패 — DB 바인딩이 실 해석되지 않음(DR-1 하중 바인딩 미폐쇄)",
        )

        // ── KoinAppDependencies 전 seam eager-touch(lazy getter라 실제 건드려야 누락 발화) ──
        val deps = KoinAppDependencies(koin)
        assertNotNull(deps.searchViewModel)
        assertNotNull(deps.bookmarkViewModel)
        assertNotNull(deps.historyViewModel)
        assertNotNull(deps.createDetailViewModel())
        assertNotNull(deps.actions)
        assertNotNull(deps.appearance)
        assertNotNull(deps.device)
        assertNotNull(deps.onboarding)
        assertNotNull(deps.consent)   // M9-후속 §2-F
        assertNotNull(deps.now())
    }
}
