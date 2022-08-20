package com.bkahlert.kommons.logging.sample

import com.bkahlert.kommons.logging.logback.Logback
import org.junit.jupiter.api.Test
import org.slf4j.LoggerFactory
import org.springframework.boot.logging.LogFile
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.core.env.Environment

@SpringBootTest(
    properties = [
        "logging.preset.console=minimal",
        "logging.preset.file=json",
    ]
)
class SampleSpringBootTest(val environment: Environment) {
    private val logger = LoggerFactory.getLogger(SampleSpringBootTest::class.java)

    @Test
    fun `should log`() {
        logger.info("Logging to {} according to Logback", Logback.activeLogFile)
        logger.info("Logging to {} according to Spring", LogFile.get(environment))
        logger.error("☁️ ⛅️ ☁️ ☁️ ☁️ ☁️ ☁️ ☁️")
        logger.warn("                      ")
        logger.info("      🎈              ")
        logger.debug("                      ")
        logger.trace("Test running... 🏃💨  ")
    }
}
