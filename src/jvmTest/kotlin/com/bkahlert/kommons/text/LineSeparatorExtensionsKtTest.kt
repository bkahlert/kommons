package com.bkahlert.kommons.text

import com.bkahlert.kommons.createTempDirectory
import com.bkahlert.kommons.createTempFile
import com.bkahlert.kommons.io.path.hasContent
import com.bkahlert.kommons.io.path.writeText
import com.bkahlert.kommons.test.expecting
import com.bkahlert.kommons.test.junit.UniqueId
import com.bkahlert.kommons.test.testsOld
import com.bkahlert.kommons.test.withTempDir
import com.bkahlert.kommons.text.LineSeparators.CRLF
import com.bkahlert.kommons.text.LineSeparators.LF
import com.bkahlert.kommons.text.LineSeparators.NEL
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
            val file = createTempFile()
            expecting { file.appendTrailingLineSeparatorIfMissing() } that { isSameInstanceAs(file) }
        }

        @Test
        fun `should append line separator if missing`(uniqueId: UniqueId) = withTempDir(uniqueId) {
            val file = createTempFile().writeText("line")
            expecting { file.appendTrailingLineSeparatorIfMissing() } that { hasContent("line$LF") }
        }

        @Test
        fun `should auto-detect line separator`(uniqueId: UniqueId) = withTempDir(uniqueId) {
            val file = createTempFile().writeText("line${NEL}line")
            expecting { file.appendTrailingLineSeparatorIfMissing() } that { hasContent("line${NEL}line$NEL") }
        }

        @Test
        fun `should use specified line separator`(uniqueId: UniqueId) = withTempDir(uniqueId) {
            val file = createTempFile().writeText("line${NEL}line")
            expecting { file.appendTrailingLineSeparatorIfMissing(CRLF) } that { hasContent("line${NEL}line$CRLF") }
        }

        @Test
        fun `should not append line separator if already present`(uniqueId: UniqueId) = withTempDir(uniqueId) {
            val file = createTempFile().writeText("line${NEL}line$NEL")
            expecting { file.appendTrailingLineSeparatorIfMissing(CRLF) } that { hasContent("line${NEL}line$NEL") }
        }

        @TestFactory
        fun `should throw on error`(uniqueId: UniqueId) = testsOld {
            withTempDir(uniqueId) {
                expectThrows<IOException> { resolve("path").appendTrailingLineSeparatorIfMissing() }
                expectThrows<IOException> { createTempFile().apply { toFile().setReadable(false) }.appendTrailingLineSeparatorIfMissing() }
                expectThrows<IOException> { createTempFile().apply { toFile().setWritable(false) }.appendTrailingLineSeparatorIfMissing() }
                expectThrows<IOException> { createTempDirectory().appendTrailingLineSeparatorIfMissing() }
            }
        }
    }
}
