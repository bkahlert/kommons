package com.bkahlert.logging.logback

import com.bkahlert.logging.logback.PlainLoggingTest.Companion.DATETIME_PATTERN
import com.bkahlert.logging.logback.PlainLoggingTest.Companion.SPACE
import com.bkahlert.logging.logback.PlainLoggingTest.Companion.plus
import com.bkahlert.logging.logback.PlainLoggingTest.Companion.q
import com.bkahlert.logging.support.LogbackUtil
import com.bkahlert.logging.support.SmartCapturedOutput
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
        LogbackUtil.fullyReset()
    }

    @Test
    fun should_log_message(output: SmartCapturedOutput) {
        LogbackUtil.loadConfiguration(EXTERNAL_CONFIGURATION)
        LoggerFactory.getLogger("MyLogger").info(LOG_MESSAGE)
        expectThat(output.all).contains(LOG_MESSAGE)
    }

    @Test
    fun should_use_plain_encoder_by_default(output: SmartCapturedOutput) {
        LogbackUtil.loadConfiguration(EXTERNAL_CONFIGURATION)
        LoggerFactory.getLogger("MyLogger").info(LOG_MESSAGE)
        expectThat(output.getOut(-1))
            .matches(DATETIME_PATTERN + SPACE + q("INFO - --- [sealone-adapter,1.2.3] [-,-,-] [")
                + SPACE + q("main] MyLogger") + SPACE + q(": Test log message via SLF4J"))
    }

    companion object {
        const val LOG_MESSAGE = "Test log message via SLF4J"
        val EXTERNAL_CONFIGURATION = Stream.of(
            "<configuration debug=\"true\">",
            "    <property name=\"CONSOLE_APPENDER\" value=\"plain\"/>",
            "    <property name=\"FILE_APPENDER\" value=\"none\"/>",
            "    <property scope=\"context\" name=\"service.name\" value=\"sealone-adapter\"/>",
            "    <property scope=\"context\" name=\"service.version\" value=\"1.2.3\"/>",
            "    <include resource=\"com/bkahlert/logging/base.xml\"/>",
            "    <logger name=\"de.lbb.kkb.sealone\" additivity=\"false\" level=\"debug\" />",
            "</configuration>").collect(Collectors.joining("\n"))
    }
}
