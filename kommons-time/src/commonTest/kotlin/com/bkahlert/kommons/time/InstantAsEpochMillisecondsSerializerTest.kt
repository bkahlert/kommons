package com.bkahlert.kommons.time

import com.bkahlert.kommons.test.testAll
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.time.Instant

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

    @Test fun descriptor() = testAll {
        InstantAsEpochMillisecondsSerializer.descriptor should {
            it.serialName shouldBe "com.bkahlert.kommons.time.InstantAsMillisecondsSerializer"
            it.kind shouldBe PrimitiveKind.LONG
        }
    }
}
