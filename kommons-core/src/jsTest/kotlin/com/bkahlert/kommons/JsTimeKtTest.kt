package com.bkahlert.kommons

import com.bkahlert.kommons.test.testAll
import io.kotest.matchers.comparables.shouldBeGreaterThanOrEqualTo
import io.kotest.matchers.comparables.shouldBeLessThan
import io.kotest.matchers.comparables.shouldBeLessThanOrEqualTo
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import kotlin.js.Date
import kotlin.test.Test
import kotlin.time.Duration.Companion.ZERO
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.nanoseconds
import kotlin.time.Duration.Companion.seconds

class JsTimeTest {

    @Test fun local_date() = testAll {
        LocalDate(2007, 11, 3) should {
            it.fullYear shouldBe 2007
            it.month shouldBe 11
            it.date shouldBe 3

            it.instant should { instant ->
                instant.compareTo(Date(2007, 11, 3, 0, 0, 0, 0)) shouldBeLessThanOrEqualTo 0
                instant.compareTo(Date(2007, 11, 3, 0, 0, 0, 0) - 1.seconds) shouldBeGreaterThanOrEqualTo 0
            }
        }

        LocalDate(2007, 11, 3) shouldBe LocalDate(2007, 11, 3)
        LocalDate(2007, 11, 1) shouldBeLessThan LocalDate(2007, 11, 3)
        LocalDate(2007, 11, 3).toString() shouldBe "2007-12-03"
    }

    private val duration = 2.days + 3.hours + 4.minutes + 5.seconds + 6.nanoseconds

    @Test
    fun now() = testAll {
        Now should {
            it.compareTo(Now) shouldBeLessThanOrEqualTo 0
            it.compareTo(Now - 1.seconds) shouldBeGreaterThanOrEqualTo 0
        }
    }

    @Test
    fun today() = testAll {
        Today should {
            it.fullYear shouldBe Now.fullYear
            it.month shouldBe Now.month
            it.date shouldBe Now.date
        }
    }

    @Test
    fun yesterday() = testAll {
        Yesterday should {
            it.fullYear shouldBe Now.fullYear
            it.month shouldBe Now.month
            it.date shouldBe Now.date - 1
        }
    }

    @Test
    fun tomorrow() = testAll {
        Tomorrow should {
            it.fullYear shouldBe Now.fullYear
            it.month shouldBe Now.month
            it.date shouldBe Now.date + 1
        }
    }

    @Test
    fun timestamp() = testAll {
        val time = Date().getTime().toLong()
        Timestamp should {
            it shouldBeLessThanOrEqualTo time
            it shouldBeGreaterThanOrEqualTo time - 500
        }
    }

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
    fun add() = testAll {
        (Date("August 19, 1975 23:15:30 GMT-11:00") + duration).time shouldBe Date("Fri Aug 22 1975 02:19:35 GMT-11:00").time
        LocalDate(1975, 7, 19) + duration shouldBe LocalDate(1975, 7, 21)
    }

    @Test
    fun subtract() = testAll {
        (Date("August 19, 1975 23:15:30 GMT-11:00") - duration).time shouldBe Date("Mon Aug 18 1975 08:11:25 GMT+0100").time
        LocalDate(1975, 7, 19) - duration shouldBe LocalDate(1975, 7, 17)
    }

    @Test
    fun subtract_self() = testAll {
        (Now - Now) should {
            it shouldBeLessThanOrEqualTo ZERO
            it shouldBeGreaterThanOrEqualTo ZERO - 1.seconds
        }
        (Today - Today) should {
            it shouldBeLessThanOrEqualTo ZERO
            it shouldBeGreaterThanOrEqualTo ZERO - 1.seconds
        }
    }
}

internal actual val instant0202: Instant = Date("February 02, 2020 02:02:02 UTC")
internal actual val instant2232: Instant = Date("February 02, 2020 22:32:02 UTC")
internal actual val localDate: LocalDate = LocalDate(2020, 1, 2)
