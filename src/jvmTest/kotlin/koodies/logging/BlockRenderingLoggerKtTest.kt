package koodies.logging

import koodies.logging.FixedWidthRenderingLogger.Border.DOTTED
import koodies.logging.FixedWidthRenderingLogger.Border.NONE
import koodies.logging.FixedWidthRenderingLogger.Border.SOLID
import koodies.logging.SimpleRenderingLogger.Companion.withUnclosedWarningDisabled
import koodies.test.output.Bordered
import koodies.test.output.Columns
import koodies.test.testEach
import koodies.text.matchesCurlyPattern
import koodies.text.toStringMatchesCurlyPattern
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory

class BlockRenderingLoggerKtTest {

    @Test
    fun InMemoryLogger.`should log`() {
        blockLogging("name") {
            logLine { "line" }
            logStatus { "text" }
        }

        expectThatLogged().matchesCurlyPattern("""
            ╭──╴{}
            │
            │   ╭──╴name
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
        blockLogging("name") {
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
            │   ╭──╴name
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
        blockLogging("name") {
            logStatus("status") { "text" }
            blockLogging("nested") {
                logStatus("status") { "text" }
            }
            blockLogging("nested") {
                logStatus("🔴🟠🟡🟢🔵🟣") { "🟥🟧🟨🟩🟦🟪" }
                logStatus { "1234567890".repeat(7) }
            }
        }

        expectThatLogged().matchesCurlyPattern("""
            {{}}
            │   │   text                                                              ◀◀ status
            {{}}
            │   │   │   text                                                          ◀◀ status
            {{}}
            │   │   │   🟥🟧🟨🟩🟦🟪                                                  ◀◀ 🔴🟠🟡🟢🔵🟣 
            │   │   │   1234567890123456789012345678901234567890123456789012          ▮▮ 
            │   │   │   345678901234567890                                            
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
        fun @receiver:Columns(20) InMemoryLogger.`should wrap long text`() {
            logText { "X".repeat(totalColumns + 1) }
            expectThatLogged().matchesCurlyPattern("""
                ╭──╴{}
                │
                │   ${"X".repeat(totalColumns)}
                │   X
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
        val logger = InMemoryLogger(name = "$label name", border = border).withUnclosedWarningDisabled.apply { block() }
        asserting { logger.expectThatLogged().toStringMatchesCurlyPattern(expectation) }
    }

    @TestFactory
    fun `should not log without any log event`() = borderTest("", "", "") {}

    @TestFactory
    fun `should log name on first log`() = borderTest(
        """
            ╭──╴SOLID name
            │
            │   line
            │
            ╰──╴✔︎
        """.trimIndent(), """
            ▶ DOTTED name
            · line
            ✔︎
        """.trimIndent(), """
            NONE name
            line
            ✔︎
        """.trimIndent()) {
        logLine { "line" }
    }

    @TestFactory
    fun `should log text`() = borderTest(
        """
            ╭──╴SOLID name
            │
            │   text
            │
            ╰──╴✔︎
        """.trimIndent(), """
            ▶ DOTTED name
            · text
            ✔︎
        """.trimIndent(), """
            NONE name
            text
            ✔︎
        """.trimIndent()) {
        logText { "text" }
    }

    @TestFactory
    fun `should log line`() = borderTest(
        """
            ╭──╴SOLID name
            │
            │   line
            │
            ╰──╴✔︎
        """.trimIndent(), """
            ▶ DOTTED name
            · line
            ✔︎
        """.trimIndent(), """
            NONE name
            line
            ✔︎
        """.trimIndent()) {
        logLine { "line" }
    }

    @Nested
    inner class LogException {

        @Test
        fun @receiver:Bordered(SOLID) InMemoryLogger.`should log exception SOLID`() {
            kotlin.runCatching { blockLogging("name") { throw RuntimeException("exception") } }

            expectThatLogged().matchesCurlyPattern(
                """
                    {{}}
                    │   ╭──╴name
                    │   │
                    │   ϟ
                    │   ╰──╴RuntimeException: exception at.(BlockRenderingLoggerKtTest.kt:{})
                    {{}}
                """.trimIndent()
            )
        }

        @Test
        fun @receiver:Bordered(DOTTED) InMemoryLogger.`should log exception DOTTED`() {
            kotlin.runCatching { blockLogging("name") { throw RuntimeException("exception") } }

            expectThatLogged().matchesCurlyPattern(
                """
                    {{}}
                    ▶ {}
                    · ϟ RuntimeException: exception at.(BlockRenderingLoggerKtTest.kt:{})
                    {{}}
                """.trimIndent()
            )
        }

        @Test
        fun @receiver:Bordered(NONE) InMemoryLogger.`should log exception NONE`() {
            kotlin.runCatching { blockLogging("name") { throw RuntimeException("exception") } }

            expectThatLogged().matchesCurlyPattern(
                """
                    {{}}
                    ϟ RuntimeException: exception at.(BlockRenderingLoggerKtTest.kt:{})
                    {{}}
                """.trimIndent()
            )
        }
    }

    @TestFactory
    fun `should log status`() = borderTest(
        """
            ╭──╴SOLID name
            │
            │   line {} ◀◀ status
            │
            ╰──╴✔︎
        """.trimIndent(), """
            ▶ DOTTED name
            · line {} ◀◀ status
            ✔︎
        """.trimIndent(), """
            NONE name
            line {} ◀◀ status
            ✔︎
        """.trimIndent()) {
        logStatus(listOf("status")) { "line" }
    }

    @TestFactory
    fun `should log result`() = borderTest(
        """
            ╭──╴SOLID name
            │
            │
            ╰──╴✔︎
        """.trimIndent(), """
            ▶ DOTTED name
            ✔︎
        """.trimIndent(), """
            NONE name
            ✔︎
        """.trimIndent()) {
        logResult { Result.success("result") }
    }

    @TestFactory
    fun `should log multiple entries`() = borderTest(
        """
            ╭──╴SOLID name
            │
            │   text
            │   line
            │   line {} ◀◀ status
            │
            ╰──╴✔︎
        """.trimIndent(), """
            ▶ DOTTED name
            · text
            · line
            · line {} ◀◀ status
            ✔︎
        """.trimIndent(), """
            NONE name
            text
            line
            line {} ◀◀ status
            ✔︎
        """.trimIndent()) {
        logText { "text" }
        logLine { "line" }
        logStatus("status") { "line" }
    }
}
