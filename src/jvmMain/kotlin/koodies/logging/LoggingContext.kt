package koodies.logging

import koodies.asString
import koodies.collections.synchronizedListOf
import koodies.logging.BorderedRenderingLogger.Border.SOLID
import koodies.runtime.Program
import koodies.takeIfDebugging
import koodies.text.ANSI.Formatter
import koodies.text.ANSI.Formatter.Companion.fromScratch
import koodies.text.ANSI.escapeSequencesRemoved
import koodies.text.Semantics.formattedAs
import koodies.text.styling.draw
import koodies.text.styling.wrapWithBorder
import koodies.time.Now
import koodies.unit.Size
import koodies.unit.bytes
import kotlin.reflect.KProperty

/**
 * Creates a logger which serves for logging a sub-process and all of its corresponding events.
 */
public val global: LoggingContext = LoggingContext("global") { print(it.draw.border.block()) }

private typealias LogMessage = Pair<BorderedRenderingLogger, String>

public class LoggingContext(name: String, processor: (String) -> Unit) : BorderedRenderingLogger(name, null) {

    private val startup = System.currentTimeMillis()

    private val messages = Recorder<LogMessage> {
        groupBy({ it.first }) { it.second.length }.mapValues { (logger, counts) -> "${logger.caption} to ${counts.sum()}" }.toString()
    }

    private var muted: Boolean = false
    private var exclusiveLogger: BorderedRenderingLogger? = null
    private val logMessageStream = Merger<BorderedRenderingLogger, String, LogMessage> { logger, message ->
        messages(Pair(logger, message)).also { mostRecent = logger }
    }

    private val defaultStream = logMessageStream.map { (_, message) ->
        if (!muted) processor(message)
    }

    override val missingParentFallback: (String) -> Unit by defaultStream
    public val RenderingLogger.logged: String
        get() = messages.joinMessages {
            val logger = this@logged
            if (logger in first.ancestors) second.escapeSequencesRemoved else ""
        }

    override fun <R> logging(
        caption: CharSequence,
        contentFormatter: Formatter?,
        decorationFormatter: Formatter?,
        returnValueFormatter: ((ReturnValue) -> String)?,
        border: Border,
        block: BorderedRenderingLogger.() -> R,
    ): R = childLogger(
        caption,
        contentFormatter ?: Formatter.PassThrough,
        decorationFormatter ?: Formatter.PassThrough,
        returnValueFormatter ?: RETURN_VALUE_FORMATTER,
        border
    ).runLogging(block)

    private fun childLogger(
        caption: CharSequence,
        contentFormatter: Formatter?,
        decorationFormatter: Formatter?,
        returnValueFormatter: ((ReturnValue) -> String)?,
        border: Border,
    ) = object : BlockRenderingLogger(caption.toString(), this@LoggingContext, contentFormatter, decorationFormatter, returnValueFormatter, border) {
        override val missingParentFallback: (String) -> Unit by defaultStream
    }

    public var mostRecent: BorderedRenderingLogger = this
        private set

    private val exclusiveLogging = logMessageStream.map { (logger, message) ->
        messages(Pair(logger, message))
        processor(message)
    }

    private fun <R> runExclusiveLogging(block: BorderedRenderingLogger.() -> R): R =
        koodies.runWrapping({ muted = true }, { muted = false }) {
            object : BlockRenderingLogger(caption, null, contentFormatter, fromScratch { formattedAs.warning }, returnValueFormatter, SOLID) {
                override val missingParentFallback: (String) -> Unit by exclusiveLogging
            }.runLogging(block)
        }

    public fun <R> runExclusive(block: BorderedRenderingLogger.() -> R): R = runExclusiveLogging(block)

    init {
        Program.onExit {
            val debugging = " in debugging mode".takeIfDebugging() ?: ""
            if (Program.isTesting) println("Session was running ${Now.passedSince(startup)}$debugging and logged a total of $bytes")
            else println("Program was running ${Now.passedSince(startup)}$debugging and logged a total of $bytes".wrapWithBorder())
        }
    }

    private var bytes = Size.ZERO
    public fun reset() {
        bytes = messages.sumBy { it.second.length }.bytes
        messages.clear()
    }
}

public open class Recorder<T>(private val messagesFormatter: List<T>.() -> String) : AbstractList<T>() {
    private val messages = synchronizedListOf<T>()
    override val size: Int get() = messages.size
    override fun get(index: Int): T = messages[index]

    public operator fun invoke(message: T): T = message.also(messages::add)

    public fun joinMessages(): String = messages.joinToString(separator = "")
    public fun joinMessages(transform: T.() -> CharSequence): String = messages.joinToString(separator = "") { it.transform() }

    public fun clear(): Unit = messages.clear()

    override fun toString(): String = asString {
        ::messages.name to messages.messagesFormatter()
    }
}

/**
 * Pipeline style component that allows to applied a series of transformations using [map]
 * to a message [R] that is created by the given [merge] applied to a source [T] and a payload [V].
 *
 * A source for this pipeline is created using a delegated property.
 * The delegated property is a simple function that takes an instance of [V]
 * which will be passed through the pipeline with the implementor as the source [T].
 *
 * The pipeline itself is built by applying [map] on the merger instance which creates a subsequent stage.
 *
 * Technically speaking this component is a container that adds a transformation
 * to its payload with each [map] call.
 */
private open class Merger<T, V, R>(private val merge: (T, V) -> R) {
    public fun <S> map(transform: (R) -> S): Merger<T, V, S> = Merger { t, v -> transform(merge(t, v)) }
    public operator fun getValue(thisRef: T, property: KProperty<*>): (V) -> Unit = { message: V -> merge(thisRef, message) }
}
