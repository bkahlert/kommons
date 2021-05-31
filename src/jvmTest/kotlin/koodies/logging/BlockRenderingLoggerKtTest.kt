package koodies.logging

import koodies.logging.FixedWidthRenderingLogger.Border.DOTTED
import koodies.logging.FixedWidthRenderingLogger.Border.NONE
import koodies.logging.FixedWidthRenderingLogger.Border.SOLID
import koodies.logging.InMemoryLogger.Companion.LOG_OPERATIONS
import koodies.logging.RenderingLogger.Companion.withUnclosedWarningDisabled
import koodies.test.Slow
import koodies.test.output.Bordered
import koodies.test.output.Columns
import koodies.test.output.InMemoryLoggerFactory
import koodies.test.testEach
import koodies.text.Unicode.characterTabulation
import koodies.text.matchesCurlyPattern
import koodies.text.toStringMatchesCurlyPattern
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory

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
    fun @receiver:Columns(60) InMemoryLogger.`should log status on same column`() {
        blockLogging("caption") {
            logStatus("status") { "text" }
            blockLogging("nested") {
                logStatus("status") { "text" }
            }
            blockLogging("nested") {
                logStatus("🔴🟠🟡🟢🔵🟣") { "🟥🟧🟨🟩🟦🟪" }
            }
        }

        expectThatLogged().matchesCurlyPattern("""
            {{}}
            │   │   text                                                              ◀◀ status
            {{}}
            │   │   │   text                                                          ◀◀ status
            {{}}
            │   │   │   🟥🟧🟨🟩🟦🟪                                                  ◀◀ 🔴🟠🟡🟢🔵🟣 
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
                @Suppress("SpellCheckingInspection")
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

    private fun borderTest(solidPattern: String, dottedPattern: String, nonePattern: String, block: FixedWidthRenderingLogger.() -> Any) = testEach(
        SOLID to solidPattern,
        DOTTED to dottedPattern,
        NONE to nonePattern,
        containerNamePattern = "border={}",
    ) { (border, expectation) ->
        val label = border.name
        val logger = InMemoryLogger(caption = "$label caption", border = border).withUnclosedWarningDisabled.apply { block() }
        asserting { logger.expectThatLogged().toStringMatchesCurlyPattern(expectation) }
    }

    @TestFactory
    fun `should not log without any log event`() = borderTest("", "", "") {}

    @TestFactory
    fun `should log caption on first log`() = borderTest(
        """
            ╭──╴SOLID caption
            │
            │   line
            │
            ╰──╴✔︎
        """.trimIndent(), """
            ▶ DOTTED caption
            · line
            ✔︎
        """.trimIndent(), """
            NONE caption
            line
            ✔︎
        """.trimIndent()) {
        logLine { "line" }
    }

    @TestFactory
    fun `should log text`() = borderTest(
        """
            ╭──╴SOLID caption
            │
            │   text
            │
            ╰──╴✔︎
        """.trimIndent(), """
            ▶ DOTTED caption
            · text
            ✔︎
        """.trimIndent(), """
            NONE caption
            text
            ✔︎
        """.trimIndent()) {
        logText { "text" }
    }

    @TestFactory
    fun `should log line`() = borderTest(
        """
            ╭──╴SOLID caption
            │
            │   line
            │
            ╰──╴✔︎
        """.trimIndent(), """
            ▶ DOTTED caption
            · line
            ✔︎
        """.trimIndent(), """
            NONE caption
            line
            ✔︎
        """.trimIndent()) {
        logLine { "line" }
    }

    @Nested
    inner class LogException {

        @Test
        fun @receiver:Bordered(SOLID) InMemoryLogger.`should log exception SOLID`() {
            logException { RuntimeException("exception") }

            expectThatLogged().matchesCurlyPattern(
                """
                    ╭──╴{}
                    │
                    │   java.lang.RuntimeException: exception
                    │   ${characterTabulation}at koodies.logging.{}
                    {{}}
                """.trimIndent()
            )
        }

        @Test
        fun @receiver:Bordered(DOTTED) InMemoryLogger.`should log exception DOTTED`() {
            logException { RuntimeException("exception") }

            expectThatLogged().matchesCurlyPattern(
                """
                    ▶ {}
                    · java.lang.RuntimeException: exception
                    · ${characterTabulation}at koodies.logging.{}
                    {{}}
                """.trimIndent()
            )
        }

        @Test
        fun @receiver:Bordered(NONE) InMemoryLogger.`should log exception NONE`() {
            logException { RuntimeException("exception") }

            expectThatLogged().matchesCurlyPattern(
                """
                    {}
                    java.lang.RuntimeException: exception
                    ${characterTabulation}at koodies.logging.{}
                    {{}}
                """.trimIndent()
            )
        }
    }

    @TestFactory
    fun `should log status`() = borderTest(
        """
            ╭──╴SOLID caption
            │
            │   line {} ◀◀ status
            │
            ╰──╴✔︎
        """.trimIndent(), """
            ▶ DOTTED caption
            · line {} ◀◀ status
            ✔︎
        """.trimIndent(), """
            NONE caption
            line {} ◀◀ status
            ✔︎
        """.trimIndent()) {
        logStatus(listOf("status")) { "line" }
    }

    @TestFactory
    fun `should log result`() = borderTest(
        """
            ╭──╴SOLID caption
            │
            │
            ╰──╴✔︎
        """.trimIndent(), """
            ▶ DOTTED caption
            ✔︎
        """.trimIndent(), """
            NONE caption
            ✔︎
        """.trimIndent()) {
        logResult { Result.success("result") }
    }

    @TestFactory
    fun `should log incomplete result`() = borderTest(
        """
            ╭──╴SOLID caption
            │
            ╵
            ╵
            ⏳️
        """.trimIndent(), """
            ▶ DOTTED caption
            ⏳️
        """.trimIndent(), """
            NONE caption
            ⏳️
        """.trimIndent()) {
        logResult {
            Result.success(InMemoryLogger.NO_RETURN_VALUE)
        }
    }

    @TestFactory
    fun `should log multiple results`() = borderTest(
        """
            ╭──╴SOLID caption
            │
            │
            ╰──╴✔︎
            ⏳️ ✔︎
            ⏳️ ✔︎
        """.trimIndent(), """
            ▶ DOTTED caption
            ✔︎
            ⏳️ ✔︎
            ⏳️ ✔︎
        """.trimIndent(), """
            NONE caption
            ✔︎
            ⏳️ ✔︎
            ⏳️ ✔︎
        """.trimIndent()) {
        logResult()
        logResult()
        logResult()
    }

    @TestFactory
    fun `should log multiple entries`() = borderTest(
        """
            ╭──╴SOLID caption
            │
            │   text
            │   line
            │   line {} ◀◀ status
            │
            ╰──╴✔︎
        """.trimIndent(), """
            ▶ DOTTED caption
            · text
            · line
            · line {} ◀◀ status
            ✔︎
        """.trimIndent(), """
            NONE caption
            text
            line
            line {} ◀◀ status
            ✔︎
        """.trimIndent()) {
        logText { "text" }
        logLine { "line" }
        logStatus("status") { "line" }
    }

    @Suppress("SpellCheckingInspection")
    @Nested
    inner class LoggingAfterResult {

        @Slow @TestFactory
        fun InMemoryLoggerFactory.`should log after logged result`() = testEach(*LOG_OPERATIONS) { (opName, op) ->
            val logger = createLogger(opName)
            var delegate: FixedWidthRenderingLogger? = null
            logger.blockLogging("test") {
                delegate = this
                logLine { "line" }
            }
            delegate?.op()
            expecting { logger } that {
                toStringMatchesCurlyPattern(
                    """
                        ╭──╴{}
                        │
                        │   ╭──╴test
                        │   │
                        │   │   line
                        │   │
                        │   ╰──╴✔︎
                        │   ⏳️ {}
                        {{}}
                    """.trimIndent()
                )
            }
        }

        @Slow @TestFactory
        fun InMemoryLoggerFactory.`should log after logged message and result`() = testEach(*LOG_OPERATIONS) { (opName, op) ->
            val logger = createLogger(opName)
            var delegate: FixedWidthRenderingLogger? = null
            logger.blockLogging("test") {
                delegate = this
            }
            delegate?.op()
            expecting { logger } that {
                toStringMatchesCurlyPattern(
                    """
                        ╭──╴{}
                        │
                        │   ╭──╴test
                        │   │
                        │   │
                        │   ╰──╴✔︎
                        │   ⏳️ {}
                        {{}}
                    """.trimIndent()
                )
            }
        }
    }
}
