package com.robin.devetym.data.remote

import com.robin.devetym.model.TermResult

/**
 * AI 생성 seam (M4 슬라이스 AD-3 해소) — `TermRepository`가 의존하는 인터페이스.
 *
 * M3 `ClaudeApi`가 final concrete class라 M4 Fake 주입 seam이 없었다(다른 협력자
 * `BundleDbSource`·`LocalTermStore`·`AnalyticsService`는 전부 interface). 이 인터페이스를 추출해
 * `ClaudeApi`가 구현하고, repository는 이 타입에 의존한다 — 테스트는 `FakeTermGenerator`로 대체.
 */
interface TermGenerator {
    /** read-through 프록시 호출 + `tool_use` 3분기. 실패는 [ClaudeException]. */
    suspend fun generate(keyword: String): TermResult
}
