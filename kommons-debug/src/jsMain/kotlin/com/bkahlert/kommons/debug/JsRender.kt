package com.bkahlert.kommons.debug

@Suppress("NOTHING_TO_INLINE") // inline to avoid impact on stack trace
internal actual inline fun calledBy(function: String, vararg callers: String): Boolean {
    val functionPatterns = function.patterns()
    val callerPatterns = callers.flatMap { it.patterns() }

//    println("Function patterns:\n${functionPatterns.joinToString("\n")}\n")
//    println("Caller patterns:\n${callerPatterns.joinToString("\n")}\n")

    return stackTrace()
        .dropWhile { callerPatterns.any { pattern -> it.contains(pattern) } }
//        .toList().also { println("STACK---\n${it.joinToString("\n")}\n------") }.asSequence()
        .firstOrNull()?.let { functionPatterns.any { pattern -> it.contains(pattern) } } ?: false
}

private fun String.patterns() = listOf(
    ".$this",
    " $this",
    "${this}$",
    "${this}_0",
    "${this}_1",
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
