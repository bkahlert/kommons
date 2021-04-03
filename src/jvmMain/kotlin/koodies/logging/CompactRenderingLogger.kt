package koodies.logging

import koodies.asString
import koodies.collections.synchronizedListOf
import koodies.terminal.AnsiCode.Companion.removeEscapeSequences
import koodies.terminal.AnsiFormats.bold
import koodies.text.ANSI.Formatter
import koodies.text.LineSeparators.withTrailingLineSeparator
import koodies.text.LineSeparators.withoutTrailingLineSeparator
import koodies.text.Semantics
import koodies.text.prefixLinesWith
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

public class CompactRenderingLogger(
    caption: CharSequence,
    contentFormatter: Formatter? = null,
    decorationFormatter: Formatter? = null,
    returnValueFormatter: ((ReturnValue) -> String)? = null,
    parent: RenderingLogger? = null,
) : RenderingLogger(caption.toString(), parent) {

    private val contentFormatter: Formatter = contentFormatter ?: Formatter.PassThrough
    private val decorationFormatter: Formatter = decorationFormatter ?: Formatter.PassThrough
    private val returnValueFormatter: (ReturnValue) -> String = returnValueFormatter ?: RETURN_VALUE_FORMATTER

    init {
        require(caption.isNotBlank()) { "No blank caption allowed." }
    }

    private val messages: MutableList<CharSequence> = synchronizedListOf()
    private val lock = ReentrantLock()

    private var loggingResult: Boolean = false

    override fun render(trailingNewline: Boolean, block: () -> CharSequence): Unit = lock.withLock {
        when {
            closed -> {
                val prefix = decorationFormatter(Semantics.Computation).toString() + " "
                log { block().toString().prefixLinesWith(prefix) }
            }
            loggingResult -> {
                val paddingAndMessages =
                    messages.joinToString(" ") { "$it".withoutTrailingLineSeparator }.let { if (it.isNotBlank()) " $it" else "" }
                log { caption.bold() + paddingAndMessages + " " + block().toString().withTrailingLineSeparator() }
            }
            else -> {
                messages.add(block())
            }
        }
    }

    override fun logText(block: () -> CharSequence) {
        block.format(contentFormatter) { render(false) { this } }
    }

    override fun logLine(block: () -> CharSequence) {
        block.format(contentFormatter) { render(false) { this } }
    }

    override fun logStatus(items: List<HasStatus>, block: () -> CharSequence) {
        val message: CharSequence? = block.format(contentFormatter) { lines().joinToString(", ") }
        val status: CharSequence? = items.format(contentFormatter) { lines().joinToString(", ", "(", ")") }
        (status?.let { "$message $status" } ?: message)?.let { render(false) { it } }
    }

    override fun <R> logResult(block: () -> Result<R>): R {
        val result = block()
        val formattedResult = returnValueFormatter(ReturnValue.of(result))
        loggingResult = true
        render(true) { formattedResult }
        loggingResult = false
        open = false
        return result.getOrThrow()
    }

    override fun logException(block: () -> Throwable) {
        returnValueFormatter(ReturnValue.of(block())).also { render(true) { it } }
    }

    override fun toString(): String = asString {
        ::open to open
        ::parent to parent?.caption
        ::caption to caption
        ::messages to messages.map { it.removeEscapeSequences() }
        ::loggingResult to loggingResult
        ::open to open
    }

    /**
     * Creates a logger which serves for logging a very short sub-process and all of its corresponding events.
     *
     * This logger logs all events using only a couple of characters. If more room is needed [compactLogging] or even [blockLogging] is more suitable.
     */
    @RenderingLoggingDsl
    public fun <R> compactLogging(
        caption: CharSequence? = null,
        contentFormatter: Formatter? = this.contentFormatter,
        decorationFormatter: Formatter? = this.decorationFormatter,
        returnValueFormatter: ((ReturnValue) -> String)? = this.returnValueFormatter,
        block: MicroLogger.() -> R,
    ): R = MicroLogger(caption?.toString() ?: "", contentFormatter, decorationFormatter, returnValueFormatter, this).runLogging(block)
}


/**
 * Creates a logger which serves for logging a sub-process and all of its corresponding events.
 *
 * This logger logs all events using a single line of text. If more room is needed [blockLogging] is more suitable.
 */
@Deprecated("only use member function")
@RenderingLoggingDsl
public fun <R> compactLogging(
    caption: CharSequence,
    contentFormatter: Formatter? = null,
    decorationFormatter: Formatter? = null,
    returnValueFormatter: ((ReturnValue) -> String)? = null,
    block: CompactRenderingLogger.() -> R,
): R = CompactRenderingLogger(caption, contentFormatter, decorationFormatter, returnValueFormatter, null).runLogging(block)

/**
 * Creates a logger which serves for logging a sub-process and all of its corresponding events.
 *
 * This logger logs all events using a single line of text. If more room is needed [blockLogging] is more suitable.
 */
@Deprecated("only use member function")
@RenderingLoggingDsl
public fun <T : RenderingLogger?, R> T.compactLogging(
    caption: CharSequence,
    contentFormatter: Formatter? = null,
    decorationFormatter: Formatter? = null,
    returnValueFormatter: ((ReturnValue) -> String)? = null,
    block: CompactRenderingLogger.() -> R,
): R =
    if (this is CompactRenderingLogger) compactLogging(caption, contentFormatter, decorationFormatter, returnValueFormatter, block)
    else koodies.logging.compactLogging(caption, contentFormatter, decorationFormatter, returnValueFormatter, block)
