package com.robin.devetym.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.robin.devetym.db.SearchHistory
import com.robin.devetym.repository.TermRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * 검색 히스토리 관리 ViewModel (M5 슬라이스 §3-4). 반응형 Flow — 삭제 후 수동 재조회 없이 자동 반영.
 */
class HistoryViewModel(private val repository: TermRepository) : ViewModel() {

    // ⚠️ limit은 recentSearchLimit(5)가 **아니다**(DR5-3): History는 개별 delete·clearAll을 가진 전체 관리
    // 화면이라 저장된 히스토리 **전량**을 노출해야 한다. M4 유일 쿼리가 limit-bound라 전량 조회는 대형 상한으로
    // 표현한다(전용 무한 쿼리 도입·M4 쿼리 확장은 하드닝 이월 — 여기선 절단 없는 전량이 목적).
    val history: StateFlow<List<SearchHistory>> =
        repository.recentSearches(HISTORY_LIMIT).stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    fun delete(keyword: String) {
        viewModelScope.launch { repository.deleteSearchHistory(keyword) }
    }

    fun clearAll() {
        viewModelScope.launch { repository.clearAllSearchHistory() }
    }

    companion object {
        /** 히스토리 전량(절단 없음, DR5-3) — recentSearchLimit(5) 재사용 금지. */
        const val HISTORY_LIMIT: Int = Int.MAX_VALUE
    }
}
