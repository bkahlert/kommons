package com.bkahlert.kommons.io

import com.bkahlert.kommons.test.Assertion
import com.bkahlert.kommons.test.TextFixture
import com.bkahlert.kommons.test.expecting
import com.bkahlert.kommons.test.testEach
import com.bkahlert.kommons.test.toStringIsEqualTo
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory
import strikt.assertions.all
import strikt.assertions.contains
import strikt.assertions.isTrue

class TeeOutputStreamTest {

    private class TestStream : ByteArrayOutputStream() {
        var flushed = false
        var closed = false
        override fun flush() {
            flushed = true
            super.flush()
        }

        override fun close() {
            closed = true
            super.close()
        }
    }

    private val text = TextFixture.text
    private fun streams() = listOf(TestStream(), TestStream(), TestStream())
    private fun withStream(block: TeeOutputStream.() -> Unit) = block

    @TestFactory
    fun `should tee all operations`() = testEach<Pair<TeeOutputStream.() -> Unit, Assertion<TestStream>>>(
        withStream { write(text.toByteArray()) } to { toStringIsEqualTo(text) },
        withStream { write(text.toByteArray(), 3, 4) } to { toStringIsEqualTo("𝕓") },
        withStream { write(text.toByteArray()[0].toInt()) } to { toStringIsEqualTo("a") },
        withStream { flush() } to { get { flushed }.isTrue() },
        withStream { close() } to { get { closed }.isTrue() },
    ) { (action, assertion) ->
        expecting {
            streams().also { TeeOutputStream(it).action() }
        } that {
            all { assertion() }
        }
    }

    @Test
    fun `should contain output and branch in toString`() {
        expecting { TeeOutputStream(streams()).also { it.write("a".toByteArray()) }.toString() } that {
            contains("TeeOutputStream")
            contains("output =")
            contains("branches = [a, a]")
        }
    }
}
