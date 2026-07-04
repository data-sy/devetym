package com.robin.devetym.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.robin.devetym.Constants
import com.robin.devetym.db.SearchHistory
import com.robin.devetym.model.TermEntry
import com.robin.devetym.repository.TermRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * 검색 화면 ViewModel (M5 슬라이스 §3-3). 자동완성 디바운스 300ms + 최근 검색 반응형 Flow.
 */
class SearchViewModel(private val repository: TermRepository) : ViewModel() {

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    private val _suggestions = MutableStateFlow<List<TermEntry>>(emptyList())
    val suggestions: StateFlow<List<TermEntry>> = _suggestions.asStateFlow()

    // 최근 검색은 반응형 Flow → stateIn (ADR-0002 — 수동 재조회 없음). SharingStarted는 배터리·구독수명
    // 소관으로 M6 이월 후보이나, 활성 구독이 있으면 upstream이 실제 수집되므로(§6 반응형 관측 규약) 오라클과 독립.
    val recent: StateFlow<List<SearchHistory>> =
        repository.recentSearches(Constants.recentSearchLimit)
            .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    private var debounceJob: Job? = null

    /** 입력 변경 — 이전 debounce job 취소 후 300ms 디바운스. trim 후 빈이면 조회 안 함(최소 1자). */
    fun onQueryChanged(value: String) {
        _query.value = value
        debounceJob?.cancel()
        val trimmed = value.trim()
        if (trimmed.isEmpty()) {
            _suggestions.value = emptyList()
            return
        }
        debounceJob = viewModelScope.launch {
            delay(Constants.autocompleteDebounceMs)
            _suggestions.value = repository.autocomplete(trimmed)
        }
    }

    /** 검색 확정 — trim만(lowercase 정규화는 repository가 저장 키 정본으로 재수행, M4). 빈이면 null. */
    fun commit(): String? = _query.value.trim().ifEmpty { null }
}
