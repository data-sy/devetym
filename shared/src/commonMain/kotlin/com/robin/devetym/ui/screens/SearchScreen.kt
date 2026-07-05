package com.robin.devetym.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.robin.devetym.db.SearchHistory
import com.robin.devetym.model.TermEntry
import com.robin.devetym.ui.SearchViewModel
import com.robin.devetym.ui.components.FlowChip
import com.robin.devetym.ui.theme.AppScheme

/** 검색 화면 (M6 §3-7). VM 구독 래퍼 → 순수 Content. 로딩·오류는 상세로 위임(이 화면엔 없음). */
@Composable
fun SearchScreen(vm: SearchViewModel, onNavigateDetail: (String) -> Unit) {
    val query by vm.query.collectAsStateWithLifecycle()
    val suggestions by vm.suggestions.collectAsStateWithLifecycle()
    val recent by vm.recent.collectAsStateWithLifecycle()
    SearchContent(
        query = query,
        suggestions = suggestions,
        recent = recent,
        onQueryChange = vm::onQueryChanged,
        onCommit = { vm.commit()?.let(onNavigateDetail) },
        onSelect = onNavigateDetail,
    )
}

@Composable
fun SearchContent(
    query: String,
    suggestions: List<TermEntry>,
    recent: List<SearchHistory>,
    onQueryChange: (String) -> Unit,
    onCommit: () -> Unit,
    onSelect: (String) -> Unit,
) {
    val dim = AppScheme.dim
    Column(Modifier.fillMaxSize().padding(horizontal = dim.screenPadding)) {
        Text("DevEtym", style = AppScheme.type.titleHero, color = AppScheme.colors.text,
            modifier = Modifier.padding(top = 24.dp))
        Text("// 개발 용어 어원 사전", style = AppScheme.type.label, color = AppScheme.colors.textMuted,
            modifier = Modifier.padding(bottom = 16.dp))

        if (suggestions.isNotEmpty()) {
            LazyColumn(Modifier.weight(1f).fillMaxWidth()) {
                items(suggestions, key = { it.keyword }) { entry ->
                    Text(
                        entry.keyword,
                        style = AppScheme.type.bodyPreview,
                        color = AppScheme.colors.text,
                        modifier = Modifier.fillMaxWidth()
                            .padding(vertical = dim.rowVPad),
                    )
                }
            }
        } else {
            Column(Modifier.weight(1f)) {
                Text("최근 검색", style = AppScheme.type.caption, color = AppScheme.colors.textMuted,
                    modifier = Modifier.padding(vertical = 8.dp))
                if (recent.isEmpty()) {
                    Text("최근 검색한 용어가 없습니다", style = AppScheme.type.label,
                        color = AppScheme.colors.textMuted)
                } else {
                    androidx.compose.foundation.lazy.LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        items(recent, key = { it.keyword }) { h ->
                            FlowChip(h.keyword) { onSelect(h.keyword) }
                        }
                    }
                }
            }
        }

        OutlinedTextField(
            value = query,
            onValueChange = onQueryChange,
            modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
            textStyle = AppScheme.type.codeInput,
            singleLine = true,
            placeholder = { Text("용어를 검색하세요", style = AppScheme.type.codeInput) },
            trailingIcon = {
                if (query.isNotEmpty()) {
                    IconButton(onClick = { onQueryChange("") }) {
                        Text("✕", style = AppScheme.type.codeAction, color = AppScheme.colors.textMuted)
                    }
                }
            },
            keyboardActions = androidx.compose.foundation.text.KeyboardActions(onSearch = { onCommit() }),
            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(imeAction = ImeAction.Search),
        )
    }
}
