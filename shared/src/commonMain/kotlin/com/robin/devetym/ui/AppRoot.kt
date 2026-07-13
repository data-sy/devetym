package com.robin.devetym.ui

import com.robin.devetym.Constants
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import kotlinx.coroutines.launch
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.robin.devetym.ui.platform.AppActions
import com.robin.devetym.ui.platform.AppearanceStore
import com.robin.devetym.ui.platform.ConsentStore
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
    val consent: ConsentStore
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

/** 엣지 스와이프-백 판정 폭/임계 (M9-후속 UX-2) — iOS 시스템 백 제스처 관례 근사. */
const val EDGE_SWIPE_EDGE_DP = 24
const val EDGE_SWIPE_THRESHOLD_DP = 80

/**
 * 엣지 스와이프-백 판정 (M9-후속 UX-2) — 순수 함수(테스트 대상). 드래그 시작점이 왼쪽 엣지 폭 안이고
 * 우측 누적 드래그가 임계를 넘으면 back. 자체 상태기반 네비라 iOS 시스템 백 제스처가 없어 직접 구현.
 */
fun isEdgeSwipeBack(startX: Float, totalDragX: Float, edgeWidth: Float, threshold: Float): Boolean =
    startX <= edgeWidth && totalDragX > threshold

/**
 * 앱 루트 (M6 §3-8 → M9-후속 셸 재설계 §2 재편) — 의존성-0 상태기반 네비. navigation-compose
 * 네이티브 링크 리스크 회피(§7-1 안전 폴백)는 유지하되, 셸 계층을 정본화:
 * `AppSurface`(배경·인셋 §2-B) ▸ 온보딩 게이트 ▸ Scaffold+페이저(뎁스0) ▸ `NavContainer`(push 스택 §2-A).
 * 네비 상태 정본 = pagerState(탭) + `TabNavState`(탭별 백스택) — 종전 detailKeys·showLicenses 분산(§1-1) 폐지.
 */
@Composable
fun AppRoot(deps: AppDependencies) {
    // M8 §3-6 외관 배선: appearance.mode(0=시스템·1=라이트·2=다크)를 실제 테마로 반영(inert 제거).
    val mode by deps.appearance.mode.collectAsStateWithLifecycle()
    val darkMode = resolveDarkMode(mode, isSystemInDarkTheme())
    AppTheme(dark = darkMode) {
        AppSurface {
            var onboarded by rememberSaveable { mutableStateOf(deps.onboarding.completed) }   // M8 영속 게이트
            if (!onboarded) {
                // 온보딩은 네비 밖 게이트 유지(§2-A) — 단 AppSurface 안이라 배경·인셋 규율은 상속.
                OnboardingScreen(onComplete = { deps.onboarding.complete(); onboarded = true })
                return@AppSurface
            }
            // M9-후속 UX-2: 탭 상태 정본 = pagerState(자체 Saver로 영속). 뎁스0은 좌우 스와이프 전환.
            val pagerState = rememberPagerState { Tab.entries.size }
            val currentTab = Tab.entries[pagerState.currentPage]
            val scope = rememberCoroutineScope()
            var nav by remember { mutableStateOf(TabNavState()) }

            Scaffold(
                bottomBar = {
                    NavigationBar(containerColor = AppScheme.colors.surface) {
                        Tab.entries.forEach { t ->
                            NavigationBarItem(
                                selected = currentTab == t,
                                // 활성 탭 재탭 = 루트 pop — iOS 탭바 관례(M9 스모크 보조 탈출구).
                                onClick = {
                                    if (currentTab == t) nav = nav.popToRoot(t.ordinal)
                                    else scope.launch { pagerState.animateScrollToPage(t.ordinal) }
                                },
                                icon = { Text(t.label, style = AppScheme.type.caption) },
                            )
                        }
                    }
                },
            ) { padding ->
                HorizontalPager(
                    state = pagerState,
                    // UX-2 제스처 충돌 관리: push 화면 표시 중엔 페이저 스와이프 비활성 —
                    // 좌우 드래그는 엣지 스와이프-백(NavContainer) 전용.
                    userScrollEnabled = nav.top(currentTab.ordinal) == null,
                    modifier = Modifier.fillMaxSize().padding(padding),
                ) { page ->
                    val t = Tab.entries[page]
                    val openDetail: (String) -> Unit = { nav = nav.push(t.ordinal, Route.Detail(it)) }
                    NavContainer(
                        stack = nav.stack(t.ordinal),
                        onBack = { nav = nav.pop(t.ordinal) },
                        root = {
                            when (t) {
                                Tab.Search -> SearchScreen(deps.searchViewModel, openDetail)
                                Tab.Bookmark -> BookmarkScreen(deps.bookmarkViewModel, openDetail)
                                Tab.History -> HistoryScreen(deps.historyViewModel, deps.now(), openDetail)
                                Tab.Settings -> {
                                    // M9-후속 §2-F: ConsentStore seam 영속.
                                    val consent by deps.consent.given.collectAsStateWithLifecycle()
                                    SettingsScreen(
                                        actions = deps.actions,
                                        appearance = deps.appearance,
                                        device = deps.device,
                                        consentGiven = consent,
                                        onConsentChange = deps.consent::set,
                                        // M8 DR-2 in-app OFL 고지 — 이제 Settings 스택 push(§2-A).
                                        onOpenLicenses = { nav = nav.push(t.ordinal, Route.Licenses) },
                                    )
                                }
                            }
                        },
                        screen = { route ->
                            when (route) {
                                is Route.Detail -> key(t, route.keyword) {
                                    val detailVm = remember(t, route.keyword) { deps.createDetailViewModel() }
                                    LaunchedEffect(route.keyword) { detailVm.load(route.keyword) }
                                    DetailScreen(
                                        keyword = route.keyword,
                                        vm = detailVm,
                                        bookmarkVm = deps.bookmarkViewModel,
                                        onBack = { nav = nav.pop(t.ordinal) },
                                        // possibleTypo 제안 수락 = top 교체(§6 — 뎁스 유지).
                                        onSelectSuggestion = { nav = nav.replaceTop(t.ordinal, Route.Detail(it)) },
                                        onShare = deps.actions::share,
                                        onCopy = deps.actions::copyToClipboard,
                                        onReport = { deps.actions.sendMail(Constants.supportEmail, "DevEtym 오류 제보: $it", "") },
                                    )
                                }
                                Route.Licenses -> LicensesScreen()
                            }
                        },
                    )
                }
            }
        }
    }
}
