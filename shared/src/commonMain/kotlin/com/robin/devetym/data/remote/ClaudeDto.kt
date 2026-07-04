package com.robin.devetym.data.remote

import com.robin.devetym.data.AppJson
import com.robin.devetym.model.Source
import com.robin.devetym.model.TermEntry
import com.robin.devetym.model.TermResult
import kotlinx.serialization.SerializationException
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive

/**
 * Anthropic Messages API 응답 (M3 슬라이스 §3-2).
 *
 * **비다형 flat DTO를 우대**(§3-2): content 원소는 `type` + `tool_use` 전용 필드(`name`/`input`)만
 * 선언하고, `thinking`/`text`/`redacted_thinking` 등 다른 블록 타입의 필드는 미선언 → [AppJson]의
 * `ignoreUnknownKeys`가 무시한다. 이렇게 두면 요청이 `thinking`(enabled)이라 `tool_use` 앞에 항상 오는
 * 선행 `thinking`(및 흔히 `text`) 블록을 관용적으로 흡수하고, 드문 `redacted_thinking` 변이도 별도
 * 등록·canned 픽스처 없이 통과한다(다형+개별등록의 미등록-변이 크래시/거짓오류화 경로를 원천 차단).
 */
@Serializable
data class ClaudeResponse(
    val content: List<ContentBlock> = emptyList(),
)

@Serializable
data class ContentBlock(
    val type: String,
    val name: String? = null,       // type == "tool_use"일 때 도구명
    val input: JsonObject? = null,  // type == "tool_use"일 때 도구 입력
)

/** 도구명 상수 (iOS 검증본 계승). */
object Tools {
    const val RETURN_TERM_ENTRY = "return_term_entry"
    const val RETURN_NOT_DEV_TERM = "return_not_dev_term"
    const val RETURN_POSSIBLE_TYPO = "return_possible_typo"
}

/**
 * `content`에서 첫 `tool_use` 블록을 찾아 도구명으로 3분기(설계 불변식, spec 2-2).
 *
 * `not_dev_term`/`possible_typo`는 **예외가 아니라 `TermResult`로** 반환(정상 분기). `tool_use` 없음·
 * 알 수 없는 도구명·`input` 디코드 실패는 [ClaudeException.InvalidResponse].
 *
 * **category는 pass-through**(M1·M2 상속): 6집합 밖 category도 거부·정규화 없이 그대로 디코드한다.
 * AI 응답 category 정규화/clamp는 M4(오케스트레이터)로 이월(§4·§7-4).
 */
fun ClaudeResponse.toTermResult(): TermResult {
    val toolUse = content.firstOrNull { it.type == "tool_use" }
        ?: throw ClaudeException.InvalidResponse
    return when (toolUse.name) {
        Tools.RETURN_TERM_ENTRY -> {
            val input = toolUse.input ?: throw ClaudeException.InvalidResponse
            val entry = try {
                AppJson.decodeFromJsonElement(TermEntry.serializer(), input)
            } catch (e: SerializationException) {
                throw ClaudeException.InvalidResponse
            }
            TermResult.Found(entry, Source.AI)
        }
        Tools.RETURN_NOT_DEV_TERM -> TermResult.NotDevTerm
        Tools.RETURN_POSSIBLE_TYPO -> {
            val suggestion = toolUse.input?.get("suggestion")?.jsonPrimitive?.contentOrNull ?: ""
            TermResult.PossibleTypo(suggestion)
        }
        else -> throw ClaudeException.InvalidResponse
    }
}
