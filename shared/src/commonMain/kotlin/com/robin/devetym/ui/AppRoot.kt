package com.robin.devetym.ui

import com.robin.devetym.Constants
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.robin.devetym.ui.platform.AppActions
import com.robin.devetym.ui.platform.AppearanceStore
import com.robin.devetym.ui.platform.DeviceInfo
import com.robin.devetym.ui.platform.OnboardingStore
import com.robin.devetym.ui.screens.LicensesScreen
import com.robin.devetym.ui.screens.BookmarkScreen
import com.robin.devetym.ui.screens.DetailScreen
import com.robin.devetym.ui.screens.HistoryScreen
import com.robin.devetym.ui.screens.OnboardingScreen
import com.robin.devetym.ui.screens.SearchScreen
import com.robin.devetym.ui.screens.SettingsScreen
import com.robin.devetym.ui.theme.AppScheme
import com.robin.devetym.ui.theme.AppTheme

/**
 * 앱 의존성 홀더 (M6 §3-8). ViewModel·seam을 제공. **실배선(Koin)은 M7** — M6는 이 인터페이스 형태만
 * 정의하고 AppRoot가 소비(셸 연결 안 함, 진입점은 여전히 M0 Greeting).
 */
interface AppDependencies {
    val searchViewModel: SearchViewModel
    val bookmarkViewModel: BookmarkViewModel
    val historyViewModel: HistoryViewModel
    fun createDetailViewModel(): DetailViewModel
    val actions: AppActions
    val appearance: AppearanceStore
    val device: DeviceInfo
    val onboarding: OnboardingStore
    fun now(): Long
}

private enum class Tab(val label: String) { Search("검색"), Bookmark("북마크"), History("히스토리"), Settings("설정") }

/**
 * 외관 mode→dark 매핑 (M9 §3-3 ii) — `AppRoot`의 `@Composable` 인라인에서 추출한 순수 함수(테스트 대상).
 * 0=시스템(`systemDark` 위임)·1=라이트(false)·2=다크(true)·그 외=시스템. Compose 무의존이라 계약 테스트 가능.
 */
fun resolveDarkMode(mode: Int, systemDark: Boolean): Boolean = when (mode) {
    1 -> false
    2 -> true
    else -> systemDark
}

/**
 * 앱 루트 (M6 §3-8) — 의존성-0 상태기반 네비(탭별 단일 push 스택 + 온보딩 게이트). navigation-compose
 * 네이티브 링크 리스크 회피(§7-1 안전 폴백). 온보딩·동의 영속은 M7/M8 seam.
 */
@Composable
fun AppRoot(deps: AppDependencies) {
    // M8 §3-6 외관 배선: appearance.mode(0=시스템·1=라이트·2=다크)를 실제 테마로 반영(inert 제거).
    // ⚠️ set→emit·재구성 전파의 실제 테마 전환은 실기기 천장(assembleDebug/link는 매핑 컴파일만 보증).
    val mode by deps.appearance.mode.collectAsStateWithLifecycle()
    val darkMode = resolveDarkMode(mode, isSystemInDarkTheme())
    AppTheme(dark = darkMode) {
        var onboarded by rememberSaveable { mutableStateOf(deps.onboarding.completed) }   // M8 영속 게이트
        if (!onboarded) {
            OnboardingScreen(onComplete = { deps.onboarding.complete(); onboarded = true })
            return@AppTheme
        }
        var showLicenses by rememberSaveable { mutableStateOf(false) }   // M8 DR-2: 라이선스 오버레이
        if (showLicenses) {
            LicensesScreen(onBack = { showLicenses = false })
            return@AppTheme
        }
        var tab by rememberSaveable { mutableStateOf(Tab.Search) }
        // 탭별 상세 push 키워드(단일 레벨 — possibleTypo는 교체). null=탭 루트.
        val detailKeys = remember { mutableStateMapOf<Tab, String?>() }

        Scaffold(
            bottomBar = {
                NavigationBar(containerColor = AppScheme.colors.surface) {
                    Tab.entries.forEach { t ->
                        NavigationBarItem(
                            selected = tab == t,
                            // 활성 탭 재탭 = 루트 pop(상세 닫기) — iOS 탭바 관례. M9 스모크 결함
                            // (Found 상세 탈출 불가)의 보조 탈출구(주 탈출구는 DetailScreen 상시 back).
                            onClick = { if (tab == t) detailKeys[t] = null else tab = t },
                            icon = { Text(t.label, style = AppScheme.type.caption) },
                        )
                    }
                }
            },
        ) { padding ->
            Box(Modifier.fillMaxSize().padding(padding)) {
                val detailKey = detailKeys[tab]
                val openDetail: (String) -> Unit = { detailKeys[tab] = it }
                val back: () -> Unit = { detailKeys[tab] = null }
                if (detailKey != null) {
                    key(tab, detailKey) {
                        val detailVm = remember(tab, detailKey) { deps.createDetailViewModel() }
                        LaunchedEffect(detailKey) { detailVm.load(detailKey) }
                        DetailScreen(
                            keyword = detailKey,
                            vm = detailVm,
                            bookmarkVm = deps.bookmarkViewModel,
                            onBack = back,
                            onSelectSuggestion = { detailKeys[tab] = it },
                            onShare = deps.actions::share,
                            onCopy = deps.actions::copyToClipboard,
                            onReport = { deps.actions.sendMail(Constants.supportEmail, "DevEtym 오류 제보: $it", "") },
                        )
                    }
                } else {
                    when (tab) {
                        Tab.Search -> SearchScreen(deps.searchViewModel, openDetail)
                        Tab.Bookmark -> BookmarkScreen(deps.bookmarkViewModel, openDetail)
                        Tab.History -> HistoryScreen(deps.historyViewModel, deps.now(), openDetail)
                        Tab.Settings -> {
                            var consent by rememberSaveable { mutableStateOf(true) }
                            SettingsScreen(
                                actions = deps.actions,
                                appearance = deps.appearance,
                                device = deps.device,
                                consentGiven = consent,
                                onConsentChange = { consent = it },
                                onOpenLicenses = { showLicenses = true },   // M8: in-app OFL 고지
                            )
                        }
                    }
                }
            }
        }
    }
}
