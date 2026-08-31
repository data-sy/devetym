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
 * term-key(캐시 키·로컬 매칭) 정규화의 단일 정본 (M3 슬라이스 §3-1, W0c §3-1에서 정의 확정).
 *
 * **정의(2026-08-26 확정)**: 구분자 — 공백류 ∪ {하이픈, 언더스코어} — 를 **양끝만이 아니라 내부까지
 * 전부 삭제**한 뒤 lowercase. `"aa-tree"` · `"AA tree"` · `"aatree"` → `"aatree"`.
 *
 * **왜 trim이 아니라 삭제인가**: 번들 `keyword`는 슬러그(`aa-tree`)인데 사용자가 치는 표기는
 * 공백형(`AA tree`)·무공백형(`aatree`)이다. trim만 하면 650 중 **272건이 자기 자신의 공백 표기로
 * 도달 불가**하고, 공백을 가진 한글 별칭 **609건 전부가 무공백 표기(`추상팩토리`)로 미도달**이었다
 * (전 항목 실측). 삭제로 접으면 표기변이 858/858이 도달하며 **엔트리 키 충돌은 0건** — 이 확장으로
 * 잃는 항목이 없다. 늘어나는 것은 별칭 충돌 1건(`cache-aside`의 별칭 `lazy loading` ↔ 엔트리
 * `lazy-loading`)뿐이고, 이는 [BundleDbSource]가 keyword를 alias보다 먼저 색인해 흡수한다.
 *
 * `BundleDbSource.search`가 이 함수로 로컬 매칭 키를 만들고, 서버 캐시 키잉도 같은 정규화를 써
 * `React`/`react`가 같은 term-key로 접혀 캐시 파편화·M4 중복 upsert가 방지된다.
 *
 * ⚠️ **키잉 전용이다 — AI에 보여줄 질의 content에는 적용하지 않는다**(§3-2): lowercase가 대소문자
 * 유의미 용어(`NaN`/`Go`/`REST`/`C`)의 의미를 뭉개 어원 오답을 유발하므로, `buildClaudeRequest`는
 * 원본 keyword를 대소문자 보존해 싣는다(iOS 검증본 계승). 키잉과 프롬프트 입력은 다른 요구다.
 *
 * ⚠️ **서버 동기화 지점 — 이 함수를 바꾸면 `devetym-proxy`도 같이 바꿔야 한다.**
 * `devetym-proxy` `src/index.js`의 `normalizeTermKey`가 이 정규화를 복제해 D1 캐시 `term_key`를
 * 만든다(서버 슬라이스 S1 §3-2). 어긋나면 캐시 미스로는 드러나지 않고 — 서버는 자기 일관적이다 —
 * INV-12 번들 승격 잡이 흘린 키를 클라가 영영 조회 못 하는 형태로 조용히 샌다.
 * 특히 `trim()`의 공백 집합이 JS와 다르다는 점이 함정이며, 양쪽 경계는
 * [NormalizeKeywordTest][com.robin.devetym.data.NormalizeKeywordTest]가 정본으로 고정한다.
 * 파이프라인 세 번째 지점은 `Scripts/db-expand/term_key.py`다 — 셋이 같은 키를 낸다.
 *
 * 🚨 **이 함수를 바꾼 채로 앱을 릴리스하기 전에 로컬 DB 재정규화가 필요하다.**
 * `term.keyword`·`searchHistory.keyword`가 이 함수의 **옛 출력**을 PRIMARY KEY로 들고 있다
 * (`DevEtym.sq`, `TermRepository`). 정의가 바뀌면 기존 사용자의 `aa-tree` 행을 새 키 `aatree`로는
 * 못 찾아 — 북마크가 해제된 것처럼 보이고 `toggleBookmark`가 **중복 행을 새로 만든다.**
 * 서버·번들은 무관하다(서버는 요청마다 키를 재파생하고 번들은 읽기 전용).
 * W0c는 코드·테스트까지만 하고 릴리스는 열지 않았다 — 재정규화 작업은 별건으로 추적한다.
 *
 * ⚠️ 삭제하는 공백 집합은 `Char.isWhitespace()` 그대로여야 한다. `isBlank()`나 정규식 `\s`로
 * 바꾸지 말 것 — 서버 `WS` 집합과 갈라진다.
 */
fun normalizeKeyword(s: String): String =
    s.filterNot { it.isWhitespace() || it == '-' || it == '_' }.lowercase()
