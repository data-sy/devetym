package com.robin.devetym.data.remote

import com.robin.devetym.Constants
import io.ktor.client.HttpClient
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

/**
 * HTTP 엔진만 플랫폼별 (M3 슬라이스 §3-4 — M2 드라이버와 동형, 교체 비용 최소화).
 *
 * 클라이언트 설정(ContentNegotiation·타임아웃)은 `commonMain` 공통, 엔진(OkHttp/Darwin)만 `actual`.
 * `actual`의 검증은 컴파일/링크다: iOS는 `linkDebugFrameworkIosSimulatorArm64`, Android는 `assembleDebug`.
 * 단위테스트는 이 실엔진을 실행하지 않고 `MockEngine`(common)을 주입한다(§6-A).
 */
expect fun httpEngine(): HttpClientEngine

fun createHttpClient(json: Json): HttpClient = HttpClient(httpEngine()) {
    install(ContentNegotiation) { json(json) }
    install(HttpTimeout) { requestTimeoutMillis = Constants.apiTimeoutMs }
}
