package com.bkahlert.kommons.uri

import com.bkahlert.kommons.test.testAll
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.ktor.utils.io.core.toByteArray
import kotlin.test.Test

class UriTest {

    @Test
    fun instantiation() = testAll {
        Uri("https", Authority("username:password", "example.com", 8080), "/poo/par", "qoo=qar&qaz", "foo=far&faz") should {
            it.scheme shouldBe "https"
            it.authority shouldBe Authority("username:password", "example.com", 8080)
            it.path shouldBe "/poo/par"
            it.query shouldBe "qoo=qar&qaz"
            it.fragment shouldBe "foo=far&faz"
        }
    }

    @Test
    fun equality() = testAll {
        Uri("https", Authority("username:password", "example.com", 8080), "/poo/par", "qoo=qar&qaz", "foo=far&faz") should {
            it shouldBe Uri("https", Authority("username:password", "example.com", 8080), "/poo/par", "qoo=qar&qaz", "foo=far&faz")
            it shouldNotBe Uri("xxx", Authority("username:password", "example.com", 8080), "/poo/par", "qoo=qar&qaz", "foo=far&faz")
            it shouldNotBe Uri("https", Authority("username:xxx", "example.com", 8080), "/poo/par", "qoo=qar&qaz", "foo=far&faz")
            it shouldNotBe Uri("https", Authority("username:password", "example.com", 8080), "/xxx/par", "qoo=qar&qaz", "foo=far&faz")
            it shouldNotBe Uri("https", Authority("username:password", "example.com", 8080), "/poo/par", "qoo=xxx&qaz", "foo=far&faz")
            it shouldNotBe Uri("https", Authority("username:password", "example.com", 8080), "/poo/par", "qoo=qar&qaz", "foo=far&xxx")
        }
    }

    @Test
    fun to_string() = testAll {
        Uri("https", Authority("username:password", "example.com", 8080), "/poo/par", "qoo=qar&qaz", "foo=far&faz")
            .toString() shouldBe "https://username:password@example.com:8080/poo/par?qoo=qar&qaz#foo=far&faz"
        Uri(null, Authority("username:password", "example.com", 8080), "/poo/par", "qoo=qar&qaz", "foo=far&faz")
            .toString() shouldBe "//username:password@example.com:8080/poo/par?qoo=qar&qaz#foo=far&faz"
        Uri("https", null, "/poo/par", "qoo=qar&qaz", "foo=far&faz")
            .toString() shouldBe "https:/poo/par?qoo=qar&qaz#foo=far&faz"
        Uri("https", Authority("username:password", "example.com", 8080), "/poo/par", null, "foo=far&faz")
            .toString() shouldBe "https://username:password@example.com:8080/poo/par#foo=far&faz"
        Uri("https", Authority("username:password", "example.com", 8080), "/poo/par", "qoo=qar&qaz", null)
            .toString() shouldBe "https://username:password@example.com:8080/poo/par?qoo=qar&qaz"
        Uri(null, null, "/poo/par", null, null)
            .toString() shouldBe "/poo/par"
        Uri(null, null, "", null, null)
            .toString() shouldBe ""
    }

    @Test
    fun regex() = testAll {
        Uri.REGEX.shouldNotBeNull()
    }

    @Test
    fun parse() = testAll {
        Uri.parse("https://username:password@example.com:8080/poo/par?qoo=qar&qaz#foo=far&faz")
            .shouldBe(Uri("https", Authority("username:password", "example.com", 8080), "/poo/par", "qoo=qar&qaz", "foo=far&faz"))
        Uri.parse("//username:password@example.com:8080/poo/par?qoo=qar&qaz#foo=far&faz")
            .shouldBe(Uri(null, Authority("username:password", "example.com", 8080), "/poo/par", "qoo=qar&qaz", "foo=far&faz"))
        Uri.parse("https:/poo/par?qoo=qar&qaz#foo=far&faz")
            .shouldBe(Uri("https", null, "/poo/par", "qoo=qar&qaz", "foo=far&faz"))
        Uri.parse("https://username:password@example.com:8080/poo/par#foo=far&faz")
            .shouldBe(Uri("https", Authority("username:password", "example.com", 8080), "/poo/par", null, "foo=far&faz"))
        Uri.parse("https://username:password@example.com:8080/poo/par?qoo=qar&qaz")
            .shouldBe(Uri("https", Authority("username:password", "example.com", 8080), "/poo/par", "qoo=qar&qaz", null))
        Uri.parse("/poo/par")
            .shouldBe(Uri(null, null, "/poo/par", null, null))
        Uri.parse("")
            .shouldBe(Uri(null, null, "", null, null))
    }

    @Test
    fun parse_data_uri() = testAll {
        Uri.parse("data:,${DataUriTest.asciiEncodedText}") should {
            it shouldBe DataUri(null, DataUriTest.asciiText.toByteArray(DEFAULT_MEDIA_TYPE_CHARSET))
        }
    }

    @Test
    fun parse_invalid() = testAll {
        shouldThrow<IllegalArgumentException> { Uri.parse("data:") }.message shouldBe "data: is no valid data URI"
        Uri.parseOrNull("data:").shouldBeNull()
    }
}
