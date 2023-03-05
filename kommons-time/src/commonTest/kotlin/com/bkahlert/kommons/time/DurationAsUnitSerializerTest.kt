package com.bkahlert.kommons.time

import com.bkahlert.kommons.test.testAll
import io.kotest.matchers.shouldBe
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.microseconds
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.nanoseconds
import kotlin.time.Duration.Companion.seconds

class DurationAsUnitSerializerTest {

    @Test
    fun serialize() = testAll {
        Json.encodeToString(DurationAsNanosecondsSerializer, 100.nanoseconds) shouldBe "100"
        Json.encodeToString(DurationAsMicrosecondsSerializer, 100.microseconds) shouldBe "100"
        Json.encodeToString(DurationAsMillisecondsSerializer, 100.milliseconds) shouldBe "100"
        Json.encodeToString(DurationAsSecondsSerializer, 100.seconds) shouldBe "100"
        Json.encodeToString(DurationAsMinutesSerializer, 100.minutes) shouldBe "100"
        Json.encodeToString(DurationAsHoursSerializer, 100.hours) shouldBe "100"
        Json.encodeToString(DurationAsDaysSerializer, 100.days) shouldBe "100"
    }

    @Test
    fun deserialize() = testAll {
        Json.decodeFromString(DurationAsNanosecondsSerializer, "100") shouldBe 100.nanoseconds
        Json.decodeFromString(DurationAsMicrosecondsSerializer, "100") shouldBe 100.microseconds
        Json.decodeFromString(DurationAsMillisecondsSerializer, "100") shouldBe 100.milliseconds
        Json.decodeFromString(DurationAsSecondsSerializer, "100") shouldBe 100.seconds
        Json.decodeFromString(DurationAsMinutesSerializer, "100") shouldBe 100.minutes
        Json.decodeFromString(DurationAsHoursSerializer, "100") shouldBe 100.hours
        Json.decodeFromString(DurationAsDaysSerializer, "100") shouldBe 100.days
    }
}
