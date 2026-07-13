package com.robin.devetym.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.robin.devetym.model.Source
import com.robin.devetym.model.TermEntry
import com.robin.devetym.model.TermResult
import com.robin.devetym.ui.BookmarkViewModel
import com.robin.devetym.ui.DetailUiState
import com.robin.devetym.ui.DetailViewModel
import com.robin.devetym.ui.components.AiBadge
import com.robin.devetym.ui.components.CategoryBadge
import com.robin.devetym.ui.components.PulsingDots
import com.robin.devetym.ui.errorMessage
import com.robin.devetym.ui.isBookmarkedFor
import com.robin.devetym.ui.theme.AppScheme

/**
 * 상세 화면 (M6 §3-7·§3-8). **2-VM 시그니처 정본**: `bookmarkVm.bookmarks`에서 `isBookmarkedFor`로
 * 파생한 `isBookmarked`를 함께 주입(DR-4 마감). Content는 두 번째 VM/Repository를 직접 모른다(§4).
 */
@Composable
fun DetailScreen(
    keyword: String,
    vm: DetailViewModel,
    bookmarkVm: BookmarkViewModel,
    onBack: () -> Unit,
    onSelectSuggestion: (String) -> Unit,
    onShare: (String) -> Unit,
    onCopy: (String) -> Unit,
    onReport: (String) -> Unit,
) {
    val state by vm.state.collectAsStateWithLifecycle()
    val bookmarks by bookmarkVm.bookmarks.collectAsStateWithLifecycle()
    DetailContent(
        keyword = keyword,
        state = state,
        isBookmarked = isBookmarkedFor(bookmarks, keyword),
        onToggleBookmark = vm::toggleBookmark,
        onBack = onBack,
        onSelectSuggestion = onSelectSuggestion,
        onShare = onShare,
        onCopy = onCopy,
        onReport = onReport,
    )
}

@Composable
fun DetailContent(
    keyword: String,
    state: DetailUiState,
    isBookmarked: Boolean,
    onToggleBookmark: () -> Unit,
    onBack: () -> Unit,
    onSelectSuggestion: (String) -> Unit,
    onShare: (String) -> Unit,
    onCopy: (String) -> Unit,
    onReport: (String) -> Unit,
) {
    val colors = AppScheme.colors
    val type = AppScheme.type
    val dim = AppScheme.dim
    Column(Modifier.fillMaxSize().padding(horizontal = dim.screenPadding)) {
        // M9 스모크 결함 수정: 상시 back — Found·Loading은 이 어포던스가 유일한 탈출구
        // (iOS는 시스템 백 제스처 없음, 상태기반 네비). LicensesScreen "← 뒤로"와 동일 패턴.
        Text("← 뒤로", style = type.codeAction, color = colors.accent,
            modifier = Modifier.clickable(onClick = onBack).padding(vertical = 16.dp))
        when (state) {
            is DetailUiState.Loading -> Column(
                Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Text(keyword, style = type.codeHero, color = colors.accent,
                    modifier = Modifier.padding(bottom = 20.dp))
                PulsingDots()
                Text("어원을 찾고 있어요", style = type.bodySub, color = colors.textDim,
                    modifier = Modifier.padding(top = 20.dp))
            }

            is DetailUiState.Result -> when (val r = state.result) {
                is TermResult.Found -> FoundBody(r.entry, r.source, isBookmarked, onToggleBookmark, onShare, onCopy, onReport)
                is TermResult.NotDevTerm -> MessageBody("?", "개발 용어를 검색해주세요", "검색으로 돌아가기", onBack)
                is TermResult.PossibleTypo -> Column(
                    Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Text("💡", style = type.titleHero)
                    Text("${r.suggestion}을(를) 찾으셨나요?", style = type.bodyEmphasis, color = colors.text,
                        modifier = Modifier.padding(vertical = 12.dp))
                    ActionText(r.suggestion) { onSelectSuggestion(r.suggestion) }
                    ActionText("아니요, 돌아가기", onBack)
                }
                }

            is DetailUiState.Error -> MessageBody("!", errorMessage(state.kind), "돌아가기", onBack)
        }
    }
}

@Composable
private fun FoundBody(
    entry: TermEntry,
    source: Source,
    isBookmarked: Boolean,
    onToggleBookmark: () -> Unit,
    onShare: (String) -> Unit,
    onCopy: (String) -> Unit,
    onReport: (String) -> Unit,
) {
    val colors = AppScheme.colors
    val type = AppScheme.type
    val dim = AppScheme.dim
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        Text(entry.keyword, style = type.titleHero, color = colors.text,
            modifier = Modifier.padding(top = 20.dp))
        Row(Modifier.padding(vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            CategoryBadge(entry.category)
            if (source == Source.AI) AiBadge()
        }
        Text(entry.summary, style = type.bodySub, color = colors.textDim)

        Section("어원") {
            // 좌측 accent 바 세부는 시각 천장(§0) — surface2 블록만 구조로 보증.
            Text(entry.etymology, style = type.bodyBlock, color = colors.text,
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(dim.radiusBlock))
                    .background(colors.surface2).padding(dim.cardPadding))
            // WU-8: copyToClipboard seam 호출처(어원 블록 복사 어포던스). seam은 구현·유닛테스트되나
            // 이전엔 호출 UI가 없어 dead code였다(M9 에뮬 스모크 발견).
            ActionText("어원 복사", { onCopy(entry.etymology) })
        }
        Section("왜 이 이름인가") {
            Text(entry.namingReason, style = type.body, color = colors.text)
        }

        Row(Modifier.padding(vertical = dim.sectionGap), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            ActionText(if (isBookmarked) "★ 북마크됨" else "☆ 북마크", onToggleBookmark)
            ActionText("공유", { onShare("${entry.keyword}\n\n${entry.summary}\n\n— DevEtym") })
        }
        ActionText("오류 제보", { onReport(entry.keyword) })
    }
}

@Composable
private fun Section(title: String, content: @Composable () -> Unit) {
    val dim = AppScheme.dim
    Column(Modifier.padding(top = dim.sectionGap)) {
        Text(title, style = AppScheme.type.caption, color = AppScheme.colors.textMuted,
            modifier = Modifier.padding(bottom = dim.labelGap))
        content()
    }
}

@Composable
private fun MessageBody(icon: String, message: String, action: String, onAction: () -> Unit) {
    Column(
        Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(icon, style = AppScheme.type.titleHero, color = AppScheme.colors.textMuted)
        Text(message, style = AppScheme.type.bodyEmphasis, color = AppScheme.colors.text,
            modifier = Modifier.padding(vertical = 12.dp))
        ActionText(action, onAction)
    }
}

@Composable
private fun ActionText(text: String, onClick: () -> Unit) {
    Text(text, style = AppScheme.type.codeAction, color = AppScheme.colors.accent,
        modifier = Modifier.clickable(onClick = onClick).padding(vertical = 8.dp))
}
