package com.robin.devetym.ui

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * M9-후속 셸 재설계 §2-A — 탭별 백스택 순수 로직 계약(commonTest → 양 축 실행).
 * push/pop/replaceTop/popToRoot + 탭 간 격리 + 빈 스택 안전.
 */
class TabNavStateTest {

    @Test
    fun test_push_pop_뎁스왕복() {
        var nav = TabNavState()
        assertNull(nav.top(0), "초기 스택은 빈 상태")
        nav = nav.push(0, Route.Detail("mutex"))
        assertEquals(Route.Detail("mutex"), nav.top(0))
        nav = nav.push(0, Route.Detail("daemon"))
        assertEquals(2, nav.stack(0).size, "2뎁스 push 누적")
        nav = nav.pop(0)
        assertEquals(Route.Detail("mutex"), nav.top(0), "pop 후 이전 top 복원")
        nav = nav.pop(0)
        assertNull(nav.top(0), "루트 복귀")
    }

    @Test
    fun test_빈스택_pop_noop_안전() {
        val nav = TabNavState().pop(3)
        assertEquals(0, nav.stack(3).size, "빈 스택 pop은 no-op(음수 뎁스 금지)")
    }

    @Test
    fun test_replaceTop_possibleTypo_교체() {
        var nav = TabNavState().push(0, Route.Detail("mutx"))
        nav = nav.replaceTop(0, Route.Detail("mutex"))
        assertEquals(1, nav.stack(0).size, "replaceTop은 뎁스 유지")
        assertEquals(Route.Detail("mutex"), nav.top(0), "top이 제안 키워드로 교체")
        // 빈 스택 replaceTop = push와 동일(방어)
        assertEquals(Route.Licenses, TabNavState().replaceTop(1, Route.Licenses).top(1))
    }

    @Test
    fun test_popToRoot_재탭관례() {
        var nav = TabNavState().push(2, Route.Detail("a")).push(2, Route.Detail("b"))
        nav = nav.popToRoot(2)
        assertNull(nav.top(2), "재탭 시 루트로 전부 pop")
    }

    @Test
    fun test_탭간_격리() {
        var nav = TabNavState().push(0, Route.Detail("mutex")).push(3, Route.Licenses)
        nav = nav.popToRoot(0)
        assertNull(nav.top(0), "탭0만 비움")
        assertEquals(Route.Licenses, nav.top(3), "탭3 스택은 영향 없음(라이선스는 Settings 스택 소속)")
    }
}
