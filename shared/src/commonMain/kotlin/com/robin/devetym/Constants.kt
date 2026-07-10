package com.robin.devetym

/**
 * 앱 전역 상수 (spec 1-3). 값은 iOS 검증본(`~/dev-etymology` `Constants.swift`) 계승.
 *
 * M3는 `proxyBaseUrl`·`claudeModel`·`apiTimeoutMs`를 참조한다(§3-2). 나머지(자동완성 디바운스·
 * 최근검색 한도)는 M5/M6 소비 — 여기 한 곳에서 관리(spec 1-3). 실제 런타임 주입 배선은 M7.
 */
object Constants {
    /** Anthropic 모델 ID — iOS 검증본 계승. 프록시가 그대로 Anthropic으로 forward(ADR-0006 투명). */
    const val claudeModel = "claude-sonnet-4-6"

    /** read-through 프록시 엔드포인트(ADR-0006). 앱에 키 없음 — 프록시가 키 주입 + 기기당 한도. */
    const val proxyBaseUrl = "https://devetym-proxy.data-sy-2.workers.dev"

    /** 네트워크 요청 타임아웃(ms). iOS `apiTimeout`(30s) 계승. */
    const val apiTimeoutMs = 30_000L

    /** 자동완성 디바운스(ms) — M6 검색 UI 소비(spec 3-2). */
    const val autocompleteDebounceMs = 300L

    /** 최근 검색 표시 개수 — M6 소비(spec 1-3). */
    const val recentSearchLimit = 5
}
