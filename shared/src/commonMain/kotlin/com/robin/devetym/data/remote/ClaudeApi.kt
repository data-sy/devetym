package com.robin.devetym.data.remote

import com.robin.devetym.Constants
import com.robin.devetym.model.TermResult
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.HttpRequestTimeoutException
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import kotlinx.coroutines.CancellationException
import kotlinx.io.IOException

/**
 * 네트워크 실패 타입 (M3 슬라이스 §3-2, spec 2-2).
 *
 * `not_dev_term`/`possible_typo`는 예외가 아니라 `TermResult`로 반환된다([ClaudeResponse.toTermResult]) —
 * 이 예외 계층은 **오류 경로만** 분류한다. `generate`는 언제나 `TermResult` 또는 이 [ClaudeException]만
 * downstream(M4/M5)에 준다(다른 원시 예외 누출 금지 — DR-1·AD-1).
 */
sealed class ClaudeException(cause: Throwable? = null) : Exception(cause) {
    /** HTTP 429 — 기기당 일일 한도 초과(프록시). */
    data object DailyLimitExceeded : ClaudeException()

    /** 요청/응답 타임아웃. */
    data object Timeout : ClaudeException()

    /** 연결 실패 등 IO 오류(원인 보존). */
    data class Network(val error: Throwable) : ClaudeException(error)

    /** tool_use 없음·도구명 불명·input 디코드 실패·non-2xx(429 제외)·비JSON/빈 바디(DR-1·AD-1). */
    data object InvalidResponse : ClaudeException()
}

/**
 * read-through 프록시(ADR-0006) 호출 + `tool_use` 3분기 (M3 슬라이스 §3-2, spec 2-2).
 *
 * 앱에 키 없음 — `X-Device-Id`만 실어 프록시로 보낸다(서버가 키 주입 + 기기당 한도). 프록시가 D1
 * read-through 캐시든 갓 생성이든 **클라엔 투명**하다(호출 형태 동일). `deviceId`·`client`(엔진)는 M7이 주입.
 */
class ClaudeApi(
    private val client: HttpClient,
    private val deviceId: () -> String,
) {
    suspend fun generate(keyword: String): TermResult {
        val res = try {
            client.post(Constants.proxyBaseUrl) {
                header("X-Device-Id", deviceId())          // 프록시 한도 카운터 키
                contentType(ContentType.Application.Json)
                setBody(buildClaudeRequest(keyword))        // 원본 keyword·system·tools(§3-2·§3-3)
            }
        } catch (e: HttpRequestTimeoutException) {
            throw ClaudeException.Timeout
        } catch (e: IOException) {                          // Ktor 멀티플랫폼 IOException(§3-4·DR-2) — 연결 실패 등
            throw ClaudeException.Network(e)
        }

        // status를 body 디코드 '전에' 검사(DR-1). 429=한도, 그 밖 non-2xx(529 Overloaded·프록시 앞단
        // 502/503/524 HTML 오류페이지)는 ClaudeResponse가 아니므로 디코드에 태우지 않고 매핑한다.
        if (res.status.value == 429) throw ClaudeException.DailyLimitExceeded
        if (!res.status.isSuccess()) throw ClaudeException.InvalidResponse

        return try {
            res.body<ClaudeResponse>().toTermResult()       // tool_use 3분기
        } catch (e: CancellationException) {
            throw e                                          // 코루틴 취소는 삼키지 않는다
        } catch (e: HttpRequestTimeoutException) {           // 바디 수신 중 타임아웃
            throw ClaudeException.Timeout
        } catch (e: IOException) {                           // 바디 수신 중 연결 끊김
            throw ClaudeException.Network(e)
        } catch (e: ClaudeException) {
            throw e                                          // toTermResult가 던진 InvalidResponse 등 — 재매핑 금지
        } catch (e: Throwable) {
            // 2xx인데 ClaudeResponse로 디코드/변환 불가: SerializationException(스키마 불일치) ·
            // NoTransformationFoundException(비JSON content-type·빈 바디 — Cloudflare 200 챌린지 HTML 등, AD-1).
            // 어떤 디코드/변환 실패든 크래시 대신 InvalidResponse로 봉인(generate의 완결 계약).
            throw ClaudeException.InvalidResponse
        }
    }
}
