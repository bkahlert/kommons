package com.bkahlert.kommons.io

import com.bkahlert.kommons.test.Assertion
import com.bkahlert.kommons.test.TextFixture
import com.bkahlert.kommons.test.expecting
import com.bkahlert.kommons.test.testEachOld
import com.bkahlert.kommons.test.toStringIsEqualTo
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory
import strikt.assertions.all
import strikt.assertions.isFalse
import strikt.assertions.isTrue

class TeeInputStreamTest {

    private class TestStream : ByteArrayOutputStream() {
        var closed = false
        override fun close() {
            closed = true
            super.close()
        }
    }

    private val text = TextFixture.text
    private fun streams() = listOf(TestStream(), TestStream(), TestStream())
    private fun withStream(block: TeeInputStream.(ByteArray) -> Unit) = block

    @TestFactory
    fun `should tee all operations`() = testEachOld<Pair<TeeInputStream.(ByteArray) -> Unit, Assertion<TestStream>>>(
        withStream { read(it) } to { toStringIsEqualTo(text) },
        withStream { read(it, 0, 1) } to { toStringIsEqualTo("a") },
        withStream { read().apply { it[0] = toByte() } } to { toStringIsEqualTo("a") },
        withStream { close() } to { get { closed }.isFalse() },
    ) { (action, assertion) ->
        expecting {
            streams().also { TeeInputStream(text.byteInputStream(), *it.toTypedArray()).action(ByteArray(text.toByteArray().size)) }
        } that {
            all { assertion() }
        }
    }

    @Test
    fun `should close branches if specified`() {
        expecting { streams().also { TeeInputStream(text.byteInputStream(), *it.toTypedArray(), closeBranches = true).close() } } that {
            all { get { closed }.isTrue() }
        }
    }

    @Test
    fun `should contain input and branches in toString`() {
        TeeInputStream(text.byteInputStream(), *streams().toTypedArray()).also { it.read() }.toString() shouldBe """
            TeeInputStream {
                input: "ByteArrayInputStream",
                branches: [
                              a,
                              a,
                              a
                          ],
                closeBranches: "❌"
            }
        """.trimIndent()
    }
}
