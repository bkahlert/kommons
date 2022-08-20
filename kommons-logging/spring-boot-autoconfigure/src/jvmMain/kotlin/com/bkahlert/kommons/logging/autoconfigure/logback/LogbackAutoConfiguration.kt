package com.bkahlert.kommons.autoconfigure.logback

import com.bkahlert.kommons.autoconfigure.propertysource.YamlPropertySourceFactory
import com.bkahlert.kommons.logging.autoconfigure.logback.LoggingProperties
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.PropertySource
import org.springframework.util.ResourceUtils

/**
 * Configuration of the Logback logging framework
 */
@Configuration(proxyBeanMethods = false) @PropertySource(
    factory = YamlPropertySourceFactory::class,
    name = LogbackAutoConfiguration.LOGGING_CONFIG_RESOURCE,
    value = [LogbackAutoConfiguration.LOGGING_CONFIG_RESOURCE]
) @EnableConfigurationProperties(
    LoggingProperties::class
)
public data class LogbackAutoConfiguration(
    public val logbackProperties: LoggingProperties,
) {

    public companion object {
        public const val LOGGING_CONFIG_RESOURCE: String =
            ResourceUtils.CLASSPATH_URL_PREFIX + "com/bkahlert/kommons/logging/logback/autoconfigure/logging.yml"
    }
}
