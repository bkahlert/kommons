package com.bkahlert.kommons.uri

import com.bkahlert.kommons.test.fixtures.EmojiTextDocumentFixture
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import java.net.URL

class DataURLStreamHandlerTest {

    private val url get() = URL(EmojiTextDocumentFixture.dataUri.toString())

    @Test
    fun content_type() {
        url.openConnection().contentLength shouldBe 57
    }

    @Test
    fun content_length_long() {
        url.openConnection().contentType shouldBe "text/plain;charset=UTF-8"
    }

    @Test
    fun input_stream() {
        url.openConnection().getInputStream().readBytes().decodeToString() shouldBe "a𝕓🫠🇩🇪👨🏾‍🦱👩‍👩‍👦‍👦"
    }
}
