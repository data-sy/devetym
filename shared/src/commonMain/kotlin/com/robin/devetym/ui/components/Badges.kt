package com.robin.devetym.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.robin.devetym.ui.categoryColor
import com.robin.devetym.ui.theme.AppScheme

/** 카테고리 뱃지 (M6 §3-6) — surface2 fill + accent 텍스트. iOS 단색 계승. */
@Composable
fun CategoryBadge(category: String) {
    val colors = AppScheme.colors
    Text(
        text = category,
        style = AppScheme.type.badge,
        color = categoryColor(category, colors),
        modifier = Modifier
            .clip(RoundedCornerShape(AppScheme.dim.radiusBadge))
            .background(colors.surface2)
            .padding(horizontal = 8.dp, vertical = 3.dp),
    )
}

/** AI 생성 뱃지 (M6 §3-6) — source==AI일 때만 상세가 표시. */
@Composable
fun AiBadge() {
    val colors = AppScheme.colors
    Text(
        text = "✦ AI 생성",
        style = AppScheme.type.badgeAI,
        color = colors.accentAI,
        modifier = Modifier
            .clip(RoundedCornerShape(AppScheme.dim.radiusBadge))
            .background(colors.surface2)
            .padding(horizontal = 8.dp, vertical = 3.dp),
    )
}
