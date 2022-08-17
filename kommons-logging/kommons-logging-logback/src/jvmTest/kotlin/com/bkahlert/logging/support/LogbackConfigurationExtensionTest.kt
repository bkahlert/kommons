package com.bkahlert.logging.support

import com.bkahlert.logging.logback.LogbackConfiguration
import com.bkahlert.logging.logback.LogbackConfiguration.Encoder.classic
import com.bkahlert.logging.logback.LogbackConfiguration.Encoder.none
import com.bkahlert.logging.logback.LogbackConfiguration.Encoder.preset
import com.bkahlert.logging.support.LogbackConfigurationExtension.LogbackTestConfiguration
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import strikt.api.expectThat
import strikt.assertions.hasEntry

@ExtendWith(LogbackConfigurationExtension::class)
class LogbackConfigurationExtensionTest {

    @Test
    @LogbackTestConfiguration(FILE_APPENDER = preset)
    fun should_leave_configuration_by_default() {
        expectThat(LogbackConfiguration.loggerContext.copyOfPropertyMap)
            .hasEntry(LogbackConfiguration.CONSOLE_APPENDER.name, preset.name)
            .hasEntry(LogbackConfiguration.FILE_APPENDER.name, preset.name)
    }

    @Test
    @LogbackTestConfiguration(CONSOLE_APPENDER = none, FILE_APPENDER = classic)
    fun should_apply_configuration() {
        expectThat(LogbackConfiguration.loggerContext.copyOfPropertyMap)
            .hasEntry(LogbackConfiguration.CONSOLE_APPENDER.name, none.name)
            .hasEntry(LogbackConfiguration.FILE_APPENDER.name, classic.name)
    }

    @Test
    @LogbackTestConfiguration fun should_log_plain_with_empty_annotation() {
        expectThat(LogbackConfiguration.loggerContext.copyOfPropertyMap)
            .hasEntry(LogbackConfiguration.CONSOLE_APPENDER.name, preset.name)
            .hasEntry(LogbackConfiguration.FILE_APPENDER.name, preset.name)
    }
}
