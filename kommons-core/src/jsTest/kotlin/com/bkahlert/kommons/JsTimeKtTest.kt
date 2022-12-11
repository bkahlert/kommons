package com.bkahlert.kommons

import com.bkahlert.kommons.test.testAll
import io.kotest.matchers.comparables.shouldBeGreaterThanOrEqualTo
import io.kotest.matchers.comparables.shouldBeLessThanOrEqualTo
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import kotlinx.datetime.toKotlinInstant
import kotlin.js.Date
import kotlin.test.Test
import kotlin.time.Duration.Companion.ZERO
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.nanoseconds
import kotlin.time.Duration.Companion.seconds

class JsTimeTest {

    @Test
    fun components() = testAll {
        Date("August 19, 1975 23:15:30 GMT-11:00") should {
            it.time shouldBe it.getTime()
            it.timezoneOffset shouldBe it.getTimezoneOffset().minutes

            it.fullYear shouldBe it.getFullYear()
            it.month shouldBe it.getMonth()
            it.date shouldBe it.getDate()
            it.day shouldBe it.getDay()
            it.hours shouldBe it.getHours()
            it.minutes shouldBe it.getMinutes()
            it.seconds shouldBe it.getSeconds()
            it.milliseconds shouldBe it.getMilliseconds()

            it.utcFullYear shouldBe it.getUTCFullYear()
            it.utcMonth shouldBe it.getUTCMonth()
            it.utcDate shouldBe it.getUTCDate()
            it.utcDay shouldBe it.getUTCDay()
            it.utcHours shouldBe it.getUTCHours()
            it.utcMinutes shouldBe it.getUTCMinutes()
            it.utcSeconds shouldBe it.getUTCSeconds()
            it.utcMilliseconds shouldBe it.getUTCMilliseconds()
        }
    }

    @Test
    fun compare_to() = testAll {
        date0202.compareTo(date0202) shouldBe 0
        date0202.compareTo(date2232) shouldBe -1
        date2232.compareTo(date0202) shouldBe +1
    }

    @Test
    fun add() = testAll {
        (Date("August 19, 1975 23:15:30 GMT-11:00") + duration).time shouldBe Date("Fri Aug 22 1975 02:19:35 GMT-11:00").time
    }

    @Test
    fun subtract() = testAll {
        (Date("August 19, 1975 23:15:30 GMT-11:00") - duration).time shouldBe Date("Mon Aug 18 1975 08:11:25 GMT+0100").time
    }

    @Test
    fun subtract_self() = testAll {
        (Date() - Date()) should {
            it shouldBeLessThanOrEqualTo ZERO
            it shouldBeGreaterThanOrEqualTo ZERO - 1.seconds
        }
    }

    @Test
    fun to_local_date_string() = testAll {
        date0202.toLocalDateString() shouldBe "February 2, 2020"
        date2232.toLocalDateString() shouldBe "February 2, 2020"
    }

    @Test
    fun to_moment_string() = testAll {
        Date() should {
            it.toMomentString() shouldBe it.toKotlinInstant().toMomentString()
        }
    }
}

private val duration = 2.days + 3.hours + 4.minutes + 5.seconds + 6.nanoseconds
private val date0202: Date = Date("February 02, 2020 02:02:02 UTC")
private val date2232: Date = Date("February 02, 2020 22:32:02 UTC")
