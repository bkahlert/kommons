package com.bkahlert.logging.logback

import com.bkahlert.logging.support.RawOutputCapture.Companion.capture
import com.bkahlert.logging.support.SmartCapturedOutput
import org.junit.jupiter.api.Test
import org.slf4j.LoggerFactory
import strikt.api.expectThat
import strikt.assertions.contains
import strikt.assertions.isEmpty

class AnsiLoggingTest {
    private val logger = LoggerFactory.getLogger(javaClass)

    @Test
    fun `should test using working ANSI encoding detection sequence`() {
        expectThat("5.378 [39mDEBUG[0;39m [i").contains(ANSI_ESCAPE)
    }

    @Test
    fun `should log using ANSI encoding`() {
        val output: SmartCapturedOutput = capture { logger.info("Test") }
        expectThat(output.out).contains(ANSI_ESCAPE)
        expectThat(output.err).isEmpty()
    }

    companion object {
        const val ANSI_ESCAPE = "\u001B"
    }
}
