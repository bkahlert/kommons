package com.bkahlert.kommons.uri

import com.bkahlert.kommons.test.testAll
import io.kotest.matchers.shouldBe
import java.net.URL
import kotlin.test.Test

class JvmUrlsKtTest {

    @Test
    fun to_uri() = testAll {
        COMPLETE_JAVA_URL.toUri() shouldBe COMPLETE_URI
    }

    @Test
    fun to_java_url() = testAll {
        COMPLETE_URI.toJavaUrl() shouldBe COMPLETE_JAVA_URL
    }

    @Test
    fun div() = testAll {
        COMPLETE_JAVA_URL / "path-segment" shouldBe URL("https://username:password@example.com:8080/poo/par/path-segment?qoo=qar&qaz#foo=far&faz")
    }
}
