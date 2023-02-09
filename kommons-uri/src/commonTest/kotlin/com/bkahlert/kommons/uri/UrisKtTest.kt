package com.bkahlert.kommons.uri

import com.bkahlert.kommons.test.testAll
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.maps.shouldContainExactly
import io.kotest.matchers.shouldBe
import io.ktor.http.Parameters
import io.ktor.util.toMap
import kotlin.test.Test

class UrisKtTest {

    @Test
    fun user_info() = testAll {
        COMPLETE_URI.userInfo shouldBe "username:password"
        EMPTY_URI.userInfo shouldBe null
    }

    @Test
    fun host() = testAll {
        COMPLETE_URI.host shouldBe "example.com"
        EMPTY_URI.host shouldBe null
    }

    @Test
    fun port() = testAll {
        COMPLETE_URI.port shouldBe 8080
        EMPTY_URI.port shouldBe null
    }


    @Test
    fun path_segments() = testAll {
        COMPLETE_URI.pathSegments.shouldContainExactly("", "poo", "par")
        EMPTY_URI.pathSegments.shouldContainExactly("")
    }

    @Test
    fun query_parameters() = testAll {
        COMPLETE_URI.queryParameters.toMap().shouldContainExactly(mapOf("qoo" to listOf("qar"), "qaz" to emptyList()))
        EMPTY_URI.queryParameters shouldBe Parameters.Empty
    }

    @Test
    fun fragment_parameters() = testAll {
        COMPLETE_URI.fragmentParameters.toMap().shouldContainExactly(mapOf("foo" to listOf("far"), "faz" to emptyList()))
        EMPTY_URI.fragmentParameters shouldBe Parameters.Empty
    }


    @Test
    fun div() = testAll {
        COMPLETE_URI / "path-segment" shouldBe Uri.parse("https://username:password@example.com:8080/poo/par/path-segment?qoo=qar&qaz#foo=far&faz")
        EMPTY_URI / "path-segment" shouldBe Uri.parse("/path-segment")
    }
}
