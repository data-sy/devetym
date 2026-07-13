package com.robin.devetym.ui

import com.robin.devetym.ui.platform.StubAppearanceStore
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * M9 §3-3 (i)(ii) — 외관 seam 순수 로직 계약(commonTest → 양 축 실행).
 * (ii) `resolveDarkMode` 매핑 · (i) `AppearanceStore.set`→emit 전파.
 */
class AppearanceMappingTest {

    @Test
    fun test_appearanceMode_dark매핑() {
        assertTrue(resolveDarkMode(2, systemDark = false), "mode=2는 항상 다크")
        assertFalse(resolveDarkMode(1, systemDark = true), "mode=1은 항상 라이트")
        // mode=0(시스템)은 systemDark에 위임
        assertTrue(resolveDarkMode(0, systemDark = true), "mode=0은 시스템 다크 위임")
        assertFalse(resolveDarkMode(0, systemDark = false), "mode=0은 시스템 라이트 위임")
        // 범위 밖은 시스템으로 폴백(코드에 클램프 없음 — else 분기)
        assertTrue(resolveDarkMode(99, systemDark = true), "범위 밖은 시스템 위임(else)")
    }

    @Test
    fun test_edgeSwipeBack_판정() {
        // M9-후속 UX-2 — 엣지 시작 && 우측 누적 드래그 임계 초과만 back.
        val edge = 24f; val th = 80f
        assertTrue(isEdgeSwipeBack(startX = 10f, totalDragX = 120f, edgeWidth = edge, threshold = th), "엣지 시작+임계 초과")
        assertTrue(isEdgeSwipeBack(startX = 24f, totalDragX = 80.1f, edgeWidth = edge, threshold = th), "경계 포함(엣지)·임계 초과")
        assertFalse(isEdgeSwipeBack(startX = 25f, totalDragX = 300f, edgeWidth = edge, threshold = th), "엣지 밖 시작은 무시(본문 드래그)")
        assertFalse(isEdgeSwipeBack(startX = 10f, totalDragX = 80f, edgeWidth = edge, threshold = th), "임계 미달(경계 미포함)")
        assertFalse(isEdgeSwipeBack(startX = 10f, totalDragX = -120f, edgeWidth = edge, threshold = th), "좌측 드래그는 back 아님")
    }

    @Test
    fun test_keyboardDismissDrag_판정() {
        // M9-후속 §2-C — 아래(+y) 누적 드래그 임계 초과만 dismiss.
        val th = 24f
        assertTrue(isKeyboardDismissDrag(totalDragY = 25f, threshold = th), "임계 초과 아래 드래그는 dismiss")
        assertFalse(isKeyboardDismissDrag(totalDragY = 24f, threshold = th), "임계 미달(경계 미포함)")
        assertFalse(isKeyboardDismissDrag(totalDragY = -80f, threshold = th), "위 방향 드래그는 무시")
    }

    @Test
    fun test_appearance_set_emit() {
        val store = StubAppearanceStore(initial = 2)
        assertEquals(2, store.mode.value, "초기값 다크")
        store.set(1)
        assertEquals(1, store.mode.value, "set(1) 후 emit=1")
        store.set(0)
        assertEquals(0, store.mode.value, "set(0) 후 emit=0")
    }
}
