package com.bkahlert.kommons.uri

import com.bkahlert.kommons.test.testAll
import io.kotest.matchers.shouldBe
import io.ktor.http.quote
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.test.Test

class UriSerializerTest {

    @Test fun deserialize() = testAll {
        Json.Default.decodeFromString<Uri>("https://username:password@example.com:8080/poo/par?qoo=qar&qaz#foo=far&faz".quote())
            .shouldBe(Uri.parse("https://username:password@example.com:8080/poo/par?qoo=qar&qaz#foo=far&faz"))
        Json.Default.decodeFromString<Uri>("data:;base64,Rm9vIGJhcg".quote())
            .shouldBe(DataUri.parse("data:;base64,Rm9vIGJhcg"))
    }

    @Test fun serialize() = testAll {
        Json.Default.encodeToString(Uri.parse("https://username:password@example.com:8080/poo/par?qoo=qar&qaz#foo=far&faz"))
            .shouldBe("https://username:password@example.com:8080/poo/par?qoo=qar&qaz#foo=far&faz".quote())
        Json.Default.encodeToString(DataUri.parse("data:;base64,Rm9vIGJhcg"))
            .shouldBe("data:;base64,Rm9vIGJhcg".quote())
    }
}
