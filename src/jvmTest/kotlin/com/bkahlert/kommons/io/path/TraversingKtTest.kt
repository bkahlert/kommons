package com.bkahlert.kommons.io.path

import com.bkahlert.kommons.test.junit.UniqueId
import com.bkahlert.kommons.test.Fixtures.directoryWithTwoFiles
import com.bkahlert.kommons.test.withTempDir
import com.bkahlert.kommons.text.LineSeparators.LF
import com.bkahlert.kommons.text.matchesCurlyPattern
import com.bkahlert.kommons.unit.Size
import com.bkahlert.kommons.unit.bytes
import org.junit.jupiter.api.Test
import strikt.api.expectThat
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

        expectThat(totalSize).isEqualTo(242.bytes)
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
}
