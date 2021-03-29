package koodies.logging

import koodies.logging.RenderingLogger.Companion.withUnclosedWarningDisabled
import koodies.test.output.Bordered
import koodies.test.output.Columns
import koodies.test.output.InMemoryLoggerFactory
import koodies.test.testEach
import koodies.text.matchesCurlyPattern
import koodies.text.toStringMatchesCurlyPattern
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode.CONCURRENT

@Execution(CONCURRENT)
class BlockRenderingLoggerKtTest {

    @Test
    fun InMemoryLogger.`should log`() {
        blockLogging("caption") {
            logLine { "line" }
            logStatus { "text" }
        }

        expectThatLogged().matchesCurlyPattern("""
            ╭──╴{}
            │   
            │   ╭──╴caption
            │   │   
            │   │   line
            │   │   text {} ▮▮
            │   │
            │   ╰──╴✔︎
            │
            ╰──╴✔︎
        """.trimIndent())
    }

    @Test
    fun InMemoryLogger.`should log nested`() {
        blockLogging("caption") {
            logLine { "outer 1" }
            logLine { "outer 2" }
            blockLogging("nested") {
                logLine { "nested 1" }
                logLine { "nested 2" }
                logLine { "nested 3" }
            }
            logLine { "outer 3" }
            logLine { "outer 4" }
        }

        expectThatLogged().matchesCurlyPattern("""
            ╭──╴{}
            │   
            │   ╭──╴caption
            │   │   
            │   │   outer 1
            │   │   outer 2
            │   │   ╭──╴nested
            │   │   │   
            │   │   │   nested 1
            │   │   │   nested 2
            │   │   │   nested 3
            │   │   │
            │   │   ╰──╴✔︎
            │   │   outer 3
            │   │   outer 4
            │   │
            │   ╰──╴✔︎
            │
            ╰──╴✔︎
        """.trimIndent())
    }

    @Test
    fun @receiver:Columns(60) InMemoryLogger.`should log status in same column`() {
        blockLogging("caption") {
            logStatus("status") { "text" }
            blockLogging("nested") {
                logStatus("status") { "text" }
            }
        }

        expectThatLogged().matchesCurlyPattern("""
            {{}}
            │   │   text                                                              ◀◀ status
            {{}}
            │   │   │   text                                                          ◀◀ status
            {{}}
        """.trimIndent())
    }

    @Nested
    inner class Wrapping {

        @Test
        fun @receiver:Columns(20) InMemoryLogger.`should wrap long line`() {
            logLine { "X".repeat(totalColumns + 1) }
            expectThatLogged().matchesCurlyPattern("""
                ╭──╴{}
                │   
                │   ${"X".repeat(totalColumns)}
                │   X
                │
                ╰──╴✔︎
            """.trimIndent())
        }

        @Test
        fun @receiver:Columns(20) InMemoryLogger.`should not wrap long text`() {
            logText { "X".repeat(totalColumns + 100) }
            expectThatLogged().matchesCurlyPattern("""
                ╭──╴{}
                │   
                │   ${"X".repeat(totalColumns + 100)}
                │
                ╰──╴✔︎
            """.trimIndent())
        }

        @Nested
        inner class LongStatus {

            @Test
            fun @receiver:Columns(20) InMemoryLogger.`should wrap long status text`() {
                logStatus { "X".repeat(20 + 1) }
                expectThatLogged().matchesCurlyPattern("""
                    ╭──╴{}
                    │   
                    │   ${"X".repeat(20)}          ▮▮
                    │   X                             
                    │
                    ╰──╴✔︎
                """.trimIndent())
            }

            @Test
            fun @receiver:Columns(20) InMemoryLogger.`should truncate long status element`() {
                logStatus("X".repeat(50)) { "X" }
                expectThatLogged().matchesCurlyPattern("""
                    ╭──╴{}
                    │   
                    │   X                             ◀◀ XXXXXXXXXXXXXXXXXXX…XXXXXXXXXXXXXXXXXXXXX
                    │
                    ╰──╴✔︎
                """.trimIndent())
            }
        }

        @Nested
        inner class URIs {

            @Test
            fun @receiver:Columns(20) InMemoryLogger.`should not wrap lines containing URIs`() {
                val text = "┬┴┬┴┤(･_├┬┴┬┴"
                val uri = "file://".padEnd(totalColumns + 1, 'X')
                logLine { uri }
                logLine { "$text$uri" }
                logLine { "$uri$text" }
                logLine { "$text$uri$text" }

                expectThatLogged().matchesCurlyPattern("""
                    ╭──╴{}
                    │   
                    │   $uri
                    │   $text$uri
                    │   $uri$text
                    │   $text$uri$text
                    │
                    ╰──╴✔︎
                """.trimIndent())
            }

            @Test
            fun @receiver:Columns(20) InMemoryLogger.`should not wrap status text containing URIs`() {
                val text = "┬┴┬┴┤(･_├┬┴┬┴"
                val uri = "file://".padEnd(totalColumns + 1, 'X')
                logStatus { uri }
                logStatus { "$text$uri" }
                logStatus { "$uri$text" }
                logStatus { "$text$uri$text" }

                expectThatLogged().matchesCurlyPattern("""
                    ╭──╴{}
                    │   
                    │   $uri▮▮
                    │   $text$uri▮▮
                    │   $uri$text▮▮
                    │   $text$uri$text▮▮
                    │
                    ╰──╴✔︎
                """.trimIndent())
            }
        }
    }

    private fun borderedTest(borderedPattern: String, nonBorderedPattern: String, block: RenderingLogger.() -> Any) = listOf(
        true to borderedPattern,
        false to nonBorderedPattern,
    ).testEach("bordered={}") { (bordered, expectation) ->
        val label = if (bordered) "bordered" else "not-bordered"
        val logger = InMemoryLogger(caption = "$label caption", bordered = bordered).withUnclosedWarningDisabled.apply { block() }
        test { logger.expectThatLogged().toStringMatchesCurlyPattern(expectation) }
    }

    @TestFactory
    fun `should not log without any log event`() = borderedTest("", "") {}

    @TestFactory
    fun `should log caption on first log`() = borderedTest(
        """
            ╭──╴bordered caption
            │{}
            │   line
            │{}
            ╰──╴✔︎
        """.trimIndent(), """
            ▶ not-bordered caption
            · line
            ✔︎
        """.trimIndent()) {
        logLine { "line" }
    }

    @TestFactory
    fun `should log text`() = borderedTest(
        """
            ╭──╴bordered caption
            │{}
            │   text
            │{}
            ╰──╴✔︎
        """.trimIndent(), """
            ▶ not-bordered caption
            · text
            ✔︎
        """.trimIndent()) {
        logText { "text" }
    }

    @TestFactory
    fun `should log line`() = borderedTest(
        """
            ╭──╴bordered caption
            │{}
            │   line
            │{}
            ╰──╴✔︎
        """.trimIndent(), """
            ▶ not-bordered caption
            · line
            ✔︎
        """.trimIndent()) {
        logLine { "line" }
    }

    @Nested
    inner class LogException {

        @Test
        fun @receiver:Bordered(true) InMemoryLogger.`should log exception bordered`() {
            logException { RuntimeException("exception") }

            expectThatLogged().matchesCurlyPattern(
                """
                    ╭──╴{}
                    │{}
                    │   java.lang.RuntimeException: exception
                    │   	at koodies.logging.{}
                    {{}}
                """.trimIndent()
            )
        }

        @Test
        fun @receiver:Bordered(false) InMemoryLogger.`should log exception not bordered`() {
            logException { RuntimeException("exception") }

            expectThatLogged().matchesCurlyPattern(
                """
                    ▶ {}
                    · java.lang.RuntimeException: exception
                    · 	at koodies.logging.{}
                    {{}}
                """.trimIndent()
            )
        }
    }

    @TestFactory
    fun `should log status`() = borderedTest(
        """
            ╭──╴bordered caption
            │{}
            │   line {} ◀◀ status
            │{}
            ╰──╴✔︎
        """.trimIndent(), """
            ▶ not-bordered caption
            · line {} ◀◀ status
            ✔︎
        """.trimIndent()) {
        logStatus("status") { "line" }
    }

    @TestFactory
    fun `should log result`() = borderedTest(
        """
            ╭──╴bordered caption
            │{}
            │{}
            ╰──╴✔︎
        """.trimIndent(), """
            ▶ not-bordered caption
            ✔︎
        """.trimIndent()) {
        logResult { Result.success("result") }
    }

    @TestFactory
    fun `should log incomplete result`() = borderedTest(
        """
            ╭──╴bordered caption
            │{}
            ╵
            ╵
            ⌛️ async computation
        """.trimIndent(), """
            ▶ not-bordered caption
            ⌛️ async computation
        """.trimIndent()) {
        logResult {
            Result.success(InMemoryLogger.NO_RETURN_VALUE)
        }
    }

    @TestFactory
    fun `should log multiple results`() = borderedTest(
        """
            ╭──╴bordered caption
            │{}
            │{}
            ╰──╴✔︎
            bordered caption ⌛️ ✔︎
            bordered caption ⌛️ ✔︎
        """.trimIndent(), """
            ▶ not-bordered caption
            ✔︎
            not-bordered caption ⌛️ ✔︎
            not-bordered caption ⌛️ ✔︎
        """.trimIndent()) {
        logResult()
        logResult()
        logResult()
    }

    @TestFactory
    fun `should log multiple entries`() = borderedTest(
        """
            ╭──╴bordered caption
            │{}
            │   text
            │   line
            │   line {} ◀◀ status
            │{}
            ╰──╴✔︎
        """.trimIndent(), """
            ▶ not-bordered caption
            · text
            · line
            · line {} ◀◀ status
            ✔︎
        """.trimIndent()) {
        logText { "text" }
        logLine { "line" }
        logStatus("status") { "line" }
    }

    @Nested
    inner class LoggingAfterResult {

        @TestFactory
        fun InMemoryLoggerFactory.`should log after logged result`() = InMemoryLogger.LOG_OPERATIONS.testEach { (opName, op) ->
            val logger = createLogger(opName)
            var delegate: BorderedRenderingLogger? = null
            logger.blockLogging("test") {
                delegate = this
                logLine { "line" }
            }
            delegate?.op()
            expect { logger }.that {
                toStringMatchesCurlyPattern(
                    """
                        ╭──╴{}
                        │   
                        │   ╭──╴test
                        │   │   
                        │   │   line
                        │   │
                        │   ╰──╴✔︎
                        │   test ⌛️ {}
                        {{}}
                    """.trimIndent()
                )
            }
        }

        @TestFactory
        fun InMemoryLoggerFactory.`should log after logged message and result`() = InMemoryLogger.LOG_OPERATIONS.testEach { (opName, op) ->
            val logger = createLogger(opName)
            var delegate: BorderedRenderingLogger? = null
            logger.blockLogging("test") {
                delegate = this
            }
            delegate?.op()
            expect { logger }.that {
                toStringMatchesCurlyPattern(
                    """
                        ╭──╴{}
                        │   
                        │   ╭──╴test
                        │   │   
                        │   │
                        │   ╰──╴✔︎
                        │   test ⌛️ {}
                        {{}}
                    """.trimIndent()
                )
            }
        }
    }
}
