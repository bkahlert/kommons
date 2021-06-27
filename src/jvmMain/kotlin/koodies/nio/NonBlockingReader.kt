package koodies.nio

import io.opentelemetry.api.trace.Span
import koodies.debug.asEmoji
import koodies.debug.debug
import koodies.exec.IO
import koodies.exec.mock.SlowInputStream
import koodies.runWrapping
import koodies.runtime.isDebugging
import koodies.text.ANSI
import koodies.text.INTERMEDIARY_LINE_PATTERN
import koodies.text.LineSeparators
import koodies.text.LineSeparators.CR
import koodies.text.LineSeparators.LF
import koodies.text.LineSeparators.hasTrailingLineSeparator
import koodies.text.LineSeparators.removeTrailingLineSeparator
import koodies.text.Semantics.formattedAs
import koodies.text.quoted
import koodies.time.Now
import koodies.time.seconds
import koodies.tracing.Tracer
import koodies.tracing.rendering.Renderer.Companion.NOOP
import koodies.tracing.rendering.spanningLine
import koodies.tracing.spanning
import koodies.unit.milli
import java.io.BufferedReader
import java.io.InputStream
import java.io.Reader
import java.lang.System.currentTimeMillis
import kotlin.time.Duration

/**
 * Non-blocking [Reader] with Unicode code support which is suitable to
 * process outputs interactively. That is, prompts that don't have a trailing
 * [LineSeparators] will also be read by [readLine], [readLines] and usual
 * helper functions.
 */
public class NonBlockingReader(
    private val inputStream: InputStream,
    private val timeout: Duration = 6.seconds,
    private val blockOnEmptyLine: Boolean = false,
) : BufferedReader(Reader.nullReader()) {

    init {
        check(timeout.isPositive()) { "Timeout must greater 0" }
    }

    private var reader: NonBlockingCharReader? = NonBlockingCharReader(inputStream, timeout = timeout / 3)
    private var lastReadLine: String? = null
    private var lastReadLineDueTimeout: Boolean? = null
    private var unfinishedLine: StringBuilder = StringBuilder()
    private var charArray: CharArray = CharArray(1)
    private val lastRead get() = unfinishedLine.substring((unfinishedLine.length - 1).coerceAtLeast(0), unfinishedLine.length)
    private val lastReadCR get() = lastRead == CR
    private val linePotentiallyComplete get() = unfinishedLine.matches(LineSeparators.INTERMEDIARY_LINE_PATTERN)
    private val justRead get() = String(charArray, 0, 1)
    private val justReadLF get() = justRead == LF
    private val justReadCRLF get() = lastReadCR && justReadLF
    private val lineComplete get() = linePotentiallyComplete && !justReadCRLF

    /**
     * Reads the next line from the [InputStream].
     *
     * Should more time pass than [timeout] the unfinished line is returned but also kept
     * for the next attempt. The unfinished line will be completed until a line separator
     * or EOF was encountered.
     */
    override fun readLine(): String? = if (reader == null) null else
        spanning(
            name = NonBlockingReader::class.simpleName + "." + ::readLine.name + "()",
            renderer = {
                if (isDebugging) it(copy(decorationFormatter = { ANSI.Colors.cyan.invoke(it) }))
                else NOOP
            },
            tracer = if (isDebugging) Tracer else Tracer.NOOP,
        ) {
            var latestReadMoment = calculateLatestReadMoment()
            log("Starting to read line for at most $timeout".formattedAs.meta)
            while (true) {

                val read: Int = runWrapping({
                    (inputStream as? SlowInputStream)?.run { parentSpan.also { parentSpan = Span.current() } }
                }, {
                    (inputStream as? SlowInputStream)?.parentSpan = it ?: error("No parent span to restore")
                }) { reader?.read(charArray, 0)!! }

                if (read == -1) {
                    log("InputStream Depleted. Closing. Unfinished Line: ${unfinishedLine.quoted}".formattedAs.meta)
                    close()
                    return@spanning if (unfinishedLine.isEmpty()) {
                        lastReadLineDueTimeout = false
                        lastReadLine = null
                        lastReadLine
                    } else {
                        lastReadLineDueTimeout = false
                        lastReadLine = "$unfinishedLine"
                        unfinishedLine.clear()
                        lastReadLine!!.removeTrailingLineSeparator
                    }
                }
                log("${Now.emoji} ${(latestReadMoment - currentTimeMillis()).milli.seconds}; 📋 ${unfinishedLine.debug}; 🆕 ${justRead.debug}".formattedAs.meta)
                if (read == 1) {

                    val lineAlreadyRead = lastReadLineDueTimeout == true && lastReadLine?.hasTrailingLineSeparator == true && !justReadCRLF

                    if (lineComplete) {
                        lastReadLineDueTimeout = false
                        lastReadLine = "$unfinishedLine"
                        unfinishedLine.clear()
                        unfinishedLine.append(charArray)
                        log(IO.Meta typed "Line Completed: ${lastReadLine.quoted}")
                        if (!lineAlreadyRead) {
                            return@spanning lastReadLine!!.removeTrailingLineSeparator
                        }
                    }
                    if (!lineAlreadyRead) {
                        unfinishedLine.append(charArray)
                        latestReadMoment = calculateLatestReadMoment()
                    }
                }

                if (currentTimeMillis() >= latestReadMoment && !(blockOnEmptyLine && unfinishedLine.isEmpty())) {
                    log("${Now.emoji} Timed out. Returning ${unfinishedLine.quoted}".formattedAs.meta)
                    // TODO evaluate if better to call a callback and continue working (without returning half-read lines)
                    lastReadLineDueTimeout = true
                    lastReadLine = "$unfinishedLine"
                    return@spanning lastReadLine!!.removeTrailingLineSeparator
                }
            }
            @Suppress("UNREACHABLE_CODE")
            error("return statement missing")
        }

    private fun calculateLatestReadMoment() = currentTimeMillis() + timeout.inWholeMilliseconds

    /**
     * Reads all lines from the [InputStream].
     *
     * Should more time pass than [timeout] the unfinished line is returned but also kept
     * for the next attempt. The unfinished line will be completed until a line separator
     * or EOF was encountered.
     */
    public fun forEachLine(block: (String) -> Unit): String = spanningLine(NonBlockingReader::class.simpleName + "." + ::forEachLine.name + "()") {
        var lineCount = 0
        while (true) {
            val readLine: String? = readLine()
            val line = readLine ?: break
            block(line)
            lineCount++
        }
        "$lineCount processed"
    }

    /**
     * Closes this reader without throwing any exception.
     */
    override fun close() {
        kotlin.runCatching { reader?.close() }
        reader = null
    }

    override fun toString(): String = listOf<Pair<String, Any?>>(
        "unfinishedLine" to unfinishedLine.debug,
        "complete?" to "${linePotentiallyComplete.debug}/${lineComplete.debug}",
        "lastRead" to "${lastRead.debug} (␍? ${(lastRead == CR).asEmoji})",
        "justRead" to "${justRead.debug} (␊? ${(justRead == LF).debug})",
        "␍␊?" to justReadCRLF.debug,
        "lastReadLine" to lastReadLine.debug,
        "lastReadLineDueTimeout?" to lastReadLineDueTimeout.debug,
        "timeout" to timeout,
        "reader" to "…",
    ).joinToString(prefix = "NonBlockingReader(",
        separator = "; ",
        postfix = ")") { "${it.first}: ${it.second}" }
}
