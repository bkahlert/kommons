package koodies.logging

import koodies.collections.synchronizedListOf
import koodies.jvm.currentThread
import koodies.text.ANSI.ansiRemoved
import koodies.text.LineSeparators.LF
import koodies.text.LineSeparators.prefixLinesWith
import koodies.text.LineSeparators.removeTrailingLineSeparator
import koodies.text.LineSeparators.runIgnoringTrailingLineSeparator
import koodies.text.padStartFixedLength
import koodies.time.Now
import java.io.OutputStream

/**
 * Logger that stores logged messages in [captured] and
 * prints enriched messages (`threadName: time: message`)
 * to the specified [outputStream].
 */
public open class InMemoryLogger(
    name: CharSequence = "💾",
    parent: SimpleRenderingLogger? = null,
    border: Border = Border.DEFAULT,
    width: Int? = null,
    private val captured: MutableList<String> = synchronizedListOf(),
    outputStream: OutputStream? = null,
    private val start: Long = System.currentTimeMillis(),
    log: (String) -> Unit = {
        val thread = currentThread.name.padStartFixedLength(30)
        val time = Now.passedSince(start).toString().padStartFixedLength(7)
        val prefix = "$thread: $time: "
        outputStream?.apply { write(it.runIgnoringTrailingLineSeparator { prefixLinesWith(prefix) }.toByteArray()) }
        captured.add(it.removeTrailingLineSeparator)
    },
) : BlockRenderingLogger(
    name = name,
    parent = parent,
    log = log,
    border = border,
    width = width,
) {

    private fun assemble(lineSkip: Int, vararg additionalLines: String): String =
        captured.drop(lineSkip).plus(additionalLines).joinToString(LF)

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
            override val successful: Boolean = false
        }
    }
}
