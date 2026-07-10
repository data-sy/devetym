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
import com.robin.devetym.data.local.toDto
import com.robin.devetym.db.Term
import com.robin.devetym.ui.BookmarkViewModel
import com.robin.devetym.ui.components.EmptyState
import com.robin.devetym.ui.theme.AppScheme

/** 북마크 화면 (M6 §3-7). 반응형 Flow — 해제 후 수동 재조회 없이 자동 반영(ADR-0002). */
@Composable
fun BookmarkScreen(vm: BookmarkViewModel, onNavigateDetail: (String) -> Unit) {
    val bookmarks by vm.bookmarks.collectAsStateWithLifecycle()
    BookmarkContent(
        bookmarks = bookmarks,
        onSelect = onNavigateDetail,
        onRemove = { term -> vm.removeBookmark(term.toDto()) },
    )
}

@Composable
fun BookmarkContent(bookmarks: List<Term>, onSelect: (String) -> Unit, onRemove: (Term) -> Unit) {
    val dim = AppScheme.dim
    Column(Modifier.fillMaxSize().padding(horizontal = dim.screenPadding)) {
        Text("북마크", style = AppScheme.type.titleTab, color = AppScheme.colors.text,
            modifier = Modifier.padding(top = 24.dp))
        Text("// 저장한 용어", style = AppScheme.type.label, color = AppScheme.colors.textMuted,
            modifier = Modifier.padding(bottom = 16.dp))

        if (bookmarks.isEmpty()) {
            EmptyState("☆", "아직 저장한 용어가 없어요")
        } else {
            LazyColumn(Modifier.fillMaxSize()) {
                items(bookmarks, key = { it.keyword }) { term ->
                    Row(
                        Modifier.fillMaxWidth().clickable { onSelect(term.keyword) }
                            .padding(vertical = dim.rowVPad),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(term.keyword, style = AppScheme.type.codeBody, color = AppScheme.colors.text)
                            Text(term.summary, style = AppScheme.type.bodyPreviewSmall,
                                color = AppScheme.colors.textMuted, maxLines = 1)
                        }
                        Text("해제", style = AppScheme.type.caption, color = AppScheme.colors.accent,
                            modifier = Modifier.clickable { onRemove(term) }.padding(start = 12.dp))
                    }
                    HorizontalDivider(color = AppScheme.colors.border, thickness = dim.strokeBorder)
                }
            }
        }
    }
}
