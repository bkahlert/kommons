package koodies.logging

import koodies.asString
import koodies.exec.IO
import koodies.regex.RegularExpressions.uriRegex
import koodies.text.ANSI.FilteringFormatter
import koodies.text.ANSI.Formatter
import koodies.text.ANSI.Text.Companion.ansi
import koodies.text.AnsiString.Companion.asAnsiString
import koodies.text.LineSeparators.LF
import koodies.text.LineSeparators.lines
import koodies.text.LineSeparators.mapLines
import koodies.text.LineSeparators.prefixLinesWith
import koodies.text.LineSeparators.wrapLines
import koodies.text.addColumn
import koodies.text.joinToTruncatedString
import koodies.text.takeUnlessBlank
import koodies.text.truncateByColumns
import koodies.text.withPrefix

/**
 * Logger interface with the ability to render its log with a border.
 */
public abstract class FixedWidthRenderingLogger(
    name: String,
    parent: SimpleRenderingLogger?,
    log: ((String) -> Unit)? = null,
    contentFormatter: FilteringFormatter? = null,
    decorationFormatter: Formatter? = null,
    returnValueFormatter: ((ReturnValue) -> ReturnValue)? = null,
    public val border: Border = BlockRenderingLogger.DEFAULT_BORDER,
    statusInformationColumn: Int? = null,
    statusInformationPadding: Int? = null,
    statusInformationColumns: Int? = null,
    width: Int? = null,
    public open val prefix: String = "",
) : SimpleRenderingLogger(name, parent, log) {

    public enum class Border {
        SOLID {
            override fun prefix(formatter: Formatter?): String = formatter.format("│").toString() + "   "

            override fun header(name: String, formatter: Formatter?): String =
                koodies.builder.buildList {
                    val nameLines = name.asAnsiString().lines()
                    +(formatter.format("╭──╴").toString() + formatter.format(nameLines.first()).ansi.bold)
                    nameLines.drop(1).forEach {
                        +"${prefix(formatter)}${formatter.format(it).ansi.bold}"
                    }
                    +formatter.format("│").toString()
                }.joinToString(LF)

            override fun footer(returnValue: ReturnValue, resultValueFormatter: (ReturnValue) -> ReturnValue, formatter: Formatter?): String {
                val processReturnValue = resultValueFormatter(returnValue)
                return when (returnValue.successful) {
                    true -> {
                        formatter.format("│").toString() + LF + formatter.format("╰──╴").toString() + processReturnValue.format()
                    }
                    null -> {
                        val halfLine = formatter.format("╵").toString()
                        val formatted: String = processReturnValue.symbol + (processReturnValue.textRepresentation?.withPrefix(" ") ?: "")
                        halfLine + LF + halfLine + (formatted.takeUnlessBlank()?.let { "$LF$it" } ?: "")
                    }
                    false -> {
                        processReturnValue.symbol + LF + formatter.format("╰──╴").toString() + (processReturnValue.textRepresentation ?: "")
                    }
                }.asAnsiString().mapLines { it.ansi.bold }
            }
        },
        DOTTED {
            private fun playSymbol(formatter: Formatter?) = formatter.format("▶").toString()
            private fun whitePlaySymbol(formatter: Formatter?) = formatter.format("▷").toString()

            override fun prefix(formatter: Formatter?): String = formatter.format("·").toString() + " "
            override fun header(name: String, formatter: Formatter?): String {
                return koodies.builder.buildList {
                    val nameLines = name.asAnsiString().lines()
                    +"${playSymbol(formatter)} ${formatter.format(nameLines.first()).ansi.bold}"
                    nameLines.drop(1).forEach {
                        +"${whitePlaySymbol(formatter)} ${formatter.format(it).ansi.bold}"
                    }
                }.joinToString(LF)
            }

            override fun footer(returnValue: ReturnValue, resultValueFormatter: (ReturnValue) -> ReturnValue, formatter: Formatter?): String =
                resultValueFormatter(returnValue).format()
                    .let { if (returnValue.successful == false) it.ansi.red else it }.ansi.bold.done
        },
        NONE {
            override fun prefix(formatter: Formatter?): String = ""
            override fun header(name: String, formatter: Formatter?): String = name.ansi.bold.done
            override fun footer(returnValue: ReturnValue, resultValueFormatter: (ReturnValue) -> ReturnValue, formatter: Formatter?): String =
                resultValueFormatter(returnValue).format()
        };

        public abstract fun prefix(formatter: Formatter?): String
        public abstract fun header(name: String, formatter: Formatter?): String
        public abstract fun footer(returnValue: ReturnValue, resultValueFormatter: (ReturnValue) -> ReturnValue, formatter: Formatter?): String

        public companion object {
            private fun Formatter?.format(text: CharSequence): CharSequence = (this ?: Formatter.PassThrough).invoke(text)
            public val DEFAULT: Border = SOLID
            public fun from(border: Boolean?): Border = when (border) {
                true -> SOLID
                false -> DOTTED
                null -> NONE
            }
        }
    }

    public val contentFormatter: FilteringFormatter = contentFormatter ?: FilteringFormatter.PassThrough
    public val decorationFormatter: Formatter = decorationFormatter ?: Formatter.PassThrough
    public val returnValueFormatter: (ReturnValue) -> ReturnValue = returnValueFormatter ?: { it }
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
            render {
                val leftColumn = wrapNonUriLines(statusInformationColumn)
                val statusColumn = items.asStatus().asAnsiString().truncateByColumns(maxColumns = statusInformationColumns - 1)
                val twoColumns = leftColumn.addColumn(statusColumn, columnWidth = statusInformationColumn + statusInformationPadding)
                twoColumns.prefixLinesWith(prefix) + LF
            }
        }
    }

    public fun logStatus(vararg statuses: CharSequence, block: () -> CharSequence = { IO.Output typed "" }): Unit =
        logStatus(statuses.toList(), block)

    protected fun CharSequence.wrapNonUriLines(length: Int): CharSequence {
        return if (uriRegex.containsMatchIn(this)) this else wrapLines(length)
    }

    /**
     * Creates a logger which serves for logging a sub-process and all of its corresponding events.
     *
     * This logger uses at least one line per log event. If less room is available [compactLogging] is more suitable.
     */
    @RenderingLoggingDsl
    public open fun <R> blockLogging(
        name: CharSequence,
        contentFormatter: FilteringFormatter? = this.contentFormatter,
        decorationFormatter: Formatter? = this.decorationFormatter,
        returnValueFormatter: ((ReturnValue) -> ReturnValue)? = this.returnValueFormatter,
        border: Border = this.border,
        block: FixedWidthRenderingLogger.() -> R,
    ): R = BlockRenderingLogger(
        name,
        this,
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
        name: CharSequence,
        contentFormatter: FilteringFormatter? = this.contentFormatter,
        decorationFormatter: Formatter? = this.decorationFormatter,
        returnValueFormatter: ((ReturnValue) -> ReturnValue)? = this.returnValueFormatter,
        block: CompactRenderingLogger.() -> R,
    ): R = CompactRenderingLogger(
        name,
        this,
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
        name: CharSequence,
        contentFormatter: FilteringFormatter? = this.contentFormatter,
        decorationFormatter: Formatter? = this.decorationFormatter,
        returnValueFormatter: ((ReturnValue) -> ReturnValue)? = this.returnValueFormatter,
        border: Border = this.border,
        block: FixedWidthRenderingLogger.() -> R,
    ): R = SmartRenderingLogger(
        name,
        this,
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
        ::name to name
        ::contentFormatter to contentFormatter
        ::decorationFormatter to decorationFormatter
        ::returnValueFormatter to returnValueFormatter
        ::statusInformationColumn to statusInformationColumn
        ::statusInformationPadding to statusInformationPadding
        ::statusInformationColumns to statusInformationColumns
    }
}

private val pauseSymbol: String = "▮▮".ansi.gray.toString()
private val playSymbol: String = "◀".ansi.gray.toString()
private val fastForwardSymbol: String = "◀◀".ansi.green.toString()

/**
 * Default implementation to render the status of a [List] of [HasStatus] instances.
 */
public fun <T : CharSequence> List<T>.asStatus(): String {
    if (size == 0) return pauseSymbol
    return joinToTruncatedString("  $playSymbol ", "$fastForwardSymbol ",
        truncated = "…",
        transform = { element: T -> element.ansi.bold },
        transformEnd = { lastElement -> lastElement.ansi.gray })
}
