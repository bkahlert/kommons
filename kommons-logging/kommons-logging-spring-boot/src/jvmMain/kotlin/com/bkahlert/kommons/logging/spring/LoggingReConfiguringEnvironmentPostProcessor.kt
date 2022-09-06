package com.bkahlert.kommons.logging.spring

import com.bkahlert.kommons.io.toPath
import com.bkahlert.kommons.logging.LoggingSystemProperties
import com.bkahlert.kommons.logging.logback.Logback
import com.bkahlert.kommons.logging.logback.StatusLogger
import com.bkahlert.kommons.logging.spring.LoggingReConfiguringEnvironmentPostProcessor.Companion
import org.springframework.boot.SpringApplication
import org.springframework.boot.context.logging.LoggingApplicationListener
import org.springframework.boot.env.EnvironmentPostProcessor
import org.springframework.boot.logging.LogFile
import org.springframework.core.annotation.Order
import org.springframework.core.env.ConfigurableEnvironment
import org.springframework.core.env.MapPropertySource
import org.springframework.core.env.PropertyResolver
import org.springframework.core.env.PropertySource
import java.nio.file.Path

/**
 * Environment post-processor which is registered via `spring.factories` and
 * invoked after the initial logging initialization by Spring Boot.
 *
 * Its purpose is to re-configure the logging to enable [LoggingProperties].
 */
@Suppress("RedundantCompanionReference")
@Order(Companion.POST_ENVIRONMENT_LOG_CONFIGURATION_PRIORITY_ORDER)
public class LoggingReConfiguringEnvironmentPostProcessor : EnvironmentPostProcessor {

    /**
     * Adds the [PropertySource] to the environment so that
     * the [LogFile] bean is correctly resolved and sets `logging.config`
     * to `logback-kommons.xml` to enable [LoggingProperties].
     */
    public override fun postProcessEnvironment(environment: ConfigurableEnvironment, application: SpringApplication) {
        environment.logFile?.also { updateLogbackLogFile(it) }
        environment.propertySources.addLast(LogbackPropertySource)
        environment.propertySources.addLast(LoggingReconfigurationPropertySource)
    }

    public companion object {
        /** The order for the [LoggingReConfiguringEnvironmentPostProcessor]. */
        public const val POST_ENVIRONMENT_LOG_CONFIGURATION_PRIORITY_ORDER: Int = LoggingApplicationListener.DEFAULT_ORDER + 20

        private fun updateLogbackLogFile(logFile: Path) {
            if (Logback.activeLogFile != logFile) {
                StatusLogger.info<LoggingReConfiguringEnvironmentPostProcessor>("Changing active log file name")
                Logback.activeLogFile = logFile
            }
        }

        private object LogbackPropertySource : PropertySource<Logback>("logbackProperties", Logback) {
            override fun getProperty(name: String): Any? = when (name) {
                LoggingProperties.CONSOLE_LOG_PRESET_PROPERTY -> source.properties[LoggingSystemProperties.CONSOLE_LOG_PRESET]
                LoggingProperties.FILE_LOG_PRESET_PROPERTY -> source.properties[LoggingSystemProperties.FILE_LOG_PRESET]
                LogFile.FILE_NAME_PROPERTY -> source.activeLogFileName
                "management.endpoint.logfile.external-file" -> source.activeLogFileName
                else -> null
            }
        }

        private object LoggingReconfigurationPropertySource : MapPropertySource(
            "loggingReconfigurationKommons", mapOf(
                LoggingApplicationListener.CONFIG_PROPERTY to "classpath:logback-kommons.xml",
            )
        )
    }
}

private val PropertyResolver.logFile: Path?
    get() = LogFile.get(this)?.toString()?.toPath()
