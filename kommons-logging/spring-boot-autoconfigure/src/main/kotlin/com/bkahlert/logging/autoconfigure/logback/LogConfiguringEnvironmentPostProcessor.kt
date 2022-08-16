package com.bkahlert.logging.autoconfigure.logback

import ch.qos.logback.classic.util.StatusViaSLF4JLoggerFactory
import com.bkahlert.logging.autoconfigure.SpringCloudDetection
import com.bkahlert.logging.autoconfigure.logback.Banners.turnOffBanner
import com.bkahlert.logging.autoconfigure.logback.Banners.useSpringCloudBanner
import com.bkahlert.logging.autoconfigure.logback.LogbackAutoConfiguration.Companion.LOGGING_CONFIG_RESOURCE
import com.bkahlert.logging.autoconfigure.propertysource.YamlPropertySource
import com.bkahlert.logging.logback.LogbackConfiguration
import org.slf4j.LoggerFactory
import org.springframework.boot.SpringApplication
import org.springframework.boot.ansi.AnsiOutput
import org.springframework.boot.context.logging.LoggingApplicationListener
import org.springframework.boot.env.EnvironmentPostProcessor
import org.springframework.boot.logging.LogFile
import org.springframework.core.annotation.Order
import org.springframework.core.env.ConfigurableEnvironment
import org.springframework.core.env.MapPropertySource
import java.nio.file.Files
import java.nio.file.Path
import java.text.MessageFormat

/**
 * **Important:** Makes [LogbackAutoConfiguration.LOGGING_CONFIG_RESOURCE] (especially property
 * `logging.config: classpath:logback-spring.xml` available to make Spring reload the logback configuration which
 * is no more `logback.xml` but `logback-spring.xml`. Since loading is done by `SpringBootJoranConfigurator`
 * all Spring extensions are available.
 */
@Order(LogConfiguringEnvironmentPostProcessor.POST_ENVIRONMENT_LOG_CONFIGURATION_PRIORITY_ORDER)
public class LogConfiguringEnvironmentPostProcessor : EnvironmentPostProcessor {
    private val logger = LoggerFactory.getLogger(javaClass)
    public override fun postProcessEnvironment(environment: ConfigurableEnvironment, application: SpringApplication) {
        reconfigureSpringLogging(environment)
        environment.propertySources.addLast(YamlPropertySource(LOGGING_CONFIG_RESOURCE, LOGGING_CONFIG_RESOURCE))
        if (SpringCloudDetection.isBootstrapApplicationContext(environment)) {
            // applies only to Spring Cloud Bootstrapper
            turnOffBanner(environment)
        } else {
            // applies to Spring Cloud bootstrapped apps and standalone Spring Boot apps
            StatusViaSLF4JLoggerFactory.addInfo(MessageFormat.format("Applying {0} to {1}@{2}", AnsiOutput.Enabled.ALWAYS,
                application.javaClass.simpleName, application.hashCode()), LogConfiguringEnvironmentPostProcessor::class.java.name)
            AnsiOutput.setEnabled(AnsiOutput.Enabled.ALWAYS)
            AnsiOutput.setConsoleAvailable(true)
            if (SpringCloudDetection.isBootstrapApplicationChildContext(environment)) {
                // applies only to Spring Cloud bootstrapped apps
                useSpringCloudBanner(environment)
            }
        }
    }

    public companion object {
        public const val POST_ENVIRONMENT_LOG_CONFIGURATION_PRIORITY_ORDER: Int = LoggingApplicationListener.DEFAULT_ORDER + 20
        public fun reconfigureSpringLogging(environment: ConfigurableEnvironment) {
            val properties: MutableMap<String, Any> = HashMap()
            properties["logging.config"] = "classpath:logback-spring.xml"
            LogbackConfiguration.activeLogFileName?.takeIf { Files.exists(Path.of(it)) }?.let { logFile ->
                val springConfiguredLogFile = LogFile.get(environment)
                if (springConfiguredLogFile == null || logFile != springConfiguredLogFile.toString()) {
                    properties[LogFile.FILE_NAME_PROPERTY] = logFile
                }
            }
            environment.propertySources.addLast(MapPropertySource("springLoggingReconfiguration", properties))
        }
    }
}
