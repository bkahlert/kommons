package koodies.logging

import koodies.text.ANSI.Formatter
import koodies.tracing.OpenTelemetrySpan

/**
 * A logger that can be used if no logging is needed.
 */
public object MutedRenderingLogger : BlockRenderingLogger("", null, log = { }) {

    init {
        withUnclosedWarningDisabled
    }

    override val span: OpenTelemetrySpan = OpenTelemetrySpan("", null)

    override fun logText(block: () -> CharSequence): Unit = Unit
    override fun logLine(block: () -> CharSequence): Unit = Unit
    override fun logStatus(items: List<CharSequence>, block: () -> CharSequence): Unit = Unit
    override fun <R> logResult(result: Result<R>): R = result.getOrThrow()

    override fun toString(): String = "log > /dev/null"

    override fun <R> blockLogging(
        caption: CharSequence,
        contentFormatter: Formatter?,
        decorationFormatter: Formatter?,
        returnValueFormatter: ((ReturnValue) -> ReturnValue)?,
        border: Border,
        block: FixedWidthRenderingLogger.() -> R,
    ): R = this.block()

    override fun <R> compactLogging(
        caption: CharSequence,
        contentFormatter: Formatter?,
        decorationFormatter: Formatter?,
        returnValueFormatter: ((ReturnValue) -> ReturnValue)?,
        block: CompactRenderingLogger.() -> R,
    ): R = COMPACT.block()

    override fun <R> logging(
        caption: CharSequence,
        contentFormatter: Formatter?,
        decorationFormatter: Formatter?,
        returnValueFormatter: ((ReturnValue) -> ReturnValue)?,
        border: Border,
        block: FixedWidthRenderingLogger.() -> R,
    ): R = this.block()

    private val COMPACT: CompactRenderingLogger = object : CompactRenderingLogger("log > /dev/null", this@MutedRenderingLogger, log = { }) {
        init {
            withUnclosedWarningDisabled
        }

        override fun logText(block: () -> CharSequence): Unit = Unit
        override fun logLine(block: () -> CharSequence): Unit = Unit
        override fun <R> logResult(result: Result<R>): R = result.getOrThrow()

        override fun toString(): String = "log > /dev/null"
    }
}
