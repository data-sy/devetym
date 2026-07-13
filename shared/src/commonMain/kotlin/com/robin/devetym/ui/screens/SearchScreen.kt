package com.robin.devetym.ui.screens

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.drag
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.robin.devetym.db.SearchHistory
import com.robin.devetym.model.TermEntry
import com.robin.devetym.ui.KEYBOARD_DISMISS_THRESHOLD_DP
import com.robin.devetym.ui.SearchViewModel
import com.robin.devetym.ui.components.FlowChip
import com.robin.devetym.ui.isKeyboardDismissDrag
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
    val keyboard = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current
    // §2-C 인셋 기반 회피: 필드가 하단 고정 + 중간 weight(1f)라 imePadding으로 필드만 키보드 위로
    // 오르고 헤더·최근 검색은 그대로(3-1). 뷰포트 시프트는 iOS 진입점에서 DoNothing으로 차단.
    Column(Modifier.fillMaxSize().imePadding().padding(horizontal = dim.screenPadding)) {
        Text("DevEtym", style = AppScheme.type.titleHero, color = AppScheme.colors.text,
            modifier = Modifier.padding(top = 24.dp))
        Text("// 개발 용어 어원 사전", style = AppScheme.type.label, color = AppScheme.colors.textMuted,
            modifier = Modifier.padding(bottom = 16.dp))

        // §2-C dismiss 경로 1: 콘텐츠 영역 아래 방향 드래그 → 키보드·포커스 해제(3-2).
        // consume하지 않고 관찰만 — 자식(LazyColumn 스크롤·칩 탭)과 충돌 없음(엣지 스와이프-백과 동일 패턴).
        Box(Modifier.weight(1f).fillMaxWidth().pointerInput(Unit) {
            val threshold = KEYBOARD_DISMISS_THRESHOLD_DP.dp.toPx()
            awaitEachGesture {
                val down = awaitFirstDown(requireUnconsumed = false)
                var dragY = 0f
                drag(down.id) { change ->
                    dragY += change.position.y - change.previousPosition.y
                }
                if (isKeyboardDismissDrag(dragY, threshold)) {
                    keyboard?.hide()
                    focusManager.clearFocus()
                }
            }
        }) {
            if (suggestions.isNotEmpty()) {
                LazyColumn(Modifier.fillMaxSize()) {
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
                Column(Modifier.fillMaxSize()) {
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
