package com.bkahlert.kommons.runtime
// TODO migrate

/**
 * Registers [handler] as to be called when this program is about to stop.
 */
public expect fun <T : () -> Unit> onExit(handler: T): T


// TODO migrate to kommons-debug
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

    public companion object {

        /**
         * Creates a call stack element for the given [receiver], [function], [function], [line] and [column].
         */
        public fun from(
            /**
             * Receiver of the [function] call.
             */
            receiver: String?,

            /**
             * Name of the invoked function.
             */
            function: String,

            /**
             * File in which the invocation takes place.
             */
            file: String?,

            /**
             * Line in which the invocation takes place.
             */
            line: Int,

            /**
             * Column in which the invocation takes place.
             */
            column: Int? = null,
        ): CallStackElement = object : CallStackElement {
            override val receiver: String? = receiver
            override val function: String = function
            override val file: String? = file
            override val line: Int = line
            override val column: Int? = column

            private val string = (receiver?.let { "$it." } ?: "") + "${function}(${file}:${line})" + (column?.let { ":$column" } ?: "")
            override fun toString(): String = string
            override val length: Int = string.length
            override fun get(index: Int): Char = string[index]
            override fun subSequence(startIndex: Int, endIndex: Int): CharSequence = string.subSequence(startIndex, endIndex)
        }
    }
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
