package koodies.logging

import com.github.ajalt.mordant.AnsiCode

/**
 * Logs like a [BlockRenderingLogger] unless only the result is logged.
 * In that case the result will be logged on the same line as the caption instead of a new one.
 */
class SmartRenderingLogger(
    val caption: CharSequence,
    override val bordered: Boolean,
    override val statusInformationColumn: Int = 100,
    override val statusInformationPadding: Int = 5,
    override val statusInformationColumns: Int = 45,
    val parent: RenderingLogger?,
    val blockRenderingLogger: () -> BlockRenderingLogger,
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
inline fun <reified R> Any?.logging(
    caption: CharSequence,
    ansiCode: AnsiCode? = null,
    bordered: Boolean = (this as? BorderedRenderingLogger)?.bordered ?: false,
    crossinline block: RenderingLogger.() -> R,
): R {
    val parent = this as? RenderingLogger
    val logger: RenderingLogger = when (this) {
        is MutedRenderingLogger -> this
        is BorderedRenderingLogger -> SmartRenderingLogger(
            caption = caption,
            bordered = bordered,
            statusInformationColumn = statusInformationColumn - prefix.length,
            statusInformationPadding = statusInformationPadding,
            statusInformationColumns = statusInformationColumns - prefix.length,
            parent = parent,
        ) { createBlockRenderingLogger(caption, bordered, ansiCode) }
        is RenderingLogger -> SmartRenderingLogger(
            caption = caption,
            bordered = bordered,
            parent = parent,
        ) { createBlockRenderingLogger(caption, bordered, ansiCode) }
        else -> SmartRenderingLogger(
            caption = caption, bordered = bordered, parent = null,
        ) { createBlockRenderingLogger(caption, bordered, ansiCode) }
    }
    val result: Result<R> = kotlin.runCatching { block(logger) }
    logger.logResult { result }
    return result.getOrThrow()
}

@RenderingLoggingDsl
inline fun <reified R> RenderingLogger.logging2(
    caption: CharSequence,
    ansiCode: AnsiCode? = null,
    bordered: Boolean = (this as? BorderedRenderingLogger)?.bordered ?: false,
    crossinline block: RenderingLogger.() -> R,
): R {
    val parent = this as? RenderingLogger
    val logger: RenderingLogger = when (this) {
        is MutedRenderingLogger -> this
        is BorderedRenderingLogger -> SmartRenderingLogger(
            caption = caption,
            bordered = bordered,
            statusInformationColumn = statusInformationColumn - prefix.length,
            statusInformationPadding = statusInformationPadding,
            statusInformationColumns = statusInformationColumns - prefix.length,
            parent = parent,
        ) { createBlockRenderingLogger(caption, bordered, ansiCode) }
        is RenderingLogger -> SmartRenderingLogger(
            caption = caption,
            bordered = bordered,
            parent = parent,
        ) { createBlockRenderingLogger(caption, bordered, ansiCode) }
        else -> SmartRenderingLogger(
            caption = caption, bordered = bordered, parent = null,
        ) { createBlockRenderingLogger(caption, bordered, ansiCode) }
    }
    val result: Result<R> = kotlin.runCatching { block(logger) }
    logger.logResult { result }
    return result.getOrThrow()
}


@RenderingLoggingDsl
inline fun <reified R> logging2(
    caption: CharSequence,
    ansiCode: AnsiCode? = null,
    bordered: Boolean = false,
    crossinline block: RenderingLogger.() -> R,
): R {
    val logger = SmartRenderingLogger(
        caption = caption, bordered = bordered, parent = null,
    ) { createBlockRenderingLogger2(caption, bordered, ansiCode) }
    val result: Result<R> = runCatching { block(logger) }
    logger.logResult { result }
    return result.getOrThrow()
}
