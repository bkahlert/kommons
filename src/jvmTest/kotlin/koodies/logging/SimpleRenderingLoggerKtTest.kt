package koodies.logging

import koodies.exec.IO
import koodies.logging.FixedWidthRenderingLogger.Border.DOTTED
import koodies.logging.FixedWidthRenderingLogger.Border.NONE
import koodies.logging.FixedWidthRenderingLogger.Border.SOLID
import koodies.logging.SimpleRenderingLogger.Companion.withUnclosedWarningDisabled
import koodies.test.Smoke
import koodies.test.output.Columns
import koodies.test.output.InMemoryLoggerFactory
import koodies.test.testEach
import koodies.text.ANSI.Text.Companion.ansi
import koodies.text.matchesCurlyPattern
import koodies.text.toStringMatchesCurlyPattern
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory
import strikt.api.expect
import strikt.api.expectThat
import strikt.assertions.contains
import strikt.assertions.isA
import strikt.assertions.isFailure

@Smoke
class SimpleRenderingLoggerKtTest {

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
        val logged = capturing {
            logText { "outer 1" }
            logging("nested log") {
                logText { "nested 1" }
                if ("1".toInt() == 1) throw IllegalStateException("an exception")
            }
        }

        logged.matchesCurlyPattern("""
            outer 1
            ╭──╴nested log
            │
            │   nested 1
            ϟ
            ╰──╴IllegalStateException: an exception at.(SimpleRenderingLoggerKtTest.kt:{})
        """.trimIndent())
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
    fun `should render multi-line name`() = testEach(
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
            logging(name = "line #1\nline #2".ansi.red, border = border) {
                logLine { "logged line" }
            }
        }

        expecting { logger } that { toStringMatchesCurlyPattern(expectation) }
    }

    @Nested
    inner class ToString {

        @Test
        fun `should contain closed state`() {
            val logger = SimpleRenderingLogger("test", null)
            expectThat(logger).toStringMatchesCurlyPattern("""
                SimpleRenderingLogger { open = false{}name = test }
            """.trimIndent())
        }
    }
}
