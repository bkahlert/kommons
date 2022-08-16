package com.bkahlert.logging.logback

import ch.qos.logback.classic.Level
import com.bkahlert.logging.logback.LogbackConfiguration.Encoder.plain
import com.bkahlert.logging.support.LogbackConfigurationExtension
import com.bkahlert.logging.support.LogbackConfigurationExtension.LogbackTestConfiguration
import com.bkahlert.logging.support.LogbackUtil
import com.bkahlert.logging.support.SmartCapturedLog
import com.bkahlert.logging.support.SmartCapturedOutput
import de.dkb.api.systemproperties.SystemProperty
import de.dkb.api.systemproperties.SystemPropertyExtension
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

@ExtendWith(SmartOutputCaptureExtension::class, SystemPropertyExtension::class, LogbackConfigurationExtension::class)
class PlainLoggingTest {
    private val logger = LoggerFactory.getLogger(javaClass)
    @Test fun should_use_working_datetime_pattern() {
        expectThat("2020-04-02 14:29:39.725").matches(DATETIME_PATTERN)
    }

    @ParameterizedTest
    @MethodSource("logbackLogLevels")
    @LogbackTestConfiguration(CONSOLE_APPENDER = plain, FILE_APPENDER = plain)
    fun should_log_message_with_all_log_levels(level: Level, output: SmartCapturedOutput, logged: SmartCapturedLog) {
        LogbackUtil.addLoggingEvent(PlainLoggingTest::class.java, level, LOG_MESSAGE)
        listOf(
            expectThat(output.getOut(-1)),
            expectThat(logged.line(-1)),
        ).forEach {
            it.contains(LOG_MESSAGE)
            it.contains(level.toString())
        }
    }

    @Test
    @SystemProperty(name = "service.version", value = "1.5.0-SNAPSHOT")
    @LogbackTestConfiguration(CONSOLE_APPENDER = plain, FILE_APPENDER = plain)
    fun should_use_plain_encoder_by_default(output: SmartCapturedOutput, logged: SmartCapturedLog) {
        LoggerFactory.getLogger("MyLogger").info(LOG_MESSAGE)
        listOf(
            expectThat(output.getOut(-1)),
            expectThat(logged.line(-1)),
        ).forEach {
            it.matches(
                DATETIME_PATTERN + SPACE + q("INFO - --- [common-logging-core,1.5.0-SNAPSHOT] [-,-,-] [")
                    + SPACE + q("main] MyLogger") + SPACE + q(": Test log message via SLF4J"))
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
