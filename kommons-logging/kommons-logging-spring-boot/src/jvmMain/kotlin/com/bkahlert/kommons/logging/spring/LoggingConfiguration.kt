package com.bkahlert.kommons.logging.spring

import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.context.properties.EnableConfigurationProperties

/**
 * Auto-configuration of the Logback logging framework using presets.
 */
@AutoConfiguration
@EnableConfigurationProperties(LoggingProperties::class)
public open class LoggingConfiguration
