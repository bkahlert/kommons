package com.bkahlert.kommons.logging.logback

import com.bkahlert.kommons.SystemLocations
import com.bkahlert.kommons.logging.LogPresets
import com.bkahlert.kommons.logging.LoggingSystemProperties.CONSOLE_LOG_PRESET
import com.bkahlert.kommons.logging.LoggingSystemProperties.FILE_LOG_PRESET
import com.bkahlert.kommons.logging.logback.StructuredArguments.v
import com.bkahlert.kommons.randomString
import com.bkahlert.kommons.test.junit.SystemProperty
import com.bkahlert.kommons.test.logging.PrintedLogEntry
import com.bkahlert.kommons.test.logging.allLogs
import com.bkahlert.kommons.test.logging.lastLog
import com.bkahlert.kommons.test.logging.logback.LogFile
import com.bkahlert.kommons.test.logging.logback.LogbackConfiguration
import com.bkahlert.kommons.test.spring.Captured
import com.bkahlert.kommons.test.testAll
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldMatch
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.slf4j.LoggerFactory
import org.slf4j.MDC
import org.springframework.boot.logging.LoggingSystemProperties.EXCEPTION_CONVERSION_WORD
import org.springframework.boot.logging.LoggingSystemProperties.LOG_DATEFORMAT_PATTERN
import org.springframework.boot.logging.LoggingSystemProperties.LOG_FILE
import org.springframework.boot.logging.LoggingSystemProperties.LOG_LEVEL_PATTERN
import org.springframework.boot.test.system.CapturedOutput
import java.nio.file.Path
import kotlin.io.path.deleteIfExists

class AppenderPresetTest {

    @BeforeEach
    fun setUp() {
        Logback.reset()
        MDC.put("foo", "bar")
        MDC.put("baz", null)
    }

    @AfterAll
    fun tearDown() {
        SystemLocations.Temp.resolve(CUSTOM_LOG_FILE_NAME).deleteIfExists()
    }

    @SystemProperty(FILE_LOG_PRESET, LogPresets.OFF_PRESET)
    @Nested inner class ConsoleLogProperties {

        @SystemProperty(CONSOLE_LOG_PRESET, LogPresets.DEFAULT_PRESET)
        @Nested inner class UsingDefaultPreset {

            @Test fun default(@Captured output: CapturedOutput) {
                UsingSpringPreset().default(output)
            }

            @SystemProperty(LOG_DATEFORMAT_PATTERN, "yyyy-MM-dd")
            @SystemProperty(LOG_LEVEL_PATTERN, "%.-1p")
            @SystemProperty(EXCEPTION_CONVERSION_WORD, "%ex{short}")
            @LogbackConfiguration
            @Test fun custom(@Captured output: CapturedOutput) {
                UsingSpringPreset().custom(output)
            }
        }

        @SystemProperty(CONSOLE_LOG_PRESET, LogPresets.SPRING_PRESET)
        @Nested inner class UsingSpringPreset {

            @Test fun default(@Captured output: CapturedOutput) {
                logInfo()
                output.lastLog.shouldMatchSpringPreset()
            }

            @SystemProperty(LOG_DATEFORMAT_PATTERN, "yyyy-MM-dd")
            @SystemProperty(LOG_LEVEL_PATTERN, "%.-1p")
            @SystemProperty(EXCEPTION_CONVERSION_WORD, "%ex{short}")
            @Test fun custom(@Captured output: CapturedOutput) {
                logInfoWithException()
                output.lastLog.shouldMatchCustomSpringPreset()
            }
        }

        @SystemProperty(CONSOLE_LOG_PRESET, LogPresets.MINIMAL_PRESET)
        @Nested inner class UsingMinimalPreset {

            @Test fun default(@Captured output: CapturedOutput) {
                logInfo()
                output.lastLog.shouldMatchMinimalPreset()
            }

            @SystemProperty(LOG_LEVEL_PATTERN, "%.-1p")
            @SystemProperty(EXCEPTION_CONVERSION_WORD, "%ex{short}")
            @Test fun custom(@Captured output: CapturedOutput) {
                logInfoWithException()
                output.lastLog.shouldMatchCustomMinimalPreset()
            }
        }

        @SystemProperty(CONSOLE_LOG_PRESET, LogPresets.JSON_PRESET)
        @Nested inner class UsingJsonPreset {

            @Test fun default(@Captured output: CapturedOutput) {
                logInfo()
                output.lastLog.shouldMatchJsonPreset()
            }
        }

        @SystemProperty(CONSOLE_LOG_PRESET, LogPresets.OFF_PRESET)
        @Nested inner class UsingOffPreset {

            @Test fun default(@Captured output: CapturedOutput) {
                logInfo()
                output.allLogs.shouldBeEmpty()
            }
        }
    }

    @SystemProperty(CONSOLE_LOG_PRESET, LogPresets.OFF_PRESET)
    @Nested inner class FileLogProperties {

        @SystemProperty(FILE_LOG_PRESET, LogPresets.SPRING_PRESET)
        @Nested inner class LogFileProperty {

            @Test fun default() = testAll {
                val randomMessage = logRandomInfo()
                SystemLocations.Temp.resolve("kommons.log").lastLog.shouldMatchSpringPreset(randomMessage)
            }

            @SystemProperty(LOG_FILE, "\${java.io.tmpdir:-/tmp}/$CUSTOM_LOG_FILE_NAME")
            @Test fun log_file() = testAll {
                val randomMessage = logRandomInfo()
                SystemLocations.Temp.resolve(CUSTOM_LOG_FILE_NAME).lastLog.shouldMatchSpringPreset(randomMessage)
            }
        }


        @SystemProperty(FILE_LOG_PRESET, LogPresets.SPRING_PRESET)
        @Nested inner class UsingSpringPreset {

            @Test fun default(@LogFile logFile: Path) {
                val randomMessage = logRandomInfo()
                logFile.lastLog.shouldMatchSpringPreset(randomMessage)
            }

            @SystemProperty(LOG_DATEFORMAT_PATTERN, "yyyy-MM-dd")
            @SystemProperty(LOG_LEVEL_PATTERN, "%.-1p")
            @SystemProperty(EXCEPTION_CONVERSION_WORD, "%ex{short}")
            @Test fun custom(@LogFile logFile: Path) {
                val randomMessage = logRandomInfoWithException()
                logFile.lastLog.shouldMatchCustomSpringPreset(randomMessage)
            }
        }

        @SystemProperty(FILE_LOG_PRESET, LogPresets.MINIMAL_PRESET)
        @Nested inner class UsingMinimalPreset {

            @Test fun default(@LogFile logFile: Path) {
                val randomMessage = logRandomInfo()
                logFile.lastLog.shouldMatchMinimalPreset(randomMessage)
            }

            @SystemProperty(LOG_LEVEL_PATTERN, "%.-1p")
            @SystemProperty(EXCEPTION_CONVERSION_WORD, "%ex{short}")
            @Test fun custom(@LogFile logFile: Path) {
                val randomMessage = logRandomInfoWithException()
                logFile.lastLog.shouldMatchCustomMinimalPreset(randomMessage)
            }
        }

        @SystemProperty(FILE_LOG_PRESET, LogPresets.JSON_PRESET)
        @Nested inner class UsingJsonPreset {

            @Test fun default(@LogFile logFile: Path) {
                val randomMessage = logRandomInfo()
                logFile.lastLog.shouldMatchJsonPreset(randomMessage)
            }
        }

        @SystemProperty(FILE_LOG_PRESET, LogPresets.OFF_PRESET)
        @Nested inner class UsingOffPreset {

            @Test fun default() {
                logRandomInfo()
                Logback.activeLogFileName shouldBe null
            }
        }
    }
}

private const val CUSTOM_LOG_FILE_NAME = "kommons-test-custom.log"

internal fun logInfo(vararg args: Any?, message: String = "message") =
    LoggerFactory.getLogger("TestLogger").info("$message with {}", v("key", "value"), *args)

internal fun logInfoWithException(vararg args: Any?, message: String = "message") =
    logInfo(*args, RuntimeException("message"), message = message)

internal fun logRandomInfo(): String =
    randomString(64).also { logInfo(message = it) }

internal fun logRandomInfoWithException(): String =
    randomString(64).also { logInfoWithException(message = it) }


fun PrintedLogEntry.shouldMatchMinimalPreset(message: String = "message"): PrintedLogEntry = this shouldMatch Regex(
    """
    \d{2}:\d{2}.\d{3} {2}INFO TestLogger {21}: $message with value
    """.trimIndent()
)

fun PrintedLogEntry.shouldMatchCustomMinimalPreset(message: String = "message"): PrintedLogEntry = this shouldMatch Regex(
    """
    \d{2}:\d{2}.\d{3} I TestLogger + : $message with value
    java\.lang\.RuntimeException: message
    \tat .*
    """.trimIndent()
)

fun PrintedLogEntry.shouldMatchSpringPreset(message: String = "message"): PrintedLogEntry = this shouldMatch Regex(
    """
    \d{4}-\d{2}-\d{2} \d{2}:\d{2}:\d{2}.\d{3} {2}INFO {3}--- \[.*] TestLogger + : $message with value
    """.trimIndent()
)

fun PrintedLogEntry.shouldMatchCustomSpringPreset(message: String = "message"): PrintedLogEntry = this shouldMatch Regex(
    """
    \d{4}-\d{2}-\d{2} I {3}--- \[.*] TestLogger + : $message with value
    java\.lang\.RuntimeException: message
    \tat .*
    """.trimIndent()
)

fun PrintedLogEntry.shouldMatchJsonPreset(message: String = "message"): PrintedLogEntry = this shouldMatch Regex(
    @Suppress("LongLine")
    """
    \{"@timestamp":"\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}.\d{1,3}[+-]\d{2}:\d{2}","level":"INFO","thread_name":".+","logger_name":"TestLogger","message":"$message with value","key":"value","foo":"bar"}
    """.trimIndent()
)
