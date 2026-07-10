package com.robin.devetym.data.remote

import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.okhttp.OkHttp

/** Android HTTP 엔진 actual — OkHttp (M3 §3-4). */
actual fun httpEngine(): HttpClientEngine = OkHttp.create()
