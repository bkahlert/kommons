package com.bkahlert.kommons.io.path

import com.bkahlert.kommons.delete
import com.bkahlert.kommons.randomFile
import com.bkahlert.kommons.test.junit.UniqueId
import com.bkahlert.kommons.test.tests
import com.bkahlert.kommons.test.withTempDir
import com.bkahlert.kommons.text.LineSeparators.LF
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
