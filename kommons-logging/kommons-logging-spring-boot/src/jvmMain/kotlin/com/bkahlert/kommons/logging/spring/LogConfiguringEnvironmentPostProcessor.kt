package com.bkahlert.kommons.logging.spring

import com.bkahlert.kommons.logging.logback.Logback
import com.bkahlert.kommons.logging.logback.StatusLogger
import com.bkahlert.kommons.logging.spring.Banners.turnOffBanner
import com.bkahlert.kommons.logging.spring.Banners.useSpringCloudBanner
import com.bkahlert.kommons.logging.spring.LogConfiguringEnvironmentPostProcessor.Companion
import com.bkahlert.kommons.logging.spring.LoggingAutoConfiguration.Companion.LOGGING_CONFIG_RESOURCE
import com.bkahlert.kommons.logging.spring.propertysource.YamlPropertySource
import org.springframework.boot.SpringApplication
import org.springframework.boot.ansi.AnsiOutput
import org.springframework.boot.context.logging.LoggingApplicationListener
import org.springframework.boot.env.EnvironmentPostProcessor
import org.springframework.boot.logging.LogFile
import org.springframework.core.annotation.Order
import org.springframework.core.env.ConfigurableEnvironment
import org.springframework.core.env.MapPropertySource
import org.springframework.core.env.PropertySource
import kotlin.io.path.exists
import kotlin.io.path.pathString

/**
 * **Important:** Makes [LoggingAutoConfiguration.LOGGING_CONFIG_RESOURCE] (especially property
 * `logging.config: classpath:logback-spring.xml` available to make Spring reload the logback configuration which
 * is no more `logback.xml` but `logback-spring.xml`. Since loading is done by `SpringBootJoranConfigurator`
 * all Spring extensions are available.
 */
@Suppress("RedundantCompanionReference")
@Order(Companion.POST_ENVIRONMENT_LOG_CONFIGURATION_PRIORITY_ORDER)
public class LogConfiguringEnvironmentPostProcessor : EnvironmentPostProcessor {

//    private fun onApplicationPreparedEvent(event: ApplicationPreparedEvent) {
//        val applicationContext = event.applicationContext
//        val beanFactory = applicationContext.beanFactory
//        if (this.logFile != null && !beanFactory.containsBean(LoggingApplicationListener.LOG_FILE_BEAN_NAME)) {
//            beanFactory.registerSingleton(LoggingApplicationListener.LOG_FILE_BEAN_NAME, this.logFile)
//        }
//    }

    public override fun postProcessEnvironment(environment: ConfigurableEnvironment, application: SpringApplication) {
        // TODO only make sure colors are logged
        reconfigureSpringLogging(environment)

        environment.propertySources.addLast(YamlPropertySource(LOGGING_CONFIG_RESOURCE, LOGGING_CONFIG_RESOURCE))
        if (SpringCloudDetection.isBootstrapApplicationContext(environment)) {
            // applies only to Spring Cloud Bootstrap context
            turnOffBanner(environment)
        } else {
            // applies to all contexts
            StatusLogger.info<LogConfiguringEnvironmentPostProcessor>(
                "Applying {} {} to {}",
                AnsiOutput::class.simpleName, AnsiOutput.Enabled.ALWAYS, application
            )
            AnsiOutput.setEnabled(AnsiOutput.Enabled.ALWAYS)
            AnsiOutput.setConsoleAvailable(true)
            if (SpringCloudDetection.isBootstrapApplicationChildContext(environment)) {
                // applies only to child contexts of Spring Cloud Bootstrap context
                useSpringCloudBanner(environment)
            }
        }
    }

    public companion object {
        private const val ATTACHED_PROPERTY_SOURCE_NAME = "kommonsLoggingLogFile"

        public const val POST_ENVIRONMENT_LOG_CONFIGURATION_PRIORITY_ORDER: Int = LoggingApplicationListener.DEFAULT_ORDER + 20
        public fun reconfigureSpringLogging(environment: ConfigurableEnvironment) {
            environment.propertySources.addFirst(object : PropertySource<Logback>("logbackProperties", Logback) {
                override fun getProperty(name: String): Any? = when (name) {
                    LogFile.FILE_NAME_PROPERTY -> Logback.activeLogFileName
                    "management.endpoint.logfile.external-file" -> Logback.activeLogFileName
                    else -> null
                }
            })

            val properties: MutableMap<String, Any> = HashMap()
            properties[LoggingApplicationListener.CONFIG_PROPERTY] = "classpath:logback-spring.xml" // TODO rename

            Logback.activeLogFile?.takeIf { it.exists() }?.let { logFile ->
                val springConfiguredLogFile = LogFile.get(environment)
                if (springConfiguredLogFile == null || logFile.pathString != springConfiguredLogFile.toString()) {
//                    properties[LogFile.FILE_NAME_PROPERTY] = logFile
                }
            }
            environment.propertySources.addLast(MapPropertySource("springLoggingReconfiguration", properties))
        }
    }
}
