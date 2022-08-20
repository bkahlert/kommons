package com.bkahlert.kommons.logging.spring

import com.bkahlert.kommons.logging.LoggingPreset.JSON
import com.bkahlert.kommons.logging.LoggingPreset.MINIMAL
import com.bkahlert.kommons.logging.LoggingSystemProperties
import com.bkahlert.kommons.logging.logback.Logback
import com.bkahlert.kommons.test.logging.lastLog
import com.bkahlert.kommons.test.logging.lastOutLog
import com.bkahlert.kommons.test.logging.logRandomInfo
import com.bkahlert.kommons.test.logging.shouldMatchJsonPreset
import com.bkahlert.kommons.test.logging.shouldMatchMinimalPreset
import com.bkahlert.kommons.test.spring.Captured
import com.bkahlert.kommons.test.spring.logFile
import com.bkahlert.kommons.test.spring.properties
import com.bkahlert.kommons.test.spring.run
import io.kotest.matchers.maps.shouldContain
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.should
import io.kotest.matchers.shouldNotBe
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import org.junit.jupiter.api.parallel.Isolated
import org.slf4j.MDC
import org.springframework.boot.autoconfigure.EnableAutoConfiguration
import org.springframework.boot.builder.SpringApplicationBuilder
import org.springframework.boot.test.system.CapturedOutput
import org.springframework.context.annotation.Configuration
import java.nio.file.Path

@Isolated
class AutoConfigurationIntegrationTest {

    @Configuration(proxyBeanMethods = false)
    @EnableAutoConfiguration
    private class AppConfig

    @BeforeEach
    fun setUp() {
        Logback.reset()
    }

    private val springApplicationBuilder = SpringApplicationBuilder(AppConfig::class.java).properties {
        port = 0
    }

    @Test fun start() {
        springApplicationBuilder.run { ctx ->
            ctx shouldNotBe null
        }
    }

    @Test fun integration(
        @Captured output: CapturedOutput,
        @TempDir tempDir: Path
    ) {
        springApplicationBuilder.properties {
            consoleLogPreset = JSON
            fileLogPreset = MINIMAL
            logPath = tempDir
        }.run { ctx ->
            Logback.properties should {
                it.shouldContain(LoggingSystemProperties.CONSOLE_LOG_PRESET, JSON.value)
                it.shouldContain(LoggingSystemProperties.FILE_LOG_PRESET, MINIMAL.value)
            }

            MDC.put("foo", "bar")
            MDC.put("baz", null)

            val randomMessage = logRandomInfo()
            output.lastOutLog.shouldMatchJsonPreset(message = randomMessage)
            ctx.logFile?.lastLog.shouldNotBeNull().shouldMatchMinimalPreset(message = randomMessage)
        }
    }
}
