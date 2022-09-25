package com.bkahlert.kommons_deprecated.shell

import com.bkahlert.kommons.test.junit.testEach
import com.bkahlert.kommons.text.LineSeparators
import com.bkahlert.kommons_deprecated.io.path.appendText
import com.bkahlert.kommons_deprecated.io.path.hasContent
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory
import org.junit.jupiter.api.io.TempDir
import strikt.api.expectThat
import java.nio.file.Path
import kotlin.io.path.readText

class FileOperationsTest {

    @Nested
    inner class RemoveLine {

        private fun Path.file(lineSeparator: String): Path =
            resolve("${lineSeparator.hashCode()}.txt").apply {
                appendText("line 1$lineSeparator")
                appendText("line 2$lineSeparator")
                appendText("line 2.1$lineSeparator")
                appendText("last line")
            }

        @TestFactory
        fun `should remove intermediary line`(@TempDir tempDir: Path) = testEach(*LineSeparators.Common) { lineSeparator ->
            val fixture = tempDir.file(lineSeparator)
            ShellScript { file(fixture) { removeLine("line 2") } }.exec.logging()
            fixture.readText() shouldBe "line 1${lineSeparator}line 2.1${lineSeparator}last line"
        }

        @TestFactory
        fun `should remove last line`(@TempDir tempDir: Path) = testEach(*LineSeparators.Common) { lineSeparator ->
            val fixture = tempDir.file(lineSeparator)
            ShellScript { file(fixture) { removeLine("last line") } }.exec.logging()
            fixture.readText() shouldBe "line 1${lineSeparator}line 2${lineSeparator}line 2.1$lineSeparator"
        }
    }

    @Nested
    inner class AppendLine {

        private fun Path.file(): Path =
            resolve("file.txt").apply {
                appendText(
                    """
                    line 1
                    line 2

                """.trimIndent()
                )
            }

        @Test
        fun `should append single-line`(@TempDir tempDir: Path) {
            val fixture = tempDir.file()
            ShellScript { file(fixture) { appendLine("line 3") } }.exec.logging()
            expectThat(fixture).hasContent("line 1\nline 2\nline 3${LineSeparators.LF}")
        }

        @Test
        fun `should append multi-line`(@TempDir tempDir: Path) {
            val fixture = tempDir.file()
            ShellScript { file(fixture) { appendLine("line 3\nline 4") } }.exec.logging()
            expectThat(fixture).hasContent("line 1\nline 2\nline 3\nline 4${LineSeparators.LF}")
        }

        @Test
        fun `should not append on already existing line separator`(@TempDir tempDir: Path) {
            val fixture = tempDir.file()
            ShellScript { file(fixture) { appendLine("line 3\r") } }.exec.logging()
            expectThat(fixture).hasContent("line 1\nline 2\nline 3${LineSeparators.LF}")
        }
    }
}
