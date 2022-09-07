package com.bkahlert.kommons.logging

import kotlin.reflect.KClass
import kotlin.reflect.KFunction

/** Name used for logging. */
internal expect val KClass<*>.loggerName: String

/** Returns the name for logging using the specified [fn] to compute the subject in case `this` object is `null`. */
internal expect fun Any?.loggerName(fn: KFunction<*>): String
