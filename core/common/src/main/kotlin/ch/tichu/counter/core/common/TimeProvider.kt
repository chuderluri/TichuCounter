package ch.tichu.counter.core.common

import kotlinx.datetime.Clock
import kotlinx.datetime.Instant

fun interface TimeProvider {
    fun now(): Instant
}

class SystemTimeProvider : TimeProvider {
    override fun now(): Instant = Clock.System.now()
}
