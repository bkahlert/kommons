package koodies.logging

import koodies.asString
import koodies.builder.buildList
import koodies.concurrent.process.IO
import koodies.regex.RegularExpressions
import koodies.terminal.AnsiFormats.bold
import koodies.terminal.AnsiString.Companion.asAnsiString
import koodies.text.ANSI.Formatter
import koodies.text.ANSI.Formatter.Companion.invoke
import koodies.text.LineSeparators.LF
import koodies.text.LineSeparators.lines
import koodies.text.TruncationStrategy.MIDDLE
import koodies.text.addColumn
import koodies.text.mapLines
import koodies.text.prefixLinesWith
import koodies.text.takeUnlessBlank
import koodies.text.truncate
import koodies.text.wrapLines

public open class BlockRenderingLogger(
    caption: CharSequence,
    parent: RenderingLogger? = null,
    override val contentFormatter: Formatter? = Formatter.PassThrough,
    override val decorationFormatter: Formatter? = Formatter.PassThrough,
    override val bordered: Boolean = false,
    override val statusInformationColumn: Int = 100,
    override val statusInformationPadding: Int = 5,
    override val statusInformationColumns: Int = 45,
    log: (String) -> Unit = { output: String -> print(output) },
) : BorderedRenderingLogger(caption.toString(), parent, log) {

    public val totalColumns: Int
        get() {
            check(statusInformationColumn > 0)
            check(statusInformationPadding > 0)
            check(statusInformationColumns > 0)
            return statusInformationColumn + statusInformationPadding + statusInformationColumns
        }

    private val playSymbol: String get() = decorationFormatter("▶").toString()
    private val whitePlaySymbol: String get() = decorationFormatter("▷").toString()

    private val blockStart: String
        get() = buildList {
            val captionLines = caption.asAnsiString().lines()
            if (bordered) {
                +(decorationFormatter("╭──╴").toString() + decorationFormatter(captionLines.first()).bold())
                captionLines.drop(1).forEach {
                    +"$prefix${decorationFormatter(it).bold()}"
                }
                +prefix
            } else {
                +"$playSymbol ${decorationFormatter(captionLines.first()).bold()}"
                captionLines.drop(1).forEach {
                    +"$whitePlaySymbol ${decorationFormatter(it).bold()}"
                }
            }
        }.joinToString(LF)

    override val prefix: String
        get() = if (bordered) decorationFormatter("│").toString() + "   "
        else decorationFormatter("·").toString() + " "

    protected fun getBlockEnd(returnValue: ReturnValue): CharSequence {
        val message: String =
            when (returnValue.successful) {
                true -> {
                    val renderedSuccess = formatReturnValue(returnValue)
                    if (bordered) decorationFormatter("│").toString() + LF + decorationFormatter("╰──╴").toString() + renderedSuccess
                    else "$renderedSuccess"
                }
                null -> {
                    val renderedUnready = formatUnreadyReturnValue(returnValue)
                    val halfLine = decorationFormatter("╵").toString()
                    if (bordered) halfLine + LF + halfLine + LF + renderedUnready
                    else "$renderedUnready"
                }
                false -> {
                    if (bordered) {
                        formatException(LF + decorationFormatter("╰──╴").toString(), returnValue) + LF
                    } else {
                        formatException(" ", returnValue)
                    }
                }
            }
        return message.asAnsiString().mapLines { it.bold() }
    }

    init {
        render(true) { blockStart }
    }

    override fun logText(block: () -> CharSequence) {
        contentFormatter(block()).run {
            render(false) {
                if (closed) this
                else asAnsiString().prefixLinesWith(prefix = prefix, ignoreTrailingSeparator = true)
            }
        }
    }

    override fun logLine(block: () -> CharSequence) {
        contentFormatter(block()).run {
            render(true) {
                val wrapped = wrapNonUriLines(totalColumns)
                if (closed) wrapped
                else wrapped.prefixLinesWith(prefix = prefix, ignoreTrailingSeparator = false)
            }
        }
    }

    override fun logStatus(items: List<HasStatus>, block: () -> CharSequence): Unit {
        block().takeUnlessBlank()?.let { contentFormatter(it) }?.run {
            render(true) {
                val leftColumn = wrapNonUriLines(statusInformationColumn).asAnsiString()
                val statusColumn =
                    items.renderStatus().asAnsiString().truncate(maxLength = statusInformationColumns - 1, MIDDLE)
                val twoColumns = leftColumn.addColumn(statusColumn, columnWidth = statusInformationColumn + statusInformationPadding)
                if (closed) twoColumns
                else twoColumns.prefixLinesWith(prefix = prefix)
            }
        }
    }

    override fun <R> logResult(block: () -> Result<R>): R {
        val result = block()
        render(true) {
            if (closed) formatReturnValue(result.toReturnValue()).asAnsiString().wrapNonUriLines(totalColumns)
            else getBlockEnd(result.toReturnValue()).wrapNonUriLines(totalColumns)
        }
        closed = true
        return result.getOrThrow()
    }

    override fun logException(block: () -> Throwable): Unit = block().let {
        render(true) {
            val message = IO.ERR(it).formatted
            if (closed) message
            else message.prefixLinesWith(prefix, ignoreTrailingSeparator = false)
        }
        closed = true
    }

    private fun CharSequence.wrapNonUriLines(length: Int): CharSequence {
        return if (RegularExpressions.uriRegex.containsMatchIn(this)) this else asAnsiString().wrapLines(length)
    }

    override fun toString(): String = asString {
        ::parent to parent?.caption
        ::caption to caption
        ::contentFormatter to contentFormatter
        ::decorationFormatter to decorationFormatter
        ::bordered to bordered
        ::statusInformationColumn to statusInformationColumn
        ::statusInformationPadding to statusInformationPadding
        ::statusInformationColumns to statusInformationColumns
    }
}


/**
 * Creates a logger which serves for logging a sub-process and all of its corresponding events.
 *
 * This logger uses at least one line per log event. If less room is available [compactLogging] is more suitable.
 */
@RenderingLoggingDsl
public fun <T : MutedRenderingLogger, R> T.blockLogging(
    caption: CharSequence,
    contentFormatter: Formatter? = Formatter.PassThrough,
    decorationFormatter: Formatter? = Formatter.PassThrough,
    bordered: Boolean = (this as? BorderedRenderingLogger)?.bordered ?: false,
    block: T.() -> R,
): R = runLogging(block)

/**
 * Creates a logger which serves for logging a sub-process and all of its corresponding events.
 *
 * This logger uses at least one line per log event. If less room is available [compactLogging] is more suitable.
 */
@RenderingLoggingDsl
public fun <T : BorderedRenderingLogger, R> T.blockLogging(
    caption: CharSequence,
    contentFormatter: Formatter? = Formatter.PassThrough,
    decorationFormatter: Formatter? = Formatter.PassThrough,
    bordered: Boolean = (this as? BorderedRenderingLogger)?.bordered ?: false,
    block: BlockRenderingLogger.() -> R,
): R = BlockRenderingLogger(
    caption, this, contentFormatter, decorationFormatter, bordered,
    statusInformationColumn = statusInformationColumn - prefix.length,
    statusInformationPadding = statusInformationPadding,
    statusInformationColumns = statusInformationColumns - prefix.length,
) { output -> logText { output } }.runLogging(block)

/**
 * Creates a logger which serves for logging a sub-process and all of its corresponding events.
 *
 * This logger uses at least one line per log event. If less room is available [compactLogging] is more suitable.
 */
@RenderingLoggingDsl
public fun <T : RenderingLogger, R> T.blockLogging(
    caption: CharSequence,
    contentFormatter: Formatter? = Formatter.PassThrough,
    decorationFormatter: Formatter? = Formatter.PassThrough,
    bordered: Boolean = (this as? BorderedRenderingLogger)?.bordered ?: false,
    block: BlockRenderingLogger.() -> R,
): R = BlockRenderingLogger(caption, this, contentFormatter, decorationFormatter, bordered) { output -> logText { output } }.runLogging(block)

/**
 * Creates a logger which serves for logging a sub-process and all of its corresponding events.
 *
 * This logger uses at least one line per log event. If less room is available [compactLogging] is more suitable.
 */
@RenderingLoggingDsl
public fun <R> blockLogging(
    caption: CharSequence,
    contentFormatter: Formatter? = Formatter.PassThrough,
    decorationFormatter: Formatter? = Formatter.PassThrough,
    bordered: Boolean = false,
    block: BlockRenderingLogger.() -> R,
): R = BlockRenderingLogger(caption, null, contentFormatter, decorationFormatter, bordered).runLogging(block)

/**
 * Creates a logger which serves for logging a sub-process and all of its corresponding events.
 *
 * This logger uses at least one line per log event. If less room is available [compactLogging] is more suitable.
 */
@JvmName("nullableBlockLogging")
@RenderingLoggingDsl
public fun <T : RenderingLogger?, R> T.blockLogging(
    caption: CharSequence,
    contentFormatter: Formatter? = Formatter.PassThrough,
    decorationFormatter: Formatter? = Formatter.PassThrough,
    bordered: Boolean = false,
    block: BlockRenderingLogger.() -> R,
): R =
    if (this is RenderingLogger) blockLogging(caption, contentFormatter, decorationFormatter, bordered, block)
    else koodies.logging.blockLogging(caption, contentFormatter, decorationFormatter, bordered, block)
