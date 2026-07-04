package com.robin.devetym.data.local

import com.robin.devetym.db.Term
import com.robin.devetym.model.Source
import com.robin.devetym.model.TermEntry
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * DTO(`TermEntry`) ↔ 엔티티(SQLDelight 생성 `Term`) 매퍼 (M2 슬라이스 §3-4).
 * M1 §7-1이 M2로 이관한 매퍼. 엔티티 타입은 `.sq`가 생성한 `Term`(별도 손수 클래스 없음).
 *
 * **aliases·source 변환은 매퍼에 둔다**(컬럼 어댑터 아님, §7-2): 그래야 INV-A 매핑측 실측이
 * 드라이버 없는 순수 commonTest로 성립한다(`toEntity(...).toDto()`가 실제 JSON 왕복을 태움).
 */

// aliases 컬럼 전용 저장-내부 인코딩. wire 정책(M3, §7-3)과 독립 —
// List<String> 인코딩엔 encodeDefaults 모호성이 없어 결정적이다.
private val aliasesJson = Json

/**
 * DTO → 엔티티. **비대칭**: `TermEntry`엔 없는 DB 전용 필드를 호출자(M4)가 주입한다.
 *
 * 네 DB 전용 필드(`source`/`createdAt`/`isBookmarked`/`seenAt`)는 **전부 기본값 없는 필수 인자**다(DR-1 폐쇄).
 * `toDto`가 이 넷을 버리므로 M4 refresh는 옛 `Term`을 읽어 재주입해야 하는데(read-modify-write;
 * `INSERT OR REPLACE`=DELETE+INSERT라 부분 갱신 불가), 기본값이 있으면 재주입 누락이 컴파일 에러 없이
 * `isBookmarked→0`(북마크 소실)·`seenAt→null`(unpin, INV-6 위반)으로 조용히 덮인다. 기본값을 없애 누락을
 * 컴파일 에러로 만든다. 신규 저장은 호출부에서 `isBookmarked = false, seenAt = null`을 명시한다.
 */
fun TermEntry.toEntity(
    source: Source,
    createdAt: Long,
    isBookmarked: Boolean,   // 기본값 없음 — 보존-임계(북마크). M4가 명시 전달, 누락=컴파일 에러
    seenAt: Long?,           // 기본값 없음 — 보존-임계(pinned, INV-6). M4가 명시 전달, 누락=컴파일 에러
): Term = Term(
    keyword = keyword,
    aliases = aliasesJson.encodeToString(aliases),   // List<String> → JSON TEXT (순서 보존)
    category = category,                             // pass-through (정규화 없음)
    summary = summary,
    etymology = etymology,
    namingReason = namingReason,
    source = source.name,                            // Source → 'BUNDLE'|'AI'
    isBookmarked = if (isBookmarked) 1L else 0L,
    createdAt = createdAt,
    seenAt = seenAt,
    schemaVersion = schemaVersion?.toLong(),         // Int? → INTEGER Long?
    promptVersion = promptVersion,
)

/**
 * 엔티티 → DTO. DB 전용 필드(`source`/`isBookmarked`/`createdAt`/`seenAt`)를 버리고 DTO shape만 복원.
 *
 * `Long? → Int?`(`schemaVersion`)는 **Int 범위 값에서만 무손실** — Int 범위 밖 `Long?`(서버 배달 경로가
 * 채울 수 있음)의 무손실 보장·범위 가드는 M4/캐시 트랙에 상속(INV-9, §4).
 */
fun Term.toDto(): TermEntry = TermEntry(
    keyword = keyword,
    aliases = aliasesJson.decodeFromString(aliases), // JSON TEXT → List<String> (순서 복원)
    category = category,
    summary = summary,
    etymology = etymology,
    namingReason = namingReason,
    schemaVersion = schemaVersion?.toInt(),          // INTEGER Long? → Int?
    promptVersion = promptVersion,
)
