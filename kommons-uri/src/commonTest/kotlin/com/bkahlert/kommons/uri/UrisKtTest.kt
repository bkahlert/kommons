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
    fun factory() = testAll {
        Uri(COMPLETE_URI_STRING) shouldBe COMPLETE_URI
        Uri(BASE_URI_STRING) shouldBe BASE_URI
        Uri(EMPTY_URI_STRING) shouldBe EMPTY_URI
    }

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

    /**
     * References resolution examples as described in [RFC3986 section 5.4.1](https://www.rfc-editor.org/rfc/rfc3986#section-5.4.1)
     */
    @Test
    fun resolve_normal_examples() = testAll(
        "g:h" to "g:h",
        "g" to "http://a/b/c/g",
        "./g" to "http://a/b/c/g",
        "g/" to "http://a/b/c/g/",
        "/g" to "http://a/g",
        "//g" to "http://g",
        "?y" to "http://a/b/c/d;p?y",
        "g?y" to "http://a/b/c/g?y",
        "#s" to "http://a/b/c/d;p?q#s",
        "g#s" to "http://a/b/c/g#s",
        "g?y#s" to "http://a/b/c/g?y#s",
        ";x" to "http://a/b/c/;x",
        "g;x" to "http://a/b/c/g;x",
        "g;x?y#s" to "http://a/b/c/g;x?y#s",
        "" to "http://a/b/c/d;p?q",
        "." to "http://a/b/c/",
        "./" to "http://a/b/c/",
        ".." to "http://a/b/",
        "../" to "http://a/b/",
        "../g" to "http://a/b/g",
        "../.." to "http://a/",
        "../../" to "http://a/",
        "../../g" to "http://a/g",
    ) { (relativeReference, targetUriString) ->
        val targetUri = Uri.parse(targetUriString)
        val uriReference = Uri.parse(relativeReference)

        BASE_URI.resolve(relativeReference) shouldBe targetUri
        BASE_URI.resolve(uriReference) shouldBe targetUri
        uriReference.resolveTo(BASE_URI) shouldBe targetUri
    }

    /**
     * References resolution examples as described in [RFC3986 section 5.4.2](https://www.rfc-editor.org/rfc/rfc3986#section-5.4.2)
     */
    @Test
    fun resolve_abnormal_examples() = testAll(
        "../../../g" to "http://a/g",
        "../../../../g" to "http://a/g",

        "/./g" to "http://a/g",
        "/../g" to "http://a/g",
        "g." to "http://a/b/c/g.",
        ".g" to "http://a/b/c/.g",
        "g.." to "http://a/b/c/g..",
        "..g" to "http://a/b/c/..g",

        "./../g" to "http://a/b/g",
        "./g/." to "http://a/b/c/g/",
        "g/./h" to "http://a/b/c/g/h",
        "g/../h" to "http://a/b/c/h",
        "g;x=1/./y" to "http://a/b/c/g;x=1/y",
        "g;x=1/../y" to "http://a/b/c/y",

        "g?y/./x" to "http://a/b/c/g?y/./x",
        "g?y/../x" to "http://a/b/c/g?y/../x",
        "g#s/./x" to "http://a/b/c/g#s/./x",
        "g#s/../x" to "http://a/b/c/g#s/../x",
    ) { (relativeReference, targetUriString) ->
        BASE_URI.resolve(relativeReference) shouldBe Uri.parse(targetUriString)
    }

    /**
     * References resolution strict examples as
     * described in [RFC3986 section 5.4.2](https://www.rfc-editor.org/rfc/rfc3986#section-5.4.2)
     */
    @Test
    fun resolve_reference_with_scheme() = testAll {
        BASE_URI.resolve("http:g", strict = true) shouldBe Uri.parse("http:g")
        BASE_URI.resolve("http:g", strict = false) shouldBe Uri.parse("http://a/b/c/g")
    }

    @Test
    fun resolve_absolute_uri() = testAll {
        val baseUri = Uri.parse("http://localhost:8080/?x#y")
        baseUri.resolve("https://a/b").toString().shouldBe("https://a/b")
        baseUri.resolve("/b").toString().shouldBe("http://localhost:8080/b")
    }


    @Test
    fun div() = testAll {
        COMPLETE_URI / "path-segment" shouldBe Uri.parse("https://username:password@example.com:8080/poo/par/path-segment?qoo=qar&qaz#foo=far&faz")
        EMPTY_URI / "path-segment" shouldBe Uri.parse("/path-segment")
    }
}
