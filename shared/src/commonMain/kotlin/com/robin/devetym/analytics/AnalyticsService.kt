package com.robin.devetym.analytics

import com.robin.devetym.model.TermResult

/**
 * 분석 로깅 인터페이스 (M4 슬라이스 §3-6, spec 2-3).
 *
 * **M4 `fetch`는 오류 경로만 `logError`로 로깅한다**(§3-3 `ClaudeException` 분기). `logSearchResult`는
 * 인터페이스에 선언해 두되 M4는 호출하지 않는다 — 결과-유형 성공 로깅 배선은 후속(M5+)으로 이월(DR-1).
 */
interface AnalyticsService {
    fun logSearchResult(keyword: String, result: TermResult)
    fun logError(keyword: String, error: Throwable)
}

/** no-op 기본 구현(실제 구현은 후속). */
class PlaceholderAnalyticsService : AnalyticsService {
    override fun logSearchResult(keyword: String, result: TermResult) {}
    override fun logError(keyword: String, error: Throwable) {}
}
