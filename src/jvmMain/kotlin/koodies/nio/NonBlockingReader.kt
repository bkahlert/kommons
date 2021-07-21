package koodies.nio

import io.opentelemetry.api.trace.Span
import koodies.debug.asEmoji
import koodies.debug.debug
import koodies.exec.mock.SlowInputStream
import koodies.runWrapping
import koodies.text.INTERMEDIARY_LINE_PATTERN
import koodies.text.LineSeparators
import koodies.text.LineSeparators.CR
import koodies.text.LineSeparators.LF
import koodies.text.LineSeparators.hasTrailingLineSeparator
import koodies.text.LineSeparators.trailingLineSeparatorRemoved
import koodies.text.Unicode
import koodies.time.seconds
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
    override fun readLine(): String? {
        if (reader == null) return null

        var latestReadMoment = calculateLatestReadMoment()
        while (true) {

            val read: Int = runWrapping({
                (inputStream as? SlowInputStream)?.run { parentSpan.also { parentSpan = Span.current() } }
            }, {
                (inputStream as? SlowInputStream)?.parentSpan = it ?: error("No parent span to restore")
            }) { reader?.read(charArray, 0)!! }

            if (read == -1) {
                close()
                return if (unfinishedLine.isEmpty()) {
                    lastReadLineDueTimeout = false
                    lastReadLine = null
                    lastReadLine
                } else {
                    lastReadLineDueTimeout = false
                    lastReadLine = "$unfinishedLine"
                    unfinishedLine.clear()
                    lastReadLine!!.trailingLineSeparatorRemoved
                }
            }

            if (read == 1) {

                val lineAlreadyRead = lastReadLineDueTimeout == true && lastReadLine?.hasTrailingLineSeparator == true && !justReadCRLF

                if (lineComplete) {
                    lastReadLineDueTimeout = false
                    lastReadLine = "$unfinishedLine"
                    unfinishedLine.clear()
                    unfinishedLine.append(charArray)
                    if (!lineAlreadyRead) {
                        return lastReadLine!!.trailingLineSeparatorRemoved
                    }
                }
                if (!lineAlreadyRead) {
                    unfinishedLine.append(charArray)
                    latestReadMoment = calculateLatestReadMoment()
                }
            }

            if (currentTimeMillis() >= latestReadMoment && !(blockOnEmptyLine && unfinishedLine.isEmpty())) {
                // TODO evaluate if better to call a callback and continue working (without returning half-read lines)
                lastReadLineDueTimeout = true
                lastReadLine = "$unfinishedLine"
                return lastReadLine!!.trailingLineSeparatorRemoved
            }
        }
        @Suppress("UNREACHABLE_CODE")
        error("return statement missing")
    }

    private fun calculateLatestReadMoment() = currentTimeMillis() + timeout.inWholeMilliseconds

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
        "reader" to Unicode.ELLIPSIS,
    ).joinToString(prefix = "NonBlockingReader(",
        separator = "; ",
        postfix = ")") { "${it.first}: ${it.second}" }
}
