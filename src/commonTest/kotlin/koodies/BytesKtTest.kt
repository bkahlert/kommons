package koodies

import koodies.test.HtmlFile
import kotlin.test.Test
import kotlin.test.assertEquals

class BytesKtTest {

    @Test
    fun should_decode_byte_arrays_to_string() {
        val byteArrays = listOf(
            HtmlFile.text.substring(0, 10).encodeToByteArray(),
            HtmlFile.text.substring(10, 20).encodeToByteArray(),
            HtmlFile.text.substring(20, HtmlFile.text.length).encodeToByteArray(),
        )

        assertEquals(byteArrays.decodeToString(), HtmlFile.text)
    }
}
