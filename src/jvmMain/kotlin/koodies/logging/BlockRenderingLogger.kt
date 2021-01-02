package koodies.logging

import koodies.builder.ListBuilder.Companion.buildList
import koodies.concurrent.process.IO
import koodies.exception.toCompactString
import koodies.logging.RenderingLogger.Companion.formatException
import koodies.logging.RenderingLogger.Companion.formatResult
import koodies.terminal.ANSI
import koodies.terminal.AnsiCode.Companion.removeEscapeSequences
import koodies.terminal.AnsiFormats.bold
import koodies.terminal.AnsiString.Companion.asAnsiString
import koodies.text.*
import koodies.text.LineSeparators.LF
import koodies.text.LineSeparators.lines
import koodies.text.TruncationStrategy.MIDDLE

open class BlockRenderingLogger(
    private val caption: CharSequence,
    val bordered: Boolean = false,
    val statusInformationColumn: Int = 100,
    val statusInformationPadding: Int = 5,
    val statusInformationColumns: Int = 45,
    val log: (String) -> Any = { output: String -> print(output) },
) : RenderingLogger {

    init {
        check(statusInformationColumn > 0)
        check(statusInformationPadding > 0)
        check(statusInformationColumns > 0)
    }

    val totalColumns = statusInformationColumn + statusInformationPadding + statusInformationColumns

    final override fun render(trailingNewline: Boolean, block: () -> CharSequence): Unit =
        block().let { message: CharSequence ->
            val finalMessage: String = "$message" + if (trailingNewline) "\n" else ""
            log.invoke(finalMessage)
        }

    private fun getStatusPadding(text: String): String =
        " ".repeat((statusInformationColumn - text.removeEscapeSequences().length).coerceAtLeast(10))

    private val playSymbol = ANSI.termColors.green("▶")
    private val whitePlaySymbol = ANSI.termColors.green("▷")

    private val blockStart: String
        get() = buildList<String> {
            val captionLines = caption.asAnsiString().lines()
            if (bordered) {
                +""
                +"╭─────╴${captionLines.first().bold()}"
                captionLines.drop(1).forEach {
                    +"$prefix   ${it.bold()}"
                }
                +prefix
            } else {
                +"$playSymbol ${captionLines.first().bold()}"
                captionLines.drop(1).forEach {
                    +"$whitePlaySymbol ${it.bold()}"
                }
            }
        }.joinToString(LF)

    val prefix: String get() = if (bordered) "│   " else "· "
    fun <R> getBlockEnd(result: Result<R>): CharSequence {
        val message: String =
            if (result.isSuccess) {
                val renderedSuccess = formatResult(result)
                if (bordered) "│\n╰─────╴$renderedSuccess\n"
                else "$renderedSuccess"
            } else {
                if (bordered) {
                    formatException("$LF╰─────╴", result.toCompactString()) + LF
                } else {
                    formatException(" ", result.toCompactString())
                }
            }
        return message.asAnsiString().mapLines { it.bold() }
    }

    init {
        render(true) { blockStart }
    }

    override fun logText(block: () -> CharSequence): Unit = block().let {
        render(false) {
            it.asAnsiString().prefixLinesWith(ignoreTrailingSeparator = true, prefix = prefix)
        }
    }


    override fun logLine(block: () -> CharSequence): Unit = block().let {
        render(true) {
            it.asAnsiString().wrapLines(totalColumns).prefixLinesWith(ignoreTrailingSeparator = false, prefix = prefix)
        }
    }

    override fun logStatus(items: List<HasStatus>, block: () -> IO): Unit = block().let { output ->
        if (output.unformatted.isNotBlank()) {
            render(true) {
                val leftColumn = output.formatted.asAnsiString().wrapLines(statusInformationColumn).asAnsiString()
                val statusColumn =
                    items.renderStatus().asAnsiString().truncate(maxLength = statusInformationColumns - 1, MIDDLE)
                leftColumn.addColumn(statusColumn, columnWidth = statusInformationColumn + statusInformationPadding)
                    .prefixLinesWith(prefix = prefix)
            }
        }
    }

    override fun logException(block: () -> Throwable): Unit = block().let {
        render(true) {
            IO.Type.ERR.format(it.stackTraceToString()).prefixLinesWith(ignoreTrailingSeparator = false, prefix)
        }
    }

    var resultLogged = false

    override fun <R> logResult(block: () -> Result<R>): R {
        val result = block()
        render(true) {
            getBlockEnd(result).asAnsiString().wrapLines(totalColumns)
        }
        return result.getOrThrow()
    }

    override fun toString(): String =
        if (!resultLogged)
            "╷".let { vline ->
                LF + vline +
                        LF + vline + IO.Type.META.format(" no result logged yet") +
                        LF + vline
            } else IO.Type.META.format("❕ result already logged")
}
