package com.bkahlert.kommons.debug

@Suppress("NOTHING_TO_INLINE") // inline to avoid impact on stack trace
internal actual inline fun calledBy(function: String, vararg callers: String): Boolean =
    StackTrace.get().findByLastKnownCallsOrNull(*callers)?.methodName == function
