package koodies.logging

import com.github.ajalt.mordant.AnsiCode
import koodies.builder.BooleanBuilder.BooleanValue
import koodies.builder.BooleanBuilder.YesNo
import koodies.builder.BooleanBuilder.YesNo.Context
import koodies.builder.BuilderTemplate
import koodies.builder.context.CapturesMap
import koodies.builder.context.CapturingContext
import koodies.builder.context.SkippableCapturingBuilderInterface
import koodies.logging.LoggingOptions.Companion.LoggingOptionsContext
import koodies.nullable.invoke

/**
 * Logs like a [BlockRenderingLogger] unless only the result is logged.
 * In that case the result will be logged on the same line as the caption instead of a new one.
 */
public class SmartRenderingLogger(
    public val caption: CharSequence,
    override val bordered: Boolean,
    override val statusInformationColumn: Int = 100,
    override val statusInformationPadding: Int = 5,
    override val statusInformationColumns: Int = 45,
    public val parent: RenderingLogger?,
    public val blockRenderingLogger: () -> BlockRenderingLogger,
) : BorderedRenderingLogger {

    private var logged: Boolean = false

    override val prefix: String
        get() {
            logged = true
            return (logger as? BlockRenderingLogger)?.prefix ?: ""
        }

    private val logger: RenderingLogger by lazy {
        if (logged) blockRenderingLogger()
        else object : CompactRenderingLogger(caption) {
            override fun render(block: () -> CharSequence) {
                parent?.apply {
                    logLine(block)
                } ?: println(block())
            }
        }
    }

    override fun render(trailingNewline: Boolean, block: () -> CharSequence) {
        logger.render(trailingNewline, block)
    }

    override fun logText(block: () -> CharSequence) {
        logged = true
        logger.logText(block)
    }

    override fun logLine(block: () -> CharSequence) {
        logged = true
        logger.logLine(block)
    }

    override fun logStatus(items: List<HasStatus>, block: () -> CharSequence) {
        logged = true
        logger.logStatus(items, block)
    }

    override fun logException(block: () -> Throwable) {
        logged = true
        logger.logException(block)
    }

    override fun <R> logResult(block: () -> Result<R>): R {
        return logger.logResult(block)
    }
}

/**
 * Creates a logger which serves for logging a sub-process and all of its corresponding events.
 */
@RenderingLoggingDsl
public inline fun <reified T : MutedRenderingLogger, reified R> T.logging(
    caption: CharSequence,
    ansiCode: AnsiCode? = null,
    bordered: Boolean = (this as? BorderedRenderingLogger)?.bordered ?: false,
    crossinline block: T.() -> R,
): R = runLogging(block)

/**
 * Creates a logger which serves for logging a sub-process and all of its corresponding events.
 */
@RenderingLoggingDsl
public inline fun <reified T : BorderedRenderingLogger, reified R> T.logging(
    caption: CharSequence,
    ansiCode: AnsiCode? = null,
    bordered: Boolean = (this as? BorderedRenderingLogger)?.bordered ?: false,
    crossinline block: SmartRenderingLogger.() -> R,
): R = SmartRenderingLogger(
    caption = caption,
    bordered = bordered,
    statusInformationColumn = statusInformationColumn - prefix.length,
    statusInformationPadding = statusInformationPadding,
    statusInformationColumns = statusInformationColumns - prefix.length,
    parent = this,
) {
    BlockRenderingLogger(
        caption = caption,
        bordered = bordered,
        statusInformationColumn = statusInformationColumn - prefix.length,
        statusInformationPadding = statusInformationPadding,
        statusInformationColumns = statusInformationColumns - prefix.length,
    ) { output -> logText { ansiCode(output) } }
}.runLogging(block)

/**
 * Creates a logger which serves for logging a sub-process and all of its corresponding events.
 */
@RenderingLoggingDsl
public inline fun <reified T : RenderingLogger, reified R> T.logging(
    caption: CharSequence,
    ansiCode: AnsiCode? = null,
    bordered: Boolean = (this as? BorderedRenderingLogger)?.bordered ?: false,
    crossinline block: SmartRenderingLogger.() -> R,
): R = SmartRenderingLogger(caption = caption, bordered = bordered, parent = this) {
    BlockRenderingLogger(caption = caption, bordered = bordered) { output -> logText { ansiCode(output) } }
}.runLogging(block)

/**
 * Creates a logger which serves for logging a sub-process and all of its corresponding events.
 */
@RenderingLoggingDsl
public inline fun <reified R> logging(
    caption: CharSequence,
    ansiCode: AnsiCode? = null,
    bordered: Boolean = false,
    crossinline block: SmartRenderingLogger.() -> R,
): R = SmartRenderingLogger(caption = caption, bordered = bordered, parent = null) {
    BlockRenderingLogger(caption = caption, bordered = bordered)
}.runLogging(block)

/**
 * Creates a logger which serves for logging a sub-process and all of its corresponding events.
 */
@JvmName("nullableLogging")
@RenderingLoggingDsl
public inline fun <reified T : RenderingLogger?, reified R> T.logging(
    caption: CharSequence,
    ansiCode: AnsiCode? = null,
    bordered: Boolean = (this as? BorderedRenderingLogger)?.bordered ?: false,
    crossinline block: SmartRenderingLogger.() -> R,
): R =
    if (this is RenderingLogger) logging(caption, ansiCode, bordered, block)
    else koodies.logging.logging(caption, ansiCode, bordered, block)

public data class LoggingOptions(val caption: CharSequence, val ansiCode: AnsiCode? = null, val bordered: Boolean = false) {
    public companion object : BuilderTemplate<LoggingOptionsContext, LoggingOptions>() {

        public class LoggingOptionsContext(override val captures: CapturesMap) : CapturingContext() {
            public val caption: SkippableCapturingBuilderInterface<() -> String, String?> by builder<String>()
            public val ansiCode: SkippableCapturingBuilderInterface<() -> AnsiCode, AnsiCode?> by builder<AnsiCode>() default null
            public val border: SkippableCapturingBuilderInterface<Context.() -> BooleanValue, Boolean> by YesNo default false
        }

        override fun BuildContext.build(): LoggingOptions = ::LoggingOptionsContext {
            LoggingOptions(::caption.eval(), ::ansiCode.eval(), ::border.eval())
        }
    }
}
