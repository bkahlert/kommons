package koodies.logging

import koodies.asString
import koodies.concurrent.process.IO
import koodies.regex.RegularExpressions
import koodies.terminal.ANSI
import koodies.terminal.AnsiColors.red
import koodies.terminal.AnsiFormats.bold
import koodies.terminal.AnsiString.Companion.asAnsiString
import koodies.text.ANSI.Colors.gray
import koodies.text.ANSI.Formatter
import koodies.text.ANSI.Formatter.Companion.invoke
import koodies.text.LineSeparators.LF
import koodies.text.LineSeparators.lines
import koodies.text.Semantics
import koodies.text.TruncationStrategy.MIDDLE
import koodies.text.Unicode
import koodies.text.addColumn
import koodies.text.joinToTruncatedString
import koodies.text.mapLines
import koodies.text.prefixLinesWith
import koodies.text.takeUnlessBlank
import koodies.text.truncate
import koodies.text.wrapLines

/**
 * Logger interface with the ability to render its log with a border.
 */
public abstract class FixedWidthRenderingLogger(
    caption: String,
    log: ((String) -> Unit)? = null,
    contentFormatter: Formatter? = null,
    decorationFormatter: Formatter? = null,
    returnValueFormatter: ((ReturnValue) -> String)? = null,
    public val border: Border = BlockRenderingLogger.DEFAULT_BORDER,
    statusInformationColumn: Int? = null,
    statusInformationPadding: Int? = null,
    statusInformationColumns: Int? = null,
    width: Int? = null,
    public open val prefix: String = "",
) : RenderingLogger(caption, log) {

    public enum class Border {
        SOLID {
            override fun prefix(formatter: Formatter?): String = formatter.invoke("│").toString() + "   "

            override fun header(caption: String, formatter: Formatter?): String {
                val prefix = prefix(formatter)
                return koodies.builder.buildList {
                    val captionLines = caption.asAnsiString().lines()
                    +(formatter("╭──╴").toString() + formatter(captionLines.first()).bold())
                    captionLines.drop(1).forEach {
                        +"${prefix(formatter)}${formatter(it).bold()}"
                    }
                    +prefix
                }.joinToString(LF)
            }

            override fun footer(returnValue: ReturnValue, resultValueFormatter: (ReturnValue) -> String, formatter: Formatter?): String {
                return when (returnValue.successful) {
                    true -> {
                        val renderedSuccess = resultValueFormatter(returnValue)
                        formatter("│").toString() + LF + formatter("╰──╴").toString() + renderedSuccess
                    }
                    null -> {
                        val renderedUnready = resultValueFormatter(returnValue)
                        val halfLine = formatter("╵").toString()
                        halfLine + LF + halfLine + LF + renderedUnready
                    }
                    false -> {
                        Semantics.Error + LF + formatter("╰──╴").toString() + returnValue.format().toString().replace(Semantics.Error, "").red()
                    }
                }.asAnsiString().mapLines { it.bold() }
            }
        },
        DOTTED {
            private fun playSymbol(formatter: Formatter?) = formatter("▶").toString()
            private fun whitePlaySymbol(formatter: Formatter?) = formatter("▷").toString()

            override fun prefix(formatter: Formatter?): String = formatter("·").toString() + " "
            override fun header(caption: String, formatter: Formatter?): String {
                return koodies.builder.buildList {
                    val captionLines = caption.asAnsiString().lines()
                    +"${playSymbol(formatter)} ${formatter(captionLines.first()).bold()}"
                    captionLines.drop(1).forEach {
                        +"${whitePlaySymbol(formatter)} ${formatter(it).bold()}"
                    }
                }.joinToString(LF)
            }

            override fun footer(returnValue: ReturnValue, resultValueFormatter: (ReturnValue) -> String, formatter: Formatter?): String {
                val message: String = when (returnValue.successful) {
                    true -> resultValueFormatter(returnValue)
                    null -> resultValueFormatter(returnValue)
                    false -> resultValueFormatter(returnValue).red()
                }
                return message.asAnsiString().mapLines { it.bold() }
            }
        },
        NONE {
            override fun prefix(formatter: Formatter?): String = ""
            override fun header(caption: String, formatter: Formatter?): String = caption.bold()
            override fun footer(returnValue: ReturnValue, resultValueFormatter: (ReturnValue) -> String, formatter: Formatter?): String =
                resultValueFormatter(returnValue)
        };

        @Suppress("PropertyName", "NonAsciiCharacters")
        protected val ϟ: String = Unicode.greekSmallLetterKoppa.toString().red()
        public abstract fun prefix(formatter: Formatter?): String
        public abstract fun header(caption: String, formatter: Formatter?): String
        public abstract fun footer(returnValue: ReturnValue, resultValueFormatter: (ReturnValue) -> String, formatter: Formatter?): String

        public companion object {
            public val DEFAULT: Border = SOLID
            public fun from(border: Boolean?): Border = when (border) {
                true -> SOLID
                false -> DOTTED
                null -> NONE
            }
        }
    }

    public val contentFormatter: Formatter = contentFormatter ?: Formatter.PassThrough
    public val decorationFormatter: Formatter = decorationFormatter ?: Formatter.PassThrough
    public val returnValueFormatter: (ReturnValue) -> String = returnValueFormatter ?: RETURN_VALUE_FORMATTER
    public val statusInformationColumn: Int by lazy {
        val value = width ?: (statusInformationColumn?.let { it - prefix.length }) ?: 100
        require(value > 0) { "statusInformationColumn must be positive but was $value" }
        value
    }
    public val statusInformationPadding: Int by lazy {
        val value = statusInformationPadding ?: 5
        require(value > 0) { "statusInformationPadding must be positive but was $value" }
        value
    }
    public val statusInformationColumns: Int by lazy {
        val value = (statusInformationColumns?.let { it - prefix.length }) ?: 45
        require(value > 0) { "statusInformationColumns must be positive but was $value" }
        value
    }

    public val totalColumns: Int get() = statusInformationColumn + statusInformationPadding + statusInformationColumns

    /**
     * Logs some programs [IO] and the status of processed [items].
     */
    public open fun logStatus(items: List<CharSequence>, block: () -> CharSequence): Unit {
        block().takeUnlessBlank()?.let { contentFormatter(it) }?.run {
            render(true) {
                val leftColumn = wrapNonUriLines(statusInformationColumn).asAnsiString()
                val statusColumn =
                    items.asStatus().asAnsiString().truncate(maxLength = statusInformationColumns - 1, MIDDLE)
                val twoColumns = leftColumn.addColumn(statusColumn, columnWidth = statusInformationColumn + statusInformationPadding)
                if (closed) twoColumns
                else twoColumns.prefixLinesWith(prefix = prefix)
            }
        }
    }

    public fun logStatus(vararg statuses: CharSequence, block: () -> CharSequence = { IO.OUT typed "" }): Unit =
        logStatus(statuses.toList(), block)

    protected fun CharSequence.wrapNonUriLines(length: Int): CharSequence {
        return if (RegularExpressions.uriRegex.containsMatchIn(this)) this else asAnsiString().wrapLines(length)
    }

    /**
     * Creates a logger which serves for logging a sub-process and all of its corresponding events.
     *
     * This logger uses at least one line per log event. If less room is available [compactLogging] is more suitable.
     */
    @RenderingLoggingDsl
    public open fun <R> blockLogging(
        caption: CharSequence,
        contentFormatter: Formatter? = this.contentFormatter,
        decorationFormatter: Formatter? = this.decorationFormatter,
        returnValueFormatter: ((ReturnValue) -> String)? = this.returnValueFormatter,
        border: Border = this.border,
        block: FixedWidthRenderingLogger.() -> R,
    ): R = BlockRenderingLogger(
        caption,
        { logText { it } },
        contentFormatter,
        decorationFormatter,
        returnValueFormatter,
        border,
        statusInformationColumn,
        statusInformationPadding,
        statusInformationColumns,
    ).runLogging(block)

    /**
     * Creates a logger which serves for logging a sub-process and all of its corresponding events.
     *
     * This logger logs all events using a single line of text. If more room is needed [blockLogging] is more suitable.
     */
    @RenderingLoggingDsl
    public open fun <R> compactLogging(
        caption: CharSequence,
        contentFormatter: Formatter? = this.contentFormatter,
        decorationFormatter: Formatter? = this.decorationFormatter,
        returnValueFormatter: ((ReturnValue) -> String)? = this.returnValueFormatter,
        block: CompactRenderingLogger.() -> R,
    ): R = CompactRenderingLogger(
        caption,
        contentFormatter,
        decorationFormatter,
        returnValueFormatter,
        { logText { it } }
    ).runLogging(block)

    /**
     * Creates a logger which serves for logging a sub-process and all of its corresponding events.
     */
    @RenderingLoggingDsl
    public open fun <R> logging(
        caption: CharSequence,
        contentFormatter: Formatter? = this.contentFormatter,
        decorationFormatter: Formatter? = this.decorationFormatter,
        returnValueFormatter: ((ReturnValue) -> String)? = this.returnValueFormatter,
        border: Border = this.border,
        block: FixedWidthRenderingLogger.() -> R,
    ): R = SmartRenderingLogger(
        caption,
        { logText { it } },
        contentFormatter,
        decorationFormatter,
        returnValueFormatter,
        border,
        statusInformationColumn,
        statusInformationPadding,
        statusInformationColumns,
        prefix,
    ).runLogging(block)

    override fun toString(): String = asString {
        ::open to open
        ::caption to caption
        ::contentFormatter to contentFormatter
        ::decorationFormatter to decorationFormatter
        ::returnValueFormatter to returnValueFormatter
        ::statusInformationColumn to statusInformationColumn
        ::statusInformationPadding to statusInformationPadding
        ::statusInformationColumns to statusInformationColumns
    }
}

/**
 * Logs some programs [IO] and the processed items [statuses].
 */
public fun <T : FixedWidthRenderingLogger> T.logStatus(vararg statuses: String, block: () -> CharSequence = { IO.OUT typed "" }): Unit =
    logStatus(statuses.toList(), block)

private val pauseSymbol = ANSI.termColors.gray("▮▮")
private val playSymbol = ANSI.termColors.gray("◀")
private val fastForwardSymbol = ANSI.termColors.green("◀◀")

/**
 * Default implementation to render the status of a [List] of [HasStatus] instances.
 */
public fun <T : CharSequence> List<T>.asStatus(): String {
    if (size == 0) return pauseSymbol
    return joinToTruncatedString("  $playSymbol ", "$fastForwardSymbol ",
        truncated = "…",
        transform = { element -> element.bold() },
        transformEnd = { lastElement -> lastElement.gray() })
}
