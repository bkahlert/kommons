package com.bkahlert.kommons.logging.core

import kotlin.reflect.KFunction

internal actual fun loggerNameOf(thisRef: Any?, fn: KFunction<*>): String =
    deriveClassName(thisRef, fn).sanitize()

private fun deriveClassName(thisRef: Any?, fn: KFunction<*>): String =
    when {
        thisRef == null -> locateCall(fn.name).className
        thisRef::class.isCompanion -> thisRef.javaClass.enclosingClass.name
        else -> thisRef.javaClass.name
    }

private fun locateCall(fn: String): StackTraceElement = RuntimeException().stackTrace
    .run {
        if (first().methodName == fn) asList()
        else dropWhile { it.methodName != fn }
    }
    .dropWhile { it.methodName == fn }
    .first()

private fun String.sanitize() = substringBefore("\$\$") // remove proxy suffixes, e.g. "$$EnhancerByCGLIB"
