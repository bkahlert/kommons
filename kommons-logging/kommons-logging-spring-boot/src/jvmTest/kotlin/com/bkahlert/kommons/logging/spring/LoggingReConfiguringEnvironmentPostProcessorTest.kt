package com.bkahlert.kommons.logging.spring

import com.bkahlert.kommons.logging.LoggingPreset.JSON
import com.bkahlert.kommons.logging.LoggingPreset.MINIMAL
import com.bkahlert.kommons.logging.LoggingPreset.SPRING
import com.bkahlert.kommons.logging.LoggingSystemProperties
import com.bkahlert.kommons.logging.MINIMAL_PRESET_VALUE
import com.bkahlert.kommons.logging.SPRING_PRESET_VALUE
import com.bkahlert.kommons.logging.logback.Logback
import com.bkahlert.kommons.test.junit.SystemProperty
import com.bkahlert.kommons.test.logging.lastLog
import com.bkahlert.kommons.test.logging.lastOutLog
import com.bkahlert.kommons.test.logging.logRandomInfo
import com.bkahlert.kommons.test.logging.shouldMatchJsonPreset
import com.bkahlert.kommons.test.logging.shouldMatchMinimalPreset
import com.bkahlert.kommons.test.logging.shouldMatchSpringPreset
import com.bkahlert.kommons.test.spring.Captured
import com.bkahlert.kommons.test.spring.logFile
import com.bkahlert.kommons.test.spring.properties
import com.bkahlert.kommons.test.spring.run
import io.kotest.matchers.maps.shouldContain
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import org.junit.jupiter.api.parallel.Isolated
import org.slf4j.MDC
import org.springframework.beans.factory.getBean
import org.springframework.boot.builder.SpringApplicationBuilder
import org.springframework.boot.test.system.CapturedOutput
import java.nio.file.Path

@Isolated
class LoggingReConfiguringEnvironmentPostProcessorTest {

    @BeforeEach
    fun setUp() {
        Logback.reset()

        MDC.put("foo", "bar")
        MDC.put("baz", null)
    }

    @AfterEach
    fun tearDown() {
        Logback.clearSystemProperties()
        Logback.reset()
    }

    private val springApplicationBuilder: SpringApplicationBuilder
        get() = SpringApplicationBuilder(TestConfig::class.java).properties {
            port = 0
        }

    @SystemProperty(LoggingSystemProperties.CONSOLE_LOG_PRESET, MINIMAL_PRESET_VALUE)
    @SystemProperty(LoggingSystemProperties.FILE_LOG_PRESET, SPRING_PRESET_VALUE)
    @Test fun reuse_existing_configuration(
        @Captured output: CapturedOutput,
    ) {
        springApplicationBuilder.run { ctx ->
            ctx.getBean<LoggingProperties>() should {
                it.preset.console shouldBe MINIMAL
                it.preset.file shouldBe SPRING
            }

            Logback.properties should {
                it.shouldContain(LoggingSystemProperties.CONSOLE_LOG_PRESET, MINIMAL.value)
                it.shouldContain(LoggingSystemProperties.FILE_LOG_PRESET, SPRING.value)
            }

            val randomMessage = logRandomInfo()
            output.lastOutLog.shouldMatchMinimalPreset(message = randomMessage)
            ctx.logFile?.lastLog.shouldNotBeNull().shouldMatchSpringPreset(message = randomMessage)
        }
    }

    @SystemProperty(LoggingSystemProperties.CONSOLE_LOG_PRESET, MINIMAL_PRESET_VALUE)
    @SystemProperty(LoggingSystemProperties.FILE_LOG_PRESET, SPRING_PRESET_VALUE)
    @Test fun logfile_configuration_using_application_properties(
        @Captured output: CapturedOutput,
        @TempDir tempDir: Path,
    ) {
        springApplicationBuilder.properties {
            logPath = tempDir
        }.run { ctx ->
            ctx.getBean<LoggingProperties>() should {
                it.preset.console shouldBe MINIMAL
                it.preset.file shouldBe SPRING
            }

            Logback.properties should {
                it.shouldContain(LoggingSystemProperties.CONSOLE_LOG_PRESET, MINIMAL.value)
                it.shouldContain(LoggingSystemProperties.FILE_LOG_PRESET, SPRING.value)
            }

            val randomMessage = logRandomInfo()
            output.lastOutLog.shouldMatchMinimalPreset(message = randomMessage)
            ctx.logFile?.lastLog.shouldNotBeNull().shouldMatchSpringPreset(message = randomMessage)
        }
    }

    @Test fun configuration_using_application_properties(
        @Captured output: CapturedOutput,
        @TempDir tempDir: Path,
    ) {
        springApplicationBuilder.properties {
            consoleLogPreset = JSON
            fileLogPreset = MINIMAL
            logPath = tempDir
        }.run { ctx ->
            ctx.getBean<LoggingProperties>() should {
                it.preset.console shouldBe JSON
                it.preset.file shouldBe MINIMAL
            }

            Logback.properties should {
                it.shouldContain(LoggingSystemProperties.CONSOLE_LOG_PRESET, JSON.value)
                it.shouldContain(LoggingSystemProperties.FILE_LOG_PRESET, MINIMAL.value)
            }

            val randomMessage = logRandomInfo()
            output.lastOutLog.shouldMatchJsonPreset(message = randomMessage)
            ctx.logFile?.lastLog.shouldNotBeNull().shouldMatchMinimalPreset(message = randomMessage)
        }
    }
}
