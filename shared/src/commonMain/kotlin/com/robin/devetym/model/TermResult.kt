package com.robin.devetym.model

/**
 * 용어 조회 결과의 출처 (M1 슬라이스 §3-2).
 * 문자열이 아니라 enum으로 — 컴파일 타임 분기 강제.
 */
enum class Source { BUNDLE, AI }

/**
 * 용어 조회 결과 (M1 슬라이스 §3-2).
 *
 * `sealed interface`라 `when`이 전수 분기하며, 결과 출처를 타입으로 표현해 컴파일 타임 분기를 강제한다.
 * 세 subtype: [Found]·[NotDevTerm]·[PossibleTypo]. subtype이 늘면 이를 소비하는 `when`이
 * (else 없이 작성된 경우) 컴파일 에러로 실패해 전수 처리를 강제한다(slice §6 `test_TermResult_when분기_전수처리`, DR-3).
 */
sealed interface TermResult {
    data class Found(val entry: TermEntry, val source: Source) : TermResult
    data object NotDevTerm : TermResult
    data class PossibleTypo(val suggestion: String) : TermResult
}
