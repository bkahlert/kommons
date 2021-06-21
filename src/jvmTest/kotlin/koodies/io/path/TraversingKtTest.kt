package koodies.io.path

import koodies.junit.UniqueId
import koodies.test.Fixtures.directoryWithTwoFiles
import koodies.test.withTempDir
import koodies.text.LineSeparators.LF
import koodies.text.matchesCurlyPattern
import koodies.unit.Size
import koodies.unit.bytes
import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.contains
import strikt.assertions.isEqualTo
import kotlin.io.path.fileSize
import kotlin.io.path.isDirectory

class TraversingKtTest {

    @Test
    fun `should accept transform as last argument`(uniqueId: UniqueId) = withTempDir(uniqueId) {
        directoryWithTwoFiles()

        val totalSize = traverse(0.bytes, Size::plus) {
            if (isDirectory()) 0.bytes
            else kotlin.runCatching { fileSize().bytes }.getOrElse { 0.bytes }
        }

        expectThat(totalSize).isEqualTo(144.bytes)
    }

    @Test
    fun `should accept operation as last argument`(uniqueId: UniqueId) = withTempDir(uniqueId) {
        directoryWithTwoFiles()

        val listing = traverse("", { pathString }) { lines, file ->
            lines + file + LF
        }

        expectThat(listing).matchesCurlyPattern("""
            {}
            {}/sub-dir
            {}/sub-dir/config.txt
            {}/example.html
        """.trimIndent())
    }

    @Test
    fun `should list matching entries`(uniqueId: UniqueId) = withTempDir(uniqueId) {
        val dir = directoryWithTwoFiles()

        val listing = listMatchingEntries { isDirectory() }

        expectThat(listing).contains(dir, dir.resolve("sub-dir"))
    }
}
