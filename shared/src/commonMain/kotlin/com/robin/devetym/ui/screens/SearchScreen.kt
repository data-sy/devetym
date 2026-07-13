package com.robin.devetym.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement

import androidx.compose.foundation.layout.Row
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventPass
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
        // ⚠️ Initial 패스로 관찰(라운드 2 수정) — drag()는 자식(제안 LazyColumn 스크롤·페이저)이
        // 이벤트를 consume하면 관찰이 중단돼 임계 미달로 새는 경합이 있었다. Initial 패스는 자식
        // 처리 전에 위치만 읽으므로 경합 없음(여전히 비consume — 스크롤·탭 정상).
        Box(Modifier.weight(1f).fillMaxWidth().pointerInput(Unit) {
            val threshold = KEYBOARD_DISMISS_THRESHOLD_DP.dp.toPx()
            awaitEachGesture {
                val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                var dragY = 0f
                while (true) {
                    val event = awaitPointerEvent(PointerEventPass.Initial)
                    val change = event.changes.firstOrNull { it.id == down.id } ?: break
                    dragY += change.position.y - change.previousPosition.y
                    if (!change.pressed) break
                }
                if (isKeyboardDismissDrag(dragY, threshold)) {
                    keyboard?.hide()
                    focusManager.clearFocus()
                }
            }
        }) {
            if (suggestions.isNotEmpty()) {
                // 라운드 2: 제안이 최근 검색 영역을 통째로 갈아끼워 "갑자기 DB 내용이 뜬" 인상 —
                // "제안" 캡션(최근 검색과 동형)으로 영역 정체 표기 + ↖(채워 넣기) 어포던스 + 행 탭 배선.
                Column(Modifier.fillMaxSize()) {
                    Text("제안", style = AppScheme.type.caption, color = AppScheme.colors.textMuted,
                        modifier = Modifier.padding(vertical = 8.dp))
                    LazyColumn(Modifier.fillMaxSize()) {
                        items(suggestions, key = { it.keyword }) { entry ->
                            Row(
                                Modifier.fillMaxWidth()
                                    // 탭 = 완성어를 필드에 채우고 바로 상세로(검색 커밋과 동일 경로).
                                    .clickable {
                                        onQueryChange(entry.keyword)
                                        onSelect(entry.keyword)
                                    }
                                    .padding(vertical = dim.rowVPad),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(entry.keyword, style = AppScheme.type.bodyPreview,
                                    color = AppScheme.colors.text, modifier = Modifier.weight(1f))
                                Text("↖", style = AppScheme.type.codeAction, color = AppScheme.colors.textMuted)
                            }
                        }
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
