package com.robin.devetym.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.ui.unit.IntOffset
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.drag
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.robin.devetym.ui.theme.AppScheme

/**
 * push 화면 공통 컨테이너 (M9-후속 셸 재설계 §2-A) — 전환 애니·엣지 스와이프-백·고정 top bar를
 * 단일 소유해 화면별 자체 back(§1-2)·상세 한정 ad hoc 제스처(§1-3)·전환 부재(§1-4, UX-5)를 대체한다.
 * 모든 push 화면이 자동 상속 — 화면은 순수 content로 되돌아간다.
 *
 * 전환: push=오른쪽 진입 + 뒤 화면 30% 패럴랙스 퇴장, pop=역방향(iOS 관례 근사).
 * 손가락 추종 인터랙티브 스와이프백은 후속(§5-1) — 전환 애니만으로 "휙"(UX-5) 해소.
 */
@Composable
fun NavContainer(
    stack: List<Route>,
    onBack: () -> Unit,
    root: @Composable () -> Unit,
    screen: @Composable (Route) -> Unit,
) {
    AnimatedContent(
        targetState = stack,
        transitionSpec = {
            val push = targetState.size >= initialState.size   // 동뎁스 교체(replaceTop)도 전진으로 취급
            // 기본 spring은 iOS 네이티브 백(~0.35s 강한 감속)보다 굼뜨게 감긴다(실기기 라운드 2 피드백)
            // → 300ms 감속 tween으로 고정. 손가락 추종 인터랙티브 스와이프백은 §5-1 후속.
            val motion = tween<IntOffset>(durationMillis = 300, easing = LinearOutSlowInEasing)
            val spec = if (push) {
                slideInHorizontally(motion) { it } togetherWith slideOutHorizontally(motion) { -it / 3 }
            } else {
                slideInHorizontally(motion) { -it / 3 } togetherWith slideOutHorizontally(motion) { it }
            }
            // 깊은 스택이 항상 위 — push는 새 화면이 덮고, pop은 떠나는 화면이 위에서 빠진다.
            spec.apply { targetContentZIndex = targetState.size.toFloat() }
        },
        modifier = Modifier.fillMaxSize(),
    ) { frame ->
        val top = frame.lastOrNull()
        if (top == null) {
            root()
        } else {
            // 배경 불투명 필수 — 패럴랙스 전환 중 뒤 화면 비침 방지. 엣지 스와이프-백은 실제 다운
            // 지점(awaitFirstDown) 판정 — 슬롭 밀림 함정(시뮬 스모크 리포트 §6) 그대로 승계.
            Box(
                Modifier.fillMaxSize().background(AppScheme.colors.bg).pointerInput(top) {
                    val edge = EDGE_SWIPE_EDGE_DP.dp.toPx()
                    val threshold = EDGE_SWIPE_THRESHOLD_DP.dp.toPx()
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        var dragX = 0f
                        drag(down.id) { change ->
                            dragX += change.position.x - change.previousPosition.x
                        }
                        if (isEdgeSwipeBack(down.position.x, dragX, edge, threshold)) onBack()
                    }
                },
            ) {
                Column(Modifier.fillMaxSize()) {
                    // 고정 top bar(§2-A-3) — 스크롤과 무관하게 상단 고정. safe area는 AppSurface(§2-B) 소관.
                    Text(
                        "← 뒤로", style = AppScheme.type.codeAction, color = AppScheme.colors.accent,
                        modifier = Modifier.clickable(onClick = onBack)
                            .padding(horizontal = AppScheme.dim.screenPadding, vertical = 16.dp),
                    )
                    Box(Modifier.weight(1f)) { screen(top) }
                }
            }
        }
    }
}
