package com.robin.devetym.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.robin.devetym.model.TermResult
import com.robin.devetym.repository.TermRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * 상세 화면 상태 (M5 슬라이스 §3-2). 번들·캐시 히트는 [Loading] 없이 즉시 [Result]로 수렴(사실상 즉시 반환).
 */
sealed interface DetailUiState {
    data object Loading : DetailUiState
    data class Result(val result: TermResult) : DetailUiState
    data class Error(val kind: ErrorKind) : DetailUiState
}

/**
 * 상세 화면 ViewModel (§3-2). `TermRepository`만 주입(architecture §4.5). fetch→상태 전이·job 취소·오류 매핑.
 */
class DetailViewModel(
    private val repository: TermRepository,
    // DR5-2 취소 내성(M7 §3-6): null이면 viewModelScope(M5 동작·테스트 보존), M7이 앱 스코프 주입 시 화면
    // 이탈에도 toggle 쓰기가 취소되지 않는다. ⚠️ '유실 제거'가 아니라 취소 내성 하드닝(실 셸 VM leak은 M8 이월).
    private val writeScope: CoroutineScope? = null,
) : ViewModel() {

    private val _state = MutableStateFlow<DetailUiState>(DetailUiState.Loading)
    val state: StateFlow<DetailUiState> = _state.asStateFlow()

    // load·refresh가 **공유하는 단일 취소 슬롯**(DR6-2): refresh가 in-flight load를(그 반대도) 취소해
    // 느린 선행 결과가 최신 결과를 나중에 덮는 stale 잔류를 막는다. 분리 job은 이 직렬화를 깬다.
    private var currentJob: Job? = null

    fun load(keyword: String) = start(keyword) { repository.fetch(it) }

    /** 명시 새로고침 — pinning 우회(INV-6). load와 같은 취소 슬롯을 공유. */
    fun refresh(keyword: String) = start(keyword) { repository.refresh(it) }

    private fun start(keyword: String, op: suspend (String) -> TermResult) {
        currentJob?.cancel()
        _state.value = DetailUiState.Loading
        currentJob = viewModelScope.launch {
            try {
                _state.value = DetailUiState.Result(op(keyword))
            } catch (e: CancellationException) {
                throw e   // 취소는 오류 아님 — 상태 안 바꿈(반드시 Throwable보다 먼저, DR5-4)
            } catch (e: Throwable) {
                _state.value = DetailUiState.Error(e.toErrorKind())
            }
        }
    }

    /**
     * 현재 상태가 [DetailUiState.Result]이고 그 [TermResult]가 [TermResult.Found]일 **때만** 그 entry를 위임(DR5-1).
     * 그 외 상태(Loading/Error/NotDevTerm/PossibleTypo)에선 no-op — entry 강제 추출 금지(별표 탭 크래시 방지).
     */
    fun toggleBookmark() {
        val result = (_state.value as? DetailUiState.Result)?.result
        val entry = (result as? TermResult.Found)?.entry ?: return
        (writeScope ?: viewModelScope).launch { repository.toggleBookmark(entry) }  // DR5-2 취소 내성
    }
}
