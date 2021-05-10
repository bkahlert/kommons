package koodies.logging

import koodies.logging.LoggingContext.Companion.BACKGROUND
import koodies.test.isVerbose
import koodies.test.output.testLocalLogger
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
    if (loggingRequestedByUser != false || isVerbose) logger else MutedRenderingLogger
