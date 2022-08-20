package com.bkahlert.kommons.test.logging.logback

import com.bkahlert.kommons.logging.LoggingPreset
import com.bkahlert.kommons.logging.LoggingSystemProperties
import com.bkahlert.kommons.logging.logback.Logback
import com.bkahlert.kommons.test.logging.allLogs
import com.bkahlert.kommons.test.logging.logInfo
import com.bkahlert.kommons.test.shouldMatchGlob
import com.bkahlert.kommons.test.testAll
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Isolated
import java.nio.file.Path
import kotlin.io.path.pathString

@Isolated
class LogbackTest {

    @LogbackConfiguration
    @Test fun default() = testAll {
        Logback.properties[LoggingSystemProperties.CONSOLE_LOG_PRESET] shouldBe "default"
        Logback.properties[LoggingSystemProperties.FILE_LOG_PRESET] shouldBe "default"
    }

    @LogbackConfiguration(console = LoggingPreset.MINIMAL)
    @Test fun console_log_preset() = testAll {
        Logback.properties[LoggingSystemProperties.CONSOLE_LOG_PRESET] shouldBe "minimal"
        Logback.properties[LoggingSystemProperties.FILE_LOG_PRESET] shouldBe "default"
    }

    @LogbackConfiguration(file = LoggingPreset.MINIMAL)
    @Test fun file_log_preset() = testAll {
        Logback.properties[LoggingSystemProperties.CONSOLE_LOG_PRESET] shouldBe "default"
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
}
