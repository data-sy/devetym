package com.robin.devetym.data.bundle

import com.robin.devetym.data.AppJson
import com.robin.devetym.data.normalizeKeyword
import com.robin.devetym.model.TermEntry
import com.robin.devetym.resources.Res
import kotlinx.serialization.json.Json

/**
 * 번들 DB(로컬 "head" 계층, INV-12) 조회 (M3 슬라이스 §3-1, spec 2-1).
 *
 * 앱 시작 1회 로드한 `terms.json`(650)을 메모리 캐시해 `search`(keyword+aliases 완전 매칭)·
 * `autocomplete`(keyword prefix)를 제공한다. 3계층 read-through의 1계층이며, 오케스트레이션(M4)이 소비한다.
 */
interface BundleDbSource {
    /** 정규화 후 `keyword` 또는 `aliases` 완전 일치(대소문자 무시). 미발견/빈 입력 → `null`. */
    fun search(keyword: String): TermEntry?

    /** `keyword` prefix 매칭(정규화). 빈 prefix → `emptyList()`. aliases는 대상 아님(spec 2-1). */
    fun autocomplete(prefix: String): List<TermEntry>
}

/**
 * 파싱·인덱스·매칭 구현 — **바이트 획득과 분리**(§3-1). 테스트가 실 번들 entries를 직접 주입할 수 있어,
 * INV-A 로더 실측(§6-B)이 실 배포 `terms.json`을 이 파서·인덱스·매칭 경로에 태운다.
 */
class InMemoryBundleDbSource(entries: List<TermEntry>) : BundleDbSource {

    // 생성자 param을 프로퍼티로 승격하지 않는다: buildMap 블록의 MutableMap 리시버에 `entries`(Map.entries)가
    // 있어 shadowing되기 때문. 별도 이름으로 보관해 인덱스·autocomplete가 참조한다.
    private val allEntries: List<TermEntry> = entries

    /**
     * 정규화 키 → 엔트리. `keyword`와 모든 `aliases`를 정규화 키로 접어 매칭 집합에 넣는다.
     *
     * 정규화 키 충돌(서로 다른 두 엔트리가 같은 정규화 키를 공유 — 실 번들 3건: `집계`→{aggregate,
     * aggregation}·`분기`→{branch, fork}·`샤딩`→{shard, sharding}) 시 **번들(리스트) 순서 first-wins**
     * (`if (key !in this) put`; common stdlib엔 `putIfAbsent` 없음). §3-1 결정적 반환 계약과 일치 —
     * last-wins 덮어쓰기 금지. 뒤로 밀린 매칭 발견성 회복은 Open Questions로 이월(M3는 결정적 반환만 보증).
     *
     * ⚠️ INV-A: `aliases`를 인덱스에서 누락하면 `search(alias)`가 조용히 miss한다(§6-B의 sharp 오라클).
     */
    private val byNormalizedKey: Map<String, TermEntry> = buildMap {
        for (entry in allEntries) {
            val keywordKey = normalizeKeyword(entry.keyword)
            if (keywordKey !in this) put(keywordKey, entry)
            for (alias in entry.aliases) {
                val aliasKey = normalizeKeyword(alias)
                if (aliasKey !in this) put(aliasKey, entry)
            }
        }
    }

    override fun search(keyword: String): TermEntry? {
        val key = normalizeKeyword(keyword)
        if (key.isEmpty()) return null
        return byNormalizedKey[key]
    }

    override fun autocomplete(prefix: String): List<TermEntry> {
        val normalized = normalizeKeyword(prefix)
        if (normalized.isEmpty()) return emptyList()
        return allEntries.filter { normalizeKeyword(it.keyword).startsWith(normalized) }
    }
}

/**
 * 앱 런타임의 실제 로드 경로 — compose-resources로 번들 바이트를 획득해 파싱·인덱스한다.
 * suspend(리소스 IO)이며 호출자(M4/M7)가 초기화 시 1회 await한다.
 *
 * ⚠️ 이 `Res.readBytes` 바이트 획득 자체(경로·인코딩)의 런타임 정확성은 plain JVM 단위테스트에서
 * 미측정(M1 fixture 실측) — 컴파일/링크 + M8 실기기 로드로 실측(§4 이월). 파서·인덱스·매칭의 aliases
 * 보존은 §6-B가 실 배포 파일을 `InMemoryBundleDbSource`에 직접 태워 실측한다.
 */
suspend fun loadBundleDbSource(json: Json = AppJson): BundleDbSource {
    val text = Res.readBytes("files/terms.json").decodeToString()
    return InMemoryBundleDbSource(json.decodeFromString<List<TermEntry>>(text))
}
