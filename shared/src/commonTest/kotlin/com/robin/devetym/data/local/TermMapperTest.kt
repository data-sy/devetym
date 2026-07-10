package com.robin.devetym.data.local

import com.robin.devetym.model.Category
import com.robin.devetym.model.Source
import com.robin.devetym.model.TermEntry
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * M2 슬라이스 §6-A — 매퍼 INV-A 실측 (드라이버 없음). **DR-1 폐쇄, DoD 필수**.
 *
 * `toEntity(...).toDto()` 순수 왕복이 실제 JSON 인코드/디코드를 태워 `aliases`(순서)·`category` 무손실을
 * 실측한다(라이브 드라이버 불요). B1 결착으로 이 테스트는 JVM(`:testDebugUnitTest`)과
 * 네이티브(`:iosSimulatorArm64Test`) **양쪽에서 실행**되어 Native `kotlinx.serialization` 왕복도 실측한다.
 */
class TermMapperTest {

    private fun dto(
        keyword: String = "mutex",
        aliases: List<String> = listOf("뮤텍스", "mutual exclusion"),
        category: String = Category.CONCURRENCY,
        schemaVersion: Int? = null,
        promptVersion: String? = null,
    ) = TermEntry(
        keyword = keyword,
        aliases = aliases,
        category = category,
        summary = "상호 배제 잠금",
        etymology = "mutual exclusion의 축약",
        namingReason = "임계 구역 보호를 위한 잠금",
        schemaVersion = schemaVersion,
        promptVersion = promptVersion,
    )

    @Test
    fun test_toEntity_toDto_왕복_aliases순서_category보존() {
        val original = dto(aliases = listOf("A", "B", "C")) // 다중·순서 유의미
        val roundTripped = original
            .toEntity(source = Source.AI, createdAt = 1L, isBookmarked = false, seenAt = null)
            .toDto()
        assertEquals(original, roundTripped)                 // 구조 동등(전 필드)
        assertEquals(original.aliases, roundTripped.aliases) // 순서 포함 명시 단언(INV-A)
        assertEquals(original.category, roundTripped.category)
    }

    @Test
    fun test_toEntity_toDto_왕복_빈aliases_보존() {
        val original = dto(aliases = emptyList())
        val roundTripped = original
            .toEntity(source = Source.BUNDLE, createdAt = 1L, isBookmarked = false, seenAt = null)
            .toDto()
        assertEquals(emptyList(), roundTripped.aliases)      // JSON [] 왕복, silent 손실 없음
    }

    @Test
    fun test_toEntity_toDto_집합밖category_passThrough() {
        // 6집합 밖 값(오타·영문·trailing space)이 매퍼 왕복에서 거부·정규화 없이 보존(M1 pass-through 상속).
        for (outOfSet in listOf("네트웤", "Database", "동시성 ")) {
            val roundTripped = dto(category = outOfSet)
                .toEntity(source = Source.AI, createdAt = 1L, isBookmarked = false, seenAt = null)
                .toDto()
            assertEquals(outOfSet, roundTripped.category)
        }
    }

    @Test
    fun test_toEntity_toDto_버전필드_null과값_왕복보존() {
        // pre-versioning(null) — INV-B
        val preVersion = dto(schemaVersion = null, promptVersion = null)
            .toEntity(source = Source.AI, createdAt = 1L, isBookmarked = false, seenAt = null)
            .toDto()
        assertEquals(null, preVersion.schemaVersion)
        assertEquals(null, preVersion.promptVersion)

        // 서버 배달(값) — INV-9, Int↔Long 무손실
        val versioned = dto(schemaVersion = 2, promptVersion = "2026-07")
            .toEntity(source = Source.AI, createdAt = 1L, isBookmarked = false, seenAt = null)
            .toDto()
        assertEquals(2, versioned.schemaVersion)
        assertEquals("2026-07", versioned.promptVersion)
    }

    @Test
    fun test_toEntity_DB전용필드_주입값보존() {
        // 호출자 주입 DB 전용 필드가 엔티티에 정확 매핑(비대칭 매퍼).
        val entity = dto().toEntity(
            source = Source.AI,
            createdAt = 123L,
            isBookmarked = true,
            seenAt = 456L,
        )
        assertEquals("AI", entity.source)
        assertEquals(1L, entity.isBookmarked)   // Boolean true → 1
        assertEquals(123L, entity.createdAt)
        assertEquals(456L, entity.seenAt)
    }
}
