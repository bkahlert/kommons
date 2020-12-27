package koodies.time

import kotlin.time.Duration

fun Duration.notPassedSince(instant: Long) = passedSince() < instant
