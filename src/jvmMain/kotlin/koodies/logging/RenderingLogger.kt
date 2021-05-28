package koodies.logging

import koodies.asString
import koodies.collections.synchronizedMapOf
import koodies.collections.synchronizedSetOf
import koodies.jvm.currentStackTrace
import koodies.logging.FixedWidthRenderingLogger.Border
import koodies.runtime.onExit
import koodies.text.ANSI.Formatter
import koodies.text.ANSI.Formatter.Companion.invoke
import koodies.text.LineSeparators.LF
import koodies.text.LineSeparators.hasTrailingLineSeparator
import koodies.text.LineSeparators.mapLines
import koodies.text.LineSeparators.prefixLinesWith
import koodies.text.Semantics.Symbols
import koodies.text.Semantics.formattedAs
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock
import kotlin.contracts.InvocationKind.EXACTLY_ONCE
import kotlin.contracts.contract

/**
 * Logger interface to implement loggers that don't just log
 * but render log messages to provide easier understandable feedback.
 */
public open class RenderingLogger(
    public val caption: String,
    log: ((String) -> Unit)? = null,
) {

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

    protected val log: (String) -> Unit by lazy { log ?: { print(it) } }
    protected fun logWithLock(message: () -> String): Unit = logLock.withLock { log(message()) }

    /**
     * Method that is responsible to render the return value of
     * the given [block].
     *
     * If [trailingNewline] is `true` the log message will be appended
     * with a line break.
     *
     * All default implemented methods use this method.
     */
    public open fun render(trailingNewline: Boolean, block: () -> CharSequence): Unit =
        logWithLock {
            if (closed) {
                val prefix = Symbols.Computation + " "
                val message = block().prefixLinesWith(prefix = prefix, ignoreTrailingSeparator = false)
                if (trailingNewline || !message.hasTrailingLineSeparator) message + LF else message
            } else {
                val message = block().toString()
                if (trailingNewline) message + LF else message
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
     * Logs the result of the process this logger is used for.
     */
    public open fun <R> logResult(block: () -> Result<R>): R {
        val result = block()
        val formattedResult = ReturnValue.format(result)
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

    override fun toString(): String = asString {
        ::open to open
        ::caption to caption
    }

    /**
     * Helper method than can be applied on [CharSequence] returning lambdas
     * to format them using the provided [f] and passing them to [transform]
     * only in case the result was not blank.
     */
    protected fun <T> (() -> CharSequence).format(f: Formatter?, transform: String.() -> T?): T? {
        return f(this()).takeUnless { it.isBlank() }?.toString()?.transform()
    }

    public companion object {

        /**
         * Helper method than can be applied on a list of elements returning the
         * rendered statuses and passing them to [transform]
         * only in case the result was not blank.
         */
        @JvmStatic
        protected fun <T : CharSequence, R> List<T>.format(f: Formatter?, transform: String.() -> R?): R? {
            if (isEmpty()) return null
            return f(asStatus()).takeUnless { it.isBlank() }?.toString()?.transform()
        }

        private fun Array<StackTraceElement>?.asString() = (this ?: emptyArray()).joinToString("") { "$LF\t\tat $it" }

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
                openLoggers.getOrPut(logger) { currentStackTrace }
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
            onExit {
                val unclosed = openLoggers.filterKeys { logger -> logger !in disabledUnclosedWarningLoggers }

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
    val result: Result<R> = kotlin.runCatching { block() }
    logResult { result }
    return result.getOrThrow()
}

/**
 * Logs the given [returnValue] as the value that is returned from the logging span.
 */
public fun <T : RenderingLogger> T.logReturnValue(returnValue: ReturnValue) {
    logResult { Result.success(returnValue) }
}

/**
 * Creates a logger which serves for logging a sub-process and all of its corresponding events.
 */
@RenderingLoggingDsl
public fun <R> logging(
    caption: CharSequence,
    contentFormatter: Formatter? = null,
    decorationFormatter: Formatter? = null,
    returnValueFormatter: ((ReturnValue) -> ReturnValue)? = null,
    border: Border = BlockRenderingLogger.DEFAULT_BORDER,
    block: FixedWidthRenderingLogger.() -> R,
): R = SmartRenderingLogger(
    caption,
    { LoggingContext.BACKGROUND.logText { it } },
    contentFormatter,
    decorationFormatter,
    returnValueFormatter,
    border,
    prefix = LoggingContext.BACKGROUND.prefix,
).runLogging(block)
