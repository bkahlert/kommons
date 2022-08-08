package com.bkahlert.kommons

import com.bkahlert.kommons.MessageDigestProviders.MD5
import com.bkahlert.kommons.MessageDigestProviders.`SHA-1`
import com.bkahlert.kommons.MessageDigestProviders.`SHA-256`
import com.bkahlert.kommons.debug.FunctionTypes.provider
import com.bkahlert.kommons.test.junit.testEach
import com.bkahlert.kommons.test.testAll
import com.bkahlert.kommons.text.string
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path
import kotlin.io.path.div
import kotlin.io.path.writeText

class MessageDigestsKtTest {

    @TestFactory fun hash(@TempDir tempDir: Path) = hashBytes.testEach { (provider, expectedBytes) ->
        string.encodeToByteArray().inputStream().hash(provider) shouldBe expectedBytes
        string.encodeToByteArray().hash(provider) shouldBe expectedBytes
        (tempDir / provider.name).apply { writeText(string) }.hash(provider) shouldBe expectedBytes
        string.hash(provider) shouldBe expectedBytes
    }

    @TestFactory fun checksum(@TempDir tempDir: Path) = checksums.testEach { (provider, expectedChecksum) ->
        string.encodeToByteArray().inputStream().checksum(provider) shouldBe expectedChecksum
        string.encodeToByteArray().checksum(provider) shouldBe expectedChecksum
        (tempDir / provider.name).apply { writeText(string) }.checksum(provider) shouldBe expectedChecksum
        string.checksum(provider) shouldBe expectedChecksum
    }

    @Test fun md5_checksum(@TempDir tempDir: Path) = testAll {
        string.encodeToByteArray().inputStream().md5Checksum() shouldBe checksums[MD5]
        string.encodeToByteArray().md5Checksum() shouldBe checksums[MD5]
        (tempDir / provider.name).apply { writeText(string) }.md5Checksum() shouldBe checksums[MD5]
        string.md5Checksum() shouldBe checksums[MD5]
    }

    @Test fun sha1_checksum(@TempDir tempDir: Path) = testAll {
        string.encodeToByteArray().inputStream().sha1Checksum() shouldBe checksums[`SHA-1`]
        string.encodeToByteArray().sha1Checksum() shouldBe checksums[`SHA-1`]
        (tempDir / provider.name).apply { writeText(string) }.sha1Checksum() shouldBe checksums[`SHA-1`]
        string.sha1Checksum() shouldBe checksums[`SHA-1`]
    }

    @Test fun sha256_checksum(@TempDir tempDir: Path) = testAll {
        string.encodeToByteArray().inputStream().sha256Checksum() shouldBe checksums[`SHA-256`]
        string.encodeToByteArray().sha256Checksum() shouldBe checksums[`SHA-256`]
        (tempDir / provider.name).apply { writeText(string) }.sha256Checksum() shouldBe checksums[`SHA-256`]
        string.sha256Checksum() shouldBe checksums[`SHA-256`]
    }
}

internal fun byteArrayOf(vararg bytes: Int) =
    bytes.map { it.toByte() }.toByteArray()

internal val hashBytes = mapOf(
    MD5 to byteArrayOf(
        0xb4, 0x5c, 0xff, 0xe0, 0x84, 0xdd, 0x3d, 0x20,
        0xd9, 0x28, 0xbe, 0xe8, 0x5e, 0x7b, 0x0f, 0x21,
    ),
    `SHA-1` to byteArrayOf(
        0xec, 0xb2, 0x52, 0x04, 0x4b, 0x5e, 0xa0, 0xf6, 0x79, 0xee,
        0x78, 0xec, 0x1a, 0x12, 0x90, 0x47, 0x39, 0xe2, 0x90, 0x4d,
    ),
    `SHA-256` to byteArrayOf(
        0x47, 0x32, 0x87, 0xf8, 0x29, 0x8d, 0xba, 0x71, 0x63, 0xa8, 0x97, 0x90, 0x89, 0x58, 0xf7, 0xc0,
        0xea, 0xe7, 0x33, 0xe2, 0x5d, 0x2e, 0x02, 0x79, 0x92, 0xea, 0x2e, 0xdc, 0x9b, 0xed, 0x2f, 0xa8,
    ),
)

@Suppress("SpellCheckingInspection")
internal val checksums = mapOf(
    MD5 to "b45cffe084dd3d20d928bee85e7b0f21",
    `SHA-1` to "ecb252044b5ea0f679ee78ec1a12904739e2904d",
    `SHA-256` to "473287f8298dba7163a897908958f7c0eae733e25d2e027992ea2edc9bed2fa8",
)
