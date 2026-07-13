package com.robin.devetym.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.robin.devetym.ui.theme.AppScheme

/** 톤 컨테이너 틴트 알파 — AccessibilityContrastTest 톤버튼 쌍이 이 값으로 합성해 게이트(변경 시 재실측). */
const val TONAL_CONTAINER_ALPHA = 0.15f

/**
 * 톤 알약 버튼 (M9-후속 UX-1, 목업 A안) — Capsule 틴트 fill + 글리프·라벨(FilledTonal 계열).
 * 주 액션은 accent 틴트 컨테이너 + [com.robin.devetym.ui.tonalActionColor] 전경,
 * 보조(오류 제보)는 surface2 + textDim 회색 톤으로 위계 분리.
 */
@Composable
fun TonalPillButton(
    glyph: String,
    label: String,
    container: Color,
    content: Color,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .clip(CircleShape)
            .background(container)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = AppScheme.dim.rowVPad),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(glyph, style = AppScheme.type.codeAction, color = content)
        Text(label, style = AppScheme.type.codeAction, color = content)
    }
}
