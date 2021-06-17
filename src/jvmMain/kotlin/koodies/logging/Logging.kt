package koodies.logging

import koodies.logging.FixedWidthRenderingLogger.Border
import koodies.text.ANSI.FilteringFormatter
import koodies.text.ANSI.Formatter
import kotlin.contracts.InvocationKind.EXACTLY_ONCE
import kotlin.contracts.contract

@DslMarker
public annotation class RenderingLoggingDsl

public inline fun <R, L : SimpleRenderingLogger> L.applyLogging(crossinline block: L.() -> R): L {
    contract { callsInPlace(block, EXACTLY_ONCE) }
    return apply { runLogging(block) }
}

public inline fun <T : SimpleRenderingLogger, R> T.runLogging(crossinline block: T.() -> R): R {
    contract { callsInPlace(block, EXACTLY_ONCE) }

//    val result: Result<R> = runCatching { span.runExceptionRecording { block() } }
    val result: Result<R> = runCatching(block)

    return logResult(result)
}

/**
 * Logs the given [returnValue] as the value that is returned from the logging span.
 */
public inline fun <reified T : SimpleRenderingLogger> T.logReturnValue(returnValue: ReturnValue) {
    logResult(returnValue)
}

/**
 * Creates a logger which serves for logging a sub-process and all of its corresponding events.
 */
@RenderingLoggingDsl
public fun <R> logging(
    name: CharSequence,
    contentFormatter: FilteringFormatter? = null,
    decorationFormatter: Formatter? = null,
    returnValueFormatter: ((ReturnValue) -> ReturnValue)? = null,
    border: Border = BlockRenderingLogger.DEFAULT_BORDER,
    block: FixedWidthRenderingLogger.() -> R,
): R = SmartRenderingLogger(
    name,
    null,
    { LoggingContext.BACKGROUND.logText { it } },
    contentFormatter,
    decorationFormatter,
    returnValueFormatter,
    border,
    prefix = LoggingContext.BACKGROUND.prefix,
).runLogging(block)

/**
 * Logs [Unit], that is *no result*, as the result of the process this logger is used for.
 */
public inline fun <T : SimpleRenderingLogger, reified R> T.logResult(result: R): R = logResult(Result.success(result))

/**
 * Logs [Unit], that is *no result*, as the result of the process this logger is used for.
 */
@Suppress("NOTHING_TO_INLINE")
public inline fun <T : SimpleRenderingLogger> T.logResult(): Unit = logResult(Result.success(Unit))
