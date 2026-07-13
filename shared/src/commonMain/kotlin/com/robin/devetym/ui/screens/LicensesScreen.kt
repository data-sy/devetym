package com.robin.devetym.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.robin.devetym.resources.Res
import com.robin.devetym.ui.theme.AppScheme

/**
 * 오픈소스 라이선스 화면 (M8 §3-5 · DR-2 마감) — OFL 폰트 라이선스 3종을 `Res.readBytes`로 로드해 표시
 * (openUrl 폴백 대신 in-app 고지로 goal(c) 실현). ⚠️ 런타임 로드 정확성은 M3 로더와 동형 실기기 천장.
 * 셸 재설계 §2-A: 전역 오버레이 → Settings 스택 push. back·safe area는 NavContainer/AppSurface 소유 —
 * 스크롤에 딸려가던 인라인 back(3-6 탭 불가)을 삭제하고 순수 content로 복귀.
 */
@Composable
fun LicensesScreen() {
    var text by remember { mutableStateOf("불러오는 중…") }
    LaunchedEffect(Unit) {
        val names = listOf("files/ofl_dmsans.txt", "files/ofl_dmmono.txt", "files/ofl_dmserifdisplay.txt")
        val parts = mutableListOf<String>()
        for (name in names) {   // suspend 루프 — Res.readBytes는 코루틴 본문에서만 호출
            parts += runCatching { Res.readBytes(name).decodeToString() }.getOrElse { "" }
        }
        text = parts.joinToString("\n\n———————\n\n")
    }
    val dim = AppScheme.dim
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = dim.screenPadding),
    ) {
        Text("오픈소스 라이선스", style = AppScheme.type.titleTab, color = AppScheme.colors.text,
            modifier = Modifier.padding(bottom = 16.dp))
        Text(text, style = AppScheme.type.caption, color = AppScheme.colors.textDim,
            modifier = Modifier.padding(bottom = 32.dp))
    }
}
