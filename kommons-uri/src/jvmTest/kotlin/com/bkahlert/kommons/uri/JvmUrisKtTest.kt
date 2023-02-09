package com.bkahlert.kommons.uri

import com.bkahlert.kommons.test.testAll
import io.kotest.matchers.shouldBe
import java.net.URI
import kotlin.test.Test

class JvmUrisKtTest {

    @Test
    fun to_uri() = testAll {
        COMPLETE_JAVA_URI.toUri() shouldBe COMPLETE_URI
        EMPTY_JAVA_URI.toUri() shouldBe EMPTY_URI
    }

    @Test
    fun to_java_uri() = testAll {
        COMPLETE_URI.toJavaUri() shouldBe COMPLETE_JAVA_URI
        EMPTY_URI.toJavaUri() shouldBe EMPTY_JAVA_URI
    }

    @Test
    fun div() = testAll {
        COMPLETE_JAVA_URI / "path-segment" shouldBe URI("https://username:password@example.com:8080/poo/par/path-segment?qoo=qar&qaz#foo=far&faz")
        EMPTY_JAVA_URI / "path-segment" shouldBe URI("/path-segment")
    }
}
