@file:Suppress("JUnitMalformedDeclaration")

package com.bkahlert.kommons.logging.spring

import com.bkahlert.kommons.logging.LoggingPreset
import com.bkahlert.kommons.logging.LoggingPreset.OFF
import com.bkahlert.kommons.logging.LoggingPreset.SPRING
import com.bkahlert.kommons.logging.logback.Logback
import com.bkahlert.kommons.logging.spring.LogFileProvider.UsingLogback
import com.bkahlert.kommons.test.spring.properties
import com.bkahlert.kommons.test.spring.run
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldEndWith
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import org.junit.jupiter.api.parallel.Isolated
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.springframework.beans.factory.getBean
import org.springframework.boot.actuate.logging.LogFileWebEndpoint
import org.springframework.boot.builder.SpringApplicationBuilder
import org.springframework.boot.logging.LogFile
import org.springframework.context.ConfigurableApplicationContext
import java.nio.file.Path
import kotlin.io.path.div
import kotlin.io.path.pathString

@Isolated
class LogFileIntegrationTest {

    @BeforeEach
    fun setUp() {
        Logback.clearSystemProperties()
        Logback.reset()
    }

    private val springApplicationBuilder
        get() = SpringApplicationBuilder(TestConfig::class.java).properties {
            actuatorEndpoints = actuatorEndpoints + "logfile"
            port = 0
            consoleLogPreset = LoggingPreset.OFF
        }

    @Nested inner class FileLoggingEnabled {

        @EnumSource(LogFileProvider::class)
        @ParameterizedTest fun explicit_log_file(
            provider: LogFileProvider,
            @TempDir tempDir: Path,
        ) {
            val logFile = tempDir / "test2.log"
            springApplicationBuilder.properties {
                fileLogPreset = SPRING
                this.logFile = logFile
            }.run {
                provider.get(it) shouldBe logFile.pathString
            }
        }

        @EnumSource(LogFileProvider::class)
        @ParameterizedTest fun implicit_log_file(
            provider: LogFileProvider,
        ) {
            springApplicationBuilder.properties {
                fileLogPreset = SPRING
                this.logFile = null
            }.run {
                provider.get(it) shouldEndWith "kommons.log"
            }
        }
    }

    @Nested inner class FileLoggingDisabled {

        @Test fun explicit_log_file(@TempDir tempDir: Path) {
            val logFile = tempDir / "test.log"
            springApplicationBuilder.properties {
                fileLogPreset = OFF
                this.logFile = logFile
            }.run {
                UsingLogback.get(it) shouldBe null
            }
        }

        @Test fun implicit_log_file() {
            springApplicationBuilder.properties {
                fileLogPreset = OFF
                this.logFile = null
            }.run {
                UsingLogback.get(it) shouldBe null
            }
        }
    }
}

enum class LogFileProvider {
    UsingLogback {
        override fun get(context: ConfigurableApplicationContext): String? =
            Logback.activeLogFileName
    },
    UsingLogFile {
        override fun get(context: ConfigurableApplicationContext): String? =
            LogFile.get(context.environment)?.toString()
    },
    UsingLogFileWebEndpoint {
        override fun get(context: ConfigurableApplicationContext): String? =
            kotlin.runCatching { context.getBean<LogFileWebEndpoint>().logFile()?.file?.path }.getOrNull()
    },
    ;

    abstract fun get(context: ConfigurableApplicationContext): String?
}
