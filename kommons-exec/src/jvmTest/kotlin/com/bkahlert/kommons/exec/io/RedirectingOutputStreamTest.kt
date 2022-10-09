package com.bkahlert.kommons.exec.io

import com.bkahlert.kommons.test.testAll
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory

class RedirectingOutputStreamTest {

    @TestFactory fun write() = testAll {
        var bytes: ByteArray? = null
        RedirectingOutputStream { bytes = it }.write("foo".toByteArray())
        bytes shouldBe "foo".toByteArray()

        RedirectingOutputStream { bytes = it }.write("foo".toByteArray(), 1, 2)
        bytes shouldBe "oo".toByteArray()

        RedirectingOutputStream { bytes = it }.write("foo".toByteArray().last().toInt())
        bytes shouldBe "o".toByteArray()
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
