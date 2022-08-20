package com.bkahlert.kommons.logging.logback

import com.bkahlert.kommons.logging.logback.support.SmartCapturedLog.Companion.loggedSoFar
import com.bkahlert.kommons.logging.logback.support.SmartCapturedOutput
import com.bkahlert.kommons.test.junit.SystemProperty
import com.bkahlert.kommons.test.logging.logback.AppenderTestPreset.Json
import com.bkahlert.kommons.test.logging.logback.LogbackConfiguration
import com.bkahlert.kommons.test.logging.logback.LogbackConfigurationExtension
import net.logstash.logback.argument.StructuredArguments
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.slf4j.LoggerFactory
import org.springframework.boot.test.system.SmartOutputCaptureExtension
import strikt.api.expectThat
import strikt.assertions.contains
import strikt.assertions.startsWith
import java.text.MessageFormat
import java.time.Duration
import java.time.Instant

@ExtendWith(SmartOutputCaptureExtension::class, LogbackConfigurationExtension::class)
class JsonLoggingTest {

    private val logger = LoggerFactory.getLogger(javaClass)

    @Test
    @LogbackConfiguration(console = Json, file = Json)
    fun should_log_message(output: SmartCapturedOutput) {
        logger.info("Info message")
        expectThat(output).contains("Info message")
        expectThat(loggedSoFar()).contains("Info message")
    }

    @Test
    @SystemProperty(name = "service.name", value = "any-service")
    @SystemProperty(name = "service.version", value = "any-version")
    @LogbackConfiguration(console = Json, file = Json)
    fun should_log_structured(output: SmartCapturedOutput) {
        logger.info("A {} call occurred", StructuredArguments.v("method", object : Any() {}.javaClass.enclosingMethod.name))
        listOf(output.assertThatMappedJSON(), loggedSoFar().assertThatMappedJSON()).forEach {
            it
                .containsKeys(*REQUIRED_FIELDS)
                .hasEntrySatisfying("timestamp") { entry ->
                    expectThat(
                        Duration.between(toInstant(checkNotNull(entry)), Instant.now()) < Duration.ofMinutes(1)
                    )
                }
                .containsEntry("level", "INFO")
                .containsEntry("service", "any-service")
                .containsEntry("version", "any-version")
                .containsEntry("logger", JsonLoggingTest::class.java.name)
                .containsEntry("message", "A should_log_structured call occurred")
                .containsEntry("method", "should_log_structured")
        }
    }

    @Test
    @LogbackConfiguration(console = Json, file = Json)
    fun should_log_superfluous_arguments(output: SmartCapturedOutput) {
        logger.warn("No placeholders", StructuredArguments.v("emoji", "🧩"))
        listOf(output.assertThatMappedJSON(), loggedSoFar().assertThatMappedJSON()).forEach {
            it
                .containsEntry("level", "WARN")
                .containsEntry("emoji", "🧩")
                .hasEntrySatisfying("message") { message -> expectThat(message.toString()).not { contains("🧩") } }
        }
    }

    @Test
    @LogbackConfiguration(console = Json, file = Json)
    fun should_not_log_indexed_placeholders(output: SmartCapturedOutput) {
        logger.warn("{0} {1} {2}", null, StructuredArguments.v("emoji", "🧩"))
        listOf(output.assertThatMappedJSON(), loggedSoFar().assertThatMappedJSON()).forEach {
            it.containsEntry("message", "{0} {1} {2}")
        }
        listOf(expectThat(output.out), expectThat(loggedSoFar())).forEach {
            it.not { contains("🧩") }
        }
    }

    @Test
    @LogbackConfiguration(console = Json, file = Json)
    fun should_log_exceptions(output: SmartCapturedOutput) {
        val message = "An exception was thrown"
        logger.error(message, StructuredArguments.v("emoji", "\ud83e\udde8"), "\ud83e\udde8", RuntimeException("luckily just a test"))
        listOf(output.assertThatMappedJSON(), loggedSoFar().assertThatMappedJSON()).forEach {
            it
                .containsKeys("timestamp", "level", "logger", "thread", "message", "stack-trace")
                .containsEntry("level", "ERROR")
                .containsEntry("logger", JsonLoggingTest::class.java.name)
                .containsEntry("message", message)
                .hasEntrySatisfying("stack-trace") { stacktrace ->
                    expectThat(stacktrace.toString())
                        .startsWith(
                            MessageFormat.format(
                                "java.lang.RuntimeException: luckily just a test\n\tat {0}.should_log_exceptions",
                                JsonLoggingTest::class.java.name
                            )
                        )
                }
        }
    }

    private fun toInstant(entry: Any): Instant {
        return Instant.parse(entry.toString().replace(' ', 'T') + 'Z')
    }

    companion object {
        val REQUIRED_FIELDS = arrayOf<String>(
            "timestamp", "level", "service", "version",
            "logger", "thread", "message"
        )
    }
}
