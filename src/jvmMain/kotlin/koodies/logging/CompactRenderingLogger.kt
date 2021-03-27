package koodies.logging

import koodies.asString
import koodies.collections.synchronizedListOf
import koodies.terminal.AnsiCode.Companion.removeEscapeSequences
import koodies.terminal.AnsiFormats.bold
import koodies.text.Semantics
import koodies.text.Semantics.formattedAs
import koodies.text.prefixLinesWith
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

public class CompactRenderingLogger(
    caption: CharSequence,
    private val formatter: Formatter = { it },
    parent: RenderingLogger? = null,
    log: (String) -> Unit = { output: String -> print(output) },
) : RenderingLogger(caption.toString(), parent, log) {

    init {
        require(caption.isNotBlank()) { "No blank caption allowed." }
    }

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
                val paddingAndMessages = messages.joinToString(" ") { "$it" }.let { if (it.isNotBlank()) " $it" else "" }
                log { caption.bold() + paddingAndMessages + " " + block() }
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
            val status: String? = if (items.isNotEmpty()) formatter(items.renderStatus())?.lines()?.joinToString(", ", "(", ")") else null
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

/**
 * Creates a logger which serves for logging a very short sub-process and all of its corresponding events.
 *
 * This logger logs all events using only a couple of characters. If more room is needed [compactLogging] or even [blockLogging] is more suitable.
 */
@RenderingLoggingDsl
public fun <@Suppress("FINAL_UPPER_BOUND") T : CompactRenderingLogger, R> T.compactLogging(
    formatter: Formatter = { it },
    block: MicroLogger.() -> R,
): R = MicroLogger(formatter = formatter, parent = this) {
    this@compactLogging.logLine { it }
}.runLogging(block)

/**
 * Creates a logger which serves for logging a sub-process and all of its corresponding events.
 *
 * This logger logs all events using a single line of text. If more room is needed [blockLogging] is more suitable.
 */
@RenderingLoggingDsl
public fun <T : RenderingLogger, R> T.compactLogging(
    caption: CharSequence,
    formatter: Formatter = { it },
    block: CompactRenderingLogger.() -> R,
): R = CompactRenderingLogger(caption, formatter, this) {
    this@compactLogging.logLine { it }
}.runLogging(block)

/**
 * Creates a logger which serves for logging a sub-process and all of its corresponding events.
 *
 * This logger logs all events using a single line of text. If more room is needed [blockLogging] is more suitable.
 */
@RenderingLoggingDsl
public fun <R> compactLogging(
    caption: CharSequence,
    formatter: Formatter = { it },
    block: CompactRenderingLogger.() -> R,
): R = CompactRenderingLogger(caption, formatter) {
    println(it)
}.runLogging(block)

/**
 * Creates a logger which serves for logging a sub-process and all of its corresponding events.
 *
 * This logger logs all events using a single line of text. If more room is needed [blockLogging] is more suitable.
 */
@JvmName("nullableCompactLogging")
@RenderingLoggingDsl
public fun <T : RenderingLogger?, R> T.compactLogging(
    caption: CharSequence,
    formatter: Formatter = { it },
    block: CompactRenderingLogger.() -> R,
): R =
    if (this is RenderingLogger) compactLogging(caption, formatter, block)
    else koodies.logging.compactLogging(caption, formatter, block)
