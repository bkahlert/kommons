package com.bkahlert.kommons.test

import kotlin.reflect.KClass

/** Returns the most descriptive name this platform has for this class: qualified where available, simple otherwise. */
@PublishedApi // used by the public inline matchers in rootCause.kt
internal expect fun KClass<*>.bestName(): String
