package com.robin.devetym.data.remote

import com.robin.devetym.data.AppJson
import com.robin.devetym.model.Source
import com.robin.devetym.model.TermResult
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.HttpRequestData
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

/**
 * M3 슬라이스 §6-A — `ClaudeApi` × `MockEngine`(네트워크 무의존, Native Ktor 파이프라인+직렬화 실측).
 *
 * canned 응답은 **실 프로덕션 shape**로 고정한다(§6-A 무효 오라클 방지): 요청이 `thinking`(enabled)이라
 * 실 응답은 `tool_use` 앞에 `thinking`(및 `text`) 블록이 온다 → 3분기 canned를 모두 선행 블록으로 감싸
 * '중첩 content 블록 네이티브 디코드'가 선행 thinking 블록을 실제로 태우게 한다. `MockEngine`은 엔진을
 * 대체하므로 실 Darwin/OkHttp 소켓·예외타입 매핑은 무측정(§5, M8 이월).
 */
class ClaudeApiTest {

    private fun api(
        status: HttpStatusCode,
        body: String,
        contentType: String = "application/json",
        deviceId: String = "test-device-42",
        captured: MutableList<HttpRequestData>? = null,
    ): ClaudeApi {
        val engine = MockEngine { request ->
            captured?.add(request)
            respond(content = body, status = status, headers = headersOf(HttpHeaders.ContentType, contentType))
        }
        val client = HttpClient(engine) { install(ContentNegotiation) { json(AppJson) } }
        return ClaudeApi(client) { deviceId }
    }

    // 프로덕션 shape 블록들 — tool_use 앞에 thinking(및 text)를 선행시킨다.
    private val thinking = """{"type":"thinking","thinking":"이 용어를 분석한다","signature":"sig-abc"}"""
    private val text = """{"type":"text","text":"내부 서술"}"""
    private fun toolUse(name: String, input: String) =
        """{"type":"tool_use","id":"tu_1","name":"$name","input":$input}"""

    private fun response(vararg blocks: String) = """{"content":[${blocks.joinToString(",")}]}"""

    private val termEntryInput =
        """{"keyword":"mutex","aliases":["뮤텍스","mutual exclusion"],"category":"동시성","summary":"요약","etymology":"어원","namingReason":"작명 이유"}"""

    @Test
    fun test_generate_정상toolUse_Found() = runTest {
        // content = [thinking, text, tool_use] — 선행 블록이 있어도 첫 tool_use를 정확히 집어낸다.
        val result = api(HttpStatusCode.OK, response(thinking, text, toolUse("return_term_entry", termEntryInput)))
            .generate("mutex")
        assertTrue(result is TermResult.Found)
        assertEquals(Source.AI, result.source)
        assertEquals("mutex", result.entry.keyword)
        assertEquals(listOf("뮤텍스", "mutual exclusion"), result.entry.aliases) // 순서 포함 왕복 보존
        assertEquals("동시성", result.entry.category)
        assertEquals("작명 이유", result.entry.namingReason)
    }

    @Test
    fun test_generate_notDevTerm_NotDevTerm() = runTest {
        val result = api(HttpStatusCode.OK, response(thinking, toolUse("return_not_dev_term", "{}")))
            .generate("바나나")
        assertEquals(TermResult.NotDevTerm, result) // 예외 아님
    }

    @Test
    fun test_generate_possibleTypo_PossibleTypo() = runTest {
        val result = api(HttpStatusCode.OK, response(thinking, toolUse("return_possible_typo", """{"suggestion":"mutex"}""")))
            .generate("mutx")
        assertTrue(result is TermResult.PossibleTypo)
        assertEquals("mutex", result.suggestion)
    }

    @Test
    fun test_generate_429_DailyLimitExceeded() = runTest {
        assertFailsWith<ClaudeException.DailyLimitExceeded> {
            api(HttpStatusCode.TooManyRequests, """{"error":"rate limited"}""").generate("mutex")
        }
    }

    @Test
    fun test_generate_402_ServiceExhausted() = runTest {
        // 프록시가 Anthropic 결제 계열 에러(크레딧 소진·지출 한도)를 402로 매핑 — 전용 안내 문구 경로.
        assertFailsWith<ClaudeException.ServiceExhausted> {
            api(HttpStatusCode.PaymentRequired, """{"error":"service_exhausted"}""").generate("mutex")
        }
    }

    @Test
    fun test_generate_5xx_InvalidResponse() = runTest {
        // 529 Overloaded·프록시 앞단 503 HTML 오류페이지 — body 디코드 전 status 매핑(DR-1, 크래시 금지).
        assertFailsWith<ClaudeException.InvalidResponse> {
            api(HttpStatusCode.ServiceUnavailable, "<html><body>503</body></html>", contentType = "text/html")
                .generate("mutex")
        }
    }

    @Test
    fun test_generate_비JSON바디_InvalidResponse() = runTest {
        // 2xx인데 application/json이나 스키마 불일치/깨진 바디 → SerializationException → InvalidResponse.
        assertFailsWith<ClaudeException.InvalidResponse> {
            api(HttpStatusCode.OK, "this is not json").generate("mutex")
        }
    }

    @Test
    fun test_generate_비JSON_contentType_InvalidResponse() = runTest {
        // AD-1: 200 + Content-Type text/html(Cloudflare 챌린지/인터스티셜 HTML) → NoTransformationFoundException
        // → 넓힌 catch가 InvalidResponse로 봉인(uncaught 크래시 금지).
        assertFailsWith<ClaudeException.InvalidResponse> {
            api(HttpStatusCode.OK, "<html>challenge</html>", contentType = "text/html").generate("mutex")
        }
    }

    @Test
    fun test_generate_빈바디_InvalidResponse() = runTest {
        // AD-1: 200 + 빈 바디 → 디코드/변환 실패 → InvalidResponse.
        assertFailsWith<ClaudeException.InvalidResponse> {
            api(HttpStatusCode.OK, "").generate("mutex")
        }
    }

    @Test
    fun test_generate_toolUse없음_InvalidResponse() = runTest {
        // content에 thinking/text만 있고 tool_use 없음 → InvalidResponse.
        assertFailsWith<ClaudeException.InvalidResponse> {
            api(HttpStatusCode.OK, response(thinking, text)).generate("mutex")
        }
    }

    @Test
    fun test_generate_집합밖category_passThrough() = runTest {
        // category="네트웤"(오타, 6집합 밖)이 거부·정규화 없이 그대로 디코드(정규화는 M4, §4).
        val input =
            """{"keyword":"x","aliases":[],"category":"네트웤","summary":"s","etymology":"e","namingReason":"n"}"""
        val result = api(HttpStatusCode.OK, response(thinking, toolUse("return_term_entry", input))).generate("x")
        assertTrue(result is TermResult.Found)
        assertEquals("네트웤", result.entry.category)
    }

    @Test
    fun test_generate_XDeviceId헤더_전송() = runTest {
        val captured = mutableListOf<HttpRequestData>()
        api(
            HttpStatusCode.OK,
            response(thinking, toolUse("return_term_entry", termEntryInput)),
            deviceId = "device-xyz",
            captured = captured,
        ).generate("mutex")
        assertEquals("device-xyz", captured.single().headers["X-Device-Id"])
    }
}
