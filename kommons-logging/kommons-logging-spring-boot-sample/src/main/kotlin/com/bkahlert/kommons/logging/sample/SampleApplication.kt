package com.bkahlert.kommons.logging.sample

import com.bkahlert.kommons.logging.core.SLF4J
import com.bkahlert.kommons.logging.sample.helloworld.HelloWorldConfiguration
import org.springframework.boot.SpringBootConfiguration
import org.springframework.boot.autoconfigure.EnableAutoConfiguration
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Import
import org.springframework.context.event.ContextRefreshedEvent
import org.springframework.context.event.EventListener

@SpringBootConfiguration(proxyBeanMethods = false)
@EnableAutoConfiguration
@Import(HelloWorldConfiguration::class)
class SampleApplication {

    private val logger by SLF4J

    @EventListener
    fun contextRefreshedEvent(event: ContextRefreshedEvent) {
        logger.info("Context refreshed: {}", event.applicationContext)
    }
}

fun main(args: Array<String>) {
    runApplication<SampleApplication>(*args)
}
