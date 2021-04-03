package koodies.logging

import koodies.asString
import koodies.collections.synchronizedListOf
import koodies.concurrent.process.IO
import koodies.terminal.AnsiCode.Companion.removeEscapeSequences
import koodies.terminal.AnsiFormats.bold
import koodies.text.ANSI.Formatter
import koodies.text.LineSeparators.withoutTrailingLineSeparator
import koodies.text.Semantics
import koodies.text.prefixLinesWith
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

public class MicroLogger(
    private val symbol: String,
    contentFormatter: Formatter? = null,
    decorationFormatter: Formatter? = null,
    returnValueFormatter: ((ReturnValue) -> String)? = null,
    log: ((String) -> Unit)? = null,
) : RenderingLogger("", log) {

    private val contentFormatter: Formatter = contentFormatter ?: Formatter.PassThrough
    private val decorationFormatter: Formatter = decorationFormatter ?: Formatter.PassThrough
    private val returnValueFormatter: (ReturnValue) -> String = returnValueFormatter ?: RETURN_VALUE_FORMATTER

    private val messages: MutableList<CharSequence> = synchronizedListOf()
    private val lock = ReentrantLock()

    private var loggingResult: Boolean = false

    override fun render(trailingNewline: Boolean, block: () -> CharSequence): Unit = lock.withLock {
        when {
            closed -> {
                val prefix = decorationFormatter(Semantics.Computation).toString() + " "
                logWithLock { block().toString().prefixLinesWith(prefix) }
            }
            loggingResult -> {
                val prefix = "(" + (symbol.trim().takeUnless { it.isEmpty() }?.let { "$it " } ?: "")
                val paddingAndMessages =
                    messages.joinToString(prefix = prefix, separator = " ˃ ", postfix = " ˃ ${block()})") { "$it".withoutTrailingLineSeparator }
                logWithLock { caption.bold() + paddingAndMessages }
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
        val status: CharSequence? = items.format(contentFormatter) { lines().size.let { "($it)" } }
        (status?.let { "$message $status" } ?: message)?.let { render(true) { it } }
    }

    public fun logStatus(vararg statuses: CharSequence, block: () -> CharSequence = { IO.OUT typed "" }): Unit =
        logStatus(statuses.toList(), block)

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
        ::caption to caption
        ::messages to messages.map { it.removeEscapeSequences() }
        ::loggingResult to loggingResult
        ::open to open
    }
}
