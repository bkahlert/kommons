package koodies.test.output

import koodies.logging.FixedWidthRenderingLogger.Border
import koodies.logging.InMemoryLogger

/**
 * Annotated instances of [InMemoryLogger] use the number of columns
 * specified by [value] instead of the default number.
 */
@Target(AnnotationTarget.VALUE_PARAMETER, AnnotationTarget.TYPE, AnnotationTarget.FUNCTION)
annotation class Columns(val value: Int)

interface InMemoryLoggerFactory {
    fun createLogger(customSuffix: String, border: Border? = null): InMemoryLogger
}
