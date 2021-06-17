package koodies.logging

import koodies.asString
import koodies.collections.synchronizedListOf
import koodies.exec.IO
import koodies.text.ANSI.FilteringFormatter
import koodies.text.ANSI.Formatter
import koodies.text.ANSI.Text.Companion.ansi
import koodies.text.ANSI.ansiRemoved
import koodies.text.LineSeparators.LF
import koodies.text.LineSeparators.removeTrailingLineSeparator
import koodies.text.LineSeparators.withTrailingLineSeparator

public open class CompactRenderingLogger(
    name: CharSequence,
    parent: SimpleRenderingLogger?,
    contentFormatter: FilteringFormatter? = null,
    decorationFormatter: Formatter? = null,
    returnValueFormatter: ((ReturnValue) -> ReturnValue)? = null,
    log: ((String) -> Unit)? = null,
) : SimpleRenderingLogger(name.toString(), parent, log) {

    private val contentFormatter: FilteringFormatter = contentFormatter ?: FilteringFormatter.PassThrough
    private val decorationFormatter: Formatter = decorationFormatter ?: Formatter.PassThrough
    private val returnValueFormatter: (ReturnValue) -> ReturnValue = returnValueFormatter ?: { it }

    private val joinElement = decorationFormatter(" ")

    init {
        require(name.isNotBlank()) { "No blank name allowed." }
    }

    private val messages: MutableList<CharSequence> = synchronizedListOf()
    private var loggingResult: Boolean = false

    override fun render(block: () -> CharSequence) {
        when {
            loggingResult -> {
                val paddingAndMessages =
                    messages.joinToString(joinElement) { "$it".removeTrailingLineSeparator }.let { if (it.isNotBlank()) "$joinElement$it" else "" }
                log { name.ansi.bold.done + paddingAndMessages + joinElement + block().toString().withTrailingLineSeparator() }
            }
            else -> {
                messages.add(block())
            }
        }
    }

    override fun logText(block: () -> CharSequence) {
        block.format(contentFormatter) { render { this } }
    }

    override fun logLine(block: () -> CharSequence): Unit = logText(block)

    public fun logStatus(items: List<CharSequence>, block: () -> CharSequence) {
        val message: CharSequence? = block.format(contentFormatter) { lines().joinToString(", ") }
        val status: CharSequence? = items.format(contentFormatter) { lines().joinToString(", ", "(", ")") }
        (status?.let { "$message $status" } ?: message)?.let { render { it } }
    }

    public fun logStatus(vararg statuses: CharSequence, block: () -> CharSequence = { IO.Output typed "" }): Unit =
        logStatus(statuses.toList(), block)

    override fun <R> logResult(result: Result<R>): R {
        val formattedResult = returnValueFormatter(ReturnValue.of(result)).format()
        loggingResult = true
        render { formattedResult + LF }
        loggingResult = false
        close(result)
        return result.getOrThrow()
    }

    override fun toString(): String = asString {
        ::open to open
        ::name to name
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
        name: CharSequence? = null,
        contentFormatter: FilteringFormatter? = this.contentFormatter,
        decorationFormatter: Formatter? = this.decorationFormatter,
        returnValueFormatter: ((ReturnValue) -> ReturnValue)? = this.returnValueFormatter,
        block: MicroLogger.() -> R,
    ): R = MicroLogger(name?.toString() ?: "", this, contentFormatter, decorationFormatter, returnValueFormatter) { logText { it } }.runLogging(block)
}
