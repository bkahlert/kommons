package com.bkahlert.kommons

import com.bkahlert.kommons.test.testAll
import io.kotest.matchers.comparables.shouldBeGreaterThanOrEqualTo
import io.kotest.matchers.comparables.shouldBeLessThanOrEqualTo
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import org.junit.jupiter.api.Test
import java.nio.file.attribute.FileTime
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.OffsetDateTime
import java.time.OffsetTime
import java.time.ZonedDateTime
import kotlin.time.Duration
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.nanoseconds
import kotlin.time.Duration.Companion.seconds
import kotlin.time.toJavaDuration

class JvmInstantTest {

    private val duration = 2.days + 3.hours + 4.minutes + 5.seconds + 6.nanoseconds
    private val javaDuration = duration.toJavaDuration()


    @Test
    fun now() = testAll {
        Now should {
            it shouldBeLessThanOrEqualTo Instant.now()
            it shouldBeGreaterThanOrEqualTo Instant.now() - 1.seconds
        }
    }

    @Test
    fun today() = testAll {
        Today shouldBe LocalDate.now()
    }

    @Test
    fun yesterday() = testAll {
        Yesterday should {
            it shouldBe LocalDate.now() - 1.days
            it shouldBe LocalDate.now() - (1.days + 1.seconds)
            it shouldBe LocalDate.now() - (2.days - 1.seconds)
            it shouldNotBe LocalDate.now() - (1.days - 1.seconds)
            it shouldNotBe LocalDate.now() - 2.days
        }
    }

    @Test
    fun tomorrow() = testAll {
        Tomorrow should {
            it shouldBe LocalDate.now() + 1.days
            it shouldBe LocalDate.now() + (1.days + 1.seconds)
            it shouldBe LocalDate.now() + (2.days - 1.seconds)
            it shouldNotBe LocalDate.now() + (1.days - 1.seconds)
            it shouldNotBe LocalDate.now() + 2.days
        }
    }

    @Test
    fun timestamp() = testAll {
        Timestamp should {
            it shouldBeLessThanOrEqualTo Instant.now().toEpochMilli()
            it shouldBeGreaterThanOrEqualTo Instant.now().toEpochMilli() - 100
        }
    }

    @Test
    fun `should add`() = testAll {
        (Instant.now() + duration) should {
            it shouldBeLessThanOrEqualTo Instant.now().plus(javaDuration)
            it shouldBeGreaterThanOrEqualTo Instant.now().plus(javaDuration) - 1.seconds
        }
        (LocalTime.now() + duration) should {
            it shouldBeLessThanOrEqualTo LocalTime.now().plus(javaDuration)
            it shouldBeGreaterThanOrEqualTo LocalTime.now().plus(javaDuration) - 1.seconds
        }
        (LocalDateTime.now() + duration) should {
            it shouldBeLessThanOrEqualTo LocalDateTime.now().plus(javaDuration)
            it shouldBeGreaterThanOrEqualTo LocalDateTime.now().plus(javaDuration) - 1.seconds
        }
        (ZonedDateTime.now() + duration) should {
            it shouldBeLessThanOrEqualTo ZonedDateTime.now().plus(javaDuration)
            it shouldBeGreaterThanOrEqualTo ZonedDateTime.now().plus(javaDuration) - 1.seconds
        }
        (OffsetDateTime.now() + duration) should {
            it shouldBeLessThanOrEqualTo OffsetDateTime.now().plus(javaDuration)
            it shouldBeGreaterThanOrEqualTo OffsetDateTime.now().plus(javaDuration) - 1.seconds
        }
        (OffsetTime.now() + duration) should {
            it shouldBeLessThanOrEqualTo OffsetTime.now().plus(javaDuration)
            it shouldBeGreaterThanOrEqualTo OffsetTime.now().plus(javaDuration) - 1.seconds
        }
        (FileTime.from(Instant.now()) + duration) should {
            it shouldBeLessThanOrEqualTo FileTime.from(Instant.now().plus(javaDuration))
            it shouldBeGreaterThanOrEqualTo FileTime.from(Instant.now().plus(javaDuration)) - 1.seconds
        }
    }

    @Test
    fun `should subtract`() = testAll {
        (Instant.now() - duration) should {
            it shouldBeLessThanOrEqualTo Instant.now().minus(javaDuration)
            it shouldBeGreaterThanOrEqualTo Instant.now().minus(javaDuration) - 1.seconds
        }
        (LocalTime.now() - duration) should {
            it shouldBeLessThanOrEqualTo LocalTime.now().minus(javaDuration)
            it shouldBeGreaterThanOrEqualTo LocalTime.now().minus(javaDuration) - 1.seconds
        }
        (LocalDateTime.now() - duration) should {
            it shouldBeLessThanOrEqualTo LocalDateTime.now().minus(javaDuration)
            it shouldBeGreaterThanOrEqualTo LocalDateTime.now().minus(javaDuration) - 1.seconds
        }
        (ZonedDateTime.now() - duration) should {
            it shouldBeLessThanOrEqualTo ZonedDateTime.now().minus(javaDuration)
            it shouldBeGreaterThanOrEqualTo ZonedDateTime.now().minus(javaDuration) - 1.seconds
        }
        (OffsetDateTime.now() - duration) should {
            it shouldBeLessThanOrEqualTo OffsetDateTime.now().minus(javaDuration)
            it shouldBeGreaterThanOrEqualTo OffsetDateTime.now().minus(javaDuration) - 1.seconds
        }
        (OffsetTime.now() - duration) should {
            it shouldBeLessThanOrEqualTo OffsetTime.now().minus(javaDuration)
            it shouldBeGreaterThanOrEqualTo OffsetTime.now().minus(javaDuration) - 1.seconds
        }
        (FileTime.from(Instant.now()) - duration) should {
            it shouldBeLessThanOrEqualTo FileTime.from(Instant.now().minus(javaDuration))
            it shouldBeGreaterThanOrEqualTo FileTime.from(Instant.now().minus(javaDuration)) - 1.seconds
        }
    }

    @Test fun `should return FileTime`() {
        val now = Instant.now()
        now.toFileTime() shouldBe FileTime.from(now)
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

internal actual val instant0202: Instant = Instant.parse("2020-02-02T02:02:02Z")
internal actual val instant2232: Instant = Instant.parse("2020-02-02T22:32:02Z")
internal actual val localDate: LocalDate = LocalDate.parse("2020-02-02")
