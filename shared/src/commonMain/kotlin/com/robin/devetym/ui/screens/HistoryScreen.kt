package com.robin.devetym.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.robin.devetym.db.SearchHistory
import com.robin.devetym.ui.HistoryViewModel
import com.robin.devetym.ui.components.EmptyState
import com.robin.devetym.ui.relativeTimeLabel
import com.robin.devetym.ui.theme.AppScheme

/** 히스토리 화면 (M6 §3-7). 전량 노출(DR5-3). 반응형 Flow — 삭제 후 자동 반영. */
@Composable
fun HistoryScreen(vm: HistoryViewModel, now: Long, onNavigateDetail: (String) -> Unit) {
    val history by vm.history.collectAsStateWithLifecycle()
    HistoryContent(
        history = history,
        now = now,
        onSelect = onNavigateDetail,
        onDelete = vm::delete,
        onClearAll = vm::clearAll,
    )
}

@Composable
fun HistoryContent(
    history: List<SearchHistory>,
    now: Long,
    onSelect: (String) -> Unit,
    onDelete: (String) -> Unit,
    onClearAll: () -> Unit,
) {
    val dim = AppScheme.dim
    Column(Modifier.fillMaxSize().padding(horizontal = dim.screenPadding)) {
        Row(Modifier.fillMaxWidth().padding(top = 24.dp, bottom = 16.dp),
            verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("히스토리", style = AppScheme.type.titleTab, color = AppScheme.colors.text)
                Text("// 최근 검색 기록", style = AppScheme.type.label, color = AppScheme.colors.textMuted)
            }
            if (history.isNotEmpty()) {
                Text("전체 삭제", style = AppScheme.type.caption, color = AppScheme.colors.accent,
                    modifier = Modifier.clickable(onClick = onClearAll))
            }
        }

        if (history.isEmpty()) {
            EmptyState("🕐", "검색 기록이 없습니다")
        } else {
            LazyColumn(Modifier.fillMaxSize()) {
                items(history, key = { it.keyword }) { h ->
                    Row(
                        Modifier.fillMaxWidth().clickable { onSelect(h.keyword) }
                            .padding(vertical = dim.rowVPad),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(h.keyword, style = AppScheme.type.codeInput, color = AppScheme.colors.textDim,
                            modifier = Modifier.weight(1f))
                        Text(relativeTimeLabel(now, h.searchedAt), style = AppScheme.type.caption,
                            color = AppScheme.colors.textMuted)
                        // "✕"는 TTS 낭독 이름이 없어 "발음할 수 없음"으로 읽힘(실주행 2026-07-14) → 라벨 부여.
                        Text("✕", style = AppScheme.type.caption, color = AppScheme.colors.textMuted,
                            modifier = Modifier.clickable { onDelete(h.keyword) }.padding(start = 12.dp)
                                .semantics { contentDescription = "삭제" })
                    }
                    HorizontalDivider(color = AppScheme.colors.border, thickness = dim.strokeBorder)
                }
            }
        }
    }
}
