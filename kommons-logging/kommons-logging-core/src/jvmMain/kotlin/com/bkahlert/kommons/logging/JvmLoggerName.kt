package com.bkahlert.kommons.logging

import kotlin.reflect.KClass
import kotlin.reflect.KFunction

/** Name used for logging. */
internal actual val KClass<*>.loggerName: String
    get() = when {
        isCompanion -> java.enclosingClass.name
        else -> java.name
    }.sanitize()

/** Returns the name for logging using the specified [fn] to compute the subject in case `this` object is `null`. */
internal actual fun Any?.loggerName(fn: KFunction<*>): String =
    when (this) {
        null -> locateCall(fn.name).className.sanitize()
        else -> this::class.loggerName
    }

private fun locateCall(fn: String): StackTraceElement = RuntimeException().stackTrace
    .run {
        if (first().methodName == fn) asList()
        else dropWhile { it.methodName != fn }
    }
    .dropWhile { it.methodName == fn }
    .first()

private fun String.sanitize() = substringBefore("\$\$") // remove proxy suffixes, e.g. "$$EnhancerByCGLIB"
