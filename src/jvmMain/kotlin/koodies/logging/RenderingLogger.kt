package koodies.logging

import koodies.asString
import koodies.collections.synchronizedListOf
import koodies.collections.synchronizedMapOf
import koodies.collections.synchronizedSetOf
import koodies.concurrent.process.IO
import koodies.io.path.bufferedWriter
import koodies.io.path.withExtension
import koodies.runtime.JVM
import koodies.runtime.Program
import koodies.terminal.ANSI
import koodies.terminal.AnsiCode.Companion.removeEscapeSequences
import koodies.terminal.AnsiColors.red
import koodies.text.ANSI.Formatter
import koodies.text.ANSI.Formatter.Companion.invoke
import koodies.text.LineSeparators.LF
import koodies.text.LineSeparators.hasTrailingLineSeparator
import koodies.text.Semantics
import koodies.text.Semantics.Document
import koodies.text.Semantics.formattedAs
import koodies.text.Unicode.greekSmallLetterKoppa
import koodies.text.mapLines
import koodies.text.prefixLinesWith
import java.nio.file.Path
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock
import kotlin.contracts.InvocationKind.EXACTLY_ONCE
import kotlin.contracts.contract
import kotlin.io.path.extension

/**
 * Logger interface to implement loggers that don't just log
 * but render log messages to provide easier understandable feedback.
 */
public open class RenderingLogger(
    public val caption: String,
    public val parent: RenderingLogger? = null,
    protected open val missingParentFallback: (String) -> Unit = { print(it) },
) {
    init {
        require(this !== parent) { "A logger cannot be its own parent." }
    }

    /**
     * Contains a list containing `this` and and all of its parent loggers.
     */
    public val ancestors: List<RenderingLogger>
        get() = generateSequence(this) { it.parent }.toList()

    protected open var initialized: Boolean = false

    /**
     * Contains whether this logger is open, that is,
     * at least one logging call was received but no result, yet.
     */
    public open var open: Boolean
        get() = isOpen(this)
        protected set(value) = setOpen(this, value)

    /**
     * Contains whether this logger is closed, that is,
     * the logging span was finished with a logged result.
     */
    public val closed: Boolean
        get() = initialized && !open

    /**
     * Lock used to
     * - render logger thread-safe
     * - recognise first logging call
     */
    private val logLock by lazy {
        ReentrantLock().also { open = true }.also { initialized = true }
    }

    protected fun log(message: () -> String): Unit = logLock.withLock {
        val text = message()
        if (parent != null) {
            parent.logText { text }
        } else {
            missingParentFallback(text)
        }
    }

    /**
     * Method that is responsible to render the return value of
     * the given [block].
     *
     * If [trailingNewline] is `true` the log message will be appended
     * with a line break.
     *
     * All default implemented methods use this method.
     */
    public open fun render(trailingNewline: Boolean, block: () -> CharSequence): Unit {
        log {
            if (closed) {
                val prefix = caption.formattedAs.meta + " " + Semantics.Computation + " "
                val message = block().prefixLinesWith(prefix = prefix, ignoreTrailingSeparator = false)
                if (trailingNewline || !message.hasTrailingLineSeparator) message + LF else message
            } else {
                val message = block().toString()
                if (trailingNewline) message + LF else message
            }
        }
    }

    /**
     * Logs raw text.
     *
     * *Please note that in contrast to the other logging methods, **no line separator is added.**.*
     */
    public open fun logText(block: () -> CharSequence): Unit =
        block().let { output ->
            render(false) { output }
        }

    /**
     * Logs a line of text.
     */
    public open fun logLine(block: () -> CharSequence): Unit =
        block().let { output ->
            render(true) { output }
        }

    /**
     * Logs some programs [IO] and the status of processed [items].
     */
    public open fun logStatus(items: List<HasStatus> = emptyList(), block: () -> CharSequence = { IO.OUT typed "" }): Unit =
        block().let { output ->
            render(true) { "$output (${items.size})" }
        }

    /**
     * Logs some programs [IO] and the status of processed [items].
     */
    public fun logStatus(vararg items: HasStatus, block: () -> CharSequence = { IO.OUT typed "" }): Unit =
        logStatus(items.toList(), block)

    /**
     * Logs some programs [IO] and the processed items [statuses].
     */
    public fun logStatus(vararg statuses: String, block: () -> CharSequence = { IO.OUT typed "" }): Unit =
        logStatus(statuses.map { it.asStatus() }, block)

    /**
     * Logs the result of the process this logger is used for.
     */
    public open fun <R> logResult(block: () -> Result<R>): R {
        val result = block()
        val formattedResult = formatResult(result)
        render(true) { formattedResult }
        open = false
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
    public open fun logException(block: () -> Throwable): Unit = logResult { Result.failure(block()) }

    /**
     * Logs a caught [Throwable]. In contrast to [logResult] with a failed [Result] and [logException]
     * this method only marks the current logging context as failed but does not escalate (rethrow).
     */
    public fun <R : Throwable> logCaughtException(block: () -> R): Unit {
        val ex = block()
        recoveredLoggers.add(this)
        val formattedResult = formatResult(Result.failure<R>(ex))
        render(true) { formattedResult }
        open = false
    }

    override fun toString(): String = asString {
        ::parent to parent?.caption
        ::caption to caption
        ::open to open
    }


    /**
     * Helper method than can be applied on [CharSequence] returning lambdas
     * to format them using the provided [f] and passing them to [transform]
     * only in case the result was not blank.
     */
    protected fun <T> (() -> CharSequence).format(f: Formatter?, transform: String.() -> T?): T? {
        return f(this()).takeUnless { it.isBlank() }?.toString()?.transform()
    }

    /**
     * Helper method than can be applied on a list of [HasStatus] returning the
     * rendered statuses and passing them to [transform]
     * only in case the result was not blank.
     */
    protected fun <T> List<HasStatus>.format(f: Formatter?, transform: String.() -> T?): T? {
        if (isEmpty()) return null
        return f(renderStatus()).takeUnless { it.isBlank() }?.toString()?.transform()
    }

    public companion object {

        private fun Array<StackTraceElement>?.asString() = (this ?: emptyArray()).joinToString("") { LF + "\t\tat " + it.toString() }

        private val openLoggers: MutableMap<RenderingLogger, Array<StackTraceElement>> = synchronizedMapOf()

        /**
         * Sets the [open] state of the given [logger].
         *
         * Loggers that are not closed the moment this program shuts down
         * are considered broken and will inflict a warning.
         *
         * This behaviour can be disabled using [disabledUnclosedWarningLoggers].
         */
        private fun setOpen(logger: RenderingLogger, open: Boolean) {
            if (open) {
                openLoggers.putIfAbsent(logger, JVM.currentStackTrace)
            } else {
                openLoggers.remove(logger)
            }
        }

        /**
         * Returns whether this [logger] is unclosed.
         */
        private fun isOpen(logger: RenderingLogger): Boolean = logger in openLoggers

        private val disabledUnclosedWarningLoggers: MutableSet<RenderingLogger> = synchronizedSetOf()

        /**
         * Disables the warning during program exit
         * that shows up if this logger's span was not closed.
         */
        public val <T : RenderingLogger> T.withUnclosedWarningDisabled: T
            get() = also { disabledUnclosedWarningLoggers.add(it) }

        init {
            Program.onExit {
                val unclosed = openLoggers.filterKeys { logger -> logger.ancestors.none { it in disabledUnclosedWarningLoggers } }

                if (unclosed.isNotEmpty()) {
                    println("${unclosed.size} started but unfinished renderer(s) found:".formattedAs.warning)
                    unclosed.forEach { (unclosedLogger: RenderingLogger, stackTrace: Array<StackTraceElement>) ->
                        println()
                        println(unclosedLogger.toString().mapLines { line -> "\t$line" })
                        println("\tcreated:")
                        println(stackTrace.asString())
                    }
                }
            }
        }

        public val recoveredLoggers: MutableList<RenderingLogger> = synchronizedListOf()

        public fun RenderingLogger.formatResult(result: Result<*>): CharSequence {
            val returnValue = result.toReturnValue()
            return when (returnValue.successful) {
                true -> formatReturnValue(returnValue)
                null -> formatUnreadyReturnValue(returnValue)
                false -> formatException(" ", returnValue)
            }
        }

        @Suppress("LocalVariableName", "NonAsciiCharacters")
        public fun formatReturnValue(@Suppress("UNUSED_PARAMETER") returnValue: ReturnValue): CharSequence = Semantics.OK

        @Suppress("LocalVariableName", "NonAsciiCharacters")
        public fun formatUnreadyReturnValue(@Suppress("UNUSED_PARAMETER") returnValue: ReturnValue): CharSequence = "${Semantics.Computation} async computation"

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
public inline fun <R, L : RenderingLogger> L.applyLogging(crossinline block: L.() -> R): L {
    contract { callsInPlace(block, EXACTLY_ONCE) }
    return apply { runLogging(block) }
}

@RenderingLoggingDsl
public inline fun <T : RenderingLogger, R> T.runLogging(crossinline block: T.() -> R): R {
    contract { callsInPlace(block, EXACTLY_ONCE) }
    val result: Result<R> = kotlin.runCatching<R> { block() }
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
): R = CompactRenderingLogger(caption, null, this).runLogging {
    logLine { IO.META typed "Logging to" }
    logLine { "$Document ${path.toUri()}" }
    path.bufferedWriter().use { ansiLog ->
        path.withExtension("no-ansi.${path.extension}").bufferedWriter().use { noAnsiLog ->
            object : BlockRenderingLogger(caption.toString(), null) {
                override val missingParentFallback: (String) -> Unit = {
                    ansiLog.appendLine(it)
                    noAnsiLog.appendLine(it.removeEscapeSequences())
                }
            }.runLogging(block)
        }
    }
}

/**
 * Returns `this` [RenderingLogger] if [Program.isDebugging]—otherwise a [MutedRenderingLogger]
 * is returned.
 */
public fun RenderingLogger?.onlyIfDebugging(): RenderingLogger? = if (Program.isDebugging) this else MutedRenderingLogger()
