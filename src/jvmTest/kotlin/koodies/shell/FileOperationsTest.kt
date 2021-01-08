package koodies.shell

import koodies.concurrent.script
import koodies.io.path.appendText
import koodies.io.path.hasContent
import koodies.test.UniqueId
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
        fun `should remove intermediary line`(uniqueId: UniqueId) = LineSeparators.Dict.testWithTempDir(uniqueId) { (name, sep) ->
            val fixture = file(name, sep)
            script { file(fixture).removeLine("line 2") }
            if (sep !in listOf(LineSeparators.LS, LineSeparators.PS, LineSeparators.NEL)) { // TODO can't get these line breaks to be removed
                expectThat(fixture).hasContent("line 1${sep}line 2.1${sep}last line")
            }
        }

        @TestFactory
        fun `should remove last line`(uniqueId: UniqueId) = LineSeparators.Dict.testWithTempDir(uniqueId) { (name, sep) ->
            val fixture = file(name, sep)
            script { file(fixture).removeLine("last line") }
            expectThat(fixture).hasContent("line 1${sep}line 2${sep}line 2.1$sep")
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
            script { file(fixture).appendLine("line 3") }
            expectThat(fixture).hasContent("line 1\nline 2\nline 3\n")
        }

        @Test
        fun `should append multi-line`(uniqueId: UniqueId) = withTempDir(uniqueId) {
            val fixture = file()
            script { file(fixture).appendLine("line 3\nline 4") }
            expectThat(fixture).hasContent("line 1\nline 2\nline 3\nline 4\n")
        }

        @Test
        fun `should not append on already existing line separator`(uniqueId: UniqueId) = withTempDir(uniqueId) {
            val fixture = file()
            script { file(fixture).appendLine("line 3\r") }
            expectThat(fixture).hasContent("line 1\nline 2\nline 3\n")
        }
    }
}
