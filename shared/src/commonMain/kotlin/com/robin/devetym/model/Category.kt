package com.robin.devetym.model

/**
 * 카테고리 정본 어휘 (M1 슬라이스 §3-3) — 번들 DB·AI 응답 공통, 6개.
 *
 * downstream(카테고리 필터·버킷팅)이 기대하는 정본 집합을 **문서화**한다. 단,
 * **M1 계약은 `category: String` pass-through다** — M1 직렬화/역직렬화는 집합 밖 값(오타 `네트웤`·
 * 영문값 `Database` 등)도 검증·거부·정규화 없이 그대로 왕복 보존한다(INV-A). 6개 집합에 대한
 * 강제/정규화는 M1 밖이다:
 * - AI 응답 경로(M3·M4)가 유입되는 집합 밖 값을 강제/정규화한다.
 * - 사람이 큐레이션하는 번들 `terms.json`은 번들 저작 계약과 번들 로더/린트(M3 `BundleDbSource`)가
 *   6개 집합 in-set를 보장한다.
 * - 서버 read-through 배달 경로는 캐시·딜리버리 트랙(cache-delivery-milestones INV-13,
 *   정규화-후-캐시쓰기)이 소유한다.
 *
 * 따라서 이 값은 M1에서 **강제에 쓰지 않는다** — 어휘 문서화 및 downstream 정규화의 참조 집합일 뿐이다.
 */
object Category {
    const val CONCURRENCY = "동시성"
    const val DATA_STRUCTURE = "자료구조"
    const val NETWORK = "네트워크"
    const val DATABASE = "DB"
    const val PATTERN = "패턴"
    const val ETC = "기타"

    /** 정본 6집합. downstream 정규화·버킷팅의 참조 집합(M1은 강제하지 않음). */
    val CANONICAL: Set<String> = setOf(
        CONCURRENCY, DATA_STRUCTURE, NETWORK, DATABASE, PATTERN, ETC,
    )
}
