package koodies.nio

import koodies.debug.asEmoji
import koodies.debug.debug
import koodies.exec.IO
import koodies.logging.FixedWidthRenderingLogger
import koodies.logging.MutedRenderingLogger
import koodies.logging.RenderingLogger.Companion.withUnclosedWarningDisabled
import koodies.text.ANSI
import koodies.text.INTERMEDIARY_LINE_PATTERN
import koodies.text.LineSeparators
import koodies.text.LineSeparators.CR
import koodies.text.LineSeparators.LF
import koodies.text.LineSeparators.hasTrailingLineSeparator
import koodies.text.LineSeparators.withoutTrailingLineSeparator
import koodies.text.quoted
import koodies.time.Now
import koodies.unit.milli
import koodies.unit.seconds
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
    inputStream: InputStream,
    private val timeout: Duration = 6.seconds,
    private val logger: FixedWidthRenderingLogger = MutedRenderingLogger,
    private val blockOnEmptyLine: Boolean = false,
) : BufferedReader(Reader.nullReader()) {
    init {
        check(timeout.isPositive()) { "Timeout must greater 0" }
        logger.withUnclosedWarningDisabled
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
        logger.logging(
            caption = NonBlockingReader::class.simpleName + "." + ::readLine.name + "()",
            decorationFormatter = { ANSI.Colors.cyan(it) }
        ) {

            var latestReadMoment = calculateLatestReadMoment()
            logLine { IO.Meta typed "Starting to read line for at most $timeout" }
            while (true) {
                val read: Int = reader?.read(charArray, 0, this@logging)!!

                if (read == -1) {
                    logLine { IO.Meta typed "InputStream Depleted. Closing. Unfinished Line: ${unfinishedLine.quoted}" }
                    close()
                    return@logging if (unfinishedLine.isEmpty()) {
                        lastReadLineDueTimeout = false
                        lastReadLine = null
                        lastReadLine
                    } else {
                        lastReadLineDueTimeout = false
                        lastReadLine = "$unfinishedLine"
                        unfinishedLine.clear()
                        lastReadLine!!.withoutTrailingLineSeparator
                    }
                }
                logLine { IO.Meta typed "${Now.emoji} ${(latestReadMoment - currentTimeMillis()).milli.seconds}; 📋 ${unfinishedLine.debug}; 🆕 ${justRead.debug}" }
                if (read == 1) {

                    val lineAlreadyRead = lastReadLineDueTimeout == true && lastReadLine?.hasTrailingLineSeparator == true && !justReadCRLF

                    if (lineComplete) {
                        lastReadLineDueTimeout = false
                        lastReadLine = "$unfinishedLine"
                        unfinishedLine.clear()
                        unfinishedLine.append(charArray)
                        logLine { IO.Meta typed "Line Completed: ${lastReadLine.quoted}" }
                        if (!lineAlreadyRead) {
                            return@logging lastReadLine!!.withoutTrailingLineSeparator
                        }
                    }
                    if (!lineAlreadyRead) {
                        unfinishedLine.append(charArray)
                        latestReadMoment = calculateLatestReadMoment()
                    }
                }

                if (currentTimeMillis() >= latestReadMoment && !(blockOnEmptyLine && unfinishedLine.isEmpty())) {
                    logLine { IO.Meta typed "${Now.emoji} Timed out. Returning ${unfinishedLine.quoted}" }
                    // TODO evaluate if better to call a callback and continue working (without returning half-read lines)
                    lastReadLineDueTimeout = true
                    lastReadLine = "$unfinishedLine"
                    return@logging lastReadLine!!.withoutTrailingLineSeparator
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
    public fun forEachLine(block: (String) -> Unit): String = logger.compactLogging(NonBlockingReader::class.simpleName + "." + ::forEachLine.name + "()") {
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
        "logger" to logger,
        "reader" to "…",
    ).joinToString(prefix = "NonBlockingReader(",
        separator = "; ",
        postfix = ")") { "${it.first}: ${it.second}" }
}
