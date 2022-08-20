package com.bkahlert.kommons.autoconfigure.logback

import net.logstash.logback.argument.StructuredArguments.v
import org.slf4j.LoggerFactory
import org.springframework.core.env.ConfigurableEnvironment
import org.springframework.core.env.MapPropertySource
import org.springframework.core.env.PropertySource
import org.springframework.util.ResourceUtils
import java.util.Collections

public object Banners {
    private val logger = LoggerFactory.getLogger(javaClass)

    public const val SPRING_BANNER_LOCATION: String = "spring.banner.location"
    public const val SPRING_MAIN_BANNER_MODE: String = "spring.main.banner-mode"

    private val SPRING_CLOUD_BANNER_LOCATION = ResourceUtils.CLASSPATH_URL_PREFIX + "com/bkahlert/kommons/logging/logback/autoconfigure/banner-boot+cloud.txt"
    private val LOG_MESSAGE = "Added {} to environment"

    public fun turnOffBanner(environment: ConfigurableEnvironment) {
        val propertySource = newPropertySource(SPRING_MAIN_BANNER_MODE, "off")
        environment.propertySources.addFirst(propertySource)
        logger.debug(LOG_MESSAGE, v(PropertySource::class.java.simpleName, propertySource))
    }

    public fun useSpringCloudBanner(environment: ConfigurableEnvironment) {
        val propertySource = newPropertySource(SPRING_BANNER_LOCATION, SPRING_CLOUD_BANNER_LOCATION)
        environment.propertySources.addLast(propertySource)
        logger.debug(LOG_MESSAGE, v(PropertySource::class.java.simpleName, propertySource))
    }

    private fun newPropertySource(key: String, value: String): MapPropertySource {
        val propertySourceName = Thread.currentThread().stackTrace[2].methodName
        return MapPropertySource(propertySourceName, Collections.singletonMap<String, Any>(key, value))
    }
}
