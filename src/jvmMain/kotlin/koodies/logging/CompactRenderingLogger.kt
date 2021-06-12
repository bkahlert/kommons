package koodies.logging

import koodies.asString
import koodies.collections.synchronizedListOf
import koodies.exec.IO
import koodies.text.ANSI.Formatter
import koodies.text.ANSI.Text.Companion.ansi
import koodies.text.ANSI.ansiRemoved
import koodies.text.LineSeparators.prefixLinesWith
import koodies.text.LineSeparators.withTrailingLineSeparator
import koodies.text.LineSeparators.withoutTrailingLineSeparator
import koodies.text.Semantics.Symbols
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

public open class CompactRenderingLogger(
    caption: CharSequence,
    parent: RenderingLogger?,
    contentFormatter: Formatter? = null,
    decorationFormatter: Formatter? = null,
    returnValueFormatter: ((ReturnValue) -> ReturnValue)? = null,
    log: ((String) -> Unit)? = null,
) : RenderingLogger(caption.toString(), parent, log) {

    private val contentFormatter: Formatter = contentFormatter ?: Formatter.PassThrough
    private val decorationFormatter: Formatter = decorationFormatter ?: Formatter.PassThrough
    private val returnValueFormatter: (ReturnValue) -> ReturnValue = returnValueFormatter ?: { it }

    init {
        require(caption.isNotBlank()) { "No blank caption allowed." }
    }

    private val messages: MutableList<CharSequence> = synchronizedListOf()
    private val lock = ReentrantLock()

    private var loggingResult: Boolean = false

    override fun render(trailingNewline: Boolean, block: () -> CharSequence): Unit = lock.withLock {
        when {
            closed -> {
                val prefix = decorationFormatter(Symbols.Computation).toString() + " "
                logWithLock { block().toString().prefixLinesWith(prefix) }
            }
            loggingResult -> {
                val paddingAndMessages =
                    messages.joinToString(" ") { "$it".withoutTrailingLineSeparator }.let { if (it.isNotBlank()) " $it" else "" }
                logWithLock { caption.ansi.bold.done + paddingAndMessages + " " + block().toString().withTrailingLineSeparator() }
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

    public fun logStatus(items: List<CharSequence>, block: () -> CharSequence) {
        val message: CharSequence? = block.format(contentFormatter) { lines().joinToString(", ") }
        val status: CharSequence? = items.format(contentFormatter) { lines().joinToString(", ", "(", ")") }
        (status?.let { "$message $status" } ?: message)?.let { render(false) { it } }
    }

    public fun logStatus(vararg statuses: CharSequence, block: () -> CharSequence = { IO.Output typed "" }): Unit =
        logStatus(statuses.toList(), block)

    override fun <R> logResult(block: () -> Result<R>): R {
        val result = block()
        val formattedResult = returnValueFormatter(ReturnValue.of(result)).format()
        loggingResult = true
        render(true) { formattedResult }
        loggingResult = false
        close(result)
        return result.getOrThrow()
    }

    override fun logException(block: () -> Throwable) {
        render(true) { ReturnValue.of(block()).format() }
    }

    override fun toString(): String = asString {
        ::open to open
        ::caption to caption
        ::messages to messages.map { it.ansiRemoved }
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
        returnValueFormatter: ((ReturnValue) -> ReturnValue)? = this.returnValueFormatter,
        block: MicroLogger.() -> R,
    ): R = MicroLogger(caption?.toString() ?: "", this, contentFormatter, decorationFormatter, returnValueFormatter) { logText { it } }.runLogging(block)
}
