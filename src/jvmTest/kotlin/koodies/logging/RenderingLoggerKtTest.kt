package koodies.logging

import koodies.exec.IO
import koodies.io.ByteArrayOutputStream
import koodies.logging.FixedWidthRenderingLogger.Border.DOTTED
import koodies.logging.FixedWidthRenderingLogger.Border.NONE
import koodies.logging.FixedWidthRenderingLogger.Border.SOLID
import koodies.logging.InMemoryLogger.Companion.LOG_OPERATIONS
import koodies.logging.RenderingLogger.Companion.withUnclosedWarningDisabled
import koodies.runtime.isDebugging
import koodies.test.Smoke
import koodies.test.output.Columns
import koodies.test.output.InMemoryLoggerFactory
import koodies.test.testEach
import koodies.text.ANSI.Text.Companion.ansi
import koodies.text.ANSI.ansiRemoved
import koodies.text.LineSeparators
import koodies.text.matchesCurlyPattern
import koodies.text.toStringMatchesCurlyPattern
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory
import strikt.api.expect
import strikt.api.expectThat
import strikt.assertions.contains
import strikt.assertions.endsWith
import strikt.assertions.isA
import strikt.assertions.isFailure

@Smoke
class RenderingLoggerKtTest {

    @Test
    fun @receiver:Columns(100) InMemoryLogger.`should log`() {
        logLine { "｀、ヽ｀ヽ｀、ヽ(ノ＞＜)ノ ｀、ヽ｀☂ヽ｀、ヽ" }
        logStatus { "☎Σ⊂⊂(☉ω☉∩)" }
        logResult { Result.success(Unit) }

        expectThatLogged().matchesCurlyPattern("""
            ╭──╴{}
            │
            │   ｀、ヽ｀ヽ｀、ヽ(ノ＞＜)ノ ｀、ヽ｀☂ヽ｀、ヽ
            │   ☎Σ⊂⊂(☉ω☉∩)                                            {}                                      ▮▮
            │
            ╰──╴✔︎{}
        """.trimIndent())
    }

    @Test
    fun @receiver:Columns(100) InMemoryLogger.`should log nested`() {
        logStatus { "outer 1" }
        logStatus { "outer 2" }
        logging("nested log") {
            logStatus { "nested 1" }
            logStatus { "nested 2" }
            logStatus { "nested 3" }
        }
        logStatus { "outer 3" }
        logStatus { "outer 4" }
        logResult { Result.success("end") }

        expectThatLogged().matchesCurlyPattern("""
            ╭──╴{}
            │
            │   outer 1                                               {}                                      ▮▮
            │   outer 2                                               {}                                      ▮▮
            │   ╭──╴nested log
            │   │
            │   │   nested 1                                          {}                                      ▮▮
            │   │   nested 2                                          {}                                      ▮▮
            │   │   nested 3                                          {}                                      ▮▮
            │   │
            │   ╰──╴✔︎{}
            │   outer 3                                               {}                                      ▮▮
            │   outer 4                                               {}                                      ▮▮
            │
            ╰──╴✔︎{}
        """.trimIndent())
    }

    @Smoke @TestFactory
    fun @receiver:Columns(100) InMemoryLoggerFactory.`should log complex layouts`() = testEach(
        SOLID to """
            ╭──╴{}
            │
            │   outer 1                                               {}                                      ▮▮
            │   outer 2{}
            │   ╭──╴nested log
            │   │
            │   │   nested 1                                          {}                                      ▮▮
            │   │   mini segment 12345 sample ✔︎
            │   │   ╭──╴nested log
            │   │   │
            │   │   │   nested 1                                      {}                                      ▮▮
            │   │   │   mini segment 12345 sample ✔︎
            │   │   │   nested 2                                      {}                                      ▮▮
            │   │   │   nested 3                                      {}                                      ▮▮
            │   │   │
            │   │   ╰──╴✔︎{}
            │   │   nested 2                                          {}                                      ▮▮
            │   │   nested 3                                          {}                                      ▮▮
            │   │
            │   ╰──╴✔︎{}
            │   outer 3                                               {}                                      ▮▮
            │   outer 4                                               {}                                      ▮▮
            │
            ╰──╴✔︎{}
        """.trimIndent(),
        DOTTED to """
            ▶ {}
            · outer 1                                                 {}                                      ▮▮
            · outer 2
            · ▶ nested log{}
            · · nested 1                                              {}                                      ▮▮
            · · mini segment 12345 sample ✔︎{}
            · · ▶ nested log{}
            · · · nested 1                                            {}                                      ▮▮
            · · · mini segment 12345 sample ✔︎{}
            · · · nested 2                                            {}                                      ▮▮
            · · · nested 3                                            {}                                      ▮▮
            · · ✔︎{}
            · · nested 2                                              {}                                      ▮▮
            · · nested 3                                              {}                                      ▮▮
            · ✔︎{}
            · outer 3                                                 {}                                      ▮▮
            · outer 4                                                 {}                                      ▮▮
            ✔︎{}
        """.trimIndent(),
        NONE to """
            {}
            outer 1                                                                                                       ▮▮
            outer 2
            nested log
            nested 1                                                                                                      ▮▮
            mini segment 12345 sample ✔︎
            nested log
            nested 1                                                                                                      ▮▮
            mini segment 12345 sample ✔︎
            nested 2                                                                                                      ▮▮
            nested 3                                                                                                      ▮▮
            ✔︎
            nested 2                                                                                                      ▮▮
            nested 3                                                                                                      ▮▮
            ✔︎
            outer 3                                                                                                       ▮▮
            outer 4                                                                                                       ▮▮
            ✔︎
        """.trimIndent(),
    ) { (border, expectation) ->
        expecting {
            lateinit var logger: InMemoryLogger
            createLogger(border.name, border).runLogging {
                logger = this
                logStatus { "outer 1" }
                logLine { "outer 2" }
                logging("nested log") {
                    logStatus { "nested 1" }
                    compactLogging("mini segment") {
                        logStatus { IO.Error typed "12345" }
                        logStatus { IO.Meta typed "sample" }
                    }
                    logging("nested log") {
                        logStatus { "nested 1" }
                        compactLogging("mini segment") {
                            logStatus { IO.Error typed "12345" }
                            logStatus { IO.Meta typed "sample" }
                        }
                        logStatus { "nested 2" }
                        logStatus { "nested 3" }
                    }
                    logStatus { "nested 2" }
                    logStatus { "nested 3" }
                }
                logStatus { "outer 3" }
                logStatus { "outer 4" }
                "Done"
            }
            logger
        } that { toStringMatchesCurlyPattern(expectation) }
    }

    @Suppress("LongLine")
    @Test
    fun @receiver:Columns(10) InMemoryLogger.`should not break status line`() {
        logStatus("1234567890") { "abc....xyz" }
        logging("nested") {
            logStatus("123456789 01234567890") { "abc....xyz" }
            logging("nested") {
                logStatus(listOf("1234567890 1234567890 1234567890 1234567890")) { "abc....xyz" }
            }
        }

        expectThatLogged()
            .contains("│   │   │   ab          ◀◀ 1234567890 1234…567890 1234567890")
    }

    @Test
    fun @receiver:Columns(100) InMemoryLogger.`should log exception`() {
        kotlin.runCatching {
            logStatus { "outer 1" }
            logStatus { "outer 2" }
            logging("nested log") {
                logStatus { "nested 1" }
                if ("1".toInt() == 1) throw IllegalStateException("an exception")
            }
            logStatus { "☎Σ⊂⊂(☉ω☉∩)" }
            logStatus { "☎Σ⊂⊂(☉ω☉∩)" }
            logResult { Result.success("success") }
        }

        expectThatLogged().matchesCurlyPattern("""
            ╭──╴{}
            │
            │   outer 1                                               {}                                      ▮▮
            │   outer 2                                               {}                                      ▮▮
            │   ╭──╴nested log
            │   │
            │   │   nested 1                                          {}                                      ▮▮
            │   ϟ{}
            │   ╰──╴IllegalStateException: an exception at.(${RenderingLoggerKtTest::class.simpleName}.kt:{}){}
        """.trimIndent(), ignoreTrailingLines = true)
    }

    @Suppress("UNREACHABLE_CODE")
    @Test
    fun `should show full exception only on outermost logger`() {
        val logger = InMemoryLogger("root", null, DOTTED).withUnclosedWarningDisabled
        expect {
            catching {
                logger.logging("level 0") {
                    logLine { "doing stuff" }
                    logging("level 1") {
                        logLine { "doing stuff" }
                        logging("level 2") {
                            logLine { "doing stuff" }
                            throw RuntimeException("something happened\nsomething happened #2\nsomething happened #3")
                            logStatus { "doing stuff" }
                            2
                        }
                        logLine { "doing stuff" }
                    }
                    logLine { "doing stuff" }
                }
            }.isFailure().isA<RuntimeException>()

            logger.expectThatLogged(closeIfOpen = false).matchesCurlyPattern("""
                ▶ root
                · ▶ level 0
                · · doing stuff
                · · ▶ level 1
                · · · doing stuff
                · · · ▶ level 2
                · · · · doing stuff
                · · · ϟ RuntimeException: something happened at.({}.kt:{})
                · · ϟ RuntimeException: something happened at.({}.kt:{})
                · ϟ RuntimeException: something happened at.({}.kt:{})
            """.trimIndent())
        }
    }

    @Suppress("UNREACHABLE_CODE")
    @Test
    fun `should show full exception logger already closed`() {
        val logger = InMemoryLogger("root", null, DOTTED).withUnclosedWarningDisabled
        expect {
            catching {
                logger.logging("level 0") {
                    logLine { "doing stuff" }
                    logging("level 1") {
                        logLine { "doing stuff" }
                        logging("level 2") {
                            logLine { "doing stuff" }
                            throw RuntimeException("something happened\nsomething happened #2\nsomething happened #3")
                            logStatus { "doing stuff" }
                            2
                        }
                        logLine { "doing stuff" }
                    }
                    logLine { "doing stuff" }
                }
            }.isFailure().isA<RuntimeException>()

            logger.expectThatLogged(closeIfOpen = false).matchesCurlyPattern("""
                ▶ root
                · ▶ level 0
                · · doing stuff
                · · ▶ level 1
                · · · doing stuff
                · · · ▶ level 2
                · · · · doing stuff
                · · · ϟ RuntimeException: something happened at.({}.kt:{})
                · · ϟ RuntimeException: something happened at.({}.kt:{})
                · ϟ RuntimeException: something happened at.({}.kt:{})
            """.trimIndent())
        }
    }

    @TestFactory
    fun `should render multi-line caption`() = testEach(
        SOLID to """
            ╭──╴{}
            │
            │   ╭──╴line #1
            │   │   line #2
            │   │
            │   │   logged line
            │   │
            │   ╰──╴✔︎
            │
            ╰──╴✔︎{}
        """.trimIndent(),
        DOTTED to """
            ╭──╴{}
            │
            │   ▶ line #1
            │   ▷ line #2
            │   · logged line
            │   ✔︎
            │
            ╰──╴✔︎{}
        """.trimIndent(),
        containerNamePattern = "border={}",
    ) { (border, expectation) ->
        val logger: InMemoryLogger = InMemoryLogger().applyLogging {
            logging(caption = "line #1\nline #2".ansi.red, border = border) {
                logLine { "logged line" }
            }
        }

        expecting { logger } that { toStringMatchesCurlyPattern(expectation) }
    }

    @Nested
    inner class LoggingAfterResult {

        private fun createLogger(
            caption: String,
            parent: RenderingLogger? = null,
            init: RenderingLogger.() -> Unit,
        ): Pair<ByteArrayOutputStream, RenderingLogger> {
            val baos = ByteArrayOutputStream()
            return baos to RenderingLogger(caption, parent) {
                if (isDebugging) print(it)
                baos.write(it.ansiRemoved.toByteArray())
            }.apply(init)
        }

        @TestFactory
        fun `should log after logged result`() = testEach(*LOG_OPERATIONS) { (opName, op) ->
            val (out, logger) = createLogger(opName) {
                logLine { "line" }
                logResult()
            }

            logger.op()

            expecting { out } that {
                toStringMatchesCurlyPattern("""
                    line
                    ✔︎
                    ⏳️ {}
                """.trimIndent())
            }
        }

        @TestFactory
        fun `should log after logged message and result`() = testEach(*LOG_OPERATIONS) { (opName, op) ->
            val (out, logger) = createLogger(opName) {
                logResult()
            }

            logger.op()

            expecting { out } that {
                toStringMatchesCurlyPattern("""
                    ✔︎
                    ⏳️ {}
                """.trimIndent())
            }
        }

        @Test
        fun `should log multi-line after logged result`() {
            val (out, logger) = createLogger("multi-line") {
                logResult()
            }

            logger.logLine { "line 1\nline 2" }
            logger.logText { "text 1\ntext 2" }

            expectThat(out.toString())
                .endsWith(LineSeparators.LF)
                .matchesCurlyPattern("""
                    ✔︎
                    ⏳️ line 1
                    ⏳️ line 2
                    ⏳️ text 1
                    ⏳️ text 2
                """.trimIndent())
        }
    }

    @Nested
    inner class ToString {

        @Test
        fun `should contain closed state`() {
            val logger = RenderingLogger("test", null)
            expectThat(logger).toStringMatchesCurlyPattern("""
                RenderingLogger { open = false{}caption = test }
            """.trimIndent())
        }
    }
}
