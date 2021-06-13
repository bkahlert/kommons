package koodies.logging

import koodies.asString
import koodies.collections.synchronizedListOf
import koodies.exec.IO
import koodies.text.ANSI.Formatter
import koodies.text.ANSI.Text.Companion.ansi
import koodies.text.ANSI.ansiRemoved
import koodies.text.LineSeparators.LF
import koodies.text.LineSeparators.removeTrailingLineSeparator
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

public class MicroLogger(
    private val symbol: String,
    parent: SimpleRenderingLogger?,
    contentFormatter: Formatter? = null,
    decorationFormatter: Formatter? = null,
    returnValueFormatter: ((ReturnValue) -> ReturnValue)? = null,
    log: ((String) -> Unit)? = null,
) : SimpleRenderingLogger("", parent, log) {

    private val contentFormatter: Formatter = contentFormatter ?: Formatter.PassThrough
    private val decorationFormatter: Formatter = decorationFormatter ?: Formatter.PassThrough
    private val returnValueFormatter: (ReturnValue) -> ReturnValue = returnValueFormatter ?: { it }

    private val joinElement = decorationFormatter(" ˃ ")

    private val messages: MutableList<CharSequence> = synchronizedListOf()
    private val lock = ReentrantLock()

    private var loggingResult: Boolean = false

    override fun render(block: () -> CharSequence): Unit = lock.withLock {
        when {
            loggingResult -> {
                val prefix = "(" + (symbol.trim().takeUnless { it.isEmpty() }?.let { "$it " } ?: "")
                val paddingAndMessages =
                    messages.joinToString(prefix = prefix, separator = joinElement, postfix = "$joinElement${block()})") { "$it".removeTrailingLineSeparator }
                log { name.ansi.bold.done + paddingAndMessages }
            }
            else -> {
                messages.add(block())
            }
        }
    }

    override fun logText(block: () -> CharSequence) {
        block.format(contentFormatter) { render { this } }
    }

    override fun logLine(block: () -> CharSequence) {
        block.format(contentFormatter) { render { this } }
    }

    public fun logStatus(items: List<CharSequence>, block: () -> CharSequence) {
        val message: CharSequence? = block.format(contentFormatter) { lines().joinToString(", ") }
        val status: CharSequence? = items.format(contentFormatter) { lines().size.let { "($it)" } }
        (status?.let { "$message $status" } ?: message)?.let { render { "$it$LF" } }
    }

    public fun logStatus(vararg statuses: CharSequence, block: () -> CharSequence = { IO.Output typed "" }): Unit =
        logStatus(statuses.toList(), block)

    override fun <R> logResult(result: Result<R>): R {
        val formattedResult: String = returnValueFormatter(ReturnValue.of(result)).format()
        loggingResult = true
        render { formattedResult }
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
}
