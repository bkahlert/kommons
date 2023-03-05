package com.bkahlert.kommons.time

import io.kotest.matchers.shouldBe
import kotlinx.datetime.Instant
import kotlinx.serialization.json.Json
import kotlin.test.Test

class InstantAsEpochMillisecondsSerializerTest {

    @Test
    fun serialize() {
        Json.encodeToString(InstantAsEpochMillisecondsSerializer, Instant.fromEpochMilliseconds(1594753507L)) shouldBe "1594753507"
        Json.encodeToString(InstantAsEpochMillisecondsSerializer, Instant.fromEpochMilliseconds(1595640878660L)) shouldBe "1595640878660"
    }
    @Test
    fun deserialize() {
        Json.decodeFromString(InstantAsEpochMillisecondsSerializer, "1594753507") shouldBe Instant.fromEpochMilliseconds(1594753507L)
        Json.decodeFromString(InstantAsEpochMillisecondsSerializer, "1595640878660") shouldBe Instant.fromEpochMilliseconds(1595640878660L)
    }
}
