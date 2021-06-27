package koodies.shell

import koodies.io.path.appendText
import koodies.io.path.hasContent
import koodies.junit.UniqueId
import koodies.test.testEach
import koodies.test.withTempDir
import koodies.text.LineSeparators
import koodies.text.LineSeparators.LF
import koodies.text.Names
import koodies.toBaseName
import koodies.tracing.TestSpan
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory
import strikt.api.expectThat
import java.nio.file.Path

class FileOperationsTest {

    @Nested
    inner class RemoveLine {

        private fun Path.file(lineSeparator: String): Path =
            resolve("${LineSeparators.Names[lineSeparator].toBaseName()}.txt").apply {
                appendText("line 1$lineSeparator")
                appendText("line 2$lineSeparator")
                appendText("line 2.1$lineSeparator")
                appendText("last line")
            }

        @TestFactory
        fun TestSpan.`should remove intermediary line`(uniqueId: UniqueId) = testEach(*LineSeparators.toTypedArray()) { lineSeparator ->
            withTempDir(uniqueId) {
                val fixture = file(lineSeparator)
                ShellScript { file(fixture) { removeLine("line 2") } }.exec.logging()
                if (lineSeparator !in listOf(LineSeparators.LS, LineSeparators.PS, LineSeparators.NEL)) { // TODO can't get these line breaks to be removed
                    expecting { fixture } that { hasContent("line 1${lineSeparator}line 2.1${lineSeparator}last line") }
                }
            }
        }

        @TestFactory
        fun TestSpan.`should remove last line`(uniqueId: UniqueId) = testEach(*LineSeparators.toTypedArray()) { lineSeparator ->
            withTempDir(uniqueId) {
                val fixture = file(lineSeparator)
                ShellScript { file(fixture) { removeLine("last line") } }.exec.logging()
                expecting { fixture } that { hasContent("line 1${lineSeparator}line 2${lineSeparator}line 2.1$lineSeparator") }
            }
        }
    }

    @Nested
    inner class AppendLine {

        private fun Path.file(): Path =
            resolve("file.txt").apply {
                appendText("""
                    line 1
                    line 2
                    
                """.trimIndent())
            }

        @Test
        fun `should append single-line`(uniqueId: UniqueId) = withTempDir(uniqueId) {
            val fixture = file()
            ShellScript { file(fixture) { appendLine("line 3") } }.exec.logging()
            expectThat(fixture).hasContent("line 1\nline 2\nline 3$LF")
        }

        @Test
        fun `should append multi-line`(uniqueId: UniqueId) = withTempDir(uniqueId) {
            val fixture = file()
            ShellScript { file(fixture) { appendLine("line 3\nline 4") } }.exec.logging()
            expectThat(fixture).hasContent("line 1\nline 2\nline 3\nline 4$LF")
        }

        @Test
        fun `should not append on already existing line separator`(uniqueId: UniqueId) = withTempDir(uniqueId) {
            val fixture = file()
            ShellScript { file(fixture) { appendLine("line 3\r") } }.exec.logging()
            expectThat(fixture).hasContent("line 1\nline 2\nline 3$LF")
        }
    }
}
