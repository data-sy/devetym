package com.robin.devetym.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * 앱 색상 토큰 (M6 §3-2). 정본 hex = iOS `Assets.xcassets/Theme/<name>.colorset`(11토큰 라이트/다크).
 * `Theme.swift` Palette는 Asset 참조 이름만 담아 hex 대조 소스는 colorset. 다크 기본.
 */
@Immutable
data class AppColors(
    val bg: Color,
    val surface: Color,
    val surface2: Color,
    val border: Color,
    val accent: Color,
    val accent2: Color,
    val accentAI: Color,
    val brand: Color,
    val text: Color,
    val textDim: Color,
    val textMuted: Color,
)

val LightColors = AppColors(
    bg = Color(0xFFFAFAFA), surface = Color(0xFFFFFFFF), surface2 = Color(0xFFF1F1F1), border = Color(0xFFE4E4E4),
    accent = Color(0xFF3F7A00), accent2 = Color(0xFF1E6B94), accentAI = Color(0xFFA35A10), brand = Color(0xFF2E5D3A),
    text = Color(0xFF0A0A0A), textDim = Color(0xFF555555), textMuted = Color(0xFF6B6B6B),
)

val DarkColors = AppColors(
    bg = Color(0xFF0A0A0A), surface = Color(0xFF111111), surface2 = Color(0xFF1A1A1A), border = Color(0xFF363636),
    accent = Color(0xFFC8F060), accent2 = Color(0xFF60C8F0), accentAI = Color(0xFFF0A060), brand = Color(0xFF2E5D3A),
    text = Color(0xFFECECEC), textDim = Color(0xFFB4B4B4), textMuted = Color(0xFF8A8A8A),
)

val LocalAppColors = staticCompositionLocalOf { DarkColors }   // 다크 기본(iOS appearanceMode 기본 2 계승)
