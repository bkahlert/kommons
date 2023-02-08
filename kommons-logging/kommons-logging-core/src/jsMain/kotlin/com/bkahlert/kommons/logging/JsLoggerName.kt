package com.bkahlert.kommons.logging

import kotlin.reflect.KClass
import kotlin.reflect.KFunction

/** Name used for logging. */
internal actual val KClass<*>.loggerName: String
    get() = js.name

/** Returns the name for logging using the specified [fn] to compute the subject in case `this` object is `null`. */
internal actual fun Any?.loggerName(fn: KFunction<*>): String =
    when (this) {
        null -> caller("loggerName", "provideDelegate") ?: "<global>"
        else -> this::class.loggerName
    }

@Suppress("NOTHING_TO_INLINE") // inline to avoid impact on stack trace
private inline fun caller(vararg callers: String): String? {
    val callerPatterns = callers.flatMap { it.patterns() }

//    println("Caller patterns:\n${callerPatterns.joinToString("\n")}\n")

    val stackTraceItem = stackTrace()
//        .toList().also { println("STACK I---\n${it.joinToString("\n")}\n------") }.asSequence()
        .dropWhile { callerPatterns.any { pattern -> it.contains(pattern) } }
//        .toList().also { println("STACK II---\n${it.joinToString("\n")}\n------") }.asSequence()
        .firstOrNull()

    return stackTraceItem
        ?.replaceFirst(Regex("^(?:\\s*at\\s+|[^<]*</)"), "") // Remove `  at ` (Node, Chrome) resp. `./path/file.js/</` (Firefox) prefix
//        ?.also { println("CANDIDATE---\n$it\n------") }
        ?.split('.', ' ', limit = 2)
        ?.first()
        ?.replace(Regex("(.*_kt)_[a-z0-9]+(?:@.*)?$")) {
            it.groupValues[1]
        }
}

private fun String.patterns() = listOf(
    ".$this",
    " $this",
    "$this@",
)

@Suppress("NOTHING_TO_INLINE") // inline to avoid impact on stack trace
private inline fun stackTrace() = try {
    throw RuntimeException()
} catch (ex: Throwable) {
    ex.stackTraceToString().removeSuffix("\n")
}.lineSequence()
    .dropWhile {
        it.startsWith("RuntimeException") ||
            it.startsWith("captureStack@") || // Firefox
            it.startsWith("captureStack ") // Chrome
    }
