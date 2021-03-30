package koodies.exception

import koodies.concurrent.process
import koodies.concurrent.process.CommandLine
import koodies.terminal.AnsiCode.Companion.removeEscapeSequences
import koodies.test.UniqueId
import koodies.test.withTempDir
import koodies.text.LineSeparators
import koodies.text.isSingleLine
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode.CONCURRENT
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import strikt.assertions.startsWith
import java.nio.file.Path

@Execution(CONCURRENT)
class FormattingKtTest {

    private val emptyException = RuntimeException()

    private val runtimeException = RuntimeException("Something happened\n" +
        " ➜ A dump has been written to:\n" +
        "   - file:///var/folders/.../file.log (unchanged)\n" +
        "   - file:///var/folders/.../file.no-ansi.log (ANSI escape/control sequences removed)\n" +
        " ➜ The last lines are:\n" +
        "    raspberry\n" +
        "    Login incorrect\n" +
        "    raspberrypi login:")

    @Nested
    inner class AThrowable {

        @Test
        fun `should format compact`() {
            expectThat(runtimeException.toCompactString()) {
                startsWith("RuntimeException: Something happened at.(FormattingKtTest.kt:24)")
                isSingleLine()
            }
        }

        @Test
        fun `should format empty message`() {
            expectThat(emptyException.toCompactString()) {
                startsWith("RuntimeException at.(FormattingKtTest.kt:22)")
                isSingleLine()
            }
        }
    }

    @Nested
    inner class WithException {

        @Test
        fun `should format compact`() {
            expectThat(Result.failure<String>(runtimeException).toCompactString()) {
                startsWith("RuntimeException: Something happened at.(FormattingKtTest.kt:24)")
                isSingleLine()
            }
        }

        @Test
        fun `should format empty message`() {
            expectThat(Result.failure<String>(emptyException).toCompactString()) {
                startsWith("RuntimeException at.(FormattingKtTest.kt:22)")
                isSingleLine()
            }
        }
    }

    @Nested
    inner class AResult {

        @Nested
        inner class WithValue {

            @Test
            fun `should format compact`() {
                expectThat(Result.success("good").toCompactString()) {
                    get { removeEscapeSequences() }.isEqualTo("good")
                    isSingleLine()
                }
            }

            @Test
            fun `should format Path instances as URI`() {
                expectThat(Result.success(Path.of("/path")).toCompactString()) {
                    get { removeEscapeSequences() }.isEqualTo("file:///path")
                    isSingleLine()
                }
            }

            @Test
            fun `should format run processes as exit code`(uniqueId: UniqueId) = withTempDir(uniqueId) {
                expectThat(Result.success(process(CommandLine("exit", "42"), expectedExitValue = 42)).toCompactString()) {
                    get { removeEscapeSequences() }.isEqualTo("42")
                    isSingleLine()
                }
            }

            @Test
            fun `should format empty collection as empty brackets`() {
                expectThat(Result.success(emptyList<Any>()).toCompactString()) {
                    get { removeEscapeSequences() }.isEqualTo("[]")
                    isSingleLine()
                }
            }

            @Test
            fun `should format array like a list`() {
                expectThat(Result.success(arrayOf("a", "b")).toCompactString()) {
                    isEqualTo(Result.success(listOf("a", "b")).toCompactString())
                }
            }

            @Test
            fun `should format replace line breaks like a list`() {
                expectThat(LineSeparators.map { "line$it" }.joinToString("").toCompactString())
                    .isEqualTo("line⏎line⏎line⏎line⏎line⏎line")
            }
        }
    }
}
