package com.bkahlert.kommons.logging

import com.bkahlert.kommons.debug.StackTrace
import com.bkahlert.kommons.debug.findByLastKnownCallsOrNull
import com.bkahlert.kommons.debug.get
import kotlin.reflect.KClass
import kotlin.reflect.KFunction

/** Name used for logging. */
internal actual val KClass<*>.loggerName: String
    get() = js.name

/** Returns the name for logging using the specified [fn] to compute the subject in case `this` object is `null`. */
internal actual fun Any?.loggerName(fn: KFunction<*>): String =
    when (this) {
        null -> StackTrace.get().findByLastKnownCallsOrNull(fn)?.receiver ?: "<global>"
        else -> this::class.loggerName
    }
