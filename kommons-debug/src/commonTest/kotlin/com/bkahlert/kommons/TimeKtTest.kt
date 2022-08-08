package com.bkahlert.kommons

import com.bkahlert.kommons.test.testAll
import io.kotest.matchers.shouldBe
import kotlin.test.Test
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

class TimeKtTest {

    @Test fun to_local_date_string() = testAll {
        instant2232.toLocalDateString() shouldBe "February 2, 2020"
        localDate.toLocalDateString() shouldBe "February 2, 2020"
    }

    @Test fun to_moment_string() = testAll {
        42.seconds.toMomentString() shouldBe 42.seconds.toMomentString(true)
        42.seconds.toMomentString() shouldBe "in 42s"
        (-42).seconds.toMomentString() shouldBe "42s ago"
        42.seconds.toMomentString(false) shouldBe "42s"
        (-42).seconds.toMomentString(false) shouldBe "-42s"

        490.milliseconds.toMomentString() shouldBe "now"

        0.5.seconds.toMomentString() shouldBe "in 1s"
        0.6.seconds.toMomentString() shouldBe "in 1s"
        59.seconds.toMomentString() shouldBe "in 59s"

        60.seconds.toMomentString() shouldBe "in 1m"
        89.seconds.toMomentString() shouldBe "in 1m"
        90.seconds.toMomentString() shouldBe "in 2m"
        59.minutes.toMomentString() shouldBe "in 59m"

        1.hours.toMomentString() shouldBe "in 1h"
        1.1.hours.toMomentString() shouldBe "in 1h 6m"
        5.9.hours.toMomentString() shouldBe "in 5h 54m"

        6.hours.toMomentString() shouldBe "in 6h"
        6.49.hours.toMomentString() shouldBe "in 6h"
        6.5.hours.toMomentString() shouldBe "in 7h"
        23.49.hours.toMomentString() shouldBe "in 23h"
        23.50.hours.toMomentString() shouldBe "in 1d"

        1.days.toMomentString() shouldBe "in 1d"
        1.1.days.toMomentString() shouldBe "in 1d 2h"
        5.9.days.toMomentString() shouldBe "in 5d 22h"

        6.0.days.toMomentString() shouldBe "in 6d"
        6.49.days.toMomentString() shouldBe "in 6d"
        6.5.days.toMomentString() shouldBe "in 7d"
        29.49.days.toMomentString() shouldBe "in 29d"
        29.50.days.toMomentString() shouldBe "in 30d"

        30.days.toMomentString() shouldBe (Now - 30.days).toLocalDateString()
        35.days.toMomentString() shouldBe (Now - 35.days).toLocalDateString()
        100.days.toMomentString() shouldBe (Now - 100.days).toLocalDateString()


        Now.toMomentString() shouldBe "now"
        (Now - 12.hours).toMomentString() shouldBe "12h ago"
        (Now + 12.hours).toMomentString() shouldBe "in 12h"


        (Yesterday - 1.days).toMomentString() shouldBe "2d ago"
        Yesterday.toMomentString() shouldBe "yesterday"
        Today.toMomentString() shouldBe "today"
        Tomorrow.toMomentString() shouldBe "tomorrow"
        (Tomorrow + 1.days).toMomentString() shouldBe "in 2d"
    }
}

internal expect val instant0202: Instant
internal expect val instant2232: Instant
internal expect val localDate: LocalDate
