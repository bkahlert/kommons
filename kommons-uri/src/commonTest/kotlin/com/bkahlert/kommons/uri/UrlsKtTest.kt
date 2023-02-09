package com.bkahlert.kommons.uri

import com.bkahlert.kommons.test.testAll
import io.kotest.matchers.shouldBe
import io.ktor.http.Url
import io.ktor.util.PlatformUtils
import kotlin.test.Test

class UrlsKtTest {

    @Test
    fun to_uri() = testAll {
        COMPLETE_URL.toUri() shouldBe COMPLETE_URI
        when (PlatformUtils.IS_BROWSER) {
            true -> {
                val uri = EMPTY_URL.toUri()
                uri shouldBe Uri.parse("http://localhost:${uri.port}") // 🤷
            }

            else -> EMPTY_URL.toUri() shouldBe Uri.parse("http://localhost")
        }
    }

    @Test
    fun to_url() = testAll {
        COMPLETE_URI.toUrl() shouldBe COMPLETE_URL
        EMPTY_URI.toUrl() shouldBe EMPTY_URL
    }


    @Test fun build_with_url() {
        Url.build(COMPLETE_URL) {
            port = 42
        }.toString() shouldBe "https://username:password@example.com:42/poo/par?qoo=qar&qaz#foo=far&faz"
    }

    @Test fun build_without_url() {
        Url.build {
            port = 42
        }.toString() shouldBe "http://localhost:42"
    }


    @Test
    fun div() = testAll {
        COMPLETE_URL / "path-segment" shouldBe Url("https://username:password@example.com:8080/poo/par/path-segment?qoo=qar&qaz#foo=far&faz")
        EMPTY_URL / "path-segment" shouldBe Url("/path-segment")
    }
}
