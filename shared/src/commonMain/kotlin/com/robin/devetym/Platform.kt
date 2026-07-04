package com.robin.devetym

/** 플랫폼 식별 — expect/actual로 갈리는 얇은 조각(architecture §3). */
interface Platform {
    val name: String
}

expect fun platform(): Platform
