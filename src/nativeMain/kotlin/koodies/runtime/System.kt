package koodies.runtime

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
public actual val ansiSupport: AnsiSupport = AnsiSupport.NONE

/**
 * Returns a [CharSequence] that represents the current caller.
 *
 * If specified, [skip] denotes the number of calls to
 * be removed from the top of the actual call stack
 * before returning it.
 */
@Suppress("NOTHING_TO_INLINE") // = avoid impact on stack trace
public actual inline fun getCaller(skip: UInt): CallStackElement = TODO()

/**
 * Returns a [CharSequence] that represents the current caller
 * which is found passing each [StackTraceElement] to the specified [locator].
 *
 * The actual [StackTraceElement] used is the predecessor of the first
 * one [locator] returned `true`.
 */
@Suppress("NOTHING_TO_INLINE") // = avoid impact on stack trace
public actual inline fun getCaller(crossinline locator: CallStackElement.() -> Boolean): CallStackElement {
    TODO("Not yet implemented")
}
