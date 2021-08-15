package com.bkahlert.kommons

import com.bkahlert.kommons.text.joinLinesToString

private fun <T : Throwable> Array<out String>.letMessage(init: (String) -> T): T? =
    if (isNotEmpty()) init(joinLinesToString()) else null

private fun <T : Throwable> Collection<String>.letMessage(init: (String) -> T): T? =
    if (isNotEmpty()) init(joinLinesToString()) else null

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

    /**
     * Throws a [IllegalArgumentException] with the [cause] as its [Throwable.cause]
     * and the given [messageLines] concatenated as its [Throwable.message].
     */
    @Suppress("FunctionName")
    public fun IAE(cause: Throwable, vararg messageLines: String): IllegalArgumentException =
        messageLines.letMessage { IllegalArgumentException(it, cause) } ?: IllegalArgumentException(cause)

    /**
     * Throws a [AssertionError] with the given [messageLines]
     * concatenated as its [Throwable.message].
     */
    @Suppress("FunctionName")
    public fun AE(vararg messageLines: String): AssertionError =
        messageLines.letMessage { AssertionError(it) } ?: AssertionError()

    /**
     * Throws a [AssertionError] with the given [messageLines]
     * concatenated as its [Throwable.message].
     */
    @Suppress("FunctionName")
    public fun AE(messageLines: Collection<String>): AssertionError =
        messageLines.letMessage { AssertionError(it) } ?: AssertionError()

    /**
     * Throws a [NoSuchElementException] with the given [messageLines]
     * concatenated as its [Throwable.message].
     */
    @Suppress("FunctionName", "SpellCheckingInspection")
    public fun NSEE(vararg messageLines: String): NoSuchElementException =
        messageLines.letMessage { NoSuchElementException(it) } ?: NoSuchElementException()

    /**
     * Throws a [NoSuchElementException] with the given [messageLines]
     * concatenated as its [Throwable.message].
     */
    @Suppress("FunctionName", "SpellCheckingInspection")
    public fun NSEE(messageLines: Collection<String>): NoSuchElementException =
        messageLines.letMessage { NoSuchElementException(it) } ?: NoSuchElementException()
}
