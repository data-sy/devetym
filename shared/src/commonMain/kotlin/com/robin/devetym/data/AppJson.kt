package com.robin.devetym.data

import kotlinx.serialization.json.Json

/**
 * 번들 로더·`ClaudeApi` 응답 디코드 공용 wire `Json` (M3 슬라이스 §3-4, M1 §7-3 이월 결착).
 *
 * `ignoreUnknownKeys = true`가 핵심 정책이다: 진화하는 서버 read-through 응답·Anthropic content 블록
 * (thinking/text 등)·미래 번들 필드가 늘어도 디코드가 깨지지 않는다.
 *
 * ⚠️ **INV-A와의 상호작용**: `ignoreUnknownKeys`는 *모르는 키를 무시*할 뿐, 알려진 `aliases` 키가
 * 다른 이름/생략되면 여전히 default `emptyList()`로 조용히 떨어진다 — 이 silent 소실은 성공 디코드로는
 * 안 잡히므로 로더 실측(§6-B, 실 번들 alias 내용·alias 검색 단언)이 오라클이다.
 *
 * M2 매퍼의 `aliasesJson`(저장-내부 List<String> 인코딩)은 이 wire 정책과 **독립**이며 M2대로 유지.
 */
val AppJson: Json = Json {
    ignoreUnknownKeys = true
}

/**
 * term-key(캐시 키·로컬 매칭) 정규화의 단일 정본 (M3 슬라이스 §3-1).
 *
 * `BundleDbSource.search`가 이 함수로 로컬 매칭 키를 만들고, 서버 캐시 키잉도 같은 정규화를 써
 * `React`/`react`가 같은 term-key로 접혀 캐시 파편화·M4 중복 upsert가 방지된다.
 *
 * ⚠️ **키잉 전용이다 — AI에 보여줄 질의 content에는 적용하지 않는다**(§3-2): lowercase가 대소문자
 * 유의미 용어(`NaN`/`Go`/`REST`/`C`)의 의미를 뭉개 어원 오답을 유발하므로, `buildClaudeRequest`는
 * 원본 keyword를 대소문자 보존해 싣는다(iOS 검증본 계승). 키잉과 프롬프트 입력은 다른 요구다.
 */
fun normalizeKeyword(s: String): String = s.trim().lowercase()
