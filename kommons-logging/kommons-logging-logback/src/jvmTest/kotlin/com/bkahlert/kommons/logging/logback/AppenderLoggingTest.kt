package com.bkahlert.kommons.logging.logback

import com.bkahlert.kommons.logging.logback.support.SmartCapturedLog.Companion.loggedSoFar
import com.bkahlert.kommons.logging.logback.support.SmartCapturedOutput
import com.bkahlert.kommons.logging.logback.support.containsExactlyOnce
import com.bkahlert.kommons.test.logging.logback.AppenderTestPreset.Json
import com.bkahlert.kommons.test.logging.logback.AppenderTestPreset.Off
import com.bkahlert.kommons.test.logging.logback.LogbackConfiguration
import com.bkahlert.kommons.test.logging.logback.LogbackConfigurationExtension
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
    @LogbackConfiguration(console = Json, file = Json)
    fun should_log_the_same_to_both(output: SmartCapturedOutput) {
        logger.info("Info message")
        expectThat(output.out).isEqualTo(loggedSoFar().content)
            .startsWith("{\"timestamp\":")
            .endsWith(",\"message\":\"Info message\"}" + "\n")
            .containsExactlyOnce("\n")
    }

    @Test
    @LogbackConfiguration(console = Off, file = Json)
    fun should_log_only_to_file(output: SmartCapturedOutput) {
        logger.info("Info message")
        expectThat(loggedSoFar())
            .startsWith("{\"timestamp\":")
            .endsWith(",\"message\":\"Info message\"}" + "\n")
            .containsExactlyOnce("\n")
        expectThat(output.out).isEmpty()
    }

    @Test
    @LogbackConfiguration(console = Json, file = Off)
    fun should_log_only_to_console(output: SmartCapturedOutput) {
        logger.info("Info message")
        expectThat(output.all)
            .startsWith("{\"timestamp\":")
            .endsWith(",\"message\":\"Info message\"}\n")
            .containsExactlyOnce("\n")
        expectThat(loggedSoFar().location).isNull()
    }

    @Test
    @LogbackConfiguration(console = Off, file = Off)
    fun should_not_log(output: SmartCapturedOutput) {
        logger.info("Info message")
        expectThat(output.out).isEmpty()
        expectThat(loggedSoFar().location).isNull()
    }
}
