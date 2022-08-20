package com.bkahlert.kommons.logging.sample

import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.event.ContextRefreshedEvent
import org.springframework.context.event.EventListener

@SpringBootApplication
class SampleApplication {

    private val logger = LoggerFactory.getLogger(SampleApplication::class.java)

    @EventListener
    fun contextRefreshedEvent(event: ContextRefreshedEvent) {
        logger.info("Context refreshed: {}", event.applicationContext)
    }
}

fun main(args: Array<String>) {
    runApplication<SampleApplication>(*args)
}
