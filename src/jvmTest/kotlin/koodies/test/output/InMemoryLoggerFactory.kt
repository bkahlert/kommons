package koodies.test.output

import koodies.logging.FixedWidthRenderingLogger.Border
import koodies.logging.InMemoryLogger

interface InMemoryLoggerFactory {
    fun createLogger(customSuffix: String, border: Border? = null): InMemoryLogger
}
