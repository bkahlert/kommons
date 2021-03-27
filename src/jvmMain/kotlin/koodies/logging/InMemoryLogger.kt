package koodies.logging

import koodies.collections.synchronizedListOf
import koodies.collections.withNegativeIndices
import koodies.concurrent.output
import koodies.concurrent.script
import koodies.io.TeeOutputStream
import koodies.runtime.JVM
import koodies.terminal.AnsiCode.Companion.removeEscapeSequences
import koodies.text.LineSeparators.LF
import koodies.text.LineSeparators.withoutTrailingLineSeparator
import koodies.text.TruncationStrategy.MIDDLE
import koodies.text.padStartFixedLength
import koodies.text.prefixLinesWith
import koodies.time.Now
import java.io.OutputStream

/**
 * Logger that stores logged messages in [captured] and
 * prints enriched messages (`threadName: time: message`)
 * to the specified [outputStream].
 */
public open class InMemoryLogger private constructor(
    caption: CharSequence,
    parent: RenderingLogger?,
    bordered: Boolean = false,
    statusInformationColumn: Int = -1,
    private val outputStream: OutputStream,
    private val captured: MutableList<String>,
    private val start: Long,
) : BlockRenderingLogger(
    caption = caption,
    parent = parent,
    bordered = bordered,
    statusInformationColumn = if (statusInformationColumn > 0) statusInformationColumn else 60,
    log = { message: String ->
        val thread = JVM.currentThread.name.padStartFixedLength(30, strategy = MIDDLE)
        val time = Now.passedSince(start).toString().padStartFixedLength(7)
        val prefix = "$thread: $time: "
        outputStream.write(message.prefixLinesWith(prefix = prefix).toByteArray())
        captured.add(message.withoutTrailingLineSeparator)
    },
) {
    public constructor(
        caption: String,
        bordered: Boolean = true,
        statusInformationColumn: Int = -1,
        vararg outputStreams: OutputStream,
    ) : this(
        caption = caption,
        parent = null,
        bordered = bordered,
        statusInformationColumn = statusInformationColumn,
        outputStream = if (outputStreams.isEmpty()) OutputStream.nullOutputStream() else TeeOutputStream(outputStreams.toList()),
        captured = synchronizedListOf<String>(),
        start = System.currentTimeMillis(),
    )

    public constructor() : this("Test", true, -1)

    /**
     * Runs this strings as a shell script,
     * logs the output and returns it.
     */
    public fun String.not(): String =
        logging("$ ${this@not}", bordered = false) {
            script { !this@not }.output().also { it.prefixLinesWith("> ") }
        }

    private val messages: List<CharSequence> by withNegativeIndices { captured }
    private val raw: String get() = messages.joinToString("\n")

    @Deprecated("use toString() / expectThatLogged")
    public val logged: String
        get() = messages.joinToString("\n").removeEscapeSequences().withoutTrailingLineSeparator.trim()

    override fun toString(): String = toString(NO_RETURN_VALUE, false)
    public fun toString(fallbackReturnValue: ReturnValue? = null, keepEscapeSequences: Boolean = false): String {
        val ansiString = raw + if (closed || fallbackReturnValue == null) "" else LF + getBlockEnd(fallbackReturnValue)
        return if (keepEscapeSequences) ansiString else ansiString.removeEscapeSequences()
    }

    public companion object {

        public val SUCCESSFUL_RETURN_VALUE: ReturnValue = object : ReturnValue {
            override val successful: Boolean = true
            override fun format(): CharSequence = ""
        }

        public val NO_RETURN_VALUE: ReturnValue = object : ReturnValue {
            override val successful: Boolean? = null
            override fun format(): CharSequence = ""
        }

        public val LOG_MESSAGE: String = "log message"
        public fun LOG_STATUS(suffix: Int): HasStatus {
            val renderedStatus = "status $suffix"
            return renderedStatus.asStatus()
        }

        public val LOG_OPERATIONS: Map<String, RenderingLogger.() -> Unit> = linkedMapOf(
            "logText { … }"
                to { logText { LOG_MESSAGE } },
            "logLine { … }"
                to { logLine { LOG_MESSAGE } },
            "logStatus(List<HasStatus>) { … }"
                to { logStatus(listOf(LOG_STATUS(1), LOG_STATUS(2))) { LOG_MESSAGE } },
            "logStatus(vararg HasStatus) { … }"
                to { logStatus(LOG_STATUS(1), LOG_STATUS(2)) { LOG_MESSAGE } },
            "logStatus(vararg String) { … }"
                to { logStatus(LOG_STATUS(1).renderStatus(), LOG_STATUS(1).renderStatus()) { LOG_MESSAGE } },
            "logResult { … }"
                to { @Suppress("RemoveExplicitTypeArguments") logResult<String> { Result.success("result") } },
            "logResult()"
                to { logResult() },
            "logException { … }"
                to { kotlin.runCatching { logException { RuntimeException("log exception") } } },
            "logCaughtException { … }"
                to { logCaughtException { RuntimeException("log exception") } },
        )
    }
}
