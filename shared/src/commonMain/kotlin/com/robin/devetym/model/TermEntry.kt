package com.robin.devetym.model

import kotlinx.serialization.Serializable

/**
 * 개발 용어 사전 항목의 직렬화 가능한 shape (M1 슬라이스 §3-1).
 *
 * 세 소비처의 공통 shape다: (a) 번들 `terms.json` 역직렬화, (b) 서버 read-through 응답,
 * (c) 로컬 DB 저장용 엔티티 변환(매퍼 경계는 M2 소관, 슬라이스 §7-1).
 *
 * **wire 키 계약(§3-1)**: JSON 키 = Kotlin 프로퍼티명(camelCase). `@SerialName`을 따로 두지 않으므로
 * 프로퍼티명이 곧 wire 키다. 외부 생산자(번들 `terms.json`·서버)는 이 키 이름으로 내보내야 한다 —
 * 다른 naming(예: `alias` 단수·`aliases` 생략)으로 내보내면 `aliases`가 default `emptyList()`로
 * 예외 없이 조용히 떨어져 별칭이 소실된다(INV-A 위반 경로). 이 계약의 실측은 slice §6
 * `test_실제번들_terms디코드_aliases내용보존`이 실제 배포 `terms.json`으로 수행한다.
 *
 * **버전 필드(INV-9)**: `schemaVersion`/`promptVersion`은 옵셔널·default `null`. 현재 번들 DB·AI
 * 응답 어디에도 없으므로 역직렬화 시 default로 채워진다(INV-B 하위호환). 서버 캐시·딜리버리 트랙
 * 착수 전까지 채우지 않되, 지금 필드만 확보해 이후 `@Serializable` DTO 마이그레이션을 회피한다.
 */
@Serializable
data class TermEntry(
    val keyword: String,
    val aliases: List<String> = emptyList(),
    val category: String,
    val summary: String,
    val etymology: String,
    val namingReason: String,
    // 버전 태깅 (INV-9) — 옵셔널·default로 기존 번들 DB/AI 응답과 호환(INV-B).
    val schemaVersion: Int? = null,
    val promptVersion: String? = null,
)
