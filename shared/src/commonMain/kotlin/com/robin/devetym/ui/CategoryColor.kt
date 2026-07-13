package com.robin.devetym.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
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

/**
 * 톤 알약 전경 (M9-후속 UX-1) — accent 틴트 컨테이너 위 WCAG AA 보장 쌍: 다크=accent(≈11:1),
 * 라이트는 accent가 4.5:1 미달(≈4.1)이라 brand 딥그린(≈6:1). AccessibilityContrastTest 톤버튼 게이트 실측.
 */
fun tonalActionColor(colors: AppColors): Color =
    if (colors.bg.luminance() < 0.5f) colors.accent else colors.brand
