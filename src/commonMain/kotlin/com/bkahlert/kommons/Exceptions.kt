package com.bkahlert.kommons

private fun <T : Throwable> Array<out String>.letMessage(init: (String) -> T): T? =
    if (isNotEmpty()) init(joinToString(LineSeparators.Default)) else null

private fun <T : Throwable> Collection<String>.letMessage(init: (String) -> T): T? =
    if (isNotEmpty()) init(joinToString(LineSeparators.Default)) else null

/**
 * Collection of [Throwable] factories.
 */
public object Exceptions {

    /**
     * Throws a [IllegalStateException] with the given [messageLines]
     * concatenated as its [Throwable.message].
     */
    @Suppress("FunctionName")
    public fun ISE(vararg messageLines: String): IllegalStateException =
        messageLines.letMessage { IllegalStateException(it) } ?: IllegalStateException()

    /**
     * Throws a [IllegalStateException] with the given [messageLines]
     * concatenated as its [Throwable.message].
     */
    @Suppress("FunctionName")
    public fun ISE(messageLines: Collection<String>): IllegalStateException =
        messageLines.letMessage { IllegalStateException(it) } ?: IllegalStateException()

    /**
     * Throws a [IllegalStateException] with the [cause] as its [Throwable.cause]
     * and the given [messageLines] concatenated as its [Throwable.message].
     */
    @Suppress("FunctionName")
    public fun ISE(cause: Throwable, vararg messageLines: String): IllegalStateException =
        messageLines.letMessage { IllegalStateException(it, cause) } ?: IllegalStateException(cause)

    /**
     * Throws a [IllegalArgumentException] with the given [messageLines]
     * concatenated as its [Throwable.message].
     */
    @Suppress("FunctionName")
    public fun IAE(vararg messageLines: String): IllegalArgumentException =
        messageLines.letMessage { IllegalArgumentException(it) } ?: IllegalArgumentException()

    /**
     * Throws a [IllegalArgumentException] with the given [messageLines]
     * concatenated as its [Throwable.message].
     */
    @Suppress("FunctionName")
    public fun IAE(messageLines: Collection<String>): IllegalArgumentException =
        messageLines.letMessage { IllegalArgumentException(it) } ?: IllegalArgumentException()
}
