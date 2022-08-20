package com.bkahlert.kommons.logging.logback

import com.bkahlert.kommons.logging.logback.support.SmartCapturedOutput
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.slf4j.LoggerFactory
import org.springframework.boot.test.system.SmartOutputCaptureExtension
import strikt.api.expectThat
import strikt.assertions.contains
import strikt.assertions.matches
import java.util.stream.Collectors
import java.util.stream.Stream

@ExtendWith(SmartOutputCaptureExtension::class)
class ExternalLogbackConfigurationLoggingTest {
    private val logger = LoggerFactory.getLogger(javaClass)

    @AfterEach
    fun tearDown() {
        Logback.fullyReset()
    }

    @Test
    fun should_log_message(output: SmartCapturedOutput) {
        Logback.loadConfiguration(EXTERNAL_CONFIGURATION)
        LoggerFactory.getLogger("MyLogger").info(LOG_MESSAGE)
        expectThat(output.all).contains(LOG_MESSAGE)
    }

    @Test
    fun should_use_plain_encoder_by_default(output: SmartCapturedOutput) {
        Logback.loadConfiguration(EXTERNAL_CONFIGURATION)
        LoggerFactory.getLogger("MyLogger").info(LOG_MESSAGE)
        expectThat(output.getOut(-1))
            .matches(TODO())
    }

    companion object {
        const val LOG_MESSAGE = "Test log message via SLF4J"
        val EXTERNAL_CONFIGURATION = Stream.of(
            "<configuration debug=\"true\">",
            "    <property name=\"CONSOLE_CONFIGURATION_PRESET\" value=\"plain\"/>",
            "    <property name=\"FILE_CONFIGURATION_PRESET\" value=\"none\"/>",
            "    <property scope=\"context\" name=\"service.name\" value=\"sealone-adapter\"/>",
            "    <property scope=\"context\" name=\"service.version\" value=\"1.2.3\"/>",
            "    <include resource=\"com/bkahlert/kommons/logging/logback/base.xml\"/>",
            "    <logger name=\"de.lbb.sealone\" additivity=\"false\" level=\"debug\" />",
            "</configuration>"
        ).collect(Collectors.joining("\n"))
    }
}
