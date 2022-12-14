package com.bkahlert.kommons.debug

@Suppress("NOTHING_TO_INLINE") // inline to avoid impact on stack trace
internal actual inline fun calledBy(function: String, vararg callers: String): Boolean {
    val functionPatterns = function.patterns()
    val callerPatterns = callers.flatMap { it.patterns() }

    return stackTrace()
        .dropWhile { it.startsWith("RuntimeException") || it.startsWith("captureStack ") }
        .dropWhile { callerPatterns.any { pattern -> it.contains(pattern) } }
        .firstOrNull()?.let { functionPatterns.any { pattern -> it.contains(pattern) } } ?: false
}

private fun String.patterns() = listOf(".$this", " $this")

@Suppress("NOTHING_TO_INLINE") // inline to avoid impact on stack trace
private inline fun stackTrace() = try {
    throw RuntimeException()
} catch (ex: Throwable) {
    ex.stackTraceToString().removeSuffix("\n")
}.lineSequence()
