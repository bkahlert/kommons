package com.bkahlert.kommons.test.logging.logback

import com.bkahlert.kommons.logging.LogPresets
import com.bkahlert.kommons.logging.LoggingSystemProperties
import com.bkahlert.kommons.logging.logback.Logback
import com.bkahlert.kommons.logging.logback.logInfo
import com.bkahlert.kommons.test.logging.allLogs
import com.bkahlert.kommons.test.logging.logback.AppenderTestPreset.Minimal
import com.bkahlert.kommons.test.shouldMatchGlob
import com.bkahlert.kommons.test.testAll
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import java.nio.file.Path
import kotlin.io.path.pathString

class LogbackConfigurationExtensionTest {

    @LogbackConfiguration
    @Test fun default() = testAll {
        Logback.properties[LoggingSystemProperties.CONSOLE_LOG_PRESET] shouldBe null
        Logback.properties[LoggingSystemProperties.FILE_LOG_PRESET] shouldBe null
    }

    @LogbackConfiguration(console = Minimal)
    @Test fun console_log_preset() = testAll {
        Logback.properties[LoggingSystemProperties.CONSOLE_LOG_PRESET] shouldBe LogPresets.MINIMAL_PRESET
        Logback.properties[LoggingSystemProperties.FILE_LOG_PRESET] shouldBe null
    }

    @LogbackConfiguration(file = Minimal)
    @Test fun file_log_preset() = testAll {
        Logback.properties[LoggingSystemProperties.CONSOLE_LOG_PRESET] shouldBe null
        Logback.properties[LoggingSystemProperties.FILE_LOG_PRESET] shouldBe LogPresets.MINIMAL_PRESET
    }

    @LogbackConfiguration(console = Minimal, file = Minimal)
    @Test fun all_log_presets() = testAll {
        Logback.properties[LoggingSystemProperties.CONSOLE_LOG_PRESET] shouldBe LogPresets.MINIMAL_PRESET
        Logback.properties[LoggingSystemProperties.FILE_LOG_PRESET] shouldBe LogPresets.MINIMAL_PRESET
    }

    @LogbackConfiguration(file = Minimal)
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
}
