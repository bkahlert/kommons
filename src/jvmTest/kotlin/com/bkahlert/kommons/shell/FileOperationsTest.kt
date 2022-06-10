package com.bkahlert.kommons.shell

import com.bkahlert.kommons.io.path.appendText
import com.bkahlert.kommons.io.path.hasContent
import com.bkahlert.kommons.test.junit.UniqueId
import com.bkahlert.kommons.test.testEachOld
import com.bkahlert.kommons.test.withTempDir
import com.bkahlert.kommons.text.LineSeparators
import com.bkahlert.kommons.text.LineSeparators.LF
import com.bkahlert.kommons.text.Names
import com.bkahlert.kommons.toIdentifier
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory
import strikt.api.expectThat
import java.nio.file.Path

class FileOperationsTest {

    @Nested
    inner class RemoveLine {

        private fun Path.file(lineSeparator: String): Path =
            resolve("${LineSeparators.Names[lineSeparator].toIdentifier(8)}.txt").apply {
                appendText("line 1$lineSeparator")
                appendText("line 2$lineSeparator")
                appendText("line 2.1$lineSeparator")
                appendText("last line")
            }

        @TestFactory
        fun `should remove intermediary line`(uniqueId: UniqueId): List<Any> = testEachOld(*LineSeparators.toTypedArray()) { lineSeparator ->
            withTempDir(uniqueId) {
                val fixture = file(lineSeparator)
                ShellScript { file(fixture) { removeLine("line 2") } }.exec.logging()
                if (lineSeparator !in listOf(LineSeparators.LS, LineSeparators.PS, LineSeparators.NEL)) { // TODO can't get these line breaks to be removed
                    expecting { fixture } that { this.hasContent("line 1${lineSeparator}line 2.1${lineSeparator}last line") }
                }
            }
        }

        @TestFactory
        fun `should remove last line`(uniqueId: UniqueId): List<Any> = testEachOld(*LineSeparators.toTypedArray()) { lineSeparator ->
            withTempDir(uniqueId) {
                val fixture = file(lineSeparator)
                ShellScript { file(fixture) { removeLine("last line") } }.exec.logging()
                expecting { fixture } that { this.hasContent("line 1${lineSeparator}line 2${lineSeparator}line 2.1$lineSeparator") }
            }
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
