package koodies.logging

import koodies.asString
import koodies.terminal.AnsiColors.red
import koodies.terminal.AnsiFormats.bold
import koodies.terminal.AnsiString.Companion.asAnsiString
import koodies.text.ANSI.Formatter
import koodies.text.ANSI.Formatter.Companion.invoke
import koodies.text.LineSeparators.LF
import koodies.text.LineSeparators.lines
import koodies.text.Semantics
import koodies.text.Unicode
import koodies.text.mapLines

/**
 * Logger interface with the ability to render its log with a border.
 */
public abstract class BorderedRenderingLogger(
    caption: String,
    parent: BorderedRenderingLogger? = null,
    contentFormatter: Formatter? = null,
    decorationFormatter: Formatter? = null,
    returnValueFormatter: ((ReturnValue) -> String)? = null,
    public val border: Border = BlockRenderingLogger.DEFAULT_BORDER,
    width: Int? = null,
    public open val prefix: String = "",
) : RenderingLogger(caption, parent) {

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
    public val statusInformationColumn: Int = width ?: parent?.let { it.statusInformationColumn - prefix.length } ?: 100
    public val statusInformationPadding: Int = parent?.statusInformationPadding ?: 5
    public val statusInformationColumns: Int = parent?.let { it.statusInformationColumns - prefix.length } ?: 45

    init {
        require(statusInformationColumn > 0) { ::statusInformationColumn.name + " must be positive but was $statusInformationColumn" }
        require(statusInformationPadding > 0) { ::statusInformationPadding.name + " must be positive but was $statusInformationPadding" }
        require(statusInformationColumns > 0) { ::statusInformationColumns.name + " must be positive but was $statusInformationColumns" }
    }

    public val totalColumns: Int = statusInformationColumn + statusInformationPadding + statusInformationColumns

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
        block: BorderedRenderingLogger.() -> R,
    ): R = BlockRenderingLogger(caption, this, contentFormatter, decorationFormatter, returnValueFormatter, border).runLogging(block)

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
    ): R = CompactRenderingLogger(caption, contentFormatter, decorationFormatter, returnValueFormatter, this).runLogging(block)

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
        block: BorderedRenderingLogger.() -> R,
    ): R = SmartRenderingLogger(caption, this, contentFormatter, decorationFormatter, returnValueFormatter, border).runLogging(block)

    override fun toString(): String = asString {
        ::open to open
        ::parent to parent?.caption
        ::caption to caption
        ::contentFormatter to contentFormatter
        ::decorationFormatter to decorationFormatter
        ::returnValueFormatter to returnValueFormatter
        ::statusInformationColumn to statusInformationColumn
        ::statusInformationPadding to statusInformationPadding
        ::statusInformationColumns to statusInformationColumns
    }
}
