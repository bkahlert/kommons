package com.bkahlert.logging.logback

import ch.qos.logback.classic.Level
import com.bkahlert.logging.logback.LogbackConfiguration.Encoder.minimal
import com.bkahlert.logging.logback.PlainLoggingTest.Companion.plus
import com.bkahlert.logging.support.LogbackConfigurationExtension
import com.bkahlert.logging.support.LogbackConfigurationExtension.LogbackTestConfiguration
import com.bkahlert.logging.support.LogbackUtil
import com.bkahlert.logging.support.SmartCapturedLog
import com.bkahlert.logging.support.SmartCapturedOutput
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import org.slf4j.LoggerFactory
import org.springframework.boot.test.system.SmartOutputCaptureExtension
import strikt.api.expectThat
import strikt.assertions.contains
import strikt.assertions.matches
import java.util.regex.Pattern
import java.util.stream.Stream

@ExtendWith(SmartOutputCaptureExtension::class, LogbackConfigurationExtension::class)
internal class MinimalLoggingTest {
    private val logger = LoggerFactory.getLogger(javaClass)

    @Test
    fun should_use_working_datetime_pattern() {
        expectThat("29:39.725").matches(DATETIME_PATTERN)
    }

    @ParameterizedTest
    @MethodSource("logbackLogLevels")
    @LogbackTestConfiguration(CONSOLE_APPENDER = minimal, FILE_APPENDER = minimal)
    fun should_log_message_with_all_log_levels(level: Level, output: SmartCapturedOutput, logged: SmartCapturedLog) {
        LogbackUtil.addLoggingEvent(MinimalLoggingTest::class.java, level, LOG_MESSAGE)
        listOf(
            expectThat(output.getOut(-1)),
            expectThat(logged.line(-1)),
        ).forEach {
            it.contains(LOG_MESSAGE)
            it.contains(level.toString())
        }
    }

    @Test
    @LogbackTestConfiguration(CONSOLE_APPENDER = minimal, FILE_APPENDER = minimal)
    fun should_log_only_minutes_seconds_milliseconds_level_logger_and_message(output: SmartCapturedOutput, logged: SmartCapturedLog) {
        LoggerFactory.getLogger("MyLogger").info(LOG_MESSAGE)
        listOf(
            expectThat(output.getOut(-1)),
            expectThat(logged.line(-1)),
        ).forEach {
            it.matches(
                DATETIME_PATTERN + SPACE + q("INFO") + SPACE + q("MyLogger") + SPACE + q(": Test log message via SLF4J")
            )
        }
    }

    @Test
    @LogbackTestConfiguration(CONSOLE_APPENDER = minimal, FILE_APPENDER = minimal)
    fun should_shorten_logger(
        output: SmartCapturedOutput,
        logged: SmartCapturedLog,
    ) {
        LoggerFactory.getLogger("tld.domain.subdomain.service.package.package.package.MyMediumSizeLoggerName").info(LOG_MESSAGE)
        listOf(
            expectThat(output.getOut(-1)),
            expectThat(logged.line(-1)),
        ).forEach {
            it.contains("s.p.p.p.MyMediumSizeLoggerName").not { contains(".s.p.p.p.MyMediumSizeLoggerName") }
        }
    }

    companion object {
        const val LOG_MESSAGE = "Test log message via SLF4J"
        val DATETIME_PATTERN = "\\d{2}:\\d{2}.\\d{3}".toRegex()
        val SPACE = "\\s+".toRegex()

        @JvmStatic
        fun logbackLogLevels(): Stream<Level> {
            return Stream.of<Level>(Level.TRACE, Level.DEBUG, Level.INFO, Level.WARN, Level.ERROR)
        }

        fun q(quote: String?): Regex {
            return Pattern.quote(quote).toRegex()
        }
    }
}
