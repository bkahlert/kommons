package com.bkahlert.kommons.logging.sample.helloworld

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.ConstructorBinding
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

/** Configuration of the hello world feature. */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(HelloWorldConfigurationProperties::class)
class HelloWorldConfiguration {

    @Bean
    fun helloWorldController(props: HelloWorldConfigurationProperties) =
        HelloWorldController(props)
}

/** Configuration properties for the hello world feature. */
@ConstructorBinding
@ConfigurationProperties("hello-world")
data class HelloWorldConfigurationProperties(
    /** How to greet. */
    var greeting: String = "Hello"
)
