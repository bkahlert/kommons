package com.bkahlert.kommons.time

import io.kotest.matchers.shouldBe
import kotlinx.datetime.Instant
import kotlinx.serialization.json.Json
import kotlin.test.Test

class InstantAsEpochSecondsSerializerTest {

    @Test
    fun serialize() {
        Json.encodeToString(InstantAsEpochSecondsSerializer, Instant.fromEpochSeconds(1594753L)) shouldBe "1594753"
        Json.encodeToString(InstantAsEpochSecondsSerializer, Instant.fromEpochSeconds(1595640878L)) shouldBe "1595640878"
    }

    @Test
    fun deserialize() {
        Json.decodeFromString(InstantAsEpochSecondsSerializer, "1594753") shouldBe Instant.fromEpochSeconds(1594753L)
        Json.decodeFromString(InstantAsEpochSecondsSerializer, "1595640878") shouldBe Instant.fromEpochSeconds(1595640878L)
    }
}
