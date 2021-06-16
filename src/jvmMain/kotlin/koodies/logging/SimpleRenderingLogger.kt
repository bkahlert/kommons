package koodies.logging

import koodies.asString
import koodies.collections.synchronizedMapOf
import koodies.collections.synchronizedSetOf
import koodies.jvm.currentStackTrace
import koodies.runtime.onExit
import koodies.text.ANSI.Formatter
import koodies.text.ANSI.Formatter.Companion.invoke
import koodies.text.LineSeparators.LF
import koodies.text.LineSeparators.mapLines
import koodies.text.LineSeparators.withTrailingLineSeparator
import koodies.text.Semantics.formattedAs
import koodies.text.takeUnlessBlank

// TODO remove InMemoryLogger.NO_RETURN_VALUE
// TODO merge render & log
// TODO replace log with parent

// TODO make interface
// TODO make logging only with use/withspan function
// TODO separate tracing and logging/rendering

/**
 * Logger interface to implement loggers that don't just log
 * but render log messages to provide easier understandable feedback.
 */
public open class SimpleRenderingLogger(
    final override val name: String,
    final override val parent: SimpleRenderingLogger?,
    private val log: ((String) -> Unit)? = null,
) : RenderingLogger {

//    public open val span: OpenTelemetrySpan = OpenTelemetrySpan(name, parent?.span)

    final override var started: Boolean = false
        private set

    protected fun lazyInit() {
        if (!started) {
            setOpen(this, true)
            started = true
            onStart()
        }
    }

    /**
     * Contains whether this logger is open, that is,
     * at least one logging call was received but no result, yet.
     */
    override val open: Boolean get() = isOpen(this)

    override fun <T> close(result: Result<T>) {
        setOpen(this, false)
//        span.end(result)
    }

    /**
     * Contains whether this logger is closed, that is,
     * the logging span was finished with a logged result.
     */
    override val closed: Boolean
        get() = started && !open

    override fun log(lazyMessage: () -> CharSequence) {
        lazyInit()
        lazyMessage().takeUnlessBlank()?.apply {
            log?.invoke(withTrailingLineSeparator(LF)) ?: print(withTrailingLineSeparator(LF))
        }
    }

    /**
     * Method that is responsible to render the return value of
     * the given [block].
     */
    public open fun render(block: () -> CharSequence): Unit =
        log(block)

    /**
     * Logs raw text.
     *
     * *Please note that in contrast to the other logging methods, **no line separator is added.**.*
     */
    public open fun logText(block: () -> CharSequence): Unit =
        render(block)

    /**
     * Logs a line of text.
     */
    public open fun logLine(block: () -> CharSequence): Unit =
        render(block)

    /**
     * Logs the result of the process this logger is used for.
     */
    public open fun <R> logResult(result: Result<R>): R {
        render { ReturnValue.format(result) }
        close(result)
        return result.getOrThrow()
    }

    override fun toString(): String = asString {
        ::open to open
        ::name to name
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

        private val openLoggers: MutableMap<SimpleRenderingLogger, Array<StackTraceElement>> = synchronizedMapOf()

        /**
         * Sets the [open] state of the given [logger].
         *
         * Loggers that are not closed the moment this program shuts down
         * are considered broken and will inflict a warning.
         *
         * This behaviour can be disabled using [disabledUnclosedWarningLoggers].
         */
        private fun setOpen(logger: SimpleRenderingLogger, open: Boolean) {
            if (open) {
                openLoggers.getOrPut(logger) { currentStackTrace }
            } else {
                openLoggers.remove(logger)
            }
        }

        /**
         * Returns whether this [logger] is unclosed.
         */
        private fun isOpen(logger: SimpleRenderingLogger): Boolean = logger in openLoggers

        private val disabledUnclosedWarningLoggers: MutableSet<SimpleRenderingLogger> = synchronizedSetOf()

        /**
         * Disables the warning during program exit
         * that shows up if this logger's span was not closed.
         */
        public val <T : SimpleRenderingLogger> T.withUnclosedWarningDisabled: T
            get() = also { disabledUnclosedWarningLoggers.add(it) }

        init {
            onExit {
                val unclosed = openLoggers.filterKeys { logger -> logger !in disabledUnclosedWarningLoggers }

                if (unclosed.isNotEmpty()) {
                    println("${unclosed.size} started but unfinished renderer(s) found:".formattedAs.warning)
                    unclosed.forEach { (unclosedLogger: SimpleRenderingLogger, stackTrace: Array<StackTraceElement>) ->
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
