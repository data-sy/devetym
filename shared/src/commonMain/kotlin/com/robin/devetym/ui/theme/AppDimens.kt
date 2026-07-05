package com.robin.devetym.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * 간격/모양 토큰 (M6 §3-4). iOS Feature 뷰 인라인 관찰값에서 **신규 추출**(iOS엔 토큰 정본 없음 —
 * 이 값이 M6가 세우는 정본). 개별 화면 실측 간격 정확성은 시각 천장(§0).
 */
@Immutable
data class AppDimens(
    val screenPadding: Dp = 18.dp,
    val cardPadding: Dp = 14.dp,
    val sectionGap: Dp = 26.dp,
    val labelGap: Dp = 9.dp,
    val rowVPad: Dp = 11.dp,
    val radiusCard: Dp = 12.dp,
    val radiusAction: Dp = 10.dp,
    val radiusBlock: Dp = 8.dp,
    val radiusBadge: Dp = 6.dp,
    val strokeBorder: Dp = 1.dp,
    val strokeField: Dp = 1.5.dp,
)

val LocalAppDimens = staticCompositionLocalOf { AppDimens() }
