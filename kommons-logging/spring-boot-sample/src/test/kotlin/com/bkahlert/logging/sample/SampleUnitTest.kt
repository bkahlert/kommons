package com.bkahlert.kommons.sample

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.slf4j.LoggerFactory
import org.springframework.boot.test.system.CapturedOutput
import org.springframework.boot.test.system.OutputCaptureExtension

/**
 * This is a test that uses a logger configured by `logback.xml`
 */
@ExtendWith(OutputCaptureExtension::class)
class SampleUnitTest {

    private val logger = LoggerFactory.getLogger(javaClass)

    @Test
    fun `should log`(output: CapturedOutput) {
        logger.error("☁️ ⛅️ ☁️ ☁️ ☁️ ☁️ ☁️ ☁️")
        logger.warn("                      ")
        logger.info("      🎈              ")
        logger.debug("                      ")
        logger.trace("Test running... 🏃💨  ")

        // TODO test output
    }
}
