package com.bkahlert.kommons.exception

import com.bkahlert.kommons.LineSeparators
import com.bkahlert.kommons.LineSeparators.LF
import com.bkahlert.kommons.LineSeparators.isSingleLine
import com.bkahlert.kommons.ansiRemoved
import com.bkahlert.kommons.exec.IOSequence
import com.bkahlert.kommons.exec.Process.State.Exited.Succeeded
import com.bkahlert.kommons.test.junit.SimpleId
import com.bkahlert.kommons.test.shouldMatchGlob
import com.bkahlert.kommons.test.withTempDir
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldMatch
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.nio.file.Paths
import java.time.Instant

// TODO delete
class FormattingKtTest {

    private val emptyException = RuntimeException()

    private val runtimeException = RuntimeException(
        "Something happened$LF" +
            " ➜ A dump has been written to:$LF" +
            "   - file:///var/folders/…/file.log (unchanged)$LF" +
            "   - file:///var/folders/…/file.ansi-removed.log (ANSI escape/control sequences removed)$LF" +
            " ➜ The last lines are:$LF" +
            "    raspberry$LF" +
            "    Login incorrect$LF" +
            "    raspberrypi login:"
    )

    @Nested
    inner class AThrowable {

        @Test
        fun `should format compact`() {
            runtimeException.toCompactString() should {
                it shouldMatchGlob "RuntimeException: Something happened at.(FormattingKtTest.kt:*)"
                it.isSingleLine() shouldBe true
            }
        }

        @Test
        fun `should format empty message`() {
            emptyException.toCompactString() should {
                it shouldMatchGlob "RuntimeException at.(FormattingKtTest.kt:*)"
                it.isSingleLine() shouldBe true
            }
        }
    }

    @Nested
    inner class SuccessfulResult {

        @Test
        fun `should format compact`() {
            Result.failure<String>(runtimeException).toCompactString() should {
                it shouldMatchGlob "RuntimeException: Something happened at.(FormattingKtTest.kt:*)"
                it.isSingleLine() shouldBe true
            }
        }

        @Test
        fun `should format empty message`() {
            Result.failure<String>(emptyException).toCompactString() should {
                it shouldMatchGlob "RuntimeException at.(FormattingKtTest.kt:*)"
                it.isSingleLine() shouldBe true
            }
        }
    }

    @Nested
    inner class FailedResult {

        @Nested
        inner class WithValue {

            @Test
            fun `should format compact`() {
                Result.success("good").toCompactString() should {
                    it.ansiRemoved shouldBe "\"good\""
                    it.isSingleLine() shouldBe true
                }
            }

            @Test
            fun `should format Path instances as URI`() {
                Result.success(Paths.get("/path")).toCompactString() should {
                    it.ansiRemoved shouldBe "file:///path"
                    it.isSingleLine() shouldBe true
                }
            }

            @Test
            fun `should format process status`(simpleId: SimpleId) = withTempDir(simpleId) {
                Result.success(Succeeded(Instant.MIN, Instant.MAX, 12345L, IOSequence.EMPTY)).toCompactString() should {
                    it.ansiRemoved.shouldMatch("Process.*\\d+.*Z".toRegex())
                    it.isSingleLine() shouldBe true
                }
            }

            @Test
            fun `should format empty collection as empty brackets`() {
                Result.success(emptyList<Any>()).toCompactString() should {
                    it.ansiRemoved shouldBe "[]"
                    it.isSingleLine() shouldBe true
                }
            }

            @Test
            fun `should format array like a list`() {
                Result.success(arrayOf("a", "b")).toCompactString() should {
                    it shouldBe Result.success(listOf("a", "b")).toCompactString()
                }
            }

            @Test
            fun `should format replace line breaks like a list`() {
                LineSeparators.Unicode.joinToString("") { "line$it" }.toCompactString()
                    .shouldBe("\"line\\r\\nline\\nline\\rline\u0085line\u2029line\u2028\"")
            }
        }
    }
}
