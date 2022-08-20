package com.bkahlert.kommons.logging.logback

import com.bkahlert.kommons.logging.logback.StructuredArguments.v
import com.bkahlert.kommons.test.logging.lastLog
import com.bkahlert.kommons.test.logging.logback.LogbackConfiguration
import com.bkahlert.kommons.test.spring.Captured
import com.bkahlert.kommons.test.testAll
import io.kotest.matchers.string.shouldMatch
import org.junit.jupiter.api.Test
import org.springframework.boot.test.system.CapturedOutput

class StatusLoggerTest {

    @LogbackConfiguration(debug = true)
    @Test fun info(@Captured output: CapturedOutput) = testAll {
        StatusLogger.info("TestLogger", "message with {}", v("key", "value"))
        output.lastLog shouldMatch Regex(
            """
            \d{2}:\d{2}:\d{2},\d{3} \|-INFO in TestLogger - message with value
            """.trimIndent()
        )
    }

    @LogbackConfiguration(debug = true)
    @Test fun info_with_exception(@Captured output: CapturedOutput) = testAll {
        StatusLogger.info("TestLogger", "message with {}", v("key", "value"), RuntimeException("message"))
        output.lastLog.lines(2) shouldMatch Regex(
            """
            \d{2}:\d{2}:\d{2},\d{3} \|-INFO in TestLogger - message with value java.lang.RuntimeException: message
            \tat java.lang.RuntimeException: message
            """.trimIndent()
        )
    }

    @LogbackConfiguration(debug = true)
    @Test fun warn(@Captured output: CapturedOutput) = testAll {
        StatusLogger.warn("TestLogger", "message with {}", v("key", "value"))
        output.lastLog shouldMatch Regex(
            """
            \d{2}:\d{2}:\d{2},\d{3} \|-WARN in TestLogger - message with value
            """.trimIndent()
        )
    }

    @LogbackConfiguration(debug = true)
    @Test fun warn_with_exception(@Captured output: CapturedOutput) = testAll {
        StatusLogger.warn("TestLogger", "message with {}", v("key", "value"), RuntimeException("message"))
        output.lastLog.lines(2) shouldMatch Regex(
            """
            \d{2}:\d{2}:\d{2},\d{3} \|-WARN in TestLogger - message with value java.lang.RuntimeException: message
            \tat java.lang.RuntimeException: message
            """.trimIndent()
        )
    }

    @LogbackConfiguration(debug = true)
    @Test fun error(@Captured output: CapturedOutput) = testAll {
        StatusLogger.error("TestLogger", "message with {}", v("key", "value"))
        output.lastLog shouldMatch Regex(
            """
            \d{2}:\d{2}:\d{2},\d{3} \|-ERROR in TestLogger - message with value
            """.trimIndent()
        )
    }

    @LogbackConfiguration(debug = true)
    @Test fun error_with_exception(@Captured output: CapturedOutput) = testAll {
        StatusLogger.error("TestLogger", "message with {}", v("key", "value"), RuntimeException("message"))
        output.lastLog.lines(2) shouldMatch Regex(
            """
            \d{2}:\d{2}:\d{2},\d{3} \|-ERROR in TestLogger - message with value java.lang.RuntimeException: message
            \tat java.lang.RuntimeException: message
            """.trimIndent()
        )
    }
}
