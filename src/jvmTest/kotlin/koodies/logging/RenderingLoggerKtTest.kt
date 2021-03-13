package koodies.logging

import koodies.concurrent.Status
import koodies.concurrent.process.IO.Type.ERR
import koodies.concurrent.process.IO.Type.META
import koodies.concurrent.process.IO.Type.OUT
import koodies.io.path.containsAtMost
import koodies.io.path.containsExactly
import koodies.io.path.randomFile
import koodies.io.path.withExtension
import koodies.terminal.AnsiColors.red
import koodies.terminal.removeEscapeSequences
import koodies.test.UniqueId
import koodies.test.output.Columns
import koodies.test.output.InMemoryLoggerFactory
import koodies.test.testEach
import koodies.test.withTempDir
import koodies.text.Semantics
import koodies.text.matchesCurlyPattern
import koodies.text.wrap
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode.CONCURRENT
import org.junit.jupiter.api.parallel.ExecutionMode.SAME_THREAD
import strikt.api.expect
import strikt.api.expectThat
import strikt.assertions.contains
import strikt.assertions.endsWith
import strikt.assertions.first
import strikt.assertions.isA
import strikt.assertions.isEqualTo
import strikt.assertions.isFailure
import java.net.URI
import kotlin.io.path.extension
import kotlin.io.path.readLines

@Execution(CONCURRENT)
class RenderingLoggerKtTest {

    @Test
    fun @receiver:Columns(100) InMemoryLogger.`should log`() {
        logLine { "｀、ヽ｀ヽ｀、ヽ(ノ＞＜)ノ ｀、ヽ｀☂ヽ｀、ヽ" }
        logStatus { OUT typed "☎Σ⊂⊂(☉ω☉∩)" }
        logResult { Result.success(Unit) }

        expectThat(logged).matchesCurlyPattern(
            """
                    ╭──╴{}
                    │{}
                    │   ｀、ヽ｀ヽ｀、ヽ(ノ＞＜)ノ ｀、ヽ｀☂ヽ｀、ヽ
                    │   ☎Σ⊂⊂(☉ω☉∩)                                            {}                                      ▮▮
                    │{}
                    ╰──╴✔︎{}
                """.trimIndent()
        )
    }

    @Test
    fun @receiver:Columns(100) InMemoryLogger.`should log nested`() {
        logStatus { OUT typed "outer 1" }
        logStatus { OUT typed "outer 2" }
        logging("nested log", null) {
            logStatus { OUT typed "nested 1" }
            logStatus { OUT typed "nested 2" }
            logStatus { OUT typed "nested 3" }
        }
        logStatus { OUT typed "outer 3" }
        logStatus { OUT typed "outer 4" }
        logResult { Result.success("end") }

        expectThat(logged).matchesCurlyPattern(
            """
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
                """.trimIndent()
        )
    }

    @Execution(SAME_THREAD)
    @TestFactory
    fun @receiver:Columns(100) InMemoryLoggerFactory.`should log complex layouts`() = listOf(
        true to """
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
        false to """
            ▶ {}
            · outer 1                                                  {}                                      ▮▮
            · outer 2
            · ▶ nested log{}
            · · nested 1                                                {}                                      ▮▮
            · · mini segment 12345 sample ✔︎{}
            · · ▶ nested log{}
            · · · nested 1                                               {}                                      ▮▮
            · · · mini segment 12345 sample ✔︎{}
            · · · nested 2                                               {}                                      ▮▮
            · · · nested 3                                               {}                                      ▮▮
            · · ✔︎{}
            · · nested 2                                                {}                                      ▮▮
            · · nested 3                                                {}                                      ▮▮
            · ✔︎{}
            · outer 3                                                  {}                                      ▮▮
            · outer 4                                                  {}                                      ▮▮
            ✔︎{}
        """.trimIndent(),
    ).testEach("bordered={}") { (bordered, expectation) ->
        val label = if (bordered) "bordered" else "not-bordered"
        with(createLogger(label, bordered = bordered)) {
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
            logResult { Result.success(Unit) }

            expect { logged }.that { matchesCurlyPattern(expectation) }
        }
    }

    @Test
    fun @receiver:Columns(100) InMemoryLogger.`should log status`() {
        logStatus(listOf(StringStatus("getting phone call"))) { OUT typed "☎Σ⊂⊂(☉ω☉∩)" }
        logResult { Result.success(Unit) }

        expectThat(logged).matchesCurlyPattern(
            """
                    ╭──╴{}
                    │{}
                    │   ☎Σ⊂⊂(☉ω☉∩)                                            {}                                      ◀◀ getting phone call
                    │{}
                    ╰──╴✔︎{}
                """.trimIndent()
        )
    }

    @Suppress("LongLine")
    @Test
    fun @receiver:Columns(100) InMemoryLogger.`should log status in same column`() {
        logStatus(listOf(StringStatus("getting phone call"))) { OUT typed "☎Σ⊂⊂(☉ω☉∩)" }
        logging("nested", null) {
            logStatus(listOf(StringStatus("getting phone call"))) { OUT typed "☎Σ⊂⊂(☉ω☉∩)" }
        }
        logResult { Result.success(Unit) }

        expectThat(logged)
            .contains("│   ☎Σ⊂⊂(☉ω☉∩)                                                                                                    ◀◀ getting phone call")
            .contains("│   │   ☎Σ⊂⊂(☉ω☉∩)                                                                                                ◀◀ getting phone call")
            .not { contains("│   │   ☎Σ⊂⊂(☉ω☉∩)                                                                                                     ◀◀ getting phone call") } // too much indent
            .not { contains("│   │   ☎Σ⊂⊂(☉ω☉∩)                                                                                           ◀◀ getting phone call") } // too few indent
    }

    @Suppress("LongLine")
    @Test
    fun @receiver:Columns(10) InMemoryLogger.`should not break status line`() {
        logStatus(listOf(StringStatus("1234567890"))) { OUT typed "abc....xyz" }
        logging("nested", null) {
            logStatus(listOf(StringStatus("123456789 01234567890"))) { OUT typed "abc....xyz" }
            logging("nested", null) {
                logStatus(listOf(StringStatus("1234567890 1234567890 1234567890 1234567890"))) { OUT typed "abc....xyz" }
            }
        }
        logResult { Result.success(Unit) }

        expectThat(logged)
            .contains("│   │   │   ab          ◀◀ 1234567890 1234…567890 1234567890")
    }

    @Test
    fun @receiver:Columns(100) InMemoryLogger.`should log exception`() {
        kotlin.runCatching {
            logStatus { OUT typed "outer 1" }
            logStatus { OUT typed "outer 2" }
            logging("nested log", null) {
                logStatus { OUT typed "nested 1" }
                if ("1".toInt() == 1) throw IllegalStateException("an exception")
            }
            logStatus { OUT typed "☎Σ⊂⊂(☉ω☉∩)" }
            logStatus { OUT typed "☎Σ⊂⊂(☉ω☉∩)" }
            logResult { Result.success("success") }
        }

        expectThat(logged).matchesCurlyPattern(
            """
                    ╭──╴{}
                    │{}
                    │   outer 1                                               {}                                      ▮▮
                    │   outer 2                                               {}                                      ▮▮
                    │   ╭──╴nested log
                    │   │{}
                    │   │   nested 1                                          {}                                      ▮▮
                    │   ϟ{}
                    │   ╰──╴IllegalStateException: an exception at.(${RenderingLoggerKtTest::class.simpleName}.kt:{}){}
                """.trimIndent(), ignoreTrailingLines = true
        )
    }

    @Test
    fun @receiver:Columns(100) InMemoryLogger.`should simple log when closed twice`() {
        logResult { Result.success(Unit) }
        logResult { Result.success(Unit) }
        expectThat(logged)
            .containsAtMost("╰──╴", 1)
            .contains("✔︎")
    }

    @Test
    fun InMemoryLogger.`should wrap long lines`() {
        val status: (String) -> HasStatus = {
            object : HasStatus {
                override fun renderStatus(): String = it
            }
        }
        val shortLine = "┬┴┬┴┤(･_├┬┴┬┴"
        val longLine = "｀、ヽ｀ヽ｀、ヽ".repeat(10) + "ノ＞＜)ノ" + " ｀、ヽ｀、ヽ｀、ヽ".repeat(10)
        logLine { shortLine }
        logLine { longLine }
        logStatus(listOf(status(shortLine))) { OUT typed shortLine }
        logStatus(listOf(status(shortLine))) { OUT typed longLine }
        logStatus(listOf(status(longLine))) { OUT typed shortLine }
        logStatus(listOf(status(longLine))) { OUT typed longLine }
        logResult { Result.success(longLine) }

        expectThat(logged).matchesCurlyPattern(
            """
                ╭──╴{}
                │   
                │   ┬┴┬┴┤(･_├┬┴┬┴
                │   ｀、ヽ｀ヽ｀、ヽ｀、ヽ｀ヽ｀、ヽ｀、ヽ｀ヽ｀、ヽ｀、ヽ｀ヽ｀、ヽ｀、ヽ｀ヽ｀、ヽ｀、ヽ｀ヽ｀、ヽ｀、ヽ｀ヽ｀、ヽ｀、ヽ｀ヽ｀、ヽ｀、ヽ｀ヽ｀、ヽ｀、ヽ｀ヽ｀、ヽノ＞＜)ノ ｀、ヽ｀、ヽ｀、ヽ ｀、ヽ｀、ヽ｀、ヽ ｀、ヽ｀
                │   、ヽ｀、ヽ ｀、ヽ｀、ヽ｀、ヽ ｀、ヽ｀、ヽ｀、ヽ ｀、ヽ｀、ヽ｀、ヽ ｀、ヽ｀、ヽ｀、ヽ ｀、ヽ｀、ヽ｀、ヽ ｀、ヽ｀、ヽ｀、ヽ ｀、ヽ｀、ヽ｀、ヽ
                │   ┬┴┬┴┤(･_├┬┴┬┴                                                         ◀◀ ┬┴┬┴┤(･_├┬┴┬┴
                │   ｀、ヽ｀ヽ｀、ヽ｀、ヽ｀ヽ｀、ヽ｀、ヽ｀ヽ｀、ヽ｀、ヽ｀ヽ｀、ヽ｀、ヽ｀ヽ｀、ヽ｀、ヽ｀ヽ｀、ヽ｀、ヽ｀ヽ｀、ヽ｀、ヽ｀          ◀◀ ┬┴┬┴┤(･_├┬┴┬┴
                │   ヽ｀、ヽ｀、ヽ｀ヽ｀、ヽ｀、ヽ｀ヽ｀、ヽノ＞＜)ノ ｀、ヽ｀、ヽ｀、ヽ ｀、ヽ｀、ヽ｀、ヽ ｀、ヽ｀、ヽ｀、ヽ ｀、ヽ｀          
                │   、ヽ｀、ヽ ｀、ヽ｀、ヽ｀、ヽ ｀、ヽ｀、ヽ｀、ヽ ｀、ヽ｀、ヽ｀、ヽ ｀、ヽ｀、ヽ｀、ヽ ｀、ヽ｀、ヽ｀、ヽ ｀、ヽ｀          
                │   、ヽ｀、ヽ                                                                 
                │   ┬┴┬┴┤(･_├┬┴┬┴                                                         ◀◀ ｀、ヽ｀ヽ｀、ヽ｀、ヽ｀ヽ｀、ヽ｀、ヽ…ヽ ｀、ヽ｀、ヽ｀、ヽ ｀、ヽ｀、ヽ｀、ヽ
                │   ｀、ヽ｀ヽ｀、ヽ｀、ヽ｀ヽ｀、ヽ｀、ヽ｀ヽ｀、ヽ｀、ヽ｀ヽ｀、ヽ｀、ヽ｀ヽ｀、ヽ｀、ヽ｀ヽ｀、ヽ｀、ヽ｀ヽ｀、ヽ｀、ヽ｀          ◀◀ ｀、ヽ｀ヽ｀、ヽ｀、ヽ｀ヽ｀、ヽ｀、ヽ…ヽ ｀、ヽ｀、ヽ｀、ヽ ｀、ヽ｀、ヽ｀、ヽ
                │   ヽ｀、ヽ｀、ヽ｀ヽ｀、ヽ｀、ヽ｀ヽ｀、ヽノ＞＜)ノ ｀、ヽ｀、ヽ｀、ヽ ｀、ヽ｀、ヽ｀、ヽ ｀、ヽ｀、ヽ｀、ヽ ｀、ヽ｀          
                │   、ヽ｀、ヽ ｀、ヽ｀、ヽ｀、ヽ ｀、ヽ｀、ヽ｀、ヽ ｀、ヽ｀、ヽ｀、ヽ ｀、ヽ｀、ヽ｀、ヽ ｀、ヽ｀、ヽ｀、ヽ ｀、ヽ｀          
                │   、ヽ｀、ヽ                                                                 
                │
                ╰──╴✔︎
                """.trimIndent()
        )
    }

    @Test
    fun InMemoryLogger.`should not wrap URIs`() {
        val status: (String) -> HasStatus = {
            object : HasStatus {
                override fun renderStatus(): String = it
            }
        }
        val uriLine = URI.create("file:///some/where/on/this/computers/drive/in/some/directory/is/where/this/uri/points/to").toString().wrap("┬┴┬┴┤(･_├┬┴┬┴")
        logLine { uriLine }
        logStatus(listOf(status(uriLine))) { OUT typed uriLine }
        logResult { Result.success(uriLine) }

        expectThat(logged).containsExactly(uriLine, 2)
    }

    @Test
    fun @receiver:Columns(200) InMemoryLogger.`should log to file`(uniqueId: UniqueId) = withTempDir(uniqueId) {
        logLine { "｀、ヽ｀ヽ｀、ヽ(ノ＞＜)ノ ｀、ヽ｀☂ヽ｀、ヽ" }
        logStatus { OUT typed "☎Σ⊂⊂(☉ω☉∩)" }
        val ansiLog = randomFile("file-log", ".log")
        fileLogging(ansiLog, "Some logging heavy operation") {
            logLine { "line" }
            logStatus { OUT typed "☎Σ⊂⊂(☉ω☉∩)" }
            logException { RuntimeException("just a test") }
            logCaughtException { RuntimeException("covered") }
            "👍"
        }
        logLine { "Normal logging continues..." }
        logResult { Result.success(Unit) }

        expect {
            that(logged).matchesCurlyPattern(
                """
                    ╭──╴{}
                    │{}
                    │   ｀、ヽ｀ヽ｀、ヽ(ノ＞＜)ノ ｀、ヽ｀☂ヽ｀、ヽ
                    │   ☎Σ⊂⊂(☉ω☉∩)                                            {}                                      ▮▮
                    │   Some logging heavy operation Logging to ${Semantics.Document} ${ansiLog.toUri()} ✔︎
                    │   Normal logging continues...
                    │{}
                    ╰──╴✔︎{}
                """.trimIndent()
            )

            that(ansiLog.readLines().filter { it.isNotBlank() }) {
                first().removeEscapeSequences().isEqualTo("▶ Some logging heavy operation")
                get { last { it.isNotBlank() } }.removeEscapeSequences().endsWith("✔︎")
            }

            val noAnsiLog = ansiLog.withExtension("no-ansi.${ansiLog.extension}")
            that(noAnsiLog.readLines().filter { it.isNotBlank() }) {
                first().isEqualTo("▶ Some logging heavy operation")
                get { last { it.isNotBlank() } }.endsWith("✔︎")
            }
        }
    }

    @Suppress("UNREACHABLE_CODE")
    @Test
    fun `should show full exception only on outermost logger`() {
        val logger = InMemoryLogger("root", false, -1, emptyList())
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

            logger.logged.matchesCurlyPattern(
                """
                ▶ root
                · ▶ level 0
                · · doing stuff
                · · ▶ level 1
                · · · doing stuff
                · · · ▶ level 2
                · · · · doing stuff
                · · · ϟ RuntimeException: something happened at.(RenderingLoggerTest.kt:{})
                · · ϟ RuntimeException: something happened at.(RenderingLoggerTest.kt:{})
                · ϟ RuntimeException: something happened at.(RenderingLoggerTest.kt:{})
            """.trimIndent()
            )
        }
    }

    @Execution(SAME_THREAD)
    @TestFactory
    fun `should render multi-line caption`() = listOf(
        true to """
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
        false to """
            ╭──╴{}
            │   
            │   ▶ line #1
            │   ▷ line #2
            │   · logged line
            │   ✔︎
            │
            ╰──╴✔︎{}
        """.trimIndent(),
    ).testEach("bordered={}") { (bordered, expectation) ->
        val logger: InMemoryLogger = InMemoryLogger().applyLogging {
            logging(caption = "line #1\nline #2".red(), bordered = bordered) {
                logLine { "logged line" }
            }
        }

        expect { logger.logged }.that { matchesCurlyPattern(expectation) }
    }

    @Execution(SAME_THREAD)
    @TestFactory
    fun `should show unsuccessful return statuses`() = listOf(
        true to """
            ╭──╴{}
            │   
            │   ╭──╴{}
            │   │   
            │   │   logged line
            │   ϟ
            │   ╰──╴𝟷↩
            │   
            ϟ
            ╰──╴𝟷↩{}
        """.trimIndent(),
        false to """
            ╭──╴{}
            │   
            │   ▶ caption
            │   · logged line
            │   ϟ 𝟷↩
            ϟ
            ╰──╴𝟷↩{}
        """.trimIndent(),
    ).testEach("bordered={}") { (bordered, expectation) ->
        val logger: InMemoryLogger = InMemoryLogger().applyLogging {
            logging(caption = "caption", bordered = bordered) {
                logLine { "logged line" }
                Status.FAILURE
            }
        }

        expect { logger.logged }.that { matchesCurlyPattern(expectation) }
    }
}
