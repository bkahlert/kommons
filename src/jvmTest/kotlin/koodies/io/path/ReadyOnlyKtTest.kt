package koodies.io.path

import koodies.test.junit.UniqueId
import koodies.test.tests
import koodies.test.withTempDir
import koodies.text.LineSeparators.LF
import org.junit.jupiter.api.TestFactory
import strikt.assertions.isEqualTo
import strikt.java.exists
import strikt.java.isExecutable
import strikt.java.isReadable
import java.nio.file.Path
import java.nio.file.ReadOnlyFileSystemException
import kotlin.io.path.moveTo
import kotlin.io.path.outputStream
import kotlin.io.path.readText

class ReadyOnlyKtTest {

    private fun Path.readOnlyFile() = randomFile().writeText("line #1\nline #2$LF").asReadOnly()

    @TestFactory
    fun `should allow`(uniqueId: UniqueId) = tests {
        withTempDir(uniqueId) {
            readOnlyFile() asserting { isReadable() }
            readOnlyFile() asserting { not { isWritable() } }
            readOnlyFile() asserting { isExecutable() }
            readOnlyFile() asserting { exists() }
            expecting { readOnlyFile().copyTo(resolveSibling("copy-$fileName")) } that { hasContent("line #1\nline #2$LF") }
            expecting { readOnlyFile().readText() } that { isEqualTo("line #1\nline #2$LF") }
        }
    }

    @TestFactory
    fun `should disallow`(uniqueId: UniqueId) = tests {
        withTempDir(uniqueId) {
            expectThrows<ReadOnlyFileSystemException> { readOnlyFile().moveTo(resolveSibling("moved-$fileName")) }
            expectThrows<ReadOnlyFileSystemException> { readOnlyFile().outputStream() }
            expectThrows<ReadOnlyFileSystemException> { readOnlyFile().delete() }
        }
    }
}
