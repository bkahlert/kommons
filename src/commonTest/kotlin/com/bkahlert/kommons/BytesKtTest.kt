package com.bkahlert.kommons

import com.bkahlert.kommons.test.HtmlFixture
import kotlin.test.Test
import kotlin.test.assertEquals

class BytesKtTest {

    @Test
    fun should_decode_byte_arrays_to_string() {
        val byteArrays = listOf(
            HtmlFixture.text.substring(0, 10).encodeToByteArray(),
            HtmlFixture.text.substring(10, 20).encodeToByteArray(),
            HtmlFixture.text.substring(20, HtmlFixture.text.length).encodeToByteArray(),
        )

        assertEquals(byteArrays.decodeToString(), HtmlFixture.text)
    }
}
