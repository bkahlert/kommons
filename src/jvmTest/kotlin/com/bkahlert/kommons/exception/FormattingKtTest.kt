package com.bkahlert.kommons.exception

import com.bkahlert.kommons.exec.IOSequence
import com.bkahlert.kommons.exec.Process.State.Exited.Succeeded
import com.bkahlert.kommons.test.junit.UniqueId
import com.bkahlert.kommons.test.withTempDir
import com.bkahlert.kommons.text.LineSeparators
import com.bkahlert.kommons.text.LineSeparators.LF
import com.bkahlert.kommons.text.ansiRemoved
import com.bkahlert.kommons.text.isSingleLine
import com.bkahlert.kommons.text.matchesCurlyPattern
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import strikt.assertions.matches
import java.nio.file.Path
import java.time.Instant

class FormattingKtTest {

    private val emptyException = RuntimeException()

    private val runtimeException = RuntimeException("Something happened$LF" +
        " ➜ A dump has been written to:$LF" +
        "   - file:///var/folders/…/file.log (unchanged)$LF" +
        "   - file:///var/folders/…/file.ansi-removed.log (ANSI escape/control sequences removed)$LF" +
        " ➜ The last lines are:$LF" +
        "    raspberry$LF" +
        "    Login incorrect$LF" +
        "    raspberrypi login:")

    @Nested
    inner class AThrowable {

        @Test
        fun `should format compact`() {
            expectThat(runtimeException.toCompactString()) {
                matchesCurlyPattern("RuntimeException: Something happened at.(FormattingKtTest.kt:{})")
                isSingleLine()
            }
        }

        @Test
        fun `should format empty message`() {
            expectThat(emptyException.toCompactString()) {
                matchesCurlyPattern("RuntimeException at.(FormattingKtTest.kt:{})")
                isSingleLine()
            }
        }
    }

    @Nested
    inner class SuccessfulResult {

        @Test
        fun `should format compact`() {
            expectThat(Result.failure<String>(runtimeException).toCompactString()) {
                matchesCurlyPattern("RuntimeException: Something happened at.(FormattingKtTest.kt:{})")
                isSingleLine()
            }
        }

        @Test
        fun `should format empty message`() {
            expectThat(Result.failure<String>(emptyException).toCompactString()) {
                matchesCurlyPattern("RuntimeException at.(FormattingKtTest.kt:{})")
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
            fun `should format process status`(uniqueId: UniqueId) = withTempDir(uniqueId) {
                expectThat(Result.success(Succeeded(Instant.MIN, Instant.MAX, 12345L, IOSequence.EMPTY)).toCompactString()) {
                    ansiRemoved.matches("Process.*\\d+.*Z".toRegex())
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
                expectThat(LineSeparators.joinToString("") { "line$it" }.toCompactString())
                    .isEqualTo("line⏎line⏎line⏎line⏎line⏎line")
            }
        }
    }
}
