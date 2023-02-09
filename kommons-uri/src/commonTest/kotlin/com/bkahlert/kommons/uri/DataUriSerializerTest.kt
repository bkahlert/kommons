package com.bkahlert.kommons.uri

import com.bkahlert.kommons.test.testAll
import com.bkahlert.kommons.uri.DataUriTest.Companion.asciiBase64Text
import io.kotest.matchers.shouldBe
import io.ktor.http.quote
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.test.Test

class DataUriSerializerTest {

    @Test fun deserialize() = testAll {
        Json.Default.decodeFromString<DataUri>("data:;base64,$asciiBase64Text".quote()) shouldBe DataUri.parse("data:;base64,$asciiBase64Text")
    }

    @Test fun serialize() = testAll {
        Json.Default.encodeToString(DataUri.parse("data:;base64,$asciiBase64Text")) shouldBe "data:;base64,$asciiBase64Text".quote()
    }
}
