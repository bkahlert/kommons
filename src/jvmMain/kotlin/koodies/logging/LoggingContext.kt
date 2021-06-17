package koodies.logging

import koodies.asString
import koodies.exec.IO
import koodies.logging.FixedWidthRenderingLogger.Border.SOLID
import koodies.runtime.isTesting
import koodies.runtime.onExit
import koodies.takeIfDebugging
import koodies.text.ANSI.Formatter
import koodies.text.LineSeparators.prefixLinesWith
import koodies.text.LineSeparators.removeTrailingLineSeparator
import koodies.text.LineSeparators.runIgnoringTrailingLineSeparator
import koodies.text.Semantics.FieldDelimiters
import koodies.text.Semantics.formattedAs
import koodies.text.styling.wrapWithBorder
import koodies.time.Now
import koodies.unit.Size
import koodies.unit.bytes
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock
import kotlin.reflect.KProperty


private typealias LogMessage = Pair<FixedWidthRenderingLogger, String>

// TODO only works because null lets BorderedRenderingLogger delegate to logText
public class LoggingContext(name: String, print: (String) -> Unit) : FixedWidthRenderingLogger(name, null) {

    private val startup = System.currentTimeMillis()

    private val messages = object : Recorder<LogMessage>() {
        override fun toString(): String {
            return use {
                groupBy({ it.first }) { it.second }.map { (logger, messages) ->
                    logger.name to messages.joinToString("").removeTrailingLineSeparator
                }.joinToString(FieldDelimiters.FIELD)
            }
        }
    }

    public val SimpleRenderingLogger.logged: String
        get() = messages.joinMessages {
            val logger = this@logged
            if (logger == first) second else ""
        }.removeTrailingLineSeparator

    public var mostRecent: FixedWidthRenderingLogger = this
        private set

    private var muted: Boolean = false
    private val baseMessageStream = Merger<FixedWidthRenderingLogger, String, LogMessage> { logger, message ->
        messages.record(Pair(logger, message)).also { mostRecent = logger }
    }

    private val defaultOut by baseMessageStream.map<Unit> { (_, message) ->
        if (!muted) print(message)
    }

    override fun render(block: () -> CharSequence): Unit {
        val message = block().toString()
        defaultOut(message)
    }

    private val exclusiveOut by baseMessageStream.map { (logger, message) -> messages.record(Pair(logger, message)); print(message) }
    public fun <R> runExclusive(block: FixedWidthRenderingLogger.() -> R): R =
        koodies.runWrapping({ muted = true }, { muted = false }) {
            BlockRenderingLogger(name,
                this,
                exclusiveOut,
                contentFormatter,
                Formatter.fromScratch { formattedAs.warning },
                returnValueFormatter,
                SOLID).runLogging(
                block)
        }

    init {
        onExit {
            val debugging = " in debugging mode".takeIfDebugging() ?: ""
            if (isTesting) println("Session was running ${Now.passedSince(startup)}$debugging and logged a total of $bytes")
            else println("Program was running ${Now.passedSince(startup)}$debugging and logged a total of $bytes".wrapWithBorder())
        }
    }

    override fun toString(): String = asString {
        ::startup to startup
        ::messages to messages
        ::mostRecent to mostRecent.name
        ::muted to muted
    }

    private var bytes: Size = Size.ZERO
    public fun reset() {
        bytes = messages.use { sumOf { it.second.length }.bytes }
        messages.clear()
    }

    public companion object {
        public val BACKGROUND: LoggingContext = LoggingContext("background") {
            val message = it.runIgnoringTrailingLineSeparator { prefixLinesWith(IO.ERASE_MARKER) }
            print(message)
        }

        public val DEBUGGING_ONLY: FixedWidthRenderingLogger = BACKGROUND.takeIfDebugging() ?: MutedRenderingLogger
    }
}

public abstract class Recorder<T> {
    private val messagesLock = ReentrantLock()
    private val messages = mutableListOf<T>()

    public fun <R> use(transform: List<T>.() -> R): R = messagesLock.withLock {
        messages.toList().transform()
    }

    public fun record(message: T): T = messagesLock.withLock {
        message.also(messages::add)
    }

    public fun joinMessages(): String = messagesLock.withLock {
        messages.joinToString(separator = "")
    }

    public fun joinMessages(transform: T.() -> CharSequence): String = messagesLock.withLock {
        messages.joinToString(separator = "") { it.transform() }
    }

    public fun clear(): Unit = messagesLock.withLock {
        messages.clear()
    }

    abstract override fun toString(): String
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
    fun <S> map(transform: (R) -> S): Merger<T, V, S> = Merger { t, v -> transform(merge(t, v)) }
    operator fun getValue(thisRef: T, property: KProperty<*>): (V) -> Unit = { message: V -> merge(thisRef, message) }
}
