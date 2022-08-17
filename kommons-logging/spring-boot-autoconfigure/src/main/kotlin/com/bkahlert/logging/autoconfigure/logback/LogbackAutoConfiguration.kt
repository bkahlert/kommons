package com.bkahlert.logging.autoconfigure.logback

import com.bkahlert.logging.autoconfigure.propertysource.YamlPropertySourceFactory
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
    LogbackProperties::class
)
public data class LogbackAutoConfiguration(
    public val logbackProperties: LogbackProperties,
) {

    public companion object {
        public const val LOGGING_CONFIG_RESOURCE: String =
            ResourceUtils.CLASSPATH_URL_PREFIX + "com/bkahlert/kommons/logging/logback/autoconfigure/logging.yml"
    }
}
