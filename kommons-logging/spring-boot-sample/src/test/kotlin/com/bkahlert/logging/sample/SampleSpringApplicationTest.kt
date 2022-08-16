package com.bkahlert.logging.sample

import com.bkahlert.logging.logback.LogbackConfiguration
import org.junit.jupiter.api.Test
import org.slf4j.LoggerFactory
import org.springframework.boot.logging.LogFile
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.core.env.Environment

/**
 * This is a spring boot test that uses a logger configured by `logback-spring.xml`
 */
@SpringBootTest(properties = ["logging.level.app=TRACE"])
class SampleSpringApplicationTest(
    val environment: Environment,
) {

    private val logger = LoggerFactory.getLogger(javaClass)

    @Test
    fun `should log`() {
        logger.info("Logging to {} according to Logback", LogbackConfiguration.activeLogFileName)
        logger.info("Logging to {} according to Spring", LogFile.get(environment))
        logger.error("☁️ ⛅️ ☁️ ☁️ ☁️ ☁️ ☁️ ☁️")
        logger.warn("                      ")
        logger.info("      🎈              ")
        logger.debug("                      ")
        logger.trace("Test running... 🏃💨  ")
    }
}
