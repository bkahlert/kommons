package com.bkahlert.kommons.test

/** Representation of a stack trace. */
internal class StackTrace(elements: List<StackTraceElement>) : List<StackTraceElement> by elements {
    override fun toString(): String {
        return joinToString("\n    at ")
    }

    internal companion object
}

/**
 * Finds the [StackTraceElement] that immediately follows the one
 * the specified [predicate] stops returning `true` for the first time.
 *
 * In other words:
 * - The [predicate] is used to drop irrelevant calls for as long as it returns `false`.
 * - As soon as it responds `true` calls that are expected to exist are dropped.
 * - Finally, the next element which represents the caller of the last matched call is returned.
 */
internal fun StackTrace.findOrNull(predicate: (StackTraceElement) -> Boolean): StackTraceElement? =
    (if (firstOrNull()?.let(predicate) == false) dropWhile { !predicate(it) } else this).dropWhile { predicate(it) }.firstOrNull()

/** Gets the current [StackTrace]. */
@Suppress("NOTHING_TO_INLINE") // inline to avoid impact on stack trace
internal inline fun StackTrace.Companion.get(): StackTrace =
    Thread.currentThread().stackTrace
        .dropWhile { it.className == Thread::class.qualifiedName }
        .let { StackTrace(it) }
