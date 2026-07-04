package com.robin.devetym

import kotlin.test.Test
import kotlin.test.assertTrue

class GreetingTest {
    @Test
    fun greeting_containsHello() {
        assertTrue(Greeting().greet().contains("Hello"))
    }
}
