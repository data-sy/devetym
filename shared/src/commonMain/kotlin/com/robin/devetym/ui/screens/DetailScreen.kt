package com.robin.devetym.ui.screens

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import com.robin.devetym.ui.components.TONAL_CONTAINER_ALPHA
import com.robin.devetym.ui.components.TonalPillButton
import com.robin.devetym.ui.LOADING_PHRASES
import com.robin.devetym.ui.detailCopyPayload
import com.robin.devetym.ui.errorMessage
import com.robin.devetym.ui.isBookmarkedFor
import com.robin.devetym.ui.loadingPhrase
import com.robin.devetym.ui.theme.AppScheme
import com.robin.devetym.ui.tonalActionColor
import kotlinx.coroutines.delay

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
    // back 어포던스는 NavContainer 고정 top bar 소유(셸 재설계 §2-A) — 인라인 "← 뒤로" 삭제.
    // onBack은 NotDevTerm·PossibleTypo·Error 본문 액션("돌아가기")에만 남는다.
    Column(Modifier.fillMaxSize().padding(horizontal = dim.screenPadding)) {
        when (state) {
            is DetailUiState.Loading -> Column(
                Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Text(keyword, style = type.codeHero, color = colors.accent,
                    modifier = Modifier.padding(bottom = 20.dp))
                PulsingDots()
                // M9-후속 UX-3: 고정 단일 문구 → 안내형 2문구 ~3초 크로스페이드 순환.
                var phraseTick by remember { mutableStateOf(0) }
                LaunchedEffect(Unit) {
                    while (true) { delay(3_000); phraseTick += 1 }
                }
                Crossfade(phraseTick % LOADING_PHRASES.size, animationSpec = tween(600),
                    modifier = Modifier.padding(top = 20.dp)) { i ->
                    Text(loadingPhrase(i), style = type.bodySub, color = colors.textDim)
                }
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
        }
        Section("왜 이 이름인가") {
            Text(entry.namingReason, style = type.body, color = colors.text)
        }

        // M9-후속 UX-1(목업 A안): ActionText(순수 텍스트, 문장처럼 읽힘) → 톤 알약. 복사(WU-8
        // copyToClipboard seam 호출처)·북마크·공유 3개 accent 틴트, 오류 제보는 회색 톤 분리.
        val tonalContainer = colors.accent.copy(alpha = TONAL_CONTAINER_ALPHA)
        val tonalContent = tonalActionColor(colors)
        Row(Modifier.padding(top = dim.sectionGap), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            // M9-후속 §2-E(UX-4): 어원 단일 필드 → 전체 페이로드(키워드+어원+왜 이 이름인가, 순수 함수).
            TonalPillButton("❏", "복사", tonalContainer, tonalContent) { onCopy(detailCopyPayload(entry)) }
            TonalPillButton(
                if (isBookmarked) "★" else "☆", if (isBookmarked) "북마크됨" else "북마크",
                tonalContainer, tonalContent, onToggleBookmark,
            )
            TonalPillButton("↗", "공유", tonalContainer, tonalContent) {
                onShare("${entry.keyword}\n\n${entry.summary}\n\n— DevEtym")
            }
        }
        Row(Modifier.padding(vertical = dim.sectionGap)) {
            TonalPillButton("!", "오류 제보", colors.surface2, colors.textDim) { onReport(entry.keyword) }
        }
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
