package com.bkahlert.kommons.test

import com.bkahlert.kommons.invoke
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant

/**
 * Returns a fixed [Clock] with its [Clock.now]
 * always returning the specified [now].
 */
public fun Clock.Companion.fixed(now: Instant): Clock = invoke { now }
