package koodies.logging

import koodies.logging.FixedWidthRenderingLogger.Border
import koodies.text.ANSI.Formatter
import koodies.tracing.runExceptionRecording
import kotlin.contracts.InvocationKind.EXACTLY_ONCE
import kotlin.contracts.contract

@DslMarker
public annotation class RenderingLoggingDsl

public inline fun <R, L : RenderingLogger> L.applyLogging(crossinline block: L.() -> R): L {
    contract { callsInPlace(block, EXACTLY_ONCE) }
    return apply { runLogging(block) }
}

public inline fun <T : RenderingLogger, R> T.runLogging(crossinline block: T.() -> R): R {
    contract { callsInPlace(block, EXACTLY_ONCE) }

    val result: Result<R> = runCatching { span.runExceptionRecording { block() } }

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
    null,
    { LoggingContext.BACKGROUND.logText { it } },
    contentFormatter,
    decorationFormatter,
    returnValueFormatter,
    border,
    prefix = LoggingContext.BACKGROUND.prefix,
).runLogging(block)
