package com.bkahlert.kommons.logging.spring

import com.bkahlert.kommons.logging.spring.LoggingAutoConfiguration.Companion
import com.bkahlert.kommons.logging.spring.propertysource.YamlPropertySourceFactory
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.PropertySource
import org.springframework.util.ResourceUtils

/**
 * Configuration of the Logback logging framework
 */
@Configuration(proxyBeanMethods = false)
@PropertySource(
    factory = YamlPropertySourceFactory::class,
    name = Companion.LOGGING_CONFIG_RESOURCE,
    value = [Companion.LOGGING_CONFIG_RESOURCE]
)
@EnableConfigurationProperties(LoggingProperties::class)
public open class LoggingAutoConfiguration(
    public val logbackProperties: LoggingProperties,
) {

    public companion object {
        public const val LOGGING_CONFIG_RESOURCE: String =
            ResourceUtils.CLASSPATH_URL_PREFIX + "com/bkahlert/kommons/logging/spring/logging.yml"
    }
}
