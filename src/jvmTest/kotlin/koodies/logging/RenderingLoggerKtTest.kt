package koodies.logging

import koodies.concurrent.Status
import koodies.concurrent.process.IO.ERR
import koodies.concurrent.process.IO.META
import koodies.concurrent.process.IO.OUT
import koodies.io.ByteArrayOutputStream
import koodies.io.path.randomFile
import koodies.io.path.withExtension
import koodies.logging.BorderedRenderingLogger.Border.DOTTED
import koodies.logging.BorderedRenderingLogger.Border.NONE
import koodies.logging.BorderedRenderingLogger.Border.SOLID
import koodies.logging.RenderingLogger.Companion.withUnclosedWarningDisabled
import koodies.runtime.Program
import koodies.terminal.AnsiCode.Companion.removeEscapeSequences
import koodies.terminal.AnsiColors.red
import koodies.terminal.escapeSequencesRemoved
import koodies.test.UniqueId
import koodies.test.output.Columns
import koodies.test.output.InMemoryLoggerFactory
import koodies.test.test
import koodies.test.testEach
import koodies.test.withTempDir
import koodies.text.LineSeparators
import koodies.text.Semantics
import koodies.text.matchesCurlyPattern
import koodies.text.toStringMatchesCurlyPattern
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode.SAME_THREAD
import strikt.api.expect
import strikt.api.expectThat
import strikt.assertions.contains
import strikt.assertions.containsExactly
import strikt.assertions.endsWith
import strikt.assertions.first
import strikt.assertions.isA
import strikt.assertions.isEqualTo
import strikt.assertions.isFailure
import strikt.assertions.isFalse
import strikt.assertions.isNotEqualTo
import strikt.assertions.isNotSameInstanceAs
import strikt.assertions.isNull
import strikt.assertions.isSameInstanceAs
import strikt.assertions.isTrue
import kotlin.io.path.extension
import kotlin.io.path.readLines
import kotlin.io.path.readText

@Execution(SAME_THREAD)
class RenderingLoggerKtTest {

    @Nested
    inner class Relations {

        @TestFactory
        fun `should equal only be identity`() = test(RenderingLogger("caption")) { logger ->
            expect { this }.that { isEqualTo(logger) }
            expect { this }.that { isSameInstanceAs(logger) }

            expect { this }.that { isNotEqualTo(RenderingLogger("caption")) }
            expect { this }.that { isNotSameInstanceAs(RenderingLogger("caption")) }

            expect { listOf(this) }.that { contains(logger) }
            expect { listOf(RenderingLogger("caption")) }.that { not { contains(logger) } }
        }

        @TestFactory
        fun `should have parent`() = test {
            val parent = RenderingLogger("parent")
            val child = RenderingLogger("child", parent)
            expect { parent.parent }.that { isNull() }
            expect { child.parent }.that { isEqualTo(parent) }
        }

        @TestFactory
        fun `should have ancestors`() = test {
            val parent = RenderingLogger("parent")
            val child = RenderingLogger("child", parent)
            expect { parent.ancestors }.that { containsExactly(parent) }
            expect { child.ancestors }.that { containsExactly(child, parent) }
            expect { child.ancestors }.that { not { contains(RenderingLogger("stranger")) } }

            expect { parent.isDescendantOf(child) }.that { isFalse() }
            expect { parent.isDescendantOf(parent) }.that { isTrue() }
            expect { child.isDescendantOf(parent) }.that { isTrue() }
            expect { child.isDescendantOf(child) }.that { isTrue() }
        }
    }

    @Test
    fun @receiver:Columns(100) InMemoryLogger.`should log`() {
        logLine { "｀、ヽ｀ヽ｀、ヽ(ノ＞＜)ノ ｀、ヽ｀☂ヽ｀、ヽ" }
        logStatus { OUT typed "☎Σ⊂⊂(☉ω☉∩)" }
        logResult { Result.success(Unit) }

        expectThatLogged().matchesCurlyPattern("""
            ╭──╴{}
            │{}
            │   ｀、ヽ｀ヽ｀、ヽ(ノ＞＜)ノ ｀、ヽ｀☂ヽ｀、ヽ
            │   ☎Σ⊂⊂(☉ω☉∩)                                            {}                                      ▮▮
            │{}
            ╰──╴✔︎{}
        """.trimIndent())
    }

    @Test
    fun @receiver:Columns(100) InMemoryLogger.`should log nested`() {
        logStatus { OUT typed "outer 1" }
        logStatus { OUT typed "outer 2" }
        logging("nested log") {
            logStatus { OUT typed "nested 1" }
            logStatus { OUT typed "nested 2" }
            logStatus { OUT typed "nested 3" }
        }
        logStatus { OUT typed "outer 3" }
        logStatus { OUT typed "outer 4" }
        logResult { Result.success("end") }

        expectThatLogged().matchesCurlyPattern("""
            ╭──╴{}
            │{}
            │   outer 1                                               {}                                      ▮▮
            │   outer 2                                               {}                                      ▮▮
            │   ╭──╴nested log
            │   │{}
            │   │   nested 1                                          {}                                      ▮▮
            │   │   nested 2                                          {}                                      ▮▮
            │   │   nested 3                                          {}                                      ▮▮
            │   │{}
            │   ╰──╴✔︎{}
            │   outer 3                                               {}                                      ▮▮
            │   outer 4                                               {}                                      ▮▮
            │{}
            ╰──╴✔︎{}
        """.trimIndent())
    }

    @TestFactory
    fun @receiver:Columns(100) InMemoryLoggerFactory.`should log complex layouts`() = listOf(
        SOLID to """
            ╭──╴{}
            │{}
            │   outer 1                                               {}                                      ▮▮
            │   outer 2{}
            │   ╭──╴nested log
            │   │{}
            │   │   nested 1                                          {}                                      ▮▮
            │   │   mini segment 12345 sample ✔︎
            │   │   ╭──╴nested log
            │   │   │{}
            │   │   │   nested 1                                      {}                                      ▮▮
            │   │   │   mini segment 12345 sample ✔︎
            │   │   │   nested 2                                      {}                                      ▮▮
            │   │   │   nested 3                                      {}                                      ▮▮
            │   │   │{}
            │   │   ╰──╴✔︎{}
            │   │   nested 2                                          {}                                      ▮▮
            │   │   nested 3                                          {}                                      ▮▮
            │   │{}
            │   ╰──╴✔︎{}
            │   outer 3                                               {}                                      ▮▮
            │   outer 4                                               {}                                      ▮▮
            │{}
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
    ).testEach("border={}") { (border, expectation) ->
        test {
            expect {
                createLogger(border.name, border).runLogging {
                    logStatus { OUT typed "outer 1" }
                    logLine { "outer 2" }
                    logging("nested log") {
                        logStatus { OUT typed "nested 1" }
                        compactLogging("mini segment") {
                            logStatus { ERR typed "12345" }
                            logStatus { META typed "sample" }
                        }
                        logging("nested log") {
                            logStatus { OUT typed "nested 1" }
                            compactLogging("mini segment") {
                                logStatus { ERR typed "12345" }
                                logStatus { META typed "sample" }
                            }
                            logStatus { OUT typed "nested 2" }
                            logStatus { OUT typed "nested 3" }
                        }
                        logStatus { OUT typed "nested 2" }
                        logStatus { OUT typed "nested 3" }
                    }
                    logStatus { OUT typed "outer 3" }
                    logStatus { OUT typed "outer 4" }
                    "Done"
                }
            }.that { toStringMatchesCurlyPattern(expectation) }
        }
    }

    @Suppress("LongLine")
    @Test
    fun @receiver:Columns(10) InMemoryLogger.`should not break status line`() {
        logStatus(listOf(StringStatus("1234567890"))) { OUT typed "abc....xyz" }
        logging("nested") {
            logStatus(listOf(StringStatus("123456789 01234567890"))) { OUT typed "abc....xyz" }
            logging("nested") {
                logStatus(listOf(StringStatus("1234567890 1234567890 1234567890 1234567890"))) { OUT typed "abc....xyz" }
            }
        }

        expectThatLogged()
            .contains("│   │   │   ab          ◀◀ 1234567890 1234…567890 1234567890")
    }

    @Test
    fun @receiver:Columns(100) InMemoryLogger.`should log exception`() {
        kotlin.runCatching {
            logStatus { OUT typed "outer 1" }
            logStatus { OUT typed "outer 2" }
            logging("nested log") {
                logStatus { OUT typed "nested 1" }
                if ("1".toInt() == 1) throw IllegalStateException("an exception")
            }
            logStatus { OUT typed "☎Σ⊂⊂(☉ω☉∩)" }
            logStatus { OUT typed "☎Σ⊂⊂(☉ω☉∩)" }
            logResult { Result.success("success") }
        }

        expectThatLogged().matchesCurlyPattern("""
            ╭──╴{}
            │{}
            │   outer 1                                               {}                                      ▮▮
            │   outer 2                                               {}                                      ▮▮
            │   ╭──╴nested log
            │   │{}
            │   │   nested 1                                          {}                                      ▮▮
            │   ϟ{}
            │   ╰──╴IllegalStateException: an exception at.(${RenderingLoggerKtTest::class.simpleName}.kt:{}){}
        """.trimIndent(), ignoreTrailingLines = true)
    }

    @Disabled
    @Test
    fun @receiver:Columns(100) InMemoryLogger.`should log caught exception`() {
        expectThat(logging("exception") {
            logLine { "line" }
            logCaughtException { RuntimeException("caught") }
            logLine { "line" }
            this
        }).toStringMatchesCurlyPattern("""
            
        """.trimIndent())
    }

    @Disabled
    @Test
    fun @receiver:Columns(200) InMemoryLogger.`should log to file`(uniqueId: UniqueId) = withTempDir(uniqueId) {
        logLine { "before" }
        val ansiLog = randomFile("file-log", ".log")
        fileLogging(ansiLog, "caption") {
            logLine { "line" }
            logStatus { "status" }
            logCaughtException { RuntimeException("caught") }
            "👍"
        }
        logLine { "after" }

        expectThatLogged().matchesCurlyPattern("""
            ╭──╴{}
            │{}
            │   before
            │   caption Logging to ${Semantics.Document} ${ansiLog.toUri()} ✔︎
            │   after
            │{}
            ╰──╴✔︎{}
        """.trimIndent())
        expect {
            that(ansiLog.also { it.readText() }.readLines().filter { it.isNotBlank() }) {
                first().escapeSequencesRemoved.isEqualTo("▶ caption")
                get { last { it.isNotBlank() } }.escapeSequencesRemoved.endsWith("✔︎")
            }

            val noAnsiLog = ansiLog.withExtension("no-ansi.${ansiLog.extension}")
            that(noAnsiLog.readLines().filter { it.isNotBlank() }) {
                first().isEqualTo("▶ caption")
                get { last { it.isNotBlank() } }.endsWith("✔︎")
            }
        }
    }

    @Suppress("UNREACHABLE_CODE")
    @Test
    fun `should show full exception only on outermost logger`() {
        val logger = InMemoryLogger("root", DOTTED).withUnclosedWarningDisabled
        expect {
            catching {
                logger.logging("level 0") {
                    logLine { "doing stuff" }
                    logging("level 1") {
                        logLine { "doing stuff" }
                        logging("level 2") {
                            logLine { "doing stuff" }
                            throw RuntimeException("something happened\nsomething happened #2\nsomething happened #3")
                            logStatus { OUT typed "doing stuff" }
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
        val logger = InMemoryLogger("root", DOTTED).withUnclosedWarningDisabled
        expect {
            catching {
                logger.logging("level 0") {
                    logLine { "doing stuff" }
                    logging("level 1") {
                        logLine { "doing stuff" }
                        logging("level 2") {
                            logLine { "doing stuff" }
                            throw RuntimeException("something happened\nsomething happened #2\nsomething happened #3")
                            logStatus { OUT typed "doing stuff" }
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

    @Execution(SAME_THREAD)
    @TestFactory
    fun `should render multi-line caption`() = listOf(
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
    ).testEach("border={}") { (border, expectation) ->
        val logger: InMemoryLogger = InMemoryLogger().applyLogging {
            logging(caption = "line #1\nline #2".red(), border = border) {
                logLine { "logged line" }
            }
        }

        expect { logger }.that { toStringMatchesCurlyPattern(expectation) }
    }

    @Execution(SAME_THREAD)
    @TestFactory
    fun `should show unsuccessful return statuses`() = listOf(
        SOLID to """
            ╭──╴{}
            │   
            │   ╭──╴{}
            │   │   
            │   │   logged line
            │   ϟ
            │   ╰──╴𝟷↩
            ϟ
            ╰──╴𝟷↩{}
        """.trimIndent(),
        DOTTED to """
            ╭──╴{}
            │   
            │   ▶ caption
            │   · logged line
            │   ϟ 𝟷↩
            ϟ
            ╰──╴𝟷↩{}
        """.trimIndent(),
    ).testEach("border={}") { (border, expectation) ->
        val logger: InMemoryLogger = InMemoryLogger().withUnclosedWarningDisabled.applyLogging {
            logging(caption = "caption", border = border) {
                logLine { "logged line" }
                Status.FAILURE
            }
        }

        test { logger.expectThatLogged().matchesCurlyPattern(expectation) }
    }

    @Nested
    inner class LoggingAfterResult {

        private fun createLogger(caption: String, init: RenderingLogger.() -> Unit): Pair<ByteArrayOutputStream, RenderingLogger> {
            val baos = ByteArrayOutputStream()
            return baos to RenderingLogger(caption) {
                if (Program.isDebugging) print(it)
                baos.write(it.removeEscapeSequences().toByteArray())
            }.apply(init)
        }

        @TestFactory
        fun `should log after logged result`() = InMemoryLogger.LOG_OPERATIONS.testEach { (opName, op) ->
            val (out, logger) = createLogger(opName) {
                logLine { "line" }
                logResult()
            }

            logger.op()

            expect { out }.that {
                toStringMatchesCurlyPattern("""
                    line
                    ✔︎
                    ⌛️ {}
                """.trimIndent())
            }
        }

        @TestFactory
        fun `should log after logged message and result`() = InMemoryLogger.LOG_OPERATIONS.testEach { (opName, op) ->
            val (out, logger) = createLogger(opName) {
                logResult()
            }

            logger.op()

            expect { out }.that {
                toStringMatchesCurlyPattern("""
                    ✔︎
                    ⌛️ {}
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
                    ⌛️ line 1
                    ⌛️ line 2
                    ⌛️ text 1
                    ⌛️ text 2
                """.trimIndent())
        }
    }

    @Nested
    inner class ToString {

        @Test
        fun `should contain closed state`() {
            val logger = RenderingLogger("test")
            expectThat(logger).toStringMatchesCurlyPattern("""
                RenderingLogger { open = false{}parent = null{}ancestors = test{}caption = test }
            """.trimIndent())
        }
    }
}
