package com.bkahlert.kommons.logging.logback

import com.bkahlert.kommons.SystemLocations
import com.bkahlert.kommons.logging.DEFAULT_PRESET_VALUE
import com.bkahlert.kommons.logging.JSON_PRESET_VALUE
import com.bkahlert.kommons.logging.LoggingPreset
import com.bkahlert.kommons.logging.LoggingSystemProperties.CONSOLE_LOG_PRESET
import com.bkahlert.kommons.logging.LoggingSystemProperties.FILE_LOG_PRESET
import com.bkahlert.kommons.logging.MINIMAL_PRESET_VALUE
import com.bkahlert.kommons.logging.OFF_PRESET_VALUE
import com.bkahlert.kommons.logging.SPRING_PRESET_VALUE
import com.bkahlert.kommons.test.junit.SystemProperty
import com.bkahlert.kommons.test.logging.allLogs
import com.bkahlert.kommons.test.logging.lastLog
import com.bkahlert.kommons.test.logging.logRandomInfo
import com.bkahlert.kommons.test.logging.logRandomInfoWithException
import com.bkahlert.kommons.test.logging.logback.LogFile
import com.bkahlert.kommons.test.logging.logback.LogbackConfiguration
import com.bkahlert.kommons.test.logging.shouldMatchCustomMinimalPreset
import com.bkahlert.kommons.test.logging.shouldMatchCustomSpringPreset
import com.bkahlert.kommons.test.logging.shouldMatchJsonPreset
import com.bkahlert.kommons.test.logging.shouldMatchMinimalPreset
import com.bkahlert.kommons.test.logging.shouldMatchSpringPreset
import com.bkahlert.kommons.test.spring.Captured
import com.bkahlert.kommons.test.testAll
import io.kotest.inspectors.forAll
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Isolated
import org.slf4j.MDC
import org.springframework.boot.logging.LoggingSystemProperties.EXCEPTION_CONVERSION_WORD
import org.springframework.boot.logging.LoggingSystemProperties.LOG_DATEFORMAT_PATTERN
import org.springframework.boot.logging.LoggingSystemProperties.LOG_FILE
import org.springframework.boot.logging.LoggingSystemProperties.LOG_LEVEL_PATTERN
import org.springframework.boot.test.system.CapturedOutput
import java.nio.file.Path
import kotlin.io.path.deleteIfExists

@Isolated
class LoggingPresetTest {

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

    @Test fun value_of_or_default() = testAll {
        LoggingPreset.values().forAll {
            LoggingPreset.valueOfOrDefault(it.name) shouldBe it
            LoggingPreset.valueOfOrDefault(it.name.uppercase()) shouldBe it
            LoggingPreset.valueOfOrDefault(it.name.lowercase()) shouldBe it
        }
        LoggingPreset.valueOfOrDefault("illegal") shouldBe LoggingPreset.DEFAULT
        LoggingPreset.valueOfOrDefault(null) shouldBe LoggingPreset.DEFAULT
    }

    @SystemProperty(FILE_LOG_PRESET, OFF_PRESET_VALUE)
    @Nested inner class ConsoleLogProperties {

        @SystemProperty(CONSOLE_LOG_PRESET, DEFAULT_PRESET_VALUE)
        @Nested inner class UsingDefaultPreset {

            @Test fun default(@Captured output: CapturedOutput) {
                val randomMessage = logRandomInfo()
                output.lastLog.shouldMatchSpringPreset(message = randomMessage)
            }

            @SystemProperty(LOG_DATEFORMAT_PATTERN, "yyyy-MM-dd")
            @SystemProperty(LOG_LEVEL_PATTERN, "%.-1p")
            @SystemProperty(EXCEPTION_CONVERSION_WORD, "%ex{short}")
            @LogbackConfiguration
            @Test fun custom(@Captured output: CapturedOutput) {
                val randomMessage = logRandomInfoWithException()
                output.lastLog.shouldMatchCustomSpringPreset(message = randomMessage)
            }
        }

        @SystemProperty(CONSOLE_LOG_PRESET, SPRING_PRESET_VALUE)
        @Nested inner class UsingSpringPreset {

            @Test fun default(@Captured output: CapturedOutput) {
                val randomMessage = logRandomInfo()
                output.lastLog.shouldMatchSpringPreset(message = randomMessage)
            }

            @SystemProperty(LOG_DATEFORMAT_PATTERN, "yyyy-MM-dd")
            @SystemProperty(LOG_LEVEL_PATTERN, "%.-1p")
            @SystemProperty(EXCEPTION_CONVERSION_WORD, "%ex{short}")
            @Test fun custom(@Captured output: CapturedOutput) {
                val randomMessage = logRandomInfoWithException()
                output.lastLog.shouldMatchCustomSpringPreset(message = randomMessage)
            }
        }

        @SystemProperty(CONSOLE_LOG_PRESET, MINIMAL_PRESET_VALUE)
        @Nested inner class UsingMinimalPreset {

            @Test fun default(@Captured output: CapturedOutput) {
                val randomMessage = logRandomInfo()
                output.lastLog.shouldMatchMinimalPreset(message = randomMessage)
            }

            @SystemProperty(LOG_LEVEL_PATTERN, "%.-1p")
            @SystemProperty(EXCEPTION_CONVERSION_WORD, "%ex{short}")
            @Test fun custom(@Captured output: CapturedOutput) {
                val randomMessage = logRandomInfoWithException()
                output.lastLog.shouldMatchCustomMinimalPreset(message = randomMessage)
            }
        }

        @SystemProperty(CONSOLE_LOG_PRESET, JSON_PRESET_VALUE)
        @Nested inner class UsingJsonPreset {

            @Test fun default(@Captured output: CapturedOutput) {
                val randomMessage = logRandomInfo()
                output.lastLog.shouldMatchJsonPreset(message = randomMessage)
            }
        }

        @SystemProperty(CONSOLE_LOG_PRESET, OFF_PRESET_VALUE)
        @Nested inner class UsingOffPreset {

            @Test fun default(@Captured output: CapturedOutput) {
                logRandomInfo()
                output.allLogs.shouldBeEmpty()
            }
        }
    }

    @SystemProperty(CONSOLE_LOG_PRESET, OFF_PRESET_VALUE)
    @Nested inner class FileLogProperties {

        @SystemProperty(FILE_LOG_PRESET, SPRING_PRESET_VALUE)
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


        @SystemProperty(FILE_LOG_PRESET, SPRING_PRESET_VALUE)
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

        @SystemProperty(FILE_LOG_PRESET, MINIMAL_PRESET_VALUE)
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

        @SystemProperty(FILE_LOG_PRESET, JSON_PRESET_VALUE)
        @Nested inner class UsingJsonPreset {

            @Test fun default(@LogFile logFile: Path) {
                val randomMessage = logRandomInfo()
                logFile.lastLog.shouldMatchJsonPreset(randomMessage)
            }
        }

        @SystemProperty(FILE_LOG_PRESET, OFF_PRESET_VALUE)
        @Nested inner class UsingOffPreset {

            @Test fun default() {
                logRandomInfo()
                Logback.activeLogFile shouldBe null
            }
        }
    }
}

private const val CUSTOM_LOG_FILE_NAME = "kommons-test-custom.log"
