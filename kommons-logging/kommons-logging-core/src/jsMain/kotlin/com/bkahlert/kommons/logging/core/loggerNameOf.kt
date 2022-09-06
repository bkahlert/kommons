package com.bkahlert.kommons.logging.core

import com.bkahlert.kommons.debug.StackTrace
import com.bkahlert.kommons.debug.findByLastKnownCallsOrNull
import com.bkahlert.kommons.debug.get
import kotlin.reflect.KFunction

internal actual fun loggerNameOf(thisRef: Any?, fn: KFunction<*>): String =
    deriveClassName(thisRef, fn)

private fun deriveClassName(thisRef: Any?, fn: KFunction<*>): String =
    when (thisRef) {
        null -> StackTrace.get().findByLastKnownCallsOrNull(fn)?.receiver
        else -> thisRef::class.js.name
    } ?: "<global>"
