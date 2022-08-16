package com.bkahlert.logging.logback

import com.bkahlert.logging.logback.LogbackConfiguration.CONSOLE_APPENDER
import com.bkahlert.logging.logback.LogbackConfiguration.Encoder
import com.bkahlert.logging.logback.LogbackConfiguration.FILE_APPENDER
import com.bkahlert.logging.support.Assertions.containsOnlyClassicLogs
import com.bkahlert.logging.support.Assertions.containsOnlyPlainLogs
import com.bkahlert.logging.support.BuildInfoProperties
import com.bkahlert.logging.support.LogbackUtil
import com.bkahlert.logging.support.SmartCapturedLog
import com.bkahlert.logging.support.SmartCapturedLog.LoggingFromNowLog
import com.bkahlert.logging.support.SmartCapturedOutput
import de.dkb.api.systemproperties.SystemProperty
import de.dkb.api.systemproperties.SystemPropertyExtension
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.slf4j.LoggerFactory
import org.springframework.boot.test.system.SmartOutputCaptureExtension
import strikt.api.expectThat
import strikt.assertions.isEmpty
import strikt.assertions.isEqualTo

@ExtendWith(SmartOutputCaptureExtension::class, SystemPropertyExtension::class)
internal class LogbackConfigurationTest {

    @BeforeEach
    fun setUp() {
        LogbackUtil.fullyReset()
    }

    @Nested
    inner class UsingSetProperty {
        @Test
        fun should_log_plain_by_default(output: SmartCapturedOutput, logged: LoggingFromNowLog) {
            doLog()
            expectThat(CONSOLE_APPENDER.encoder).isEqualTo(Encoder.preset)
            expectThat(FILE_APPENDER.encoder).isEqualTo(Encoder.preset)
            expectThat(output.getOut(-1)).containsOnlyPlainLogs()
            expectThat(logged).isEmpty()
        }

        @Test
        fun should_support_undefining_value(output: SmartCapturedOutput, logged: SmartCapturedLog) {
            FILE_APPENDER.encoder = Encoder.json
            FILE_APPENDER.encoder = Encoder.preset
            doLog()
            expectThat(FILE_APPENDER.encoder).isEqualTo(Encoder.preset)
            expectThat(output.getOut(-1)).containsOnlyPlainLogs()
            expectThat(logged).isEmpty()
        }

        @Test
        fun should_apply_console_change(output: SmartCapturedOutput, logged: SmartCapturedLog) {
            CONSOLE_APPENDER.encoder = Encoder.none
            doLog()
            expectThat(CONSOLE_APPENDER.encoder).isEqualTo(Encoder.none)
            expectThat(output.getOut(-1)).isEmpty()
            expectThat(logged).isEmpty()
        }

        @Test
        fun should_apply_file_change(output: SmartCapturedOutput, logged: SmartCapturedLog) {
            FILE_APPENDER.encoder = Encoder.classic
            doLog()
            expectThat(FILE_APPENDER.encoder).isEqualTo(Encoder.classic)
            expectThat(output.getOut(-1)).containsOnlyPlainLogs()
            expectThat(logged).containsOnlyClassicLogs()
        }
    }

    @Nested
    inner class UsingCommandLineArguments {
        @BeforeEach fun setUp() {
            CONSOLE_APPENDER.encoder = Encoder.json
        }

        @Test
        fun should_use_build_artifact_as_default_name(output: SmartCapturedOutput) {
            doLog()
            output.assertThatMappedJSON().containsEntry(
                "service",
                BuildInfoProperties.load()?.artifact)
        }

        @Test
        @SystemProperty(name = "service.name", value = "☞ test name ☜")
        fun should_accept_name(output: SmartCapturedOutput) {
            doLog()
            output.assertThatMappedJSON().containsEntry("service", "☞ test name ☜")
        }

        @Test
        fun should_use_build_version_as_default_version(output: SmartCapturedOutput) {
            doLog()
            output.assertThatMappedJSON().containsEntry("version",
                BuildInfoProperties.load()?.version)
        }

        @Test
        @SystemProperty(name = "service.version", value = "1.2.3-build") fun should_accept_version(output: SmartCapturedOutput) {
            doLog()
            output.assertThatMappedJSON().containsEntry("version", "1.2.3-build")
        }
    }

    companion object {
        fun doLog() {
            LoggerFactory.getLogger(LogbackConfigurationTest::class.java).info("Test")
        }
    }
}
