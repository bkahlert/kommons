package koodies.runtime

import koodies.runtime.AnsiSupport.NONE
import koodies.text.Semantics.Symbols

/**
 * Whether this program is running an integrated development environment.
 */
public actual val isDeveloping: Boolean = false

/**
 * Whether this program is running in debug mode.
 */
public actual val isDebugging: Boolean = false

/**
 * Whether this program is running in test mode.
 */
public actual val isTesting: Boolean by lazy { isDebugging }

/**
 * Registers [handler] as to be called when this program is about to stop.
 */
public actual fun <T : () -> Unit> onExit(handler: T): T = handler

/**
 * Supported level for [ANSI escape codes](https://en.wikipedia.org/wiki/ANSI_escape_code).
 */
public actual val ansiSupport: AnsiSupport = NONE

/**
 * Returns a [CharSequence] that represents the current caller.
 *
 * If specified, [skip] denotes the number of calls to
 * be removed from the top of the actual call stack
 * before returning it.
 */
@Suppress("NOTHING_TO_INLINE") // = avoid impact on stack trace
public actual inline fun getCaller(skip: UInt): CallStackElement {
    return JsCallStackElement()
}

public class JsCallStackElement : CallStackElement {
    private val q = Symbols.Unknown
    override val length: Int = q.length
    override fun get(index: Int): Char = q[index]
    override fun subSequence(startIndex: Int, endIndex: Int): CharSequence = q.subSequence(startIndex, endIndex)
    override val receiver: String? = null
    override val function: String = q
    override val file: String? = null
    override val line: Int = 0
    override val column: Int? = null
}

/**
 * Returns a [CharSequence] that represents the current caller
 * which is found passing each [CallStackElement] to the specified [locator].
 *
 * The actual [CallStackElement] used is the predecessor of the first
 * one [locator] returned `true`.
 */
@Suppress("NOTHING_TO_INLINE") // = avoid impact on stack trace
public actual inline fun getCaller(crossinline locator: CallStackElement.() -> Boolean): CallStackElement {
    TODO("Not yet implemented")
}
