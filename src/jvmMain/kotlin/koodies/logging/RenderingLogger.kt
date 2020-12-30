package koodies.logging

import com.github.ajalt.mordant.AnsiCode
import koodies.concurrent.process.IO
import koodies.concurrent.process.IO.Type.OUT
import koodies.exception.toCompactString
import koodies.io.path.bufferedWriter
import koodies.nullable.invoke
import koodies.terminal.ANSI
import koodies.terminal.AnsiCode.Companion.removeEscapeSequences
import koodies.terminal.AnsiColors.green
import koodies.terminal.AnsiColors.red
import koodies.text.Unicode
import koodies.text.Unicode.Emojis.`➜`
import koodies.text.Unicode.Emojis.heavyCheckMark
import koodies.text.Unicode.greekSmallLetterKoppa
import java.nio.file.Path
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

/**
 * Logger interface to implement loggers that don't just log
 * but render log messages to provide easier understandable feedback.
 */
interface RenderingLogger {

    /**
     * Method that is responsible to render what gets logged.
     *
     * All default implemented methods use this method.
     */
    fun render(trailingNewline: Boolean, block: () -> CharSequence)

    /**
     * Logs raw text.
     *
     * *Please note that in contrast to the other logging methods, **no line separator is added.**.*
     */
    fun logText(block: () -> CharSequence): Unit = block().let { output ->
        render(false) { output }
    }

    /**
     * Logs a line of text.
     */
    fun logLine(block: () -> CharSequence): Unit = block().let { output ->
        render(true) { output }
    }

    /**
     * Logs some programs [IO] and the status of processed [items].
     */
    fun logStatus(items: List<HasStatus> = emptyList(), block: () -> IO = { OUT typed "" }): Unit = block().let { output ->
        render(true) { "${output.formatted} (${items.size})" }
    }

    /**
     * Logs some programs [IO] and the status of processed [items].
     */
    fun logStatus(vararg items: HasStatus, block: () -> IO = { OUT typed "" }): Unit =
        logStatus(items.toList(), block)

    /**
     * Logs some programs [IO] and the processed items [statuses].
     */
    fun logStatus(vararg statuses: String, block: () -> IO = { OUT typed "" }): Unit =
        logStatus(statuses.map { it.asStatus() }, block)

    /**
     * Logs the result of the process this logger is used for.
     */
    fun <R> logResult(block: () -> Result<R>): R {
        val result = block()
        render(true) { formatResult(result) }
        return result.getOrThrow()
    }

    /**
     * Explicitly logs a [Throwable]. The behaviour is the same as simply throwing it,
     * which is covered by [logResult] with a failed [Result].
     */
    fun logException(block: () -> Throwable): Unit = block().let {
        logResult { Result.failure(it) }
    }

    /**
     * Logs a caught [Throwable]. In contrast to [logResult] with a failed [Result] and [logException]
     * this method only marks the current logging context as failed but does not escalate (rethrow).
     */
    fun <R : Throwable> logCaughtException(block: () -> R): Unit = block().let { ex ->
        recoveredLoggers.add(this)
        render(true) { formatResult(Result.failure<R>(ex)) }
    }

    companion object {
        val DEFAULT: RenderingLogger = object : RenderingLogger {
            override fun render(trailingNewline: Boolean, block: () -> CharSequence) = block().let { if (trailingNewline) println(it) else print(it) }
        }

        val recoveredLoggers = mutableListOf<RenderingLogger>()

        fun RenderingLogger.formatResult(result: Result<*>): CharSequence =
            if (result.isSuccess) formatReturnValue(result.toCompactString()) else formatException(" ", result.toCompactString())

        @Suppress("LocalVariableName", "NonAsciiCharacters")
        fun formatReturnValue(formattedResult: CharSequence): CharSequence {
            return if (formattedResult.isEmpty()) heavyCheckMark.green()
            else `➜`.emojiVariant.green() + " $formattedResult"
        }

        @Suppress("LocalVariableName", "NonAsciiCharacters")
        fun RenderingLogger.formatException(prefix: CharSequence, oneLiner: CharSequence?): String {
            val format = if (recoveredLoggers.contains(this)) ANSI.termColors.green else ANSI.termColors.red
            val ϟ = format("$greekSmallLetterKoppa")
            return oneLiner?.let { ϟ + prefix + it.red() } ?: ϟ
        }
    }
}

@DslMarker
annotation class RenderingLoggingDsl

@RenderingLoggingDsl
inline fun <reified R, reified L : RenderingLogger> L.applyLogging(crossinline block: L.() -> R): L {
    contract { callsInPlace(block, InvocationKind.EXACTLY_ONCE) }
    logResult { runCatching { block() } }
    return this
}

@RenderingLoggingDsl
inline fun <reified R, reified L : RenderingLogger> L.runLogging(crossinline block: L.() -> R): R {
    contract { callsInPlace(block, InvocationKind.EXACTLY_ONCE) }
    return logResult { runCatching { block() } }
}


/**
 * Creates a logger which serves for logging a sub-process and all of its corresponding events.
 *
 * This logger uses at least one line per log event. If less room is available [compactLogging] is more suitable.
 */
@RenderingLoggingDsl
inline fun <reified R> Any?.logging(
    caption: CharSequence,
    ansiCode: AnsiCode? = null,
    borderedOutput: Boolean = (this as? BlockRenderingLogger)?.borderedOutput ?: false,
    block: BlockRenderingLogger.() -> R,
): R {
    val logger: BlockRenderingLogger = when (this) {
        is MutedRenderingLogger -> this
        is BlockRenderingLogger -> BlockRenderingLogger(
            caption = caption,
            borderedOutput = borderedOutput,
            statusInformationColumn = statusInformationColumn - prefix.length,
            statusInformationPadding = statusInformationPadding,
            statusInformationColumns = statusInformationColumns - prefix.length,
        ) { output -> logText { ansiCode.invoke(output) } }
        is RenderingLogger -> BlockRenderingLogger(caption = caption, borderedOutput = borderedOutput) { output -> logText { ansiCode.invoke(output) } }
        else -> BlockRenderingLogger(caption = caption, borderedOutput = borderedOutput)
    }
    val result: Result<R> = kotlin.runCatching { block(logger) }
    logger.logResult { result }
    return result.getOrThrow()
}

/**
 * Creates a logger which logs to [path].
 */
@RenderingLoggingDsl
inline fun <reified R> RenderingLogger?.fileLogging(
    path: Path,
    caption: CharSequence,
    block: RenderingLogger.() -> R,
): R = logging(caption) {
    logLine { "This process might produce pretty much log messages. Logging to …" }
    logLine { "${Unicode.Emojis.pageFacingUp} ${path.toUri()}" }
    val writer = path.bufferedWriter()
    val logger: RenderingLogger = BlockRenderingLogger(
        caption = caption,
        borderedOutput = false,
        log = { output: String ->
            writer.appendLine(output.removeEscapeSequences())
        },
    )
    kotlin.runCatching { block(logger) }.also { logger.logResult { it }; writer.close() }.getOrThrow()
}

/**
 * Creates a logger which serves for logging a sub-process and all of its corresponding events.
 *
 * This logger logs all events using a single line of text. If more room is needed [logging] is more suitable.
 */
@RenderingLoggingDsl
inline fun <reified R> RenderingLogger?.compactLogging(
    caption: CharSequence,
    noinline block: CompactRenderingLogger.() -> R,
): R {
    val logger = object : CompactRenderingLogger(caption) {
        override fun render(block: () -> CharSequence) {
            this@compactLogging?.apply { logLine(block) } ?: println(block())
        }
    }
    return kotlin.runCatching { block(logger) }.let { logger.logResult { it } }
}

/**
 * Creates a logger which serves for logging a very short sub-process and all of its corresponding events.
 *
 * This logger logs all events using only a couple of characters. If more room is needed [compactLogging] or even [logging] is more suitable.
 */
@RenderingLoggingDsl
inline fun <reified R> CompactRenderingLogger.compactLogging(
    noinline block: MicroLogger.() -> R,
): R = run {
    val logger: MicroLogger = object : MicroLogger() {
        override fun render(block: () -> CharSequence) {
            this@compactLogging.logLine(block)
        }
    }
    kotlin.runCatching { block(logger) }.let { logger.logResult { it } }
}
