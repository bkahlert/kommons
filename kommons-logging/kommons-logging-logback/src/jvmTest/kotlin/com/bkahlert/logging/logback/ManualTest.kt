package com.bkahlert.logging.logback

import ch.qos.logback.classic.Level
import com.bkahlert.kommons.test.junit.SystemProperty
import com.bkahlert.logging.logback.LogbackConfiguration.Encoder.plain
import com.bkahlert.logging.support.LogbackConfigurationExtension
import com.bkahlert.logging.support.LogbackConfigurationExtension.LogbackTestConfiguration
import com.bkahlert.logging.support.SmartCapturedLog
import com.bkahlert.logging.support.SmartCapturedOutput
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.slf4j.LoggerFactory
import org.springframework.boot.test.system.SmartOutputCaptureExtension
import strikt.api.expectThat
import strikt.assertions.matches
import java.util.regex.Pattern
import java.util.stream.Stream

@ExtendWith(SmartOutputCaptureExtension::class, LogbackConfigurationExtension::class)
class ManualTest {

    // TODO prefix all environment variables CONFIG_PATH
    // TODO custom CONFIG_PATH
    // TODO default CONSOLE_APPENDER preset = plain
    // TODO default FILE_APPENDER preset = plain

    @Test
    @SystemProperty(name = "service.version", value = "1.5.0-SNAPSHOT")
    @SystemProperty(name = "CONSOLE_APPENDER", value = "classic")
    @SystemProperty(name = "FILE_APPENDER", value = "classic")
    @LogbackTestConfiguration(CONSOLE_APPENDER = plain, FILE_APPENDER = plain)
    fun `should apply environment variables`(output: SmartCapturedOutput, logged: SmartCapturedLog) {
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
