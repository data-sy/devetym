package com.robin.devetym.data.remote

import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.darwin.Darwin

/** iOS HTTP 엔진 actual — Darwin(NSURLSession) (M3 §3-4). */
actual fun httpEngine(): HttpClientEngine = Darwin.create()
