package koodies.time

import kotlin.time.Duration

public fun Duration.notPassedSince(instant: Long): Boolean = passedSince() < instant
