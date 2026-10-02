package com.bkahlert.kommons.logging.logback

import com.bkahlert.kommons.logging.LoggingSystemProperties
import com.bkahlert.kommons.test.logging.SpringLoggingSystemProperties
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Isolated
import org.springframework.boot.logging.LoggingSystemProperty
import org.springframework.boot.logging.logback.RollingPolicySystemProperty

@Isolated
class LogbackSystemPropertiesTest {

    @Test fun clear_system_properties() {
        val names = LoggingSystemProperty.entries.map { it.environmentVariableName } +
            RollingPolicySystemProperty.entries.map { it.environmentVariableName } +
            listOf(LoggingSystemProperties.CONSOLE_LOG_PRESET, LoggingSystemProperties.FILE_LOG_PRESET, "LOGGED_APPLICATION_NAME")
        names.forEach { System.setProperty(it, "set-by-test") }

        Logback.clearSystemProperties()

        names.forEach { System.getProperty(it).shouldBeNull() }
    }

    @Test fun names_match_spring_boot() {
        SpringLoggingSystemProperties.LOG_FILE shouldBe LoggingSystemProperty.LOG_FILE.environmentVariableName
        SpringLoggingSystemProperties.LOG_LEVEL_PATTERN shouldBe LoggingSystemProperty.LEVEL_PATTERN.environmentVariableName
        SpringLoggingSystemProperties.LOG_DATEFORMAT_PATTERN shouldBe LoggingSystemProperty.DATEFORMAT_PATTERN.environmentVariableName
        SpringLoggingSystemProperties.EXCEPTION_CONVERSION_WORD shouldBe LoggingSystemProperty.EXCEPTION_CONVERSION_WORD.environmentVariableName
    }
}
