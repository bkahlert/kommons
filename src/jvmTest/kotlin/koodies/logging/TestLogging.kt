package koodies.logging

import koodies.logging.LoggingContext.Companion.BACKGROUND
import koodies.test.isVerbose
import koodies.test.output.testLocalLogger
import koodies.text.ANSI.ansiRemoved
import koodies.text.LineSeparators.LF
import koodies.text.Semantics.formattedAs
import org.junit.jupiter.api.extension.ExtensionContext

/**
 * Returns the provided [testLocalLogger] that if [loggingRequestedByUser] or
 * **if this is the only test running**.
 *
 * If no [logger] is explicitly set the [testLocalLogger] is used
 * if one exists. If that is not the case [BACKGROUND] is used.
 */
fun ExtensionContext.conditionallyVerboseLogger(
    loggingRequestedByUser: Boolean? = false,
    logger: FixedWidthRenderingLogger = BACKGROUND,
): FixedWidthRenderingLogger =
    if (loggingRequestedByUser == true || isVerbose) logger else MutedRenderingLogger

/**
 * Runs the specified [block] with this [RenderingLogger] and returns the intercepted
 * log messages.
 */
fun InMemoryLogger.capturing(block: (SimpleRenderingLogger) -> Unit): String {
    val captured = StringBuilder()
    InMemoryLogger("capturing ${name.formattedAs.input}", this) { logText { it }; captured.append(it) }.applyLogging(block)
    return captured.ansiRemoved.lines().drop(2).dropLast(2).joinToString(LF) { it.removePrefix("│   ") }
}
