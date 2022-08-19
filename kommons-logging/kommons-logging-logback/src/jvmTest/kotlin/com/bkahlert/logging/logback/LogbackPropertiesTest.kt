package com.bkahlert.logging.logback

import ch.qos.logback.classic.Level
import com.bkahlert.kommons.test.junit.SystemProperty
import com.bkahlert.logging.logback.LogbackConfiguration.Encoder
import com.bkahlert.logging.logback.LogbackConfiguration.Encoder.classic
import com.bkahlert.logging.logback.LogbackConfiguration.Encoder.plain
import com.bkahlert.logging.logback.LogbackConfiguration.Encoder.spring
import com.bkahlert.logging.logback.StructuredArguments.v
import com.bkahlert.logging.support.LogbackConfigurationExtension.LogbackTestConfiguration
import com.bkahlert.logging.support.SmartCapturedLog
import com.bkahlert.logging.support.SmartCapturedOutput
import io.kotest.matchers.string.shouldMatch
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.slf4j.LoggerFactory
import org.slf4j.MDC
import org.springframework.boot.test.system.CapturedOutput
import org.springframework.boot.test.system.OutputCaptureExtension
import org.springframework.boot.test.system.SmartOutputCaptureExtension
import strikt.api.expectThat
import strikt.assertions.matches
import java.util.regex.Pattern
import java.util.stream.Stream

@ExtendWith(OutputCaptureExtension::class)
annotation class Captured

// TODO  %clr([%mdc])
class LogbackPropertiesTest {

    @BeforeEach
    fun setUp() {
        MDC.put("foo", "bar")
        MDC.put("baz", null)
    }

    @LogbackTestConfiguration(CONSOLE_APPENDER = Encoder.minimal)
    @Test fun minimal(@Captured output: CapturedOutput) {
        LoggerFactory.getLogger("TestLogger").info("message with {}", v("key", "value"))
        output.out shouldMatch Regex(
            """
            \d{2}:\d{2}.\d{3} {2}INFO TestLogger {21}: message with value\n
            """.trimIndent()
        )
    }

    @LogbackTestConfiguration(CONSOLE_APPENDER = spring)
    @Test fun spring(@Captured output: CapturedOutput) {
        LoggerFactory.getLogger("TestLogger").info("message with {}", v("key", "value"))
        output.out shouldMatch Regex(
            """
            \d{4}-\d{2}-\d{2} \d{2}:\d{2}:\d{2}.\d{3} {2}INFO {3}--- \[.*] TestLogger {30} : message with value\n
            """.trimIndent()
        )
    }

    @LogbackTestConfiguration(CONSOLE_APPENDER = Encoder.json)
    @Test fun json(@Captured output: CapturedOutput) {
        LoggerFactory.getLogger("TestLogger").info("message with {}", v("key", "value"))
        @Suppress("LongLine")
        output.out shouldMatch Regex(
            """
            \{"@timestamp":"\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}.\d{3}[+-]\d{2}:\d{2}","level":"INFO","thread_name":".+","logger_name":"TestLogger","message":"message with value","key":"value","foo":"bar"}\n
            """.trimIndent()
        )
    }

    @Disabled
    @LogbackTestConfiguration(CONSOLE_APPENDER = classic)
    @Test fun classic() {
        LoggerFactory.getLogger("TestLogger").info("message with {}", v("key", "value"))
        expectThat("2020-04-02 14:29:39.725").matches(DATETIME_PATTERN)
    }

    @Disabled
    @LogbackTestConfiguration(CONSOLE_APPENDER = plain)
    @Test fun plain() {
        LoggerFactory.getLogger("TestLogger").info("message with {}", v("key", "value"))
        expectThat("2020-04-02 14:29:39.725").matches(DATETIME_PATTERN)
    }

    @Disabled
    @LogbackTestConfiguration(CONSOLE_APPENDER = Encoder.none)
    @Test fun none() {
        LoggerFactory.getLogger("TestLogger").info("message with {}", v("key", "value"))
        expectThat("2020-04-02 14:29:39.725").matches(DATETIME_PATTERN)
    }

    @Disabled
    @Test
    @ExtendWith(SmartOutputCaptureExtension::class)
    @SystemProperty(name = "build.version", value = "1.5.0-SNAPSHOT")
    @LogbackTestConfiguration(CONSOLE_APPENDER = plain, FILE_APPENDER = plain)
    fun should_use_plain_encoder_by_default(output: SmartCapturedOutput, logged: SmartCapturedLog) {
        LoggerFactory.getLogger("MyLogger").info(LOG_MESSAGE)
        listOf(
            expectThat(output.getOut(-1)),
            expectThat(logged.line(-1)),
        ).forEach {
            it.matches(
                DATETIME_PATTERN + SPACE + q("INFO - --- [common-logging-core,1.5.0-SNAPSHOT] [-,-,-] [")
                    + SPACE + q("main] MyLogger") + SPACE + q(": Test log message via SLF4J")
            )
        }
    }

    companion object {
        const val LOG_MESSAGE = "Test log message via SLF4J"
        val DATETIME_PATTERN = "\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}.\\d{3}".toRegex()
        val SPACE = "\\s+".toRegex()

        @JvmStatic
        fun logbackLogLevels(): Stream<Level> {
            return Stream.of(Level.TRACE, Level.DEBUG, Level.INFO, Level.WARN, Level.ERROR)
        }

        fun q(quote: String?): Regex = Pattern.quote(quote).toRegex()
        operator fun Regex.plus(other: Regex): Regex = Regex(pattern + other.pattern)
    }
}
