package koodies.logging

import koodies.concurrent.completableFuture
import koodies.concurrent.process.IO
import koodies.debug.CapturedOutput
import koodies.io.ByteArrayOutputStream
import koodies.logging.LoggingContext.Companion.BACKGROUND
import koodies.runtime.JVM
import koodies.test.SystemIoExclusive
import koodies.test.toStringContains
import koodies.test.toStringIsEqualTo
import koodies.text.ANSI
import koodies.text.ANSI.Text.Companion.ansi
import koodies.text.ANSI.ansiRemoved
import koodies.text.LineSeparators.LF
import koodies.text.matchesCurlyPattern
import koodies.text.prefixLinesWith
import koodies.text.randomString
import koodies.text.toStringMatchesCurlyPattern
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode.SAME_THREAD
import strikt.api.Assertion
import strikt.api.expectThat
import strikt.assertions.contains
import strikt.assertions.isEmpty
import strikt.assertions.isEqualTo
import strikt.assertions.isNotNull
import java.util.concurrent.CompletableFuture

@Execution(SAME_THREAD)
class LoggingContextTest {

    private val out: ByteArrayOutputStream = ByteArrayOutputStream()
    private lateinit var root: LoggingContext

    public val <T : FixedWidthRenderingLogger> Assertion.Builder<T>.logged: Assertion.Builder<String>
        get() = get("recorded log %s") { with(root) { logged } }

    @BeforeEach
    fun setup() {
        root = LoggingContext("test") {
            out.write(it.toByteArray())
            // TODO write tiny verbosity extension to get information to control if to log to console as well or not
            print(it.prefixLinesWith(IO.ERASE_MARKER))
        }
    }

    @AfterEach
    fun tearDown() {
        root.logResult()
        root.reset()
        out.reset()
    }

    @Nested
    inner class Root {

        @Test
        fun `should be logging context`() {
            expectThat(root::class).isEqualTo(LoggingContext::class)
        }

        @Test
        fun `should leave system out unchanged`() {
            expectThat(System.out).isNotNull()
        }

        @Test
        fun `should log with no extras`() {
            root.logLine { "line" }
            expectThat(root).logged.toStringIsEqualTo("line")
        }
    }

    @Test
    fun `should print to test system out`() {
        val random: String = randomString(42)
        root.logging("test") { logLine { random } }
        expectThat(out).printed.toStringContains(random)
    }

    @Test
    fun `should log to context out`() {
        root.logging("test") { logLine { "line" }; 42 }
        expectThat(out).printed.toStringMatchesCurlyPattern("""
            ╭──╴test
            │   
            │   line
            │
            ╰──╴✔︎
        """.trimIndent())
    }

    @Test
    fun `should record log`() {
        root.logging("test 1") { logLine { "line 1" }; 42 }
        root.logging("test 2") { logLine { "line 2" }; 42 }
        root.logging("test 3") { logLine { "line 3" }; 42 }
        expectThat(root).logged.matchesCurlyPattern("""
            ╭──╴test 1
            │   
            │   line 1
            │
            ╰──╴✔︎
            ╭──╴test 2
            │   
            │   line 2
            │
            ╰──╴✔︎
            ╭──╴test 3
            │   
            │   line 3
            │
            ╰──╴✔︎
        """.trimIndent())
    }

    @Test
    fun `should make last logger available`() {
        root.logging("test") { logLine { "line" }; 42 }
        expectThat(root.mostRecent).logged.matchesCurlyPattern("""
            ╭──╴test
            │   
            │   line
            │
            ╰──╴✔︎
        """.trimIndent())
    }

    @Test
    fun `should make default to root if no last logger found`() {
        expectThat(root.mostRecent).logged.isEmpty()
    }


    @Test
    fun `should handle concurrent access`() {
        val result = listOf("foo", "bar", "baz").map { name ->
            completableFuture {
                val color = ANSI.Colors.random(120)
                root.logging(name, decorationFormatter = color, contentFormatter = color) {
                    (0..20).forEach { logLine { "${JVM.currentThread.name}: $name $it" } }
                }
            }
        }.toTypedArray().let { CompletableFuture.allOf(*it) }

        val color = ANSI.Colors.random(20)
        root.logging("main", decorationFormatter = color, contentFormatter = color) {
            (0..5).forEach { logLine { "${JVM.currentThread.name}: shared $it" } }
            with(root) {
                runExclusive {
                    (10..15).forEach { logLine { "exclusive $it" } }
                    logging("exclusive child") {
                        (15..20).forEach { logLine { "exclusive child $it" } }
                    }
                }
            }
            (20..25).forEach { logLine { "${JVM.currentThread.name}: shared $it" } }
        }

        result.join()

        expectThat(out).printed.matchesCurlyPattern("""
            {{}}
            ╭──╴test
            │   
            │   exclusive 10
            │   exclusive 11
            │   exclusive 12
            │   exclusive 13
            │   exclusive 14
            │   exclusive 15
            │   ╭──╴exclusive child
            │   │   
            │   │   exclusive child 15
            │   │   exclusive child 16
            │   │   exclusive child 17
            │   │   exclusive child 18
            │   │   exclusive child 19
            │   │   exclusive child 20
            │   │
            │   ╰──╴✔︎
            │
            ╰──╴✔︎
            {{}}
        """.trimIndent())
    }

    @Nested
    inner class Global {

        @SystemIoExclusive
        @Test
        fun `should prefix log messages with IO erase marker`(capturedOutput: CapturedOutput) {
            with(BACKGROUND) {
                logLine { "This does not appear in the captured output." }
                logLine { "But it shows on the actual console.".ansi.italic }
            }
            print("This line is captured.$LF")

            BACKGROUND.expectLogged
                .contains("This does not appear in the captured output.")
                .contains("But it shows on the actual console.")

            expectThat(capturedOutput.toString()).isEqualTo("This line is captured.$LF")
        }
    }
}

private val Assertion.Builder<ByteArrayOutputStream>.printed: Assertion.Builder<String>
    get() = get("printed to simulated system out") { toString() }

/**
 * Returns an [Assertion.Builder] for all log messages recorded in this [LoggingContext].
 */
public val LoggingContext.expectLogged
    get() = expectThat(this).logged

/**
 * Returns an [Assertion.Builder] for all log messages recorded in the asserted [LoggingContext].
 */
public val Assertion.Builder<LoggingContext>.logged: Assertion.Builder<String>
    get() = get("record log messages in %s") { logged.ansiRemoved }
