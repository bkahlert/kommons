package koodies.logging

import koodies.exception.toCompactString
import koodies.logging.RenderingLogger.Companion.formatException
import koodies.terminal.AnsiFormats.bold
import kotlin.properties.Delegates.vetoable
import kotlin.reflect.KProperty

abstract class CompactRenderingLogger(caption: CharSequence) : RenderingLogger {
    init {
        require(caption.isNotBlank()) { "No blank caption allowed." }
    }

    var strings: List<String>? by vetoable(listOf(caption.bold()),
        onChange = { _: KProperty<*>, oldValue: List<String>?, _: List<String>? -> oldValue != null })

    abstract fun render(block: () -> CharSequence)

    override fun render(trailingNewline: Boolean, block: () -> CharSequence) {
        strings = strings?.plus("${block()}")
    }

    override fun logStatus(items: List<HasStatus>, block: () -> CharSequence) {
        strings = strings?.plus(block().lines().joinToString(", "))
        if (items.isNotEmpty()) strings =
            strings?.plus(items.renderStatus().lines().joinToString(", ", "(", ")"))
    }

    override fun logException(block: () -> Throwable) {
        strings = strings?.plus(formatException(" ", block().toCompactString()))
    }

    override fun <R> logResult(block: () -> Result<R>): R {
        val returnValue = super.logResult(block)
        render { strings?.joinToString(" ") ?: "" }
        return returnValue
    }
}

/**
 * Creates a logger which serves for logging a sub-process and all of its corresponding events.
 *
 * This logger logs all events using a single line of text. If more room is needed [blockLogging] is more suitable.
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
    return logger.runLogging(block)
}

/**
 * Creates a logger which serves for logging a very short sub-process and all of its corresponding events.
 *
 * This logger logs all events using only a couple of characters. If more room is needed [compactLogging] or even [blockLogging] is more suitable.
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
    val result: Result<R> = kotlin.runCatching { block(logger) }
    logger.logResult { result }
    return result.getOrThrow()
}
