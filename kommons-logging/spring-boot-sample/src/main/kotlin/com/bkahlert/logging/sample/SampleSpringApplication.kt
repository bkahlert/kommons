package com.bkahlert.kommons.sample

import com.bkahlert.kommons.autoconfigure.logback.Banners
import com.fasterxml.jackson.core.JsonProcessingException
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.event.ContextRefreshedEvent
import org.springframework.context.event.EventListener
import org.springframework.core.env.ConfigurableEnvironment
import org.springframework.core.env.Environment
import java.util.Optional
import javax.annotation.PostConstruct

@SpringBootApplication
class SampleSpringApplication {

    private val logger = LoggerFactory.getLogger(javaClass)

    @Autowired
    private var environmentLogger: EnvironmentLogger? = null

    @PostConstruct
    fun init() {
        logger.error("☁️ ⛅️ ☁️ ☁️ ☁️ ☁️ ☁️ ☁️")
        logger.warn("                      ")
        logger.info("      🎈              ")
        logger.debug("                      ")
        logger.trace("App running...  🏃💨  ")
    }

    @EventListener @Throws(JsonProcessingException::class) fun contextRefreshedEvent(event: ContextRefreshedEvent) {
        val environment = Optional.of<Environment>(event.getApplicationContext().getEnvironment())
            .filter { obj: Environment? -> ConfigurableEnvironment::class.java.isInstance(obj) }
            .map { obj: Environment? -> ConfigurableEnvironment::class.java.cast(obj) }
            .orElseThrow { IllegalStateException() }
        logger.debug(environment.getProperty(Banners.SPRING_BANNER_LOCATION))
    }

    companion object {
        @JvmStatic fun main(args: Array<String>) {
            SpringApplication.run(SampleSpringApplication::class.java, "--spring.banner.location=classpath:banner.txt", "--build.version=37.5")
        }
    }
}

fun main(args: Array<String>) {
    runApplication<SampleSpringApplication>(*args)
}
