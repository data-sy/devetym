package com.robin.devetym.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.robin.devetym.ui.theme.AppScheme

/** 최근 검색·자동완성 미리보기 칩 (M6 §3-6) — Capsule + surface2 + codeChip. */
@Composable
fun FlowChip(text: String, onClick: () -> Unit) {
    val colors = AppScheme.colors
    Text(
        text = text,
        style = AppScheme.type.codeChip,
        color = colors.text,
        modifier = Modifier
            .clip(CircleShape)
            .background(colors.surface2)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp),
    )
}
