package koodies.logging

import koodies.collections.withNegativeIndices
import koodies.concurrent.output
import koodies.concurrent.script
import koodies.concurrent.synchronized
import koodies.terminal.AnsiCode.Companion.removeEscapeSequences
import koodies.text.LineSeparators.withoutTrailingLineSeparator
import koodies.text.TruncationStrategy.MIDDLE
import koodies.text.padStartFixedLength
import koodies.text.prefixLinesWith
import koodies.time.Now
import org.apache.commons.io.output.TeeOutputStream
import java.io.OutputStream

open class InMemoryLogger private constructor(
    caption: CharSequence,
    bordered: Boolean = false,
    statusInformationColumn: Int = -1,
    private val outputStream: TeeOutputStream,
    private val captured: MutableList<String>,
    private val start: Long,
) : BlockRenderingLogger(
    caption = caption,
    bordered = bordered,
    statusInformationColumn = if (statusInformationColumn > 0) statusInformationColumn else 60,
    log = { message: String ->
        val thread = Thread.currentThread().name.padStartFixedLength(30, strategy = MIDDLE)
        val time = Now.passedSince(start).toString().padStartFixedLength(7)
        val prefix = "$thread: $time: "
        outputStream.write(message.prefixLinesWith(prefix = prefix).toByteArray())
        captured.add(message.withoutTrailingLineSeparator)
    },
) {
    constructor(
        caption: String,
        bordered: Boolean = true,
        statusInformationColumn: Int = -1,
        outputStreams: List<OutputStream>,
    ) : this(
        caption = caption,
        bordered = bordered,
        statusInformationColumn = statusInformationColumn,
        outputStream = outputStreams.foldRight(TeeOutputStream(
            OutputStream.nullOutputStream(),
            OutputStream.nullOutputStream()
        ),
            { os, tos -> TeeOutputStream(os, tos) }),
        captured = mutableListOf<String>().synchronized(),
        start = System.currentTimeMillis(),
    )

    constructor() : this("Test", true, -1, emptyList())

    /**
     * Runs this strings as a shell script,
     * logs the output and returns it.
     */
    fun String.not(): String =
        logging("$ ${this@not}", bordered = false) {
            script { !this@not }.output().also { it.prefixLinesWith("> ") }
        }

    private val messages: List<CharSequence> by withNegativeIndices { captured }
    private val raw: String get() = messages.joinToString("\n")
    val logged: String get() = messages.joinToString("\n").removeEscapeSequences().withoutTrailingLineSeparator.trim()

    /**
     * Returns the so far rendered content—pretending this block was finished with [result].
     */
    fun <R> finalizedDump(result: Result<R>): String = raw + "\n" + getBlockEnd(result)

    @Suppress("UNCHECKED_CAST")
    override fun toString(): String = finalizedDump(Result.success(Unit) as Result<Nothing>)
}
