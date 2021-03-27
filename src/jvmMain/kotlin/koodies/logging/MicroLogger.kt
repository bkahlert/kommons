package koodies.logging

import koodies.asString
import koodies.collections.synchronizedListOf
import koodies.terminal.AnsiCode.Companion.removeEscapeSequences
import koodies.terminal.AnsiFormats.bold
import koodies.text.GraphemeCluster
import koodies.text.Semantics
import koodies.text.Semantics.formattedAs
import koodies.text.prefixLinesWith
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

public class MicroLogger(
    private val symbol: GraphemeCluster? = null,
    private val formatter: Formatter = { it },
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
                    messages.mapNotNull(formatter).joinToString(prefix = "(" + (symbol?.let { "$it " } ?: ""), separator = " ˃ ", postfix = " ˃ ${block()})")
                log { caption.bold() + paddingAndMessages }
            }
            else -> {
                messages.add(block())
            }
        }
    }

    override fun logText(block: () -> CharSequence) {
        formatter(block())?.let { super.logText { it } }
    }

    override fun logLine(block: () -> CharSequence) {
        formatter(block())?.let { super.logLine { it } }
    }

    override fun logStatus(items: List<HasStatus>, block: () -> CharSequence) {
        formatter(block())?.lines()?.joinToString(", ")?.let { message ->
            val status: String? = if (items.isNotEmpty()) formatter(items.renderStatus())?.lines()?.size.let { "(${it ?: 0})" } else null
            status?.let { "$message $status" } ?: message
        }?.also { render(true) { it } }
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
