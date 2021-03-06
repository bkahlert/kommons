package koodies.time

import kotlin.time.Duration

public fun Duration.passedSince(): Long = System.currentTimeMillis() - toLongMilliseconds()
public fun Duration.passedSince(instant: Long): Boolean = passedSince() >= instant
