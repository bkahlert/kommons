package koodies.time

import kotlin.time.Duration

fun Duration.passedSince(): Long = System.currentTimeMillis() - toLongMilliseconds()

fun Duration.passedSince(instant: Long) = passedSince() >= instant
