package koodies.logging

import koodies.asString
import koodies.collections.synchronizedListOf
import koodies.terminal.AnsiCode.Companion.removeEscapeSequences
import koodies.terminal.AnsiFormats.bold
import koodies.text.ANSI.Formatter
import koodies.text.LineSeparators.withoutTrailingLineSeparator
import koodies.text.Semantics
import koodies.text.Semantics.formattedAs
import koodies.text.prefixLinesWith
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

public class MicroLogger(
    private val symbol: String,
    private val formatter: Formatter? = Formatter.PassThrough,
    parent: RenderingLogger? = null,
) : RenderingLogger("", parent) {

    private val messages: MutableList<CharSequence> = synchronizedListOf()
    private val lock = ReentrantLock()

    private var loggingResult: Boolean = false

    override fun render(trailingNewline: Boolean, block: () -> CharSequence): Unit = lock.withLock {
        when {
            closed -> {
                val prefix = caption.formattedAs.meta + " " + Semantics.Computation + " "
                log { block().toString().prefixLinesWith(prefix) }
            }
            loggingResult -> {
                val prefix = "(" + (symbol.trim().takeUnless { it.isEmpty() }?.let { "$it " } ?: "")
                val paddingAndMessages =
                    messages.joinToString(prefix = prefix, separator = " ˃ ", postfix = " ˃ ${block()})") { "$it".withoutTrailingLineSeparator }
                log { caption.bold() + paddingAndMessages }
            }
            else -> {
                messages.add(block())
            }
        }
    }

    override fun logText(block: () -> CharSequence) {
        block.format(formatter) { render(false) { this } }
    }

    override fun logLine(block: () -> CharSequence) {
        block.format(formatter) { render(false) { this } }
    }

    override fun logStatus(items: List<HasStatus>, block: () -> CharSequence) {
        val message: CharSequence? = block.format(formatter) { lines().joinToString(", ") }
        val status: CharSequence? = items.format(formatter) { lines().size.let { "($it)" } }
        (status?.let { "$message $status" } ?: message)?.let { render(true) { it } }
    }

    override fun <R> logResult(block: () -> Result<R>): R {
        val result = block()
        val formattedResult = formatResult(result)
        loggingResult = true
        render(true) { formattedResult }
        loggingResult = false
        open = false
        return result.getOrThrow()
    }

    override fun logException(block: () -> Throwable) {
        formatException(" ", block().toReturnValue()).also { render(true) { it } }
    }

    override fun toString(): String = asString {
        ::parent to parent?.caption
        ::caption to caption
        ::messages to messages.map { it.removeEscapeSequences() }
        ::loggingResult to loggingResult
        ::open to open
    }
}
