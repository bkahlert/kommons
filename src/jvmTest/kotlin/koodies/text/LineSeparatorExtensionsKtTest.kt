package koodies.text

import koodies.io.path.hasContent
import koodies.io.path.writeText
import koodies.io.randomDirectory
import koodies.io.randomFile
import koodies.io.randomPath
import koodies.io.tempFile
import koodies.test.UniqueId
import koodies.test.expecting
import koodies.test.tests
import koodies.test.withTempDir
import koodies.text.LineSeparators.CRLF
import koodies.text.LineSeparators.LF
import koodies.text.LineSeparators.NEL
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory
import strikt.assertions.isSameInstanceAs
import java.io.IOException

class LineSeparatorExtensionsKtTest {

    @Nested
    inner class AppendTrailingLineSeparatorIfMissing {

        @Test
        fun `should return same path`(uniqueId: UniqueId) = withTempDir(uniqueId) {
            val file = tempFile()
            expecting { file.appendTrailingLineSeparatorIfMissing() } that { isSameInstanceAs(file) }
        }

        @Test
        fun `should append line separator if missing`(uniqueId: UniqueId) = withTempDir(uniqueId) {
            val file = tempFile().writeText("line")
            expecting { file.appendTrailingLineSeparatorIfMissing() } that { hasContent("line$LF") }
        }

        @Test
        fun `should auto-detect line separator`(uniqueId: UniqueId) = withTempDir(uniqueId) {
            val file = tempFile().writeText("line${NEL}line")
            expecting { file.appendTrailingLineSeparatorIfMissing() } that { hasContent("line${NEL}line$NEL") }
        }

        @Test
        fun `should use specified line separator`(uniqueId: UniqueId) = withTempDir(uniqueId) {
            val file = tempFile().writeText("line${NEL}line")
            expecting { file.appendTrailingLineSeparatorIfMissing(CRLF) } that { hasContent("line${NEL}line$CRLF") }
        }

        @Test
        fun `should not append line separator if already present`(uniqueId: UniqueId) = withTempDir(uniqueId) {
            val file = tempFile().writeText("line${NEL}line$NEL")
            expecting { file.appendTrailingLineSeparatorIfMissing(CRLF) } that { hasContent("line${NEL}line$NEL") }
        }

        @TestFactory
        fun `should throw on error`(uniqueId: UniqueId) = tests {
            withTempDir(uniqueId) {
                expectThrows<IOException> { randomPath().appendTrailingLineSeparatorIfMissing() }
                expectThrows<IOException> { randomFile().apply { toFile().setReadable(false) }.appendTrailingLineSeparatorIfMissing() }
                expectThrows<IOException> { randomFile().apply { toFile().setWritable(false) }.appendTrailingLineSeparatorIfMissing() }
                expectThrows<IOException> { randomDirectory().appendTrailingLineSeparatorIfMissing() }
            }
        }
    }
}
