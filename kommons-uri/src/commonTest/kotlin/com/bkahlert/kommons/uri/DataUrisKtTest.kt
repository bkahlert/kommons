package com.bkahlert.kommons.uri

import io.kotest.matchers.shouldBe
import io.ktor.http.ContentType
import io.ktor.http.withCharset
import io.ktor.utils.io.charsets.Charsets
import kotlin.test.Test

class DataUrisKtTest {

    @Test
    fun build_plaintext_without_encoding() {
        DataUri(ContentType.Text.Plain, "aö")
            .toString() shouldBe "data:text/plain;charset=UTF-8;base64,YcO2"
    }

    @Test
    fun build_plaintext_with_encoding() {
        DataUri(ContentType.Text.Plain.withCharset(Charsets.ISO_8859_1), "aö")
            .toString() shouldBe "data:text/plain;charset=ISO-8859-1;base64,YfY"
    }

    @Test
    fun build_any_without_encoding() {
        DataUri(ContentType.Text.Html, "a<b>ö</b>")
            .toString() shouldBe "data:text/html;charset=UTF-8;base64,YTxiPsO2PC9iPg"
    }

    @Test
    fun build_any_with_encoding() {
        DataUri(ContentType.Text.Html.withCharset(Charsets.ISO_8859_1), "a<b>ö</b>")
            .toString() shouldBe "data:text/html;charset=ISO-8859-1;base64,YTxiPvY8L2I+"
    }
}
