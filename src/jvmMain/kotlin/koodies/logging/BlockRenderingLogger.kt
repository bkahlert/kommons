package koodies.logging

import com.github.ajalt.mordant.AnsiCode
import koodies.builder.ListBuilder.Companion.buildList
import koodies.concurrent.process.IO
import koodies.logging.RenderingLogger.Companion.formatException
import koodies.logging.RenderingLogger.Companion.formatReturnValue
import koodies.nullable.invoke
import koodies.regex.RegularExpressions
import koodies.terminal.ANSI
import koodies.terminal.AnsiFormats.bold
import koodies.terminal.AnsiString.Companion.asAnsiString
import koodies.text.LineSeparators.LF
import koodies.text.LineSeparators.lines
import koodies.text.TruncationStrategy.MIDDLE
import koodies.text.addColumn
import koodies.text.mapLines
import koodies.text.prefixLinesWith
import koodies.text.truncate
import koodies.text.wrapLines

public open class BlockRenderingLogger(
    private val caption: CharSequence,
    override val bordered: Boolean = false,
    override val statusInformationColumn: Int = 100,
    override val statusInformationPadding: Int = 5,
    override val statusInformationColumns: Int = 45,
    public val log: (String) -> Any = { output: String -> print(output) },
) : BorderedRenderingLogger {

    public val totalColumns: Int
        get() {
            check(statusInformationColumn > 0)
            check(statusInformationPadding > 0)
            check(statusInformationColumns > 0)
            return statusInformationColumn + statusInformationPadding + statusInformationColumns
        }

    final override fun render(trailingNewline: Boolean, block: () -> CharSequence): Unit =
        block().let { message: CharSequence ->
            val finalMessage: String = message.toString() + if (trailingNewline) LF else ""
            log.invoke(finalMessage)
        }

    private val playSymbol = ANSI.termColors.green("▶")
    private val whitePlaySymbol = ANSI.termColors.green("▷")

    private val blockStart: String
        get() = buildList {
            val captionLines = caption.asAnsiString().lines()
            if (bordered) {
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

    override val prefix: String get() = if (bordered) "│   " else "· "
    public fun <R> getBlockEnd(result: Result<R>): CharSequence {
        val returnValue = result.toReturnValue()
        val message: String =
            if (returnValue.successful) {
                val renderedSuccess = formatReturnValue(returnValue)
                if (bordered) "│$LF╰─────╴$renderedSuccess"
                else "$renderedSuccess"
            } else {
                if (bordered) {
                    formatException("$LF╰─────╴", returnValue) + LF
                } else {
                    formatException(" ", returnValue)
                }
            }
        return message.asAnsiString().mapLines { it.bold() }
    }

    init {
        render(true) { blockStart }
    }

    override fun logText(block: () -> CharSequence): Unit = block().let {
        render(false) {
            it.asAnsiString().prefixLinesWith(prefix = prefix, ignoreTrailingSeparator = true)
        }
    }

    override fun logLine(block: () -> CharSequence): Unit = block().let {
        render(true) {
            it.wrapNonUriLines(totalColumns).prefixLinesWith(prefix = prefix, ignoreTrailingSeparator = false)
        }
    }

    override fun logStatus(items: List<HasStatus>, block: () -> CharSequence): Unit = block().let { output ->
        if (output.isNotBlank()) {
            render(true) {
                val leftColumn = output.wrapNonUriLines(statusInformationColumn).asAnsiString()
                val statusColumn =
                    items.renderStatus().asAnsiString().truncate(maxLength = statusInformationColumns - 1, MIDDLE)
                leftColumn.addColumn(statusColumn, columnWidth = statusInformationColumn + statusInformationPadding)
                    .prefixLinesWith(prefix = prefix)
            }
        }
    }

    override fun logException(block: () -> Throwable): Unit = block().let {
        render(true) {
            IO.Type.ERR.format(it.stackTraceToString()).prefixLinesWith(prefix, ignoreTrailingSeparator = false)
        }
    }

    public var resultLogged: Boolean = false

    override fun <R> logResult(block: () -> Result<R>): R {
        val result = block()
        render(true) {
            getBlockEnd(result).wrapNonUriLines(totalColumns)
        }
        return result.getOrThrow()
    }

    private fun CharSequence.wrapNonUriLines(length: Int): CharSequence {
        return if (RegularExpressions.uriRegex.containsMatchIn(this)) this else asAnsiString().wrapLines(length)
    }

    override fun toString(): String =
        if (!resultLogged)
            "╷".let { vline ->
                LF + vline +
                    LF + vline + IO.Type.META.format(" no result logged yet") +
                    LF + vline
            } else IO.Type.META.format("❕ result already logged")
}


/**
 * Creates a logger which serves for logging a sub-process and all of its corresponding events.
 *
 * This logger uses at least one line per log event. If less room is available [compactLogging] is more suitable.
 */
@RenderingLoggingDsl
public inline fun <reified R> Any?.blockLogging(
    caption: CharSequence,
    ansiCode: AnsiCode? = null,
    bordered: Boolean = (this as? BorderedRenderingLogger)?.bordered ?: false,
    crossinline block: BlockRenderingLogger.() -> R,
): R {
    val logger: BlockRenderingLogger = createBlockRenderingLogger(caption, bordered, ansiCode)
    val result: Result<R> = kotlin.runCatching { block(logger) }
    logger.logResult { result }
    return result.getOrThrow()
}

public fun Any?.createBlockRenderingLogger(
    caption: CharSequence,
    bordered: Boolean,
    ansiCode: AnsiCode?,
): BlockRenderingLogger = when (this) {
    is MutedRenderingLogger -> this
    is BorderedRenderingLogger -> BlockRenderingLogger(
        caption = caption,
        bordered = bordered,
        statusInformationColumn = statusInformationColumn - prefix.length,
        statusInformationPadding = statusInformationPadding,
        statusInformationColumns = statusInformationColumns - prefix.length,
    ) { output -> logText { ansiCode.invoke(output) } }
    is RenderingLogger -> BlockRenderingLogger(
        caption = caption,
        bordered = bordered
    ) { output -> logText { ansiCode.invoke(output) } }
    else -> BlockRenderingLogger(caption = caption, bordered = bordered)
}


public fun createBlockRenderingLogger2(
    caption: CharSequence,
    bordered: Boolean,
    ansiCode: AnsiCode?,
): BlockRenderingLogger = BlockRenderingLogger(caption = caption, bordered = bordered)

public fun MutedRenderingLogger.createBlockRenderingLogger2(
    caption: CharSequence,
    bordered: Boolean,
    ansiCode: AnsiCode?,
): MutedRenderingLogger = this

public fun BorderedRenderingLogger.createBlockRenderingLogger2(
    caption: CharSequence,
    bordered: Boolean,
    ansiCode: AnsiCode?,
): BlockRenderingLogger = BlockRenderingLogger(
    caption = caption,
    bordered = bordered,
    statusInformationColumn = statusInformationColumn - prefix.length,
    statusInformationPadding = statusInformationPadding,
    statusInformationColumns = statusInformationColumns - prefix.length,
) { output -> logText { ansiCode.invoke(output) } }

public fun RenderingLogger.createBlockRenderingLogger2(
    caption: CharSequence,
    bordered: Boolean,
    ansiCode: AnsiCode?,
): BlockRenderingLogger = BlockRenderingLogger(
    caption = caption,
    bordered = bordered
) { output -> logText { ansiCode.invoke(output) } }

public fun BlockRenderingLogger.createBlockRenderingLogger2(
    caption: CharSequence,
    bordered: Boolean,
    ansiCode: AnsiCode?,
): BlockRenderingLogger = BlockRenderingLogger(
    caption = caption,
    bordered = bordered
) { output -> logText { ansiCode.invoke(output) } }
