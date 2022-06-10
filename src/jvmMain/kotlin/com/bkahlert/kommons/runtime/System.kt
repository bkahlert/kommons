package com.bkahlert.kommons.runtime

/**
 * Registers [handler] as to be called when this program is about to stop.
 */
public actual fun <T : () -> Unit> onExit(handler: T): T = addShutDownHook(handler)

/**
 * Representation of a single element of a (call) stack trace.
 */
public class StackTraceElement(native: java.lang.StackTraceElement) : CallStackElement {
    private val string: String = "${native.className}.${native.methodName}(${native.fileName}:${native.lineNumber})"
    override fun toString(): String = string
    override val length: Int = string.length
    override fun get(index: Int): Char = string[0]
    override fun subSequence(startIndex: Int, endIndex: Int): CharSequence = string.subSequence(startIndex, endIndex)

    override val receiver: String = native.className
    override val function: String = native.methodName
    override val file: String? = native.fileName
    override val line: Int = native.lineNumber
    override val column: Int? = null
}

/**
 * Returns a [CallStackElement] that represents the current caller.
 *
 * If specified, [skip] denotes the number of calls to
 * be removed from the top of the actual call stack
 * before returning it.
 */
@Suppress("NOTHING_TO_INLINE") // = avoid impact on stack trace
public actual inline fun getCaller(skip: UInt): CallStackElement = currentStackTrace
    .let { if (it.size > skip.toInt()) it.drop(skip.toInt()).first() else it.last() }
    .let { StackTraceElement(it) }

/**
 * Returns a [CharSequence] that represents the current caller
 * which is found passing each [StackTraceElement] to the specified [locator].
 *
 * The actual [StackTraceElement] used is the predecessor of the last
 * one [locator] returned `true`.
 */
@Suppress("NOTHING_TO_INLINE") // = avoid impact on stack trace
public actual inline fun getCaller(crossinline locator: CallStackElement.() -> Boolean): CallStackElement =
    currentStackTrace.asSequence()
        .map { StackTraceElement(it) }
        .dropWhile { element -> !locator(element) }
        .dropWhile { element -> locator(element) }
        .first()
