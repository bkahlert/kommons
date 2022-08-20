package com.bkahlert.kommons.logging.autoconfigure.logback

import com.bkahlert.kommons.autoconfigure.logback.LogConfiguringEnvironmentPostProcessor
import com.bkahlert.kommons.autoconfigure.logback.LogbackAutoConfiguration
import com.bkahlert.kommons.logback.LogbackConfiguration
import com.bkahlert.kommons.logging.autoconfigure.Properties
import org.assertj.core.api.Assertions
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assumptions
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.springframework.boot.actuate.logging.LogFileWebEndpoint
import org.springframework.boot.autoconfigure.EnableAutoConfiguration
import org.springframework.boot.builder.SpringApplicationBuilder
import org.springframework.boot.logging.LogFile
import org.springframework.context.ConfigurableApplicationContext
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import strikt.assertions.isNotNull
import java.util.concurrent.atomic.AtomicReference

/**
 * Integration tests to assert that Spring Boot considers the active log file
 * at the same location as it actually is.
 *
 *
 * Especially important for components like the Actuator [LogFileWebEndpoint].
 *
 * @author Björn Kahlert
 */
class LogFileWebEndpointIntegrationTest {

    @ParameterizedTest
    @CsvSource(
        "                   , custom-name.log  ",
        " custom-path       , custom-name.log  ",
        " /custom-path      , custom-name.log  ",
        " \${java.io.tmpdir} ,                  ",
        " \${java.io.tmpdir} , custom-name.log  ",
    )
    fun should_always_work_with_the_same_log_file(path: String, name: String) {
        System.setProperty("FILE_LOG_PRESET", "json")
        val context = startContextUsing(path, name)
        val activeLogFile = LogbackConfiguration.activeLogFileName
        expectThat(activeLogFile).isNotNull()
        expectThat(activeLogFile).isEqualTo(LogFile.get(context.environment).toString())
        val logFileWebEndpoint: LogFileWebEndpoint? = context.beanFactory.getBean(LogFileWebEndpoint::class.java)
        expectThat(logFileWebEndpoint).isNotNull()
        expectThat(activeLogFile).isEqualTo(logFileWebEndpoint?.logFile()?.filename)
    }

    @ParameterizedTest
    @CsvSource(
        "                   ,                  ",
        " custom-path       ,                  ",
        " /custom-path      ,                  ",
        "                   , /custom-name.log ",
        " custom-path       , /custom-name.log ",
        " /custom-path      , /custom-name.log ",
        " \${java.io.tmpdir} , /custom-name.log ",
    )
    fun should_always_work_with_the_same_log_file_if_log_file_creation_possible_on_system(path: String, name: String) {
        val context: ConfigurableApplicationContext? = assumeContextUsing(path, name)
        assertThat(LogbackConfiguration.activeLogFileName).isEqualTo(LogFile.get(context?.environment).toString())
    }

    @Configuration(proxyBeanMethods = false)
    @Import(LogConfiguringEnvironmentPostProcessor::class)
    @EnableAutoConfiguration(exclude = [LogbackAutoConfiguration::class])
    private class TestConfig

    companion object {
        fun startContextUsing(path: String, name: String): ConfigurableApplicationContext {
            val properties: Properties = Properties().loggingFilePath(path).loggingFileName(name).includeActuatorEndpoint("logfile").anyPort()
            return SpringApplicationBuilder(TestConfig::class.java).properties(properties).run()
        }

        fun assumeContextUsing(path: String, name: String): ConfigurableApplicationContext? {
            val context: AtomicReference<ConfigurableApplicationContext?> = AtomicReference<ConfigurableApplicationContext?>()
            Assumptions.assumeThat(Assertions.catchThrowable { context.set(startContextUsing(path, name)) })
                .doesNotThrowAnyException()
            return context.get()
        }
    }
}
