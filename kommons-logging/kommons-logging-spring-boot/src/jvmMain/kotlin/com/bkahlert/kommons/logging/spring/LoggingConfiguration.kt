package com.bkahlert.kommons.logging.spring

import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Configuration

/**
 * Configuration of the Logback logging framework using presets.
 */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(LoggingProperties::class)
public open class LoggingConfiguration
