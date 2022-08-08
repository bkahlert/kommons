package com.bkahlert.kommons.debug

import kotlin.reflect.KFunction

/** Representation of a stack trace. */
public class StackTrace(elements: List<StackTraceElement>) : List<StackTraceElement> by elements {
    override fun toString(): String {
        return joinToString("\n    at ")
    }

    public companion object
}

/** Representation of a single element of a [StackTrace]. */
public interface StackTraceElement {

    /** The receiver of the [function] call. */
    public val receiver: String?

    /** The name of the invoked function. */
    public val function: String?

    /** The name of the invoked function with mangling information removed. */
    public val demangledFunction: String?

    /** The file in which the invocation takes place. */
    public val file: String?

    /** The line in which the invocation takes place. */
    public val line: Int

    /** The column in which the invocation takes place. */
    public val column: Int?
}

/** Returns the specified [function] with mangling information removed. */
public expect fun StackTrace.Companion.demangleFunction(function: String): String

/** Gets the current [StackTrace]. */
public expect inline fun StackTrace.Companion.get(): StackTrace


/**
 * Finds the [StackTraceElement] that immediately follows the one
 * the specified [predicate] stops returning `true` for the first time.
 *
 * In other words:
 * - The [predicate] is used to drop irrelevant calls for as long as it returns `false`.
 * - As soon as it responds `true` calls that are expected to exist are dropped.
 * - Finally, the next element which represents the caller of the last matched call is returned.
 */
public fun StackTrace.findOrNull(predicate: (StackTraceElement) -> Boolean): StackTraceElement? =
    (if (firstOrNull()?.let(predicate) == false) dropWhile { !predicate(it) } else this).dropWhile { predicate(it) }.firstOrNull()

/**
 * Finds the [StackTraceElement] that immediately follows the one
 * the specified [predicate] stops returning `true` for the first time.
 *
 * In other words:
 * - The [predicate] is used to drop irrelevant calls for as long as it returns `false`.
 * - As soon as it responds `true` calls that are expected to exist are dropped.
 * - Finally, the next element which represents the caller of the last matched call is returned.
 */
public fun StackTrace.find(predicate: (StackTraceElement) -> Boolean): StackTraceElement =
    findOrNull(predicate) ?: throw NoSuchElementException()

/**
 * Finds the [StackTraceElement] that represents the caller
 * invoking the [StackTraceElement] matching a call to the specified [functions].
 */
public expect fun StackTrace.findByLastKnownCallsOrNull(vararg functions: String): StackTraceElement?

/**
 * Finds the [StackTraceElement] that represents the caller
 * invoking the [StackTraceElement] matching a call to the specified [functions].
 */
public expect fun StackTrace.findByLastKnownCallsOrNull(vararg functions: KFunction<*>): StackTraceElement?
