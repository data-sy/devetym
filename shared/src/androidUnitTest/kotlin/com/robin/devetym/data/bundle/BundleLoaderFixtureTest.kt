package com.robin.devetym.data.bundle

import com.robin.devetym.data.AppJson
import com.robin.devetym.model.TermEntry
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * M3 슬라이스 §6-B — 실 번들 로더 INV-A 실측 (DR-1 로더측 폐쇄, 필수).
 *
 * `Res.readBytes`(compose-resources 런타임)는 plain JVM 단위테스트에서 미실행(M1 fixture 실측)이라,
 * `:shared:testDebugUnitTest`(=androidUnitTest)에서 실 배포 `terms.json`을 `File`로 읽어
 * **`InMemoryBundleDbSource`의 파서·인덱스·매칭 경로에 태운다**(classpath 사본 아닌 실 배포 파일).
 *
 * **성공 디코드·리스트 존재를 오라클로 삼지 않는다**(§4·M1 §7-4): 반드시 (a) aliases *내용*과
 * (b) *alias 검색 성립*을 단언한다. (b)가 M1 fixture 대비 증분 폐쇄점 — 인덱스를 keyword로만 구성하면
 * 디코드는 aliases를 보여도 `search(alias)`가 silent miss한다.
 */
class BundleLoaderFixtureTest {

    private val bundleFile = File("src/commonMain/composeResources/files/terms.json")

    @Test
    fun test_실번들로더_aliases내용보존_및_alias검색() {
        assertTrue(
            bundleFile.exists(),
            "배포 번들을 찾지 못함: ${bundleFile.absolutePath} (작업 디렉터리가 :shared 모듈 루트가 아닐 수 있음)",
        )
        val entries = AppJson.decodeFromString<List<TermEntry>>(bundleFile.readText())

        // (c) 항목 수 650 + aliases 보유 term 다수(전부 empty면 wire 키 계약 위반 신호 — M1 fixture 계승).
        assertEquals(650, entries.size)
        assertTrue(entries.count { it.aliases.isNotEmpty() } > 0, "aliases를 가진 term이 하나도 없다 — wire 키 계약 위반 의심")

        val source = InMemoryBundleDbSource(entries)

        // (a) aliases 내용·category를 로더가 파싱·보존.
        val aaTree = source.search("aa-tree")!!
        assertEquals(listOf("AA 트리", "Arne Andersson tree"), aaTree.aliases) // 순서 포함
        assertEquals("자료구조", aaTree.category)

        // (b) alias가 로더 인덱스의 검색 집합에 편입됐음 — 실 번들로 실측(증분 폐쇄점).
        assertEquals("aa-tree", source.search("Arne Andersson tree")?.keyword)
        assertEquals("aa-tree", source.search("AA 트리")?.keyword)
    }
}
