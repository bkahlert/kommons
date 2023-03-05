package com.bkahlert.kommons.time

import com.bkahlert.kommons.test.testAll
import io.kotest.matchers.comparables.shouldBeGreaterThanOrEqualTo
import io.kotest.matchers.comparables.shouldBeLessThanOrEqualTo
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn
import kotlin.test.Test
import kotlin.time.Duration
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

class TimeKtTest {

    @Test
    fun clock() = testAll(instant0202, instant2232) {
        Clock { it }.now() shouldBe it
    }

    @Test
    fun now() = testAll {
        Now should {
            it shouldBeLessThanOrEqualTo Clock.System.now()
            it shouldBeGreaterThanOrEqualTo Clock.System.now() - 1.seconds
        }
    }

    @Test
    fun today() = testAll {
        Today shouldBe Clock.System.todayIn(TimeZone.currentSystemDefault())
    }

    @Test
    fun yesterday() = testAll {
        Yesterday should {
            it shouldBe Clock.System.todayIn(TimeZone.currentSystemDefault()) - 1.days
            it shouldBe Clock.System.todayIn(TimeZone.currentSystemDefault()) - (1.days + 1.seconds)
            it shouldBe Clock.System.todayIn(TimeZone.currentSystemDefault()) - (2.days - 1.seconds)
            it shouldNotBe Clock.System.todayIn(TimeZone.currentSystemDefault()) - (1.days - 1.seconds)
            it shouldNotBe Clock.System.todayIn(TimeZone.currentSystemDefault()) - 2.days
        }
    }

    @Test
    fun tomorrow() = testAll {
        Tomorrow should {
            it shouldBe Clock.System.todayIn(TimeZone.currentSystemDefault()) + 1.days
            it shouldBe Clock.System.todayIn(TimeZone.currentSystemDefault()) + (1.days + 1.seconds)
            it shouldBe Clock.System.todayIn(TimeZone.currentSystemDefault()) + (2.days - 1.seconds)
            it shouldNotBe Clock.System.todayIn(TimeZone.currentSystemDefault()) + (1.days - 1.seconds)
            it shouldNotBe Clock.System.todayIn(TimeZone.currentSystemDefault()) + 2.days
        }
    }

    @Test
    fun timestamp() = testAll {
        Timestamp should {
            it shouldBeLessThanOrEqualTo Clock.System.now().toEpochMilliseconds()
            it shouldBeGreaterThanOrEqualTo Clock.System.now().toEpochMilliseconds() - 100
        }
    }

    @Test fun to_local_date_string() = testAll {
        instant2232.toLocalDateString() shouldBe "February 2, 2020"
        localDate.toLocalDateString() shouldBe "February 2, 2020"
    }

    @Test
    fun x() = testAll {
        0.51.seconds.toMomentString() shouldBe "in 1s"
        6.51.hours.toMomentString() shouldBe "in 7h"
    }

    @Test fun to_moment_string() = testAll {
        42.seconds.toMomentString() shouldBe 42.seconds.toMomentString(true)
        42.seconds.toMomentString() shouldBe "in 42s"
        (-42).seconds.toMomentString() shouldBe "42s ago"
        42.seconds.toMomentString(false) shouldBe "42s"
        (-42).seconds.toMomentString(false) shouldBe "-42s"

        490.milliseconds.toMomentString() shouldBe "now"

        0.51.seconds.toMomentString() shouldBe "in 1s"
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
        6.51.hours.toMomentString() shouldBe "in 7h"
        23.49.hours.toMomentString() shouldBe "in 23h"
        23.50.hours.toMomentString() shouldBe "in 1d"

        1.days.toMomentString() shouldBe "in 1d"
        1.1.days.toMomentString() shouldBe "in 1d 2h"
        5.9.days.toMomentString() shouldBe "in 5d 22h"

        6.0.days.toMomentString() shouldBe "in 6d"
        6.49.days.toMomentString() shouldBe "in 6d"
        6.51.days.toMomentString() shouldBe "in 7d"
        29.49.days.toMomentString() shouldBe "in 29d"
        29.50.days.toMomentString() shouldBe "in 30d"

        30.days.toMomentString() shouldBe (Now + 30.days).toLocalDateString()
        35.days.toMomentString() shouldBe (Now + 35.days).toLocalDateString()
        100.days.toMomentString() shouldBe (Now + 100.days).toLocalDateString()
        (-100).days.toMomentString() shouldBe (Now - 100.days).toLocalDateString()

        Now.toMomentString() shouldBe "now"
        (Now - 12.hours).toMomentString() shouldBe "12h ago"
        (Now + 12.hours).toMomentString() shouldBe "in 12h"
        (Now - 100.days).toMomentString() shouldBe (Now - 100.days).toLocalDateString()
        (Now + 100.days).toMomentString() shouldBe (Now + 100.days).toLocalDateString()
        Instant.fromEpochMilliseconds(0L).toMomentString() shouldBe Instant.fromEpochMilliseconds(0L).toLocalDateString()

        (Yesterday - 1.days).toMomentString() shouldBe "2d ago"
        Yesterday.toMomentString() shouldBe "yesterday"
        Today.toMomentString() shouldBe "today"
        Tomorrow.toMomentString() shouldBe "tomorrow"
        (Tomorrow + 1.days).toMomentString() shouldBe "in 2d"
        LocalDate.fromEpochDays(0).toMomentString() shouldBe LocalDate.fromEpochDays(0).toLocalDateString()
    }

    @Test fun subtract_self() = testAll {
        (Now - Now) should {
            it shouldBeLessThanOrEqualTo Duration.ZERO
            it shouldBeGreaterThanOrEqualTo Duration.ZERO - 1.seconds
        }
        (Today - Today) should {
            it shouldBeLessThanOrEqualTo Duration.ZERO
            it shouldBeGreaterThanOrEqualTo Duration.ZERO - 1.seconds
        }
    }

    @Test fun components() = testAll {
        Instant.parse("1975-08-20T10:15:30.0Z") should {
            it.utcHours shouldBe 10
            it.utcMinutes shouldBe 15
        }
    }
}

internal val instant0202: Instant = Instant.parse("2020-02-02T02:02:02Z")
internal val instant2232: Instant = Instant.parse("2020-02-02T22:32:02Z")
internal val localDate: LocalDate = LocalDate.parse("2020-02-02")
