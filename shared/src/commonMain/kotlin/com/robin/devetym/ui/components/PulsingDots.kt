package com.robin.devetym.ui.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.robin.devetym.ui.theme.AppScheme

/**
 * 로딩 인디케이터 (M6 §3-6) — 좌→우 pulse 3닷. 애니메이션 타이밍은 검증 천장(§0), 컴파일만 보증.
 */
@Composable
fun PulsingDots() {
    val colors = AppScheme.colors
    val transition = rememberInfiniteTransition(label = "dots")
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        repeat(3) { i ->
            val alpha by transition.animateFloat(
                initialValue = 0.3f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(durationMillis = 600, delayMillis = i * 200),
                    repeatMode = RepeatMode.Reverse,
                ),
                label = "dot$i",
            )
            androidx.compose.foundation.layout.Box(
                Modifier.size(8.dp).alpha(alpha).clip(CircleShape).background(colors.accent),
            )
        }
    }
}
