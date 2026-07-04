package com.robin.devetym.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.robin.devetym.db.Term
import com.robin.devetym.model.TermEntry
import com.robin.devetym.repository.TermRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * 북마크 목록 ViewModel (M5 슬라이스 §3-4). 반응형 Flow — 토글 후 수동 재조회 없이 자동 반영(ADR-0002).
 */
class BookmarkViewModel(private val repository: TermRepository) : ViewModel() {

    val bookmarks: StateFlow<List<Term>> =
        repository.bookmarkedTerms().stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    /** 북마크 해제 — toggleBookmark 위임. Flow 재방출로 목록에서 자동 제거(수동 재조회 없음). */
    fun removeBookmark(entry: TermEntry) {
        viewModelScope.launch { repository.toggleBookmark(entry) }
    }
}
