package koodies.logging

import koodies.concurrent.process.IO
import koodies.concurrent.process.IO.Type.OUT
import koodies.io.path.bufferedWriter
import koodies.terminal.ANSI
import koodies.terminal.AnsiCode.Companion.removeEscapeSequences
import koodies.terminal.AnsiColors.green
import koodies.terminal.AnsiColors.red
import koodies.text.Unicode
import koodies.text.Unicode.Emojis.heavyCheckMark
import koodies.text.Unicode.Emojis.heavyRoundTippedRightwardsArrow
import koodies.text.Unicode.greekSmallLetterKoppa
import java.io.BufferedWriter
import java.nio.file.Path
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

/**
 * Logger interface to implement loggers that don't just log
 * but render log messages to provide easier understandable feedback.
 */
fun interface RenderingLogger {

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
    fun logStatus(items: List<HasStatus> = emptyList(), block: () -> CharSequence = { OUT typed "" }): Unit =
        block().let { output ->
            render(true) { "$output (${items.size})" }
        }

    /**
     * Logs some programs [IO] and the status of processed [items].
     */
    fun logStatus(vararg items: HasStatus, block: () -> CharSequence = { OUT typed "" }): Unit =
        logStatus(items.toList(), block)

    /**
     * Logs some programs [IO] and the processed items [statuses].
     */
    fun logStatus(vararg statuses: String, block: () -> CharSequence = { OUT typed "" }): Unit =
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
     * Logs [Unit], that is *no result*, as the result of the process this logger is used for.
     */
    fun logResult(): Unit = logResult { Result.success(Unit) }

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

        val recoveredLoggers = mutableListOf<RenderingLogger>()

        fun RenderingLogger.formatResult(result: Result<*>): CharSequence {
            val returnValue = result.toReturnValue()
            return if (returnValue.successful) formatReturnValue(returnValue) else formatException(" ", returnValue)
        }

        @Suppress("LocalVariableName", "NonAsciiCharacters")
        fun formatReturnValue(returnValue: ReturnValue): CharSequence {
            return if (returnValue.successful) heavyCheckMark.green()
            else heavyRoundTippedRightwardsArrow.emojiVariant.green() + " $returnValue"
        }

        @Suppress("LocalVariableName", "NonAsciiCharacters")
        fun RenderingLogger.formatException(prefix: CharSequence, returnValue: ReturnValue): String {
            val format = if (recoveredLoggers.contains(this)) ANSI.termColors.green else ANSI.termColors.red
            val ϟ = format("$greekSmallLetterKoppa")
            return ϟ + prefix + returnValue.format().red()
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
 * Creates a logger which logs to [path].
 */
@RenderingLoggingDsl
inline fun <reified R> RenderingLogger?.fileLogging(
    path: Path,
    caption: CharSequence,
    crossinline block: RenderingLogger.() -> R,
): R = blockLogging(caption) {
    logLine { "This process might produce pretty much log messages. Logging to …" }
    logLine { "${Unicode.Emojis.pageFacingUp} ${path.toUri()}" }
    val writer: BufferedWriter = path.bufferedWriter()
    val logger: RenderingLogger = BlockRenderingLogger(
        caption = caption,
        bordered = false,
        log = { output: String ->
            writer.appendLine(output.removeEscapeSequences())
        },
    )
    kotlin.runCatching { block(logger) }.also { logger.logResult { it }; writer.close() }.getOrThrow()
}
