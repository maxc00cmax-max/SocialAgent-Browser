package com.example.browser

import kotlinx.coroutines.delay
import kotlin.random.Random

object HumanBehaviorEngine {

    suspend fun humanTypingDelay(textLength: Int) {
        val baseDelay = (textLength * Random.nextInt(40, 90)).coerceIn(300, 3500)
        delay(baseDelay.toLong())
    }

    suspend fun naturalPause(minMs: Long = 400, maxMs: Long = 1200) {
        val waitTime = Random.nextLong(minMs, maxMs)
        delay(waitTime)
    }

    suspend fun simulatedScrollStep() {
        delay(Random.nextLong(200, 500))
    }
}
