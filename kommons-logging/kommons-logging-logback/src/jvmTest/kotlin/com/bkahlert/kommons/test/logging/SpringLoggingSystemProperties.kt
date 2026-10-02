package com.bkahlert.kommons.test.logging

/**
 * Names of the system properties Spring Boot's `LoggingSystemProperty` writes.
 * Boot 4 has no String constants for them, and annotation arguments need constants.
 */
object SpringLoggingSystemProperties {
    const val LOG_FILE: String = "LOG_FILE"
    const val LOG_LEVEL_PATTERN: String = "LOG_LEVEL_PATTERN"
    const val LOG_DATEFORMAT_PATTERN: String = "LOG_DATEFORMAT_PATTERN"
    const val EXCEPTION_CONVERSION_WORD: String = "LOG_EXCEPTION_CONVERSION_WORD"
}
