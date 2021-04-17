package koodies.logging

import koodies.asString
import koodies.collections.synchronizedListOf
import koodies.concurrent.process.IO
import koodies.text.ANSI.Formatter
import koodies.text.ANSI.Text.Companion.ansi
import koodies.text.ANSI.ansiRemoved
import koodies.text.LineSeparators.withoutTrailingLineSeparator
import koodies.text.Semantics.Symbols
import koodies.text.prefixLinesWith
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

public class MicroLogger(
    private val symbol: String,
    contentFormatter: Formatter? = null,
    decorationFormatter: Formatter? = null,
    returnValueFormatter: ((ReturnValue) -> ReturnValue)? = null,
    log: ((String) -> Unit)? = null,
) : RenderingLogger("", log) {

    private val contentFormatter: Formatter = contentFormatter ?: Formatter.PassThrough
    private val decorationFormatter: Formatter = decorationFormatter ?: Formatter.PassThrough
    private val returnValueFormatter: (ReturnValue) -> ReturnValue = returnValueFormatter ?: { it }

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
                val prefix = "(" + (symbol.trim().takeUnless { it.isEmpty() }?.let { "$it " } ?: "")
                val paddingAndMessages =
                    messages.joinToString(prefix = prefix, separator = " ˃ ", postfix = " ˃ ${block()})") { "$it".withoutTrailingLineSeparator }
                logWithLock { caption.ansi.bold.done + paddingAndMessages }
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
        val formattedResult: String = returnValueFormatter(ReturnValue.of(result)).format()
        loggingResult = true
        render(true) { formattedResult }
        loggingResult = false
        open = false
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
}
