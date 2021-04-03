package koodies.logging

import koodies.asString

/**
 * A logger that can be used if no logging is needed.
 */
public open class MutedRenderingLogger : BlockRenderingLogger("", null) {

    init {
        withUnclosedWarningDisabled
    }

    override fun logText(block: () -> CharSequence): Unit = Unit
    override fun logLine(block: () -> CharSequence): Unit = Unit
    override fun logStatus(items: List<CharSequence>, block: () -> CharSequence): Unit = Unit
    override fun <R> logResult(block: () -> Result<R>): R {
        open = false
        return block().getOrThrow()
    }

    override fun logException(block: () -> Throwable): Unit {
        open = false
    }

    override fun toString(): String = asString {
        ::open to open
        ::caption to caption
    }

    /**
     * Creates a logger which serves for logging a sub-process and all of its corresponding events.
     */
    @RenderingLoggingDsl
    public fun <R> logging(block: MutedRenderingLogger.() -> R): R = runLogging(block)

    /**
     * Creates a logger which serves for logging a sub-process and all of its corresponding events.
     *
     * This logger uses at least one line per log event. If less room is available [compactLogging] is more suitable.
     */
    @RenderingLoggingDsl
    public fun <R> blockLogging(block: MutedRenderingLogger.() -> R): R = runLogging(block)
}
