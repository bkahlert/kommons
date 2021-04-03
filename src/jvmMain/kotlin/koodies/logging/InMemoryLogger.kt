package koodies.logging

import koodies.collections.synchronizedListOf
import koodies.collections.withNegativeIndices
import koodies.runtime.JVM
import koodies.text.ANSI.escapeSequencesRemoved
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
public open class InMemoryLogger(
    caption: CharSequence = "💾",
    border: Border = Border.DEFAULT,
    width: Int? = null,
    private val captured: MutableList<String> = synchronizedListOf(),
    outputStream: OutputStream? = null,
    parent: BorderedRenderingLogger? = null,
    private val start: Long = System.currentTimeMillis(),
    override val missingParentFallback: (String) -> Unit = {
        val thread = JVM.currentThread.name.padStartFixedLength(30, strategy = MIDDLE)
        val time = Now.passedSince(start).toString().padStartFixedLength(7)
        val prefix = "$thread: $time: "
        outputStream?.apply { write(it.prefixLinesWith(prefix = prefix).toByteArray()) }
        captured.add(it.withoutTrailingLineSeparator)
    },
) : BlockRenderingLogger(
    caption = caption,
    parent = parent,
    border = border,
    width = width,
) {

    @Deprecated("no more used")
    private val messages: List<CharSequence> by withNegativeIndices { captured }

    private fun assemble(lineSkip: Int, vararg additionalLines: String): String =
        sequenceOf(captured.asSequence().drop(lineSkip), additionalLines.asSequence()).flatten().joinToString(LF)

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
        return if (keepEscapeSequences) assembled else assembled.escapeSequencesRemoved
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

        private val LOG_MESSAGE: String = "log message"
        private fun LOG_STATUS(suffix: Int): HasStatus {
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
