package com.bkahlert.logging.logback

import com.bkahlert.kommons.test.testAll
import mu.KotlinLogging
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.api.fail
import org.slf4j.LoggerFactory
import org.springframework.boot.test.system.CapturedOutput
import org.springframework.boot.test.system.OutputCaptureExtension

private val logger = KotlinLogging.logger {}
private val logger2 = LoggerFactory.getLogger("com.bkahlert.logging.logback.LoggerFactoryTest")

@ExtendWith(OutputCaptureExtension::class)
class MyTest {

    @Test fun xxx(output: CapturedOutput) = testAll {
        logger.info { "Hello World" }
        logger2.info("Hello World")
        logger2.info("Hello World")
        logger2.info("Hello World")
        logger2.info("Hello World")
        fail("fail")
    }
}
