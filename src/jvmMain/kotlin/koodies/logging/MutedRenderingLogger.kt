package koodies.logging

import koodies.text.ANSI.Formatter

/**
 * A logger that can be used if no logging is needed.
 */
public object MutedRenderingLogger : BlockRenderingLogger("", log = { }) {

    init {
        withUnclosedWarningDisabled
    }

    override fun logText(block: () -> CharSequence): Unit = Unit
    override fun logLine(block: () -> CharSequence): Unit = Unit
    override fun logStatus(items: List<CharSequence>, block: () -> CharSequence): Unit = Unit
    override fun <R> logResult(block: () -> Result<R>): R = block().getOrThrow()
    override fun logException(block: () -> Throwable): Unit = Unit

    override fun toString(): String = "log > /dev/null"

    override fun <R> blockLogging(
        caption: CharSequence,
        contentFormatter: Formatter?,
        decorationFormatter: Formatter?,
        returnValueFormatter: ((ReturnValue) -> ReturnValue)?,
        border: Border,
        block: FixedWidthRenderingLogger.() -> R,
    ): R = runLogging(block)

    override fun <R> compactLogging(
        caption: CharSequence,
        contentFormatter: Formatter?,
        decorationFormatter: Formatter?,
        returnValueFormatter: ((ReturnValue) -> ReturnValue)?,
        block: CompactRenderingLogger.() -> R,
    ): R = COMPACT.runLogging(block)

    override fun <R> logging(
        caption: CharSequence,
        contentFormatter: Formatter?,
        decorationFormatter: Formatter?,
        returnValueFormatter: ((ReturnValue) -> ReturnValue)?,
        border: Border,
        block: FixedWidthRenderingLogger.() -> R,
    ): R = runLogging(block)

    private val COMPACT: CompactRenderingLogger = object : CompactRenderingLogger("log > /dev/null", log = { }) {
        init {
            withUnclosedWarningDisabled
        }

        override fun logText(block: () -> CharSequence): Unit = Unit
        override fun logLine(block: () -> CharSequence): Unit = Unit
        override fun <R> logResult(block: () -> Result<R>): R = block().getOrThrow()
        override fun logException(block: () -> Throwable): Unit = Unit

        override fun toString(): String = "log > /dev/null"
    }
}
