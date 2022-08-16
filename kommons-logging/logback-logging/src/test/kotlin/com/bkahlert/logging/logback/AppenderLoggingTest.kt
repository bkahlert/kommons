package com.bkahlert.logging.logback

import com.bkahlert.logging.logback.LogbackConfiguration.Encoder
import com.bkahlert.logging.support.LogbackConfigurationExtension
import com.bkahlert.logging.support.LogbackConfigurationExtension.LogbackTestConfiguration
import com.bkahlert.logging.support.SmartCapturedLog.Companion.loggedSoFar
import com.bkahlert.logging.support.SmartCapturedOutput
import com.bkahlert.logging.support.containsExactlyOnce
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.slf4j.LoggerFactory
import org.springframework.boot.test.system.SmartOutputCaptureExtension
import strikt.api.expectThat
import strikt.assertions.endsWith
import strikt.assertions.isEmpty
import strikt.assertions.isEqualTo
import strikt.assertions.isNull
import strikt.assertions.startsWith

@ExtendWith(SmartOutputCaptureExtension::class, LogbackConfigurationExtension::class)
class AppenderLoggingTest {
    private val logger = LoggerFactory.getLogger(javaClass)

    @Test
    @LogbackTestConfiguration(CONSOLE_APPENDER = Encoder.json, FILE_APPENDER = Encoder.json)
    fun should_log_the_same_to_both(output: SmartCapturedOutput) {
        logger.info("Info message")
        expectThat(output.out).isEqualTo(loggedSoFar().content)
            .startsWith("{\"timestamp\":")
            .endsWith(",\"message\":\"Info message\"}" + "\n")
            .containsExactlyOnce("\n")
    }

    @Test
    @LogbackTestConfiguration(CONSOLE_APPENDER = Encoder.none, FILE_APPENDER = Encoder.json)
    fun should_log_only_to_file(output: SmartCapturedOutput) {
        logger.info("Info message")
        expectThat(loggedSoFar())
            .startsWith("{\"timestamp\":")
            .endsWith(",\"message\":\"Info message\"}" + "\n")
            .containsExactlyOnce("\n")
        expectThat(output.out).isEmpty()
    }

    @Test
    @LogbackTestConfiguration(CONSOLE_APPENDER = Encoder.json, FILE_APPENDER = Encoder.none)
    fun should_log_only_to_console(output: SmartCapturedOutput) {
        logger.info("Info message")
        expectThat(output.all)
            .startsWith("{\"timestamp\":")
            .endsWith(",\"message\":\"Info message\"}\n")
            .containsExactlyOnce("\n")
        expectThat(loggedSoFar().location).isNull()
    }

    @Test
    @LogbackTestConfiguration(CONSOLE_APPENDER = Encoder.none, FILE_APPENDER = Encoder.none)
    fun should_not_log(output: SmartCapturedOutput) {
        logger.info("Info message")
        expectThat(output.out).isEmpty()
        expectThat(loggedSoFar().location).isNull()
    }
}
