package com.robin.devetym.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import com.robin.devetym.resources.Res
import com.robin.devetym.resources.dmmono_light
import com.robin.devetym.resources.dmmono_medium
import com.robin.devetym.resources.dmmono_regular
import com.robin.devetym.resources.dmsans_medium
import com.robin.devetym.resources.dmsans_regular
import com.robin.devetym.resources.dmserifdisplay_italic
import com.robin.devetym.resources.dmserifdisplay_regular
import org.jetbrains.compose.resources.Font

/**
 * 폰트 패밀리 (M6 §3-1). 하이브리드 전략(iOS Theme.swift 계승):
 * - `bodyFamily` = `FontFamily.Default`(플랫폼 시스템 — 한글이 커스텀 폰트 박스에 작게 끼는 문제 회피)
 * - `codeFamily` = DM Mono(영문 코드·키워드)
 * - `serifFamily` = DM Serif(시그니처 헤더)
 */
@Immutable
data class AppFonts(
    val bodyFamily: FontFamily,
    val codeFamily: FontFamily,
    val serifFamily: FontFamily,
    val sansFamily: FontFamily,
)

@Composable
fun rememberAppFonts(): AppFonts = AppFonts(
    bodyFamily = FontFamily.Default,
    codeFamily = FontFamily(
        Font(Res.font.dmmono_light, FontWeight.Light),
        Font(Res.font.dmmono_regular, FontWeight.Normal),
        Font(Res.font.dmmono_medium, FontWeight.Medium),
    ),
    serifFamily = FontFamily(
        Font(Res.font.dmserifdisplay_regular, FontWeight.Normal),
        Font(Res.font.dmserifdisplay_italic, FontWeight.Normal, androidx.compose.ui.text.font.FontStyle.Italic),
    ),
    sansFamily = FontFamily(
        Font(Res.font.dmsans_regular, FontWeight.Normal),
        Font(Res.font.dmsans_medium, FontWeight.Medium),
    ),
)
