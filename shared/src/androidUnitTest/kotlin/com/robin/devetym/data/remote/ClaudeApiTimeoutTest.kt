package com.robin.devetym.data.remote

import com.robin.devetym.data.AppJson
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertFailsWith

/**
 * M3 슬라이스 §6-A(타임아웃) — §7-5대로 `androidUnitTest`(JVM)로 격리.
 *
 * 타임아웃 유발은 `MockEngine` 지연 + 짧은 `HttpTimeout`로 실측한다. `runTest`(가상시계)는 Ktor
 * `HttpTimeout`의 실시간 delay와 상호작용이 불안정하므로 `runBlocking`(실시간)으로 결정적으로 태운다.
 * 네이티브 런루프 안정성 위해 4축 commonTest가 아니라 여기 격리(네이티브 타임아웃 경로는 M8 실기기 이월).
 */
class ClaudeApiTimeoutTest {

    @Test
    fun test_generate_타임아웃_Timeout() = runBlocking {
        val engine = MockEngine {
            delay(5_000)  // requestTimeout(100ms)보다 훨씬 김 — 타임아웃이 먼저 발화
            respond("{}", HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, "application/json"))
        }
        val client = HttpClient(engine) {
            install(ContentNegotiation) { json(AppJson) }
            install(HttpTimeout) { requestTimeoutMillis = 100 }
        }
        assertFailsWith<ClaudeException.Timeout> {
            ClaudeApi(client) { "device" }.generate("mutex")
        }
        Unit
    }
}
