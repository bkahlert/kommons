package com.bkahlert.kommons.io

import com.bkahlert.kommons.test.TextFixture
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.isEqualTo

class RedirectingOutputStreamTest {

    @Test
    fun `should redirect`() {
        val text = TextFixture.text
        val captured = mutableListOf<ByteArray>()
        text.byteInputStream().copyTo(RedirectingOutputStream { captured.add(it) })
        expectThat(captured.joinToString("") { it.decodeToString() }).isEqualTo(text)
    }

    @Test
    fun `should redirect non latin`() {
        val text = TextFixture.text
        val captured = mutableListOf<ByteArray>()
        text.byteInputStream().copyTo(RedirectingOutputStream { captured.add(it) })
        expectThat(captured.joinToString("") { it.decodeToString() }).isEqualTo(text)
    }

    @Test
    fun `should override toString`() {
        val redirection = object : (ByteArray) -> Unit {
            override operator fun invoke(byteArray: ByteArray): Unit = Unit
            override fun toString(): String = "sample"
        }
        RedirectingOutputStream(redirection).toString() shouldBe "RedirectingOutputStream { redirection: sample }"
    }
}
