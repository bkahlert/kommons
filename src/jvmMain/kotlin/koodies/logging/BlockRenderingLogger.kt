package koodies.logging

import koodies.builder.buildList
import koodies.concurrent.process.IO
import koodies.logging.RenderingLogger.Companion.formatException
import koodies.logging.RenderingLogger.Companion.formatReturnValue
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
                +"╭──╴${captionLines.first().bold()}"
                captionLines.drop(1).forEach {
                    +"$prefix${it.bold()}"
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
                if (bordered) "│$LF╰──╴$renderedSuccess"
                else "$renderedSuccess"
            } else {
                if (bordered) {
                    formatException("$LF╰──╴", returnValue) + LF
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
public inline fun <reified T : MutedRenderingLogger, reified R> T.blockLogging(
    caption: CharSequence,
    formatter: koodies.text.ANSI.Formatter? = null,
    bordered: Boolean = (this as? BorderedRenderingLogger)?.bordered ?: false,
    crossinline block: T.() -> R,
): R = runLogging(block)

/**
 * Creates a logger which serves for logging a sub-process and all of its corresponding events.
 *
 * This logger uses at least one line per log event. If less room is available [compactLogging] is more suitable.
 */
@RenderingLoggingDsl
public inline fun <reified T : BorderedRenderingLogger, reified R> T.blockLogging(
    caption: CharSequence,
    formatter: koodies.text.ANSI.Formatter? = null,
    bordered: Boolean = (this as? BorderedRenderingLogger)?.bordered ?: false,
    crossinline block: BlockRenderingLogger.() -> R,
): R = BlockRenderingLogger(
    caption = caption,
    bordered = bordered,
    statusInformationColumn = statusInformationColumn - prefix.length,
    statusInformationPadding = statusInformationPadding,
    statusInformationColumns = statusInformationColumns - prefix.length,
) { output -> logText { formatter?.invoke(output) ?: output } }.runLogging(block)

/**
 * Creates a logger which serves for logging a sub-process and all of its corresponding events.
 *
 * This logger uses at least one line per log event. If less room is available [compactLogging] is more suitable.
 */
@RenderingLoggingDsl
public inline fun <reified T : RenderingLogger, reified R> T.blockLogging(
    caption: CharSequence,
    formatter: koodies.text.ANSI.Formatter? = null,
    bordered: Boolean = (this as? BorderedRenderingLogger)?.bordered ?: false,
    crossinline block: BlockRenderingLogger.() -> R,
): R = BlockRenderingLogger(
    caption = caption,
    bordered = bordered
) { output -> logText { formatter?.invoke(output) ?: output } }.runLogging(block)

/**
 * Creates a logger which serves for logging a sub-process and all of its corresponding events.
 *
 * This logger uses at least one line per log event. If less room is available [compactLogging] is more suitable.
 */
@RenderingLoggingDsl
public inline fun <reified R> blockLogging(
    caption: CharSequence,
    formatter: koodies.text.ANSI.Formatter? = null,
    bordered: Boolean = false,
    crossinline block: BlockRenderingLogger.() -> R,
): R = BlockRenderingLogger(caption = caption, bordered = bordered).runLogging(block)

/**
 * Creates a logger which serves for logging a sub-process and all of its corresponding events.
 *
 * This logger uses at least one line per log event. If less room is available [compactLogging] is more suitable.
 */
@JvmName("nullableBlockLogging")
@RenderingLoggingDsl
public inline fun <reified T : RenderingLogger?, reified R> T.blockLogging(
    caption: CharSequence,
    formatter: koodies.text.ANSI.Formatter? = null,
    bordered: Boolean = false,
    crossinline block: BlockRenderingLogger.() -> R,
): R =
    if (this is RenderingLogger) blockLogging(caption, formatter, bordered, block)
    else koodies.logging.blockLogging(caption, formatter, bordered, block)
