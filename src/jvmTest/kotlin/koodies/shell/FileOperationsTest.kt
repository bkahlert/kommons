package koodies.shell

import koodies.concurrent.script
import koodies.debug.replaceNonPrintableCharacters
import koodies.io.path.appendText
import koodies.io.path.hasContent
import koodies.logging.InMemoryLogger
import koodies.test.UniqueId
import koodies.test.output.InMemoryLoggerFactory
import koodies.test.testWithTempDir
import koodies.test.withTempDir
import koodies.text.LineSeparators
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode.CONCURRENT
import strikt.api.expectThat
import java.nio.file.Path

@Execution(CONCURRENT)
class FileOperationsTest {

    @Nested
    inner class RemoveLine {
        private fun Path.file(name: String, lineSeparator: String): Path =
            resolve("$name.txt").apply {
                appendText("line 1$lineSeparator")
                appendText("line 2$lineSeparator")
                appendText("line 2.1$lineSeparator")
                appendText("last line")
            }


        @TestFactory
        fun InMemoryLoggerFactory.`should remove intermediary line`(uniqueId: UniqueId) = LineSeparators.testWithTempDir(uniqueId) { lineSeparator ->
            val logger = createLogger(lineSeparator.replaceNonPrintableCharacters())
            val fixture = file(lineSeparator.replaceNonPrintableCharacters(), lineSeparator)
            script(logger) { file(fixture).removeLine("line 2") }
            if (lineSeparator !in listOf(LineSeparators.LS, LineSeparators.PS, LineSeparators.NEL)) { // TODO can't get these line breaks to be removed
                expectThat(fixture).hasContent("line 1${lineSeparator}line 2.1${lineSeparator}last line")
            }
        }

        @TestFactory
        fun InMemoryLoggerFactory.`should remove last line`(uniqueId: UniqueId) = LineSeparators.testWithTempDir(uniqueId) { lineSeparator ->
            val logger = createLogger(lineSeparator.replaceNonPrintableCharacters())
            val fixture = file(lineSeparator.replaceNonPrintableCharacters(), lineSeparator)
            script(logger) { file(fixture).removeLine("last line") }
            expectThat(fixture).hasContent("line 1${lineSeparator}line 2${lineSeparator}line 2.1$lineSeparator")
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
        fun InMemoryLogger.`should append single-line`(uniqueId: UniqueId) = withTempDir(uniqueId) {
            val logger = this@`should append single-line`
            val fixture = file()
            script(logger) { file(fixture).appendLine("line 3") }
            expectThat(fixture).hasContent("line 1\nline 2\nline 3\n")
        }

        @Test
        fun InMemoryLogger.`should append multi-line`(uniqueId: UniqueId) = withTempDir(uniqueId) {
            val logger = this@`should append multi-line`
            val fixture = file()
            script(logger) { file(fixture).appendLine("line 3\nline 4") }
            expectThat(fixture).hasContent("line 1\nline 2\nline 3\nline 4\n")
        }

        @Test
        fun InMemoryLogger.`should not append on already existing line separator`(uniqueId: UniqueId) = withTempDir(uniqueId) {
            val logger = this@`should not append on already existing line separator`
            val fixture = file()
            script(logger) { file(fixture).appendLine("line 3\r") }
            expectThat(fixture).hasContent("line 1\nline 2\nline 3\n")
        }
    }
}
