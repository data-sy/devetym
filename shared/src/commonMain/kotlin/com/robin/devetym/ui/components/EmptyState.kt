package com.robin.devetym.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.robin.devetym.ui.theme.AppScheme

/** 빈 목록 공통 (M6 §3-6) — 북마크·히스토리·자동완성 없음. */
@Composable
fun EmptyState(icon: String, message: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(text = icon, style = AppScheme.type.titleHero, color = AppScheme.colors.textMuted)
        Text(
            text = message,
            style = AppScheme.type.label,
            color = AppScheme.colors.textMuted,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 12.dp),
        )
    }
}
