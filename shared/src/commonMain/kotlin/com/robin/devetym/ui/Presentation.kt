package com.robin.devetym.ui

import com.robin.devetym.data.normalizeKeyword
import com.robin.devetym.db.Term
import com.robin.devetym.model.TermResult

/**
 * 상태→표시 순수 매핑 (M6 §3-3·§6). `DetailUiState`/`TermResult`를 화면이 그릴 의도(kind·icon·message)로
 * 분기(`when` 전수, `else` 없음 — DR-3 canary). ⚠️ 이 매핑의 반환 정확성만 §6가 실측하며,
 * `DetailContent`가 이 키대로 렌더한다는 결속은 보증하지 않는다(시각 천장 §0 — 아침 리뷰 소관).
 */
enum class DetailIcon { None, Question, Lightbulb, Error }

data class DetailPresentation(val kind: String, val icon: DetailIcon, val message: String?)

fun detailPresentation(state: DetailUiState): DetailPresentation = when (state) {
    is DetailUiState.Loading -> DetailPresentation("loading", DetailIcon.None, null)
    is DetailUiState.Result -> when (val r = state.result) {
        is TermResult.Found -> DetailPresentation("found", DetailIcon.None, null)
        is TermResult.NotDevTerm -> DetailPresentation("notDevTerm", DetailIcon.Question, "개발 용어를 검색해주세요")
        is TermResult.PossibleTypo -> DetailPresentation("possibleTypo", DetailIcon.Lightbulb, "${r.suggestion}을(를) 찾으셨나요?")
    }
    is DetailUiState.Error -> DetailPresentation("error", DetailIcon.Error, errorMessage(state.kind))
}

/**
 * DR-4 종결 순수 헬퍼 (M6 §3-8). 저장 로우 keyword는 정규화값(AI 경로 `copy(keyword=key)`·`toggleBookmark`의
 * `normalizeKeyword(entry.keyword)`)이므로, 표시 측도 **조회 시점에 `normalizeKeyword`로 맞춰야** 매치된다.
 * 라우트 원형·번들 저작 원문(`OAuth`)을 정규화 없이 비교하면 저장값 `oauth`와 불일치→별표 오표시.
 */
fun isBookmarkedFor(bookmarks: List<Term>, keyword: String): Boolean {
    val key = normalizeKeyword(keyword)
    return bookmarks.any { it.keyword == key }
}
