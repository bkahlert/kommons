package koodies.nio

import io.opentelemetry.api.trace.Span
import koodies.debug.asEmoji
import koodies.debug.debug
import koodies.exec.mock.SlowInputStream
import koodies.runWrapping
import koodies.text.ANSI.ansiRemoved
import koodies.text.INTERMEDIARY_LINE_PATTERN
import koodies.text.LineSeparators
import koodies.text.LineSeparators.CR
import koodies.text.LineSeparators.LF
import koodies.text.LineSeparators.hasTrailingLineSeparator
import koodies.text.LineSeparators.trailingLineSeparatorRemoved
import koodies.text.Unicode
import koodies.text.asCodePoint
import koodies.time.seconds
import koodies.tracing.CurrentSpan
import koodies.tracing.Key
import koodies.tracing.rendering.Renderer.Companion.NOOP
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
        spanning("reading line non-blocking", TIMEOUT_ATTRIBUTE to timeout, renderer = { NOOP }) {
            var latestReadMoment = calculateLatestReadMoment()
            while (true) {

                val read: Int = runWrapping({
                    (inputStream as? SlowInputStream)?.run { parentSpan.also { parentSpan = Span.current() } }
                }, {
                    (inputStream as? SlowInputStream)?.parentSpan = it ?: error("No parent span to restore")
                }) { reader?.read(charArray, 0)!! }

                if (read == -1) {
                    eofEvent()
                    close()
                    return@spanning if (unfinishedLine.isEmpty()) {
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

                characterReadEvent(latestReadMoment)

                if (read == 1) {

                    val lineAlreadyRead = lastReadLineDueTimeout == true && lastReadLine?.hasTrailingLineSeparator == true && !justReadCRLF

                    if (lineComplete) {
                        lastReadLineDueTimeout = false
                        lastReadLine = "$unfinishedLine".also { lineReadEvent(it) }
                        unfinishedLine.clear()
                        unfinishedLine.append(charArray)
                        if (!lineAlreadyRead) {
                            return@spanning lastReadLine!!.trailingLineSeparatorRemoved
                        }
                    }
                    if (!lineAlreadyRead) {
                        unfinishedLine.append(charArray)
                        latestReadMoment = calculateLatestReadMoment()
                    }
                }

                if (currentTimeMillis() >= latestReadMoment && !(blockOnEmptyLine && unfinishedLine.isEmpty())) {
                    event("timeout",
                        LINE_ATTRIBUTE to unfinishedLine,
                        TIMEOUT_ATTRIBUTE to (latestReadMoment - currentTimeMillis()).milli.seconds)
                    // TODO evaluate if better to call a callback and continue working (without returning half-read lines)
                    lastReadLineDueTimeout = true
                    lastReadLine = "$unfinishedLine"
                    return@spanning lastReadLine!!.trailingLineSeparatorRemoved
                }
            }
            @Suppress("UNREACHABLE_CODE")
            error("return statement missing")
        }

    private fun CurrentSpan.characterReadEvent(latestReadMoment: Long) {
        event("character read",
            CHAR_ATTRIBUTE to justRead,
            CODEPOINT_ATTRIBUTE to justRead,
            LINE_ATTRIBUTE to unfinishedLine,
            TIMEOUT_ATTRIBUTE to (latestReadMoment - currentTimeMillis()).milli.seconds)
    }

    private fun CurrentSpan.lineReadEvent(line: String) {
        event("line-read", LINE_ATTRIBUTE to line)
    }

    private fun CurrentSpan.eofEvent() {
        event("EOF", LINE_ATTRIBUTE to unfinishedLine)
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

    public companion object {
        private val CHAR_ATTRIBUTE: Key<String, String> = Key.stringKey("character")
        private val CODEPOINT_ATTRIBUTE: Key<String, String> = Key.stringKey("codepoint") { char -> char.asCodePoint()?.let { "0x${it.hexCode}" } ?: "" }
        private val LINE_ATTRIBUTE: Key<String, CharSequence> = Key.stringKey("line") { it.ansiRemoved }
        private val TIMEOUT_ATTRIBUTE: Key<String, Duration> = Key.stringKey("timeout") { it.toString() }
    }
}
