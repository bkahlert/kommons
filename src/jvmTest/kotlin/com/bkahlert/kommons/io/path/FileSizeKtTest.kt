package com.bkahlert.kommons.io.path

import com.bkahlert.kommons.randomFile
import com.bkahlert.kommons.test.junit.UniqueId
import com.bkahlert.kommons.test.withTempDir
import com.bkahlert.kommons.unit.bytes
import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.containsExactly
import strikt.assertions.isEqualTo
import java.nio.file.Path

class FileSizeKtTest {

    private fun Path.getSmall() = randomFile("small").writeText("123")
    private fun Path.getMedium() = randomFile("medium").writeText("123456")
    private fun Path.getLarge() = randomFile("large").appendBytes(ByteArray(3_123_456))

    @Test
    fun `should compare files by size`(uniqueId: UniqueId) = withTempDir(uniqueId) {
        val largeFile = getLarge()
        val smallFile = getSmall()
        val mediumFile = getMedium()
        expectThat(listOf(largeFile, smallFile, mediumFile).sortedWith(FileSizeComparator)).containsExactly(smallFile, mediumFile, largeFile)
    }

    @Test
    fun `should have size`(uniqueId: UniqueId) = withTempDir(uniqueId) {
        expectThat(getLarge().getSize()).isEqualTo(3_123_456.bytes)
    }

    @Test
    fun `should have rounded size`(uniqueId: UniqueId) = withTempDir(uniqueId) {
        expectThat(getLarge().getRoundedSize()).isEqualTo(3_000_000.bytes)
    }
}
