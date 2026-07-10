package com.robin.devetym.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * 타이포 토큰 (M6 §3-3). iOS `Theme.swift` Typography 21종을 `TextStyle`로 이관.
 *
 * **정본에서 직접 얻는 값은 (fontFamily, fontSize, fontWeight)뿐**이다(`Theme.swift`는 폰트+size만 —
 * lineHeight/letterSpacing 값 없음). letterSpacing/lineHeight의 정확값은 iOS Feature 뷰의 `.tracking()`/
 * `.lineSpacing()`에 흩어져 있어 **시각 천장(§0)으로 이월** — 아래 letterSpacing은 시각 근사치이며 green이
 * 보증하지 않는다(§6은 패밀리·대표 size/weight만 실측).
 */
@Immutable
data class AppTypography(
    val titleHero: TextStyle,
    val titleTab: TextStyle,
    val bodyLarge: TextStyle,
    val body: TextStyle,
    val bodySub: TextStyle,
    val bodyEmphasis: TextStyle,
    val bodyBlock: TextStyle,
    val bodyNotice: TextStyle,
    val bodyPreview: TextStyle,
    val bodyPreviewSmall: TextStyle,
    val codeHero: TextStyle,
    val codeBody: TextStyle,
    val codeInput: TextStyle,
    val codeValue: TextStyle,
    val badge: TextStyle,
    val badgeAI: TextStyle,
    val codeChip: TextStyle,
    val codeAction: TextStyle,
    val sectionHeader: TextStyle,
    val label: TextStyle,
    val caption: TextStyle,
)

/** `AppFonts`로부터 21 토큰 구성. size/weight는 정본, letterSpacing은 시각 근사(천장). */
fun appTypography(f: AppFonts): AppTypography = AppTypography(
    titleHero = TextStyle(fontFamily = f.serifFamily, fontSize = 28.sp, fontWeight = FontWeight.Normal, letterSpacing = (-0.3).sp),
    titleTab = TextStyle(fontFamily = f.serifFamily, fontSize = 20.sp, fontWeight = FontWeight.Normal, letterSpacing = (-0.2).sp),
    bodyLarge = TextStyle(fontFamily = f.bodyFamily, fontSize = 17.sp, letterSpacing = (-0.05).sp),
    body = TextStyle(fontFamily = f.bodyFamily, fontSize = 16.sp, lineHeight = 24.sp, letterSpacing = (-0.1).sp),
    bodySub = TextStyle(fontFamily = f.bodyFamily, fontSize = 16.sp, lineHeight = 22.sp),
    bodyEmphasis = TextStyle(fontFamily = f.bodyFamily, fontSize = 17.sp, fontWeight = FontWeight.SemiBold, letterSpacing = (-0.1).sp),
    bodyBlock = TextStyle(fontFamily = f.bodyFamily, fontSize = 16.sp, lineHeight = 24.sp),
    bodyNotice = TextStyle(fontFamily = f.bodyFamily, fontSize = 16.sp, lineHeight = 20.sp),
    bodyPreview = TextStyle(fontFamily = f.bodyFamily, fontSize = 17.sp, letterSpacing = (-0.1).sp),
    bodyPreviewSmall = TextStyle(fontFamily = f.bodyFamily, fontSize = 13.sp, letterSpacing = (-0.05).sp),
    codeHero = TextStyle(fontFamily = f.codeFamily, fontSize = 20.sp, fontWeight = FontWeight.Medium, letterSpacing = (-0.3).sp),
    codeBody = TextStyle(fontFamily = f.codeFamily, fontSize = 17.sp, fontWeight = FontWeight.Medium, letterSpacing = (-0.2).sp),
    codeInput = TextStyle(fontFamily = f.codeFamily, fontSize = 17.sp, letterSpacing = (-0.2).sp),
    codeValue = TextStyle(fontFamily = f.codeFamily, fontSize = 15.sp, letterSpacing = (-0.1).sp),
    badge = TextStyle(fontFamily = f.codeFamily, fontSize = 12.sp, fontWeight = FontWeight.Medium, letterSpacing = 0.8.sp),
    badgeAI = TextStyle(fontFamily = f.bodyFamily, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 0.2.sp),
    codeChip = TextStyle(fontFamily = f.codeFamily, fontSize = 13.sp, fontWeight = FontWeight.Medium, letterSpacing = (-0.1).sp),
    codeAction = TextStyle(fontFamily = f.bodyFamily, fontSize = 16.sp, fontWeight = FontWeight.Medium, letterSpacing = (-0.05).sp),
    sectionHeader = TextStyle(fontFamily = f.bodyFamily, fontSize = 15.sp, fontWeight = FontWeight.Medium, letterSpacing = 0.6.sp),
    label = TextStyle(fontFamily = f.bodyFamily, fontSize = 15.sp, letterSpacing = (-0.05).sp),
    caption = TextStyle(fontFamily = f.bodyFamily, fontSize = 12.sp, letterSpacing = 0.4.sp),
)

// 폰트가 필요해 초기값은 시스템 패밀리 근사. 실제 값은 AppTheme가 rememberAppFonts로 주입.
private val fallbackFonts = AppFonts(
    androidx.compose.ui.text.font.FontFamily.Default,
    androidx.compose.ui.text.font.FontFamily.Monospace,
    androidx.compose.ui.text.font.FontFamily.Serif,
    androidx.compose.ui.text.font.FontFamily.Default,
)
val LocalAppTypography = staticCompositionLocalOf { appTypography(fallbackFonts) }
