package com.bkahlert.kommons.logging.sample

import io.kotest.matchers.string.shouldContain
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.slf4j.LoggerFactory
import org.springframework.boot.test.system.CapturedOutput
import org.springframework.boot.test.system.OutputCaptureExtension

@ExtendWith(OutputCaptureExtension::class)
class SampleUnitTest {

    private val logger = LoggerFactory.getLogger(SampleUnitTest::class.java)

    @Test fun `should log`(output: CapturedOutput) {
        val message = "Test running... 🏃💨"
        logger.info(message)
        output.out shouldContain message
    }
}
