package com.robin.devetym.ui

import androidx.compose.ui.graphics.Color
import com.robin.devetym.model.Category
import com.robin.devetym.ui.theme.AppColors

/**
 * 카테고리 뱃지 색 순수 매핑 (M6 §3-6·§6). iOS는 카테고리 6종 전부 **accent 단색** — 범위 밖 카테고리도
 * accent로 클램프(크래시 없음). §6 `test_categoryColor_클램프` 실측.
 */
fun categoryColor(category: String, colors: AppColors): Color = when (category) {
    in Category.CANONICAL -> colors.accent
    else -> colors.accent   // 범위밖도 accent 기본(클램프)
}
