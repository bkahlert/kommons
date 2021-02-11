package koodies

private fun <T : Throwable> Array<out String>.letMessage(init: (String) -> T): T? =
    if (isNotEmpty()) init(joinToString("\n")) else null

object Exceptions {
    fun ISE(vararg messageLines: String): IllegalStateException =
        messageLines.letMessage { IllegalStateException(it) } ?: IllegalStateException()

    fun ISE(cause: Throwable, vararg messageLines: String): IllegalStateException =
        messageLines.letMessage { IllegalStateException(it, cause) } ?: IllegalStateException(cause)

    fun IAE(vararg messageLines: String): IllegalArgumentException =
        messageLines.letMessage { IllegalArgumentException(it) } ?: IllegalArgumentException()

    fun IAE(cause: Throwable, vararg messageLines: String): IllegalArgumentException =
        messageLines.letMessage { IllegalArgumentException(it, cause) } ?: IllegalArgumentException(cause)

    fun AE(vararg messageLines: String): AssertionError =
        messageLines.letMessage { AssertionError(it) } ?: AssertionError()
}
