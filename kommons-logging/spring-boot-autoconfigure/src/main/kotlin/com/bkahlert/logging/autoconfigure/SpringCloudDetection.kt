package com.bkahlert.logging.autoconfigure

import org.slf4j.LoggerFactory
import org.springframework.cloud.bootstrap.BootstrapApplicationListener
import org.springframework.context.ConfigurableApplicationContext
import org.springframework.core.env.ConfigurableEnvironment
import org.springframework.core.env.EnvironmentCapable

public object SpringCloudDetection {
    private val logger = LoggerFactory.getLogger(javaClass)

    public const val SPRING_CLOUD_BOOTSTRAP_ENABLED: String = "spring.cloud.bootstrap.enabled"
    public const val SPRING_CLOUD_BOOTSTRAP_NAME: String = "spring.cloud.bootstrap.name"
    public const val DEFAULT_SPRING_CLOUD_BOOTSTRAP_NAME: String = "bootstrap"

    public fun isSpringCloud(applicationContext: ConfigurableApplicationContext): Boolean {
        return isBootstrapApplicationContext(applicationContext) || isBootstrapApplicationChildContext(applicationContext)
    }

    public fun isSpringCloud(environment: ConfigurableEnvironment): Boolean {
        return isBootstrapApplicationContext(environment) || isBootstrapApplicationChildContext(environment)
    }

    public fun isBootstrapApplicationContext(applicationContext: ConfigurableApplicationContext): Boolean {
        return getBootstrapApplicationId(applicationContext) == applicationContext.getId() || isBootstrapApplicationContext(applicationContext.getEnvironment())
    }

    public fun isBootstrapApplicationContext(environment: ConfigurableEnvironment): Boolean {
        return environment.propertySources.contains(BootstrapApplicationListener.BOOTSTRAP_PROPERTY_SOURCE_NAME)
    }

    public fun isBootstrapApplicationChildContext(applicationContext: ConfigurableApplicationContext): Boolean {
        return isBootstrapApplicationChildContext(applicationContext.getEnvironment())
    }

    public fun isBootstrapApplicationChildContext(environment: ConfigurableEnvironment): Boolean {
        return environment.propertySources.contains(BootstrapApplicationListener.DEFAULT_PROPERTIES)
    }

    public fun getBootstrapApplicationId(applicationContext: EnvironmentCapable): String {
        return applicationContext.environment.getProperty(SPRING_CLOUD_BOOTSTRAP_NAME, DEFAULT_SPRING_CLOUD_BOOTSTRAP_NAME)
    }
}
