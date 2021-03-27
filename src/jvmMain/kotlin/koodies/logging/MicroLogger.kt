package koodies.logging

import koodies.asString
import koodies.collections.synchronizedListOf
import koodies.terminal.AnsiCode.Companion.removeEscapeSequences
import koodies.terminal.AnsiFormats.bold
import koodies.text.ANSI.Formatter
import koodies.text.GraphemeCluster
import koodies.text.Semantics
import koodies.text.Semantics.formattedAs
import koodies.text.prefixLinesWith
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

public class MicroLogger(
    private val symbol: GraphemeCluster? = null,
    private val formatter: Formatter? = Formatter.PassThrough,
    parent: RenderingLogger? = null,
    log: (String) -> Unit = { output: String -> print(output) },
) : RenderingLogger(symbol?.toString() ?: "", parent, log) {

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
                val paddingAndMessages =
                    messages.joinToString(prefix = "(" + (symbol?.let { "$it " } ?: ""), separator = " ˃ ", postfix = " ˃ ${block()})")
                log { caption.bold() + paddingAndMessages }
            }
            else -> {
                messages.add(block())
            }
        }
    }

    override fun logText(block: () -> CharSequence) {
        block.format(formatter) { super.logText { this } }
    }

    override fun logLine(block: () -> CharSequence) {
        block.format(formatter) { super.logLine { this } }
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
        closed = true
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
        ::closed to closed
    }
}
