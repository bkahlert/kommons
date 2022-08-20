package com.bkahlert.kommons.logging.logback

import com.bkahlert.kommons.logging.logback.Appender.Console
import com.bkahlert.kommons.logging.logback.Appender.File
import com.bkahlert.kommons.logging.logback.AppenderPreset.Default
import com.bkahlert.kommons.logging.logback.AppenderPreset.Json
import com.bkahlert.kommons.logging.logback.AppenderPreset.Minimal
import com.bkahlert.kommons.logging.logback.AppenderPreset.Off
import com.bkahlert.kommons.logging.logback.support.Assertions.containsOnlyClassicLogs
import com.bkahlert.kommons.logging.logback.support.Assertions.containsOnlyPlainLogs
import com.bkahlert.kommons.logging.logback.support.SmartCapturedLog
import com.bkahlert.kommons.logging.logback.support.SmartCapturedLog.LoggingFromNowLog
import com.bkahlert.kommons.logging.logback.support.SmartCapturedOutput
import com.bkahlert.kommons.test.junit.SystemProperty
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.slf4j.LoggerFactory
import org.springframework.boot.test.system.SmartOutputCaptureExtension
import strikt.api.expectThat
import strikt.assertions.isEmpty
import strikt.assertions.isEqualTo

@ExtendWith(SmartOutputCaptureExtension::class)
internal class LogbackConfigurationOldTest {

    @BeforeEach
    fun setUp() {
        Logback.fullyReset()
    }

    @Nested
    inner class UsingSetProperty {
        @Test
        fun should_log_plain_by_default(output: SmartCapturedOutput, logged: LoggingFromNowLog) {
            doLog()
            expectThat(Console.preset).isEqualTo(Default)
            expectThat(File.preset).isEqualTo(Default)
            expectThat(output.getOut(-1)).containsOnlyPlainLogs()
            expectThat(logged).isEmpty()
        }

        @Test
        fun should_support_undefining_value(output: SmartCapturedOutput, logged: SmartCapturedLog) {
            File.preset = Json
            File.preset = Default
            doLog()
            expectThat(File.preset).isEqualTo(Default)
            expectThat(output.getOut(-1)).containsOnlyPlainLogs()
            expectThat(logged).isEmpty()
        }

        @Test
        fun should_apply_console_change(output: SmartCapturedOutput, logged: SmartCapturedLog) {
            Console.preset = Off
            doLog()
            expectThat(Console.preset).isEqualTo(Off)
            expectThat(output.getOut(-1)).isEmpty()
            expectThat(logged).isEmpty()
        }

        @Test
        fun should_apply_file_change(output: SmartCapturedOutput, logged: SmartCapturedLog) {
            File.preset = Minimal
            doLog()
            expectThat(File.preset).isEqualTo(Minimal)
            expectThat(output.getOut(-1)).containsOnlyPlainLogs()
            expectThat(logged).containsOnlyClassicLogs()
        }
    }

    @Nested
    inner class UsingCommandLineArguments {
        @BeforeEach fun setUp() {
            Console.preset = Json
        }

        @Test
        fun should_use_build_artifact_as_default_name(output: SmartCapturedOutput) {
            doLog()
            output.assertThatMappedJSON().containsEntry(
                "service",
                "REMOVE"
            )
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
            output.assertThatMappedJSON().containsEntry(
                "version",
                "REMOVE"
            )
        }

        @Test
        @SystemProperty(name = "service.version", value = "1.2.3-build") fun should_accept_version(output: SmartCapturedOutput) {
            doLog()
            output.assertThatMappedJSON().containsEntry("version", "1.2.3-build")
        }
    }

    companion object {
        fun doLog() {
            LoggerFactory.getLogger(LogbackConfigurationOldTest::class.java).info("Test")
        }
    }
}
