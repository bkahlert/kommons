package com.bkahlert.kommons.exec.io

import com.bkahlert.kommons.test.testAll
import io.kotest.inspectors.forAll
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory

class TeeInputStreamTest {

    @TestFactory fun read() = testAll {
        var byte: Int?
        buildList {
            byte = TeeInputStream(testInputStream(), testOutputStream(), testOutputStream()).read()
        }.forAll {
            it.toString() shouldBe "foo"
            byte shouldBe 'f'.code
        }

        var bytes: ByteArray = ByteArray(3) { '_'.code.toByte() }
        buildList {
            TeeInputStream(testInputStream(), testOutputStream(), testOutputStream()).read(bytes, 1, 2)
        }.forAll {
            it.toString() shouldBe "_fo_"
        }

        bytes = ByteArray(3) { '_'.code.toByte() }
        buildList {
            TeeInputStream(testInputStream(), testOutputStream(), testOutputStream()).read(bytes)
        }.forAll {
            it.toString() shouldBe "foo_"
        }
    }

    @TestFactory fun close() = testAll {
        var inputStream = TestInputStream("")
        buildList {
            TeeInputStream(inputStream, testOutputStream(), testOutputStream()).close()
        }.forAll {
            inputStream.closed shouldBe true
            it.closed shouldBe false
        }

        inputStream = TestInputStream("")
        buildList {
            TeeInputStream(inputStream, testOutputStream(), testOutputStream(), closeBranches = true).close()
        }.forAll {
            inputStream.closed shouldBe true
            it.closed shouldBe false
        }
    }

    @Test fun to_string() {
        TeeInputStream(TestInputStream("foo"), TestOutputStream(), TestOutputStream())
            .also { it.read() }
            .toString() shouldBe """
                TeeInputStream {
                    input: "TestInputStream",
                    branches: [
                                  f,
                                  f
                              ],
                    closeBranches: "❌"
                }
            """.trimIndent()
    }
}
