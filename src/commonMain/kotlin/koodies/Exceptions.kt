package koodies

import koodies.text.LineSeparators.LF

private fun <T : Throwable> Array<out String>.letMessage(init: (String) -> T): T? =
    if (isNotEmpty()) init(joinToString(LF)) else null

/**
 * Collection of [Throwable] factories.
 */
public object Exceptions {

    /**
     * Throws a [IllegalStateException] with the given [messageLines]
     * concatenated as its [Throwable.message].
     */
    public fun ISE(vararg messageLines: String): IllegalStateException =
        messageLines.letMessage { IllegalStateException(it) } ?: IllegalStateException()

    /**
     * Throws a [IllegalStateException] with the [cause] as its [Throwable.cause]
     * and the given [messageLines] concatenated as its [Throwable.message].
     */
    public fun ISE(cause: Throwable, vararg messageLines: String): IllegalStateException =
        messageLines.letMessage { IllegalStateException(it, cause) } ?: IllegalStateException(cause)

    /**
     * Throws a [IllegalArgumentException] with the given [messageLines]
     * concatenated as its [Throwable.message].
     */
    public fun IAE(vararg messageLines: String): IllegalArgumentException =
        messageLines.letMessage { IllegalArgumentException(it) } ?: IllegalArgumentException()

    /**
     * Throws a [IllegalArgumentException] with the [cause] as its [Throwable.cause]
     * and the given [messageLines] concatenated as its [Throwable.message].
     */
    public fun IAE(cause: Throwable, vararg messageLines: String): IllegalArgumentException =
        messageLines.letMessage { IllegalArgumentException(it, cause) } ?: IllegalArgumentException(cause)

    /**
     * Throws a [AssertionError] with the given [messageLines]
     * concatenated as its [Throwable.message].
     */
    public fun AE(vararg messageLines: String): AssertionError =
        messageLines.letMessage { AssertionError(it) } ?: AssertionError()

    /**
     * Throws a [NoSuchElementException] with the given [messageLines]
     * concatenated as its [Throwable.message].
     */
    public fun NSEE(vararg messageLines: String): NoSuchElementException =
        messageLines.letMessage { NoSuchElementException(it) } ?: NoSuchElementException()
}
