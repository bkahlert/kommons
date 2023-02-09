package com.bkahlert.kommons.uri

import com.bkahlert.kommons.test.testAll
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import kotlin.test.Test

class StringsKtTest {

    @Test
    fun to_uri() = testAll {
        "https://username:password@example.com:8080/poo/par?qoo=qar&qaz#foo=far&faz".toUri()
            .shouldBe(Uri.parse("https://username:password@example.com:8080/poo/par?qoo=qar&qaz#foo=far&faz"))

        shouldThrow<IllegalArgumentException> { "data:".toUri() }
    }

    @Test
    fun to_uri_or_null() = testAll {
        "https://username:password@example.com:8080/poo/par?qoo=qar&qaz#foo=far&faz".toUriOrNull()
            .shouldBe(Uri.parse("https://username:password@example.com:8080/poo/par?qoo=qar&qaz#foo=far&faz"))

        "data:".toUriOrNull()
            .shouldBeNull()
    }
}
