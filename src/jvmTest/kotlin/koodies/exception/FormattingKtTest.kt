package koodies.exception

import koodies.exec.CommandLine
import koodies.test.UniqueId
import koodies.test.withTempDir
import koodies.text.LineSeparators
import koodies.text.LineSeparators.LF
import koodies.text.ansiRemoved
import koodies.text.isSingleLine
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode.CONCURRENT
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import strikt.assertions.matches
import strikt.assertions.startsWith
import java.nio.file.Path

@Execution(CONCURRENT)
class FormattingKtTest {

    private val emptyException = RuntimeException()

    private val runtimeException = RuntimeException("Something happened$LF" +
        " ➜ A dump has been written to:$LF" +
        "   - file:///var/folders/.../file.log (unchanged)$LF" +
        "   - file:///var/folders/.../file.ansi-removed.log (ANSI escape/control sequences removed)$LF" +
        " ➜ The last lines are:$LF" +
        "    raspberry$LF" +
        "    Login incorrect$LF" +
        "    raspberrypi login:")

    @Nested
    inner class AThrowable {

        @Test
        fun `should format compact`() {
            expectThat(runtimeException.toCompactString()) {
                startsWith("RuntimeException: Something happened at.(FormattingKtTest.kt:25)")
                isSingleLine()
            }
        }

        @Test
        fun `should format empty message`() {
            expectThat(emptyException.toCompactString()) {
                startsWith("RuntimeException at.(FormattingKtTest.kt:23)")
                isSingleLine()
            }
        }
    }

    @Nested
    inner class SuccessfulResult {

        @Test
        fun `should format compact`() {
            expectThat(Result.failure<String>(runtimeException).toCompactString()) {
                startsWith("RuntimeException: Something happened at.(FormattingKtTest.kt:25)")
                isSingleLine()
            }
        }

        @Test
        fun `should format empty message`() {
            expectThat(Result.failure<String>(emptyException).toCompactString()) {
                startsWith("RuntimeException at.(FormattingKtTest.kt:23)")
                isSingleLine()
            }
        }
    }

    @Nested
    inner class FailedResult {

        @Nested
        inner class WithValue {

            @Test
            fun `should format compact`() {
                expectThat(Result.success("good").toCompactString()) {
                    ansiRemoved.isEqualTo("good")
                    isSingleLine()
                }
            }

            @Test
            fun `should format Path instances as URI`() {
                expectThat(Result.success(Path.of("/path")).toCompactString()) {
                    ansiRemoved.isEqualTo("file:///path")
                    isSingleLine()
                }
            }

            @Test
            fun `should format processes as their status`(uniqueId: UniqueId) = withTempDir(uniqueId) {
                expectThat(Result.success(CommandLine("exit", "42").toExec(false, emptyMap(), this, null)).toCompactString()) {
                    ansiRemoved.matches("Process.*\\d+.*\\.".toRegex())
                    isSingleLine()
                }
            }

            @Test
            fun `should format empty collection as empty brackets`() {
                expectThat(Result.success(emptyList<Any>()).toCompactString()) {
                    ansiRemoved.isEqualTo("[]")
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
