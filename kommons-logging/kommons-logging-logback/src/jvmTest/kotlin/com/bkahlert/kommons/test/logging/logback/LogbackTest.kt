package com.bkahlert.kommons.test.logging.logback

import com.bkahlert.kommons.logging.LoggingPreset
import com.bkahlert.kommons.logging.LoggingSystemProperties
import com.bkahlert.kommons.logging.logback.Logback
import com.bkahlert.kommons.test.logging.allLogs
import com.bkahlert.kommons.test.logging.logInfo
import com.bkahlert.kommons.test.logging.logRandomInfo
import com.bkahlert.kommons.test.shouldMatchGlob
import com.bkahlert.kommons.test.testAll
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import org.junit.jupiter.api.parallel.Isolated
import java.nio.file.Path
import kotlin.io.path.div
import kotlin.io.path.pathString

@Isolated
class LogbackTest {

    @LogbackConfiguration
    @Test fun default() = testAll {
        Logback.properties[LoggingSystemProperties.CONSOLE_LOG_PRESET] shouldBe "spring"
        Logback.properties[LoggingSystemProperties.FILE_LOG_PRESET] shouldBe "off"
    }

    @LogbackConfiguration(console = LoggingPreset.MINIMAL)
    @Test fun console_log_preset() = testAll {
        Logback.properties[LoggingSystemProperties.CONSOLE_LOG_PRESET] shouldBe "minimal"
        Logback.properties[LoggingSystemProperties.FILE_LOG_PRESET] shouldBe "off"
    }

    @LogbackConfiguration(file = LoggingPreset.MINIMAL)
    @Test fun file_log_preset() = testAll {
        Logback.properties[LoggingSystemProperties.CONSOLE_LOG_PRESET] shouldBe "spring"
        Logback.properties[LoggingSystemProperties.FILE_LOG_PRESET] shouldBe "minimal"
    }

    @LogbackConfiguration(console = LoggingPreset.MINIMAL, file = LoggingPreset.MINIMAL)
    @Test fun all_log_presets() = testAll {
        Logback.properties[LoggingSystemProperties.CONSOLE_LOG_PRESET] shouldBe "minimal"
        Logback.properties[LoggingSystemProperties.FILE_LOG_PRESET] shouldBe "minimal"
    }

    @LogbackConfiguration(file = LoggingPreset.MINIMAL)
    @Test fun log_file(@LogFile logFile: Path) = testAll {
        logInfo()
        logFile.pathString should {
            it shouldMatchGlob "*kommons-test-*.log"
            it shouldBe Logback.activeLogFileName
        }
        logFile.allLogs should {
            it shouldHaveSize 1
            it.first() shouldMatchGlob "*INFO TestLogger*: message with value"
        }
    }

    @LogbackConfiguration(file = LoggingPreset.MINIMAL)
    @Test fun log_file__change(@TempDir tempDir: Path) = testAll {
        logRandomInfo()
        val logFile = tempDir / "new-log-file.log"
        Logback.activeLogFile = logFile
        val message = logRandomInfo()
        logFile.pathString should {
            it shouldBe logFile.pathString
            it shouldBe Logback.activeLogFileName
        }
        logFile.allLogs should {
            it shouldHaveSize 1
            it.first() shouldMatchGlob "*INFO TestLogger*: $message with value"
        }
    }
}
