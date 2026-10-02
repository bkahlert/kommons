package com.bkahlert.kommons.test

import com.bkahlert.kommons.time.invoke
import kotlin.time.Clock
import kotlin.time.Instant

/**
 * Returns a fixed [Clock] with its [Clock.now]
 * always returning the specified [now].
 */
public fun Clock.Companion.fixed(now: Instant): Clock = invoke { now }
