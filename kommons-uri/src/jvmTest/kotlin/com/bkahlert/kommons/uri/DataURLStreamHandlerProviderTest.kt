package com.bkahlert.kommons.uri

import com.bkahlert.kommons.test.testAll
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import java.net.URL

class DataURLStreamHandlerProviderTest {

    @Test
    fun create_url_stream_handler() = testAll {
        val provider = DataURLStreamHandlerProvider()
        provider.createURLStreamHandler("data").shouldBe(DataURLStreamHandler)
        provider.createURLStreamHandler("https").shouldBeNull()
    }

    @Test
    fun integration() {
        URL("data:;base64,Rm9vIGJhcg").readText() shouldBe "Foo bar"
    }
}
