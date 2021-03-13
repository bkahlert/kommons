package koodies.logging

import koodies.concurrent.process.IO
import koodies.concurrent.process.IO.Type.OUT
import koodies.io.path.bufferedWriter
import koodies.io.path.withExtension
import koodies.runtime.Program
import koodies.terminal.ANSI
import koodies.terminal.AnsiCode.Companion.removeEscapeSequences
import koodies.terminal.AnsiColors.green
import koodies.terminal.AnsiColors.red
import koodies.text.Semantics
import koodies.text.Semantics.Document
import koodies.text.Unicode.greekSmallLetterKoppa
import java.nio.file.Path
import kotlin.contracts.InvocationKind.EXACTLY_ONCE
import kotlin.contracts.contract
import kotlin.io.path.extension

/**
 * Logger interface to implement loggers that don't just log
 * but render log messages to provide easier understandable feedback.
 */
public fun interface RenderingLogger {

    /**
     * Method that is responsible to render what gets logged.
     *
     * All default implemented methods use this method.
     */
    public fun render(trailingNewline: Boolean, block: () -> CharSequence)

    /**
     * Logs raw text.
     *
     * *Please note that in contrast to the other logging methods, **no line separator is added.**.*
     */
    public fun logText(block: () -> CharSequence): Unit = block().let { output ->
        render(false) { output }
    }

    /**
     * Logs a line of text.
     */
    public fun logLine(block: () -> CharSequence): Unit = block().let { output ->
        render(true) { output }
    }

    /**
     * Logs some programs [IO] and the status of processed [items].
     */
    public fun logStatus(items: List<HasStatus> = emptyList(), block: () -> CharSequence = { OUT typed "" }): Unit =
        block().let { output ->
            render(true) { "$output (${items.size})" }
        }

    /**
     * Logs some programs [IO] and the status of processed [items].
     */
    public fun logStatus(vararg items: HasStatus, block: () -> CharSequence = { OUT typed "" }): Unit =
        logStatus(items.toList(), block)

    /**
     * Logs some programs [IO] and the processed items [statuses].
     */
    public fun logStatus(vararg statuses: String, block: () -> CharSequence = { OUT typed "" }): Unit =
        logStatus(statuses.map { it.asStatus() }, block)

    /**
     * Logs the result of the process this logger is used for.
     */
    public fun <R> logResult(block: () -> Result<R>): R {
        val result = block()
        render(true) { formatResult(result) }
        return result.getOrThrow()
    }

    /**
     * Logs [Unit], that is *no result*, as the result of the process this logger is used for.
     */
    public fun logResult(): Unit = logResult { Result.success(Unit) }

    /**
     * Explicitly logs a [Throwable]. The behaviour is the same as simply throwing it,
     * which is covered by [logResult] with a failed [Result].
     */
    public fun logException(block: () -> Throwable): Unit = block().let {
        logResult { Result.failure(it) }
    }

    /**
     * Logs a caught [Throwable]. In contrast to [logResult] with a failed [Result] and [logException]
     * this method only marks the current logging context as failed but does not escalate (rethrow).
     */
    public fun <R : Throwable> logCaughtException(block: () -> R): Unit = block().let { ex ->
        recoveredLoggers.add(this)
        render(true) { formatResult(Result.failure<R>(ex)) }
    }

    public companion object {

        public val recoveredLoggers: MutableList<RenderingLogger> = mutableListOf()

        public fun RenderingLogger.formatResult(result: Result<*>): CharSequence {
            val returnValue = result.toReturnValue()
            return if (returnValue.successful) formatReturnValue(returnValue) else formatException(" ", returnValue)
        }

        @Suppress("LocalVariableName", "NonAsciiCharacters")
        public fun formatReturnValue(returnValue: ReturnValue): CharSequence {
            return if (returnValue.successful) Semantics.OK
            else Semantics.PointNext.green() + " $returnValue"
        }

        @Suppress("LocalVariableName", "NonAsciiCharacters")
        public fun RenderingLogger.formatException(prefix: CharSequence, returnValue: ReturnValue): String {
            val format = if (recoveredLoggers.contains(this)) ANSI.termColors.green else ANSI.termColors.red
            val ϟ = format("$greekSmallLetterKoppa")
            return ϟ + prefix + returnValue.format().red()
        }
    }
}

@DslMarker
public annotation class RenderingLoggingDsl

@RenderingLoggingDsl
public inline fun <reified R, reified L : RenderingLogger> L.applyLogging(crossinline block: L.() -> R): L {
    contract { callsInPlace(block, EXACTLY_ONCE) }
    return apply { runLogging(block) }
}

@RenderingLoggingDsl
public inline fun <reified T : RenderingLogger, reified R> T.runLogging(crossinline block: T.() -> R): R {
    contract { callsInPlace(block, EXACTLY_ONCE) }
    val result: Result<R> = kotlin.runCatching { block() }
    logResult { result }
    return result.getOrThrow()
}

/**
 * Creates a logger which logs to [path].
 */
@RenderingLoggingDsl
public inline fun <reified T : RenderingLogger, reified R> T.fileLogging(
    path: Path,
    caption: CharSequence,
    crossinline block: RenderingLogger.() -> R,
): R = compactLogging(caption, block = fileLoggingBlock(path, caption, block))

/**
 * Creates a logger which logs to [path].
 */
@RenderingLoggingDsl
public inline fun <reified R> fileLogging(
    path: Path,
    caption: CharSequence,
    crossinline block: RenderingLogger.() -> R,
): R = compactLogging(caption, block = fileLoggingBlock(path, caption, block))

/**
 * Creates a logger which logs to [path].
 */
@JvmName("nullableFileLogging")
@RenderingLoggingDsl
public inline fun <reified T : RenderingLogger?, reified R> T.fileLogging(
    path: Path,
    caption: CharSequence,
    crossinline block: RenderingLogger.() -> R,
): R =
    if (this is RenderingLogger) fileLogging(path, caption, block)
    else koodies.logging.fileLogging(path, caption, block)

public inline fun <reified R> fileLoggingBlock(
    path: Path,
    caption: CharSequence,
    crossinline block: RenderingLogger.() -> R,
): CompactRenderingLogger.() -> R = {
    logLine { IO.Type.META typed "Logging to" }
    logLine { "$Document ${path.toUri()}" }
    path.bufferedWriter().use { ansiLog ->
        path.withExtension("no-ansi.${path.extension}").bufferedWriter().use { noAnsiLog ->
            val logger: RenderingLogger = BlockRenderingLogger(
                caption = caption,
                bordered = false,
            ) { output ->
                ansiLog.appendLine(output)
                noAnsiLog.appendLine(output.removeEscapeSequences())
            }
            runCatching<R> { logger.block() }.fold(
                { logger.logResult { Result.success(it) } },
                { logger.logResult { Result.failure(it) } })
        }
    }
}

/**
 * Returns `this` [RenderingLogger] if [Program.isDebugging]—otherwise a [MutedRenderingLogger]
 * is returned.
 */
public fun RenderingLogger?.onlyIfDebugging(): RenderingLogger? = if (Program.isDebugging) this else MutedRenderingLogger()
