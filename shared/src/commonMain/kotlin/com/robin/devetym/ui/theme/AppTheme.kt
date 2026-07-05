package com.robin.devetym.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable

/**
 * 테마 프로바이더 (M6 §3-5). 커스텀 토큰(색/타이포/간격)을 `CompositionLocal`로 내리고 Material은
 * 스캐폴드 컴포넌트 색만 담당. 앱은 주로 `AppScheme`로 커스텀 토큰을 직접 참조. 다크 기본.
 */
@Composable
fun AppTheme(dark: Boolean = true, content: @Composable () -> Unit) {
    val colors = if (dark) DarkColors else LightColors
    val fonts = rememberAppFonts()
    val typography = appTypography(fonts)
    CompositionLocalProvider(
        LocalAppColors provides colors,
        LocalAppTypography provides typography,
        LocalAppDimens provides AppDimens(),
    ) {
        MaterialTheme(colorScheme = colors.toMaterialColorScheme(dark)) { content() }
    }
}

/** 토큰 접근 편의(iOS `.typoX` 단일 진입점 계승). */
object AppScheme {
    val colors: AppColors
        @Composable @ReadOnlyComposable get() = LocalAppColors.current
    val type: AppTypography
        @Composable @ReadOnlyComposable get() = LocalAppTypography.current
    val dim: AppDimens
        @Composable @ReadOnlyComposable get() = LocalAppDimens.current
}

/** 커스텀 팔레트 → Material colorScheme 최소 매핑(스캐폴드 컴포넌트 색만). */
private fun AppColors.toMaterialColorScheme(dark: Boolean) =
    if (dark) {
        darkColorScheme(
            primary = accent, background = bg, surface = surface, onPrimary = bg,
            onBackground = text, onSurface = text,
        )
    } else {
        lightColorScheme(
            primary = accent, background = bg, surface = surface, onPrimary = surface,
            onBackground = text, onSurface = text,
        )
    }
