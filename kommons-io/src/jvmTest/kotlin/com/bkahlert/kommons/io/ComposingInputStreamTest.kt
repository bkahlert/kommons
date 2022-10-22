package com.bkahlert.kommons.io

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class ComposingInputStreamTest {

    @Test
    fun `should handle empty byte array list`() {
        ComposingInputStream().readBytes() shouldBe ByteArray(0)
    }

    @Test
    fun `should handle single element byte array list`() {
        ComposingInputStream("foo".toByteArray()).readBytes() shouldBe "foo".toByteArray()
    }

    @Test
    fun `should handle multi element byte array list`() {
        ComposingInputStream(
            "foo".toByteArray(),
            "".toByteArray(),
            "-".toByteArray(),
            "bar".toByteArray()
        ).readBytes() shouldBe "foo-bar".toByteArray()
    }

    @Test
    fun `should handle dynamic byte array list`() {
        val bytes = mutableListOf("foo".toByteArray())
        val inputStream = ComposingInputStream(bytes)
        inputStream.readBytes() shouldBe "foo".toByteArray()

        bytes.add("bar".toByteArray())
        inputStream.readBytes() shouldBe "bar".toByteArray()

        bytes.add("1".toByteArray())
        bytes.add("2".toByteArray())
        inputStream.readBytes() shouldBe "12".toByteArray()
    }
}
