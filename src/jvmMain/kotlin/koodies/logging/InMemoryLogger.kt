package koodies.logging

import koodies.collections.synchronizedListOf
import koodies.jvm.currentThread
import koodies.text.ANSI.ansiRemoved
import koodies.text.LineSeparators.LF
import koodies.text.LineSeparators.prefixLinesWith
import koodies.text.LineSeparators.withoutTrailingLineSeparator
import koodies.text.TruncationStrategy.MIDDLE
import koodies.text.padStartFixedLength
import koodies.time.Now
import java.io.OutputStream

/**
 * Logger that stores logged messages in [captured] and
 * prints enriched messages (`threadName: time: message`)
 * to the specified [outputStream].
 */
public open class InMemoryLogger(
    caption: CharSequence = "💾",
    border: Border = Border.DEFAULT,
    width: Int? = null,
    private val captured: MutableList<String> = synchronizedListOf(),
    outputStream: OutputStream? = null,
    private val start: Long = System.currentTimeMillis(),
    log: (String) -> Unit = {
        val thread = currentThread.name.padStartFixedLength(30, strategy = MIDDLE)
        val time = Now.passedSince(start).toString().padStartFixedLength(7)
        val prefix = "$thread: $time: "
        outputStream?.apply { write(it.prefixLinesWith(prefix = prefix).toByteArray()) }
        captured.add(it.withoutTrailingLineSeparator)
    },
) : BlockRenderingLogger(
    caption = caption,
    log = log,
    border = border,
    width = width,
) {

    private fun assemble(lineSkip: Int, vararg additionalLines: String): String =
        captured.drop(lineSkip).plus(additionalLines).joinToString(LF)

    /**
     * Clears the captured strings.
     */
    public fun clear(): Unit {
        captured.clear()
        open = true
    }

    override fun toString(): String = toString(NO_RETURN_VALUE, false)
    public fun toString(
        fallbackReturnValue: ReturnValue? = null,
        keepEscapeSequences: Boolean = false,
        lineSkip: Int = 0,
    ): String {
        val assembled = if (!open || fallbackReturnValue == null) {
            assemble(lineSkip)
        } else {
            assemble(lineSkip, getBlockEnd(fallbackReturnValue).toString())
        }
        return if (keepEscapeSequences) assembled else assembled.ansiRemoved
    }

    public companion object {

        public val SUCCESSFUL_RETURN_VALUE: ReturnValue = object : ReturnValue {
            override val successful: Boolean = true
        }

        public val NO_RETURN_VALUE: ReturnValue = object : ReturnValue {
            override val successful: Boolean? = null
        }

        private const val LOG_MESSAGE: String = "log message"

        public val LOG_OPERATIONS: Array<Pair<String, RenderingLogger.() -> Unit>> = arrayOf(
            "logText { … }"
                to { logText { LOG_MESSAGE } },
            "logLine { … }"
                to { logLine { LOG_MESSAGE } },
            "logResult { … }"
                to { @Suppress("RemoveExplicitTypeArguments") logResult<String> { Result.success("result") } },
            "logResult()"
                to { logResult() },
            "logException { … }"
                to { kotlin.runCatching { logException { RuntimeException("log exception") } } },
        )
    }
}
