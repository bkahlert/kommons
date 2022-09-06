package com.bkahlert.kommons.logging.sample

import com.bkahlert.kommons.logging.core.SLF4J
import io.kotest.matchers.string.shouldContain
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.boot.test.system.CapturedOutput
import org.springframework.boot.test.system.OutputCaptureExtension

@ExtendWith(OutputCaptureExtension::class)
class SampleUnitTest {

    private val logger by SLF4J

    @Test fun `should log`(output: CapturedOutput) {
        val message = "Unit test running... 🏃💨"
        logger.info(message)
        output.out shouldContain message
    }
}
