package koodies.test.output

import koodies.logging.InMemoryLogger

interface InMemoryLoggerFactory {
    fun createLogger(customSuffix: String, bordered: Boolean = true): InMemoryLogger
}
