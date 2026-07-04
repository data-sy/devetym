package com.robin.devetym

/** M0 골격 확인용 — 양 플랫폼에서 공유 코드가 도는지 증명하는 최소 표면. */
class Greeting {
    private val platform = platform()

    fun greet(): String = "Hello, ${platform.name}! — DevEtym KMP 골격"
}
