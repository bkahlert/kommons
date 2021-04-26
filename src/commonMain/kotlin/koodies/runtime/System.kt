package koodies.runtime

/**
 * Whether this program is running an integrated development environment.
 */
public expect val isDeveloping: Boolean

/**
 * Whether this program is running in debug mode.
 */
public expect val isDebugging: Boolean

/**
 * Whether this program is running in test mode.
 */
public expect val isTesting: Boolean

/**
 * Registers [handler] as to be called when this program is about to stop.
 */
public expect fun <T : () -> Unit> onExit(handler: T): T

/**
 * Supported level for [ANSI escape codes](https://en.wikipedia.org/wiki/ANSI_escape_code).
 */
public expect val ansiSupport: AnsiSupport

/**
 * Representation of a single element of a (call) stack trace.
 */
public interface CallStackElement : CharSequence {

    /**
     * Receiver of the [function] call.
     */
    public val receiver: String?

    /**
     * Name of the invoked function.
     */
    public val function: String

    /**
     * File in which the invocation takes place.
     */
    public val file: String?

    /**
     * Line in which the invocation takes place.
     */
    public val line: Int

    /**
     * Column in which the invocation takes place.
     */
    public val column: Int?
}

/**
 * Returns a [CallStackElement] that represents the current caller.
 *
 * If specified, [skip] denotes the number of calls to
 * be removed from the top of the actual call stack
 * before returning it.
 */
public expect inline fun getCaller(skip: UInt = 0u): CallStackElement

/**
 * Returns a [CharSequence] that represents the current caller
 * which is found passing each [CallStackElement] to the specified [locator].
 *
 * The actual [CallStackElement] used is the predecessor of the first
 * one [locator] returned `true`.
 */
public expect inline fun getCaller(crossinline locator: CallStackElement.() -> Boolean): CallStackElement
