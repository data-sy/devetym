package com.robin.devetym.ui

import com.robin.devetym.data.remote.ClaudeException

/**
 * 오류 화면이 분기할 오류 분류 (M5 슬라이스 §3-1). 문구(오프라인 구분 등)는 M6 UI 소관 — 여기선 분류만.
 */
enum class ErrorKind { Timeout, Network, InvalidResponse, DailyLimitExceeded, ServiceExhausted, Unknown }

/**
 * `Throwable` → [ErrorKind] 순수 매핑 (§3-1).
 *
 * 바깥 `when`은 [ClaudeException](sealed)만 안쪽 `when`으로 **전수 분기**하고 — 안쪽 `when`엔 `else`가 없어
 * 하위타입이 늘면 컴파일 canary가 발화한다(DR-3·§4 「`else` 금지」). `else`는 비-`ClaudeException`(예상 밖
 * 예외)만 [ErrorKind.Unknown]으로 접는다 — 장래 추가되는 `ClaudeException` 하위타입이 조용히 `Unknown`으로
 * 강등되지 않는다.
 */
fun Throwable.toErrorKind(): ErrorKind = when (this) {
    is ClaudeException -> when (this) {
        is ClaudeException.Timeout -> ErrorKind.Timeout
        is ClaudeException.Network -> ErrorKind.Network
        is ClaudeException.InvalidResponse -> ErrorKind.InvalidResponse
        is ClaudeException.DailyLimitExceeded -> ErrorKind.DailyLimitExceeded
        is ClaudeException.ServiceExhausted -> ErrorKind.ServiceExhausted
    }
    else -> ErrorKind.Unknown
}
