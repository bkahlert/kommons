package com.bkahlert.kommons.logging.spring

import org.springframework.boot.autoconfigure.EnableAutoConfiguration
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer

@Configuration(proxyBeanMethods = false)
@Import(LogConfiguringEnvironmentPostProcessor::class)
@EnableAutoConfiguration(exclude = [LoggingAutoConfiguration::class])
open class TestConfig
