package com.bkahlert.kommons.io

import com.bkahlert.kommons.test.testAll
import io.kotest.inspectors.forAll
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class TeeInputStreamTest {

    @Test fun read_byte() = testAll {
        val inputStream = TestInputStream("foo")
        val outputStream1 = TestOutputStream()
        val outputStream2 = TestOutputStream()
        TeeInputStream(inputStream, outputStream1, outputStream2).read() shouldBe 'f'.code
        listOf(outputStream1, outputStream2).forAll { it.toString() shouldBe "f" }
    }

    @Test fun read_some_bytes() = testAll {
        val inputStream = TestInputStream("foo")
        val outputStream1 = TestOutputStream()
        val outputStream2 = TestOutputStream()
        val bytes: ByteArray = ByteArray(3) { '_'.code.toByte() }
        TeeInputStream(inputStream, outputStream1, outputStream2).read(bytes, 1, 2) shouldBe 2
        bytes.decodeToString() shouldBe "_fo"
        listOf(outputStream1, outputStream2).forAll { it.toString() shouldBe "fo" }
    }

    @Test fun read_bytes() = testAll {
        val inputStream = TestInputStream("foo")
        val outputStream1 = TestOutputStream()
        val outputStream2 = TestOutputStream()
        val bytes: ByteArray = ByteArray(3) { '_'.code.toByte() }
        TeeInputStream(inputStream, outputStream1, outputStream2).read(bytes) shouldBe 3
        bytes.decodeToString() shouldBe "foo"
        listOf(outputStream1, outputStream2).forAll { it.toString() shouldBe "foo" }
    }

    @Test fun close() = testAll {
        val inputStream = TestInputStream("foo")
        val outputStream1 = TestOutputStream()
        val outputStream2 = TestOutputStream()
        TeeInputStream(inputStream, outputStream1, outputStream2).close()
        inputStream.closed shouldBe true
        listOf(
            outputStream1,
            outputStream2,
        ).forAll { it.closed shouldBe false }
    }

    @Test fun close_branches() = testAll {
        val inputStream = TestInputStream("foo")
        val outputStream1 = TestOutputStream()
        val outputStream2 = TestOutputStream()
        TeeInputStream(inputStream, outputStream1, outputStream2, closeBranches = true).close()
        listOf(
            inputStream,
            outputStream1,
            outputStream2,
        ).forAll { it.closed shouldBe true }
    }
}
