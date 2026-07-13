package com.robin.devetym.ui

/**
 * 네비게이션 route 정본 (M9-후속 셸 재설계 §2-A). 화면 추가 시 여기 확장.
 * 라이선스는 전역 오버레이가 아니라 Settings 탭 스택 소속(§1-1 해소) — 인셋·제스처·전환을 자동 상속.
 */
sealed interface Route {
    data class Detail(val keyword: String) : Route
    data object Licenses : Route
}

/**
 * 탭별 백스택 순수 로직 (M9-후속 셸 재설계 §2-A) — Compose 무의존 불변 값(commonTest 대상).
 * `AppRoot`가 `mutableStateOf`로 관찰하며, 종전 `detailKeys: Map<Tab,String?>`·`showLicenses: Boolean`
 * 분산 상태(§1-1)를 대체한다. 탭 키는 pager와 동일한 ordinal.
 */
data class TabNavState(private val stacks: Map<Int, List<Route>> = emptyMap()) {
    fun stack(tab: Int): List<Route> = stacks[tab] ?: emptyList()
    fun top(tab: Int): Route? = stack(tab).lastOrNull()

    fun push(tab: Int, route: Route): TabNavState =
        copy(stacks = stacks + (tab to stack(tab) + route))

    /** 빈 스택 pop은 no-op(안전) — back 연타·엣지 스와이프 경합에서 음수 뎁스 방지. */
    fun pop(tab: Int): TabNavState =
        copy(stacks = stacks + (tab to stack(tab).dropLast(1)))

    /** possibleTypo 제안 수락용(§6) — top 교체. 빈 스택이면 push와 동일(방어). */
    fun replaceTop(tab: Int, route: Route): TabNavState =
        copy(stacks = stacks + (tab to stack(tab).dropLast(1) + route))

    /** 활성 탭 재탭 관례(M9 스모크 보조 탈출구) — 루트로 전부 pop. */
    fun popToRoot(tab: Int): TabNavState =
        copy(stacks = stacks + (tab to emptyList()))
}
