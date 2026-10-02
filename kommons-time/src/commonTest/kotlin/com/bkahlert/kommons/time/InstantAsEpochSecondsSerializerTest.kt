package com.bkahlert.kommons.time

import com.bkahlert.kommons.test.testAll
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.time.Instant

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

    @Test fun descriptor() = testAll {
        InstantAsEpochSecondsSerializer.descriptor should {
            it.serialName shouldBe "com.bkahlert.kommons.time.InstantAsSecondsSerializer"
            it.kind shouldBe PrimitiveKind.LONG
        }
    }
}
