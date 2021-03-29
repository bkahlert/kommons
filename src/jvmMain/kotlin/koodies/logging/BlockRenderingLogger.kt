package koodies.logging

import koodies.asString
import koodies.builder.buildList
import koodies.concurrent.process.IO
import koodies.logging.BlockRenderingLogger.Companion.BORDERED_BY_DEFAULT
import koodies.regex.RegularExpressions
import koodies.terminal.AnsiFormats.bold
import koodies.terminal.AnsiString.Companion.asAnsiString
import koodies.text.ANSI.Colors.red
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
import kotlin.properties.Delegates

public open class BlockRenderingLogger(
    caption: CharSequence,
    parent: BorderedRenderingLogger? = null,
    contentFormatter: Formatter? = null,
    decorationFormatter: Formatter? = null,
    bordered: Boolean = BORDERED_BY_DEFAULT,
    width: Int? = null,
) : BorderedRenderingLogger(caption.toString(), parent, contentFormatter, decorationFormatter, bordered, width, prefixFor(bordered, decorationFormatter)) {

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

    override var initialized: Boolean by Delegates.observable(false) { _, oldValue, newValue ->
        if (!oldValue && newValue) {
            render(true) { blockStart }
        }
    }

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
        open = false
        return result.getOrThrow()
    }

    override fun logException(block: () -> Throwable): Unit = block().let {
        render(true) {
            val message = IO.ERR(it).formatted
            if (closed) message
            else message.prefixLinesWith(prefix, ignoreTrailingSeparator = false)
        }
        open = false
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
        ::prefix to prefix
        ::statusInformationColumn to statusInformationColumn
        ::statusInformationPadding to statusInformationPadding
        ::statusInformationColumns to statusInformationColumns
    }

    public companion object {
        public const val BORDERED_BY_DEFAULT: Boolean = true
        public fun prefixFor(bordered: Boolean?, decorationFormatter: Formatter?): String {
            return if (bordered ?: BORDERED_BY_DEFAULT) decorationFormatter("│").toString() + "   "
            else decorationFormatter("·").toString() + " "
        }
    }
}


/**
 * Creates a logger which serves for logging a sub-process and all of its corresponding events.
 *
 * This logger uses at least one line per log event. If less room is available [compactLogging] is more suitable.
 */
@Deprecated("only use member function")
@RenderingLoggingDsl
public fun <R> blockLogging(
    caption: CharSequence,
    contentFormatter: Formatter? = null,
    decorationFormatter: Formatter? = null,
    bordered: Boolean? = null,
    block: BorderedRenderingLogger.() -> R,
): R = BlockRenderingLogger(caption, null, contentFormatter, decorationFormatter, bordered ?: BORDERED_BY_DEFAULT).runLogging(block)

/**
 * Creates a logger which serves for logging a sub-process and all of its corresponding events.
 *
 * This logger uses at least one line per log event. If less room is available [compactLogging] is more suitable.
 */
@Deprecated("only use member function")
@RenderingLoggingDsl
public fun <T : RenderingLogger?, R> T.blockLogging(
    caption: CharSequence,
    contentFormatter: Formatter? = null,
    decorationFormatter: Formatter? = Formatter { it.red() },
    bordered: Boolean? = null,
    block: BorderedRenderingLogger.() -> R,
): R =
    if (this is BorderedRenderingLogger) blockLogging(caption, contentFormatter, decorationFormatter, bordered, block)
    else koodies.logging.blockLogging(caption, contentFormatter, decorationFormatter, bordered, block)
