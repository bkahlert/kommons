package com.bkahlert.kommons.io

import com.bkahlert.kommons.test.testAll
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class RedirectingOutputStreamTest {

    @Test fun write_byte() = testAll {
        var bytes: ByteArray? = null
        RedirectingOutputStream { bytes = it }.write("foo".toByteArray())
        bytes shouldBe "foo".toByteArray()
    }

    @Test fun write_some_bytes() = testAll {
        var bytes: ByteArray? = null
        RedirectingOutputStream { bytes = it }.write("foo".toByteArray(), 1, 2)
        bytes shouldBe "oo".toByteArray()
    }

    @Test fun write_bytes() = testAll {
        var bytes: ByteArray? = null
        RedirectingOutputStream { bytes = it }.write("foo".toByteArray().last().toInt())
        bytes shouldBe "o".toByteArray()
    }
}
