package koodies.time

import koodies.test.testEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode.CONCURRENT
import strikt.api.expectCatching
import strikt.api.expectThat
import strikt.assertions.isA
import strikt.assertions.isEqualTo
import strikt.assertions.isFailure
import strikt.assertions.isGreaterThan
import strikt.assertions.isLessThan
import strikt.assertions.isLessThanOrEqualTo
import java.nio.file.attribute.FileTime
import java.time.temporal.Temporal
import java.time.temporal.UnsupportedTemporalTypeException
import kotlin.time.Duration
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.nanoseconds
import kotlin.time.Duration.Companion.seconds
import kotlin.time.measureTime
import java.time.Duration as JavaDuration

class TimeOperationsKtTest {

    @Nested
    @Execution(CONCURRENT)
    inner class SleepKtTest {

        @Test
        fun `should sleep on positive duration`() {
            expectThat(measureTime { seconds(1).sleep() }).isGreaterThan(milliseconds(900)).isLessThan(milliseconds(1100))
        }

        @Test
        fun `should sleep and call block on positive duration`() {
            expectThat(seconds(1).sleep { 42 }).isEqualTo(42)
        }

        @Test
        fun `should not sleep on zero duration`() {
            expectThat(measureTime { Duration.ZERO.sleep() }).isLessThanOrEqualTo(milliseconds(100))
        }

        @Test
        fun `should not sleep and call block on zero duration`() {
            expectThat(Duration.ZERO.sleep { 42 }).isEqualTo(42)
        }

        @Test
        fun `should throw on negative duration`() {
            expectCatching { measureTime { seconds((-1)).sleep() } }.isFailure().isA<IllegalArgumentException>()
        }

        @Test
        fun `should throw and not run block on negative duration`() {
            expectCatching { seconds((-1)).sleep { throw RuntimeException("error") } }.isFailure().isA<IllegalArgumentException>()
        }
    }

    @Nested
    inner class ToIntMilliseconds {

        @Test
        fun `should correspond to long value`() {
            val duration = hours(14.5)
            expectThat(duration.toIntMilliseconds().toLong()).isEqualTo(duration.inWholeMilliseconds)
        }
    }

    @Nested
    inner class Plus {

        @TestFactory
        fun `should add`() = testEach(
            days(2) to JavaDuration.ofDays(2),
            hours(3) to JavaDuration.ofHours(3),
            minutes(4) to JavaDuration.ofMinutes(4),
            seconds(5) to JavaDuration.ofSeconds(5),
            nanoseconds(6) to JavaDuration.ofNanos(6),
        ) { (kotlinDuration, javaDuration) ->
            listOf(
                Now.instant,
                Now.localTime,
                Now.localDateTime,
                Now.zonedDateTime,
                Now.offsetDateTime,
                Now.offsetTime,
            ).forEach { time ->
                expecting("from ${time::class.simpleName}") {
                    time + kotlinDuration
                } that {
                    isA<Temporal>().isEqualTo(time.plus(javaDuration))
                }
            }

            val fileTime = Now.fileTime
            expecting("from FileTime") {
                fileTime + kotlinDuration
            } that {
                isA<FileTime>().isEqualTo(fileTime.toInstant().plus(javaDuration).toFileTime())
            }
        }

        @TestFactory
        fun `should throw if differ in conceptual days`() = testEach(
            Now.localDate,
            Now.yearMonth,
            Now.year,
        ) { time ->
            expectThrows<UnsupportedTemporalTypeException> { time + days(2) }
        }
    }

    @Nested
    inner class Minus {

        @TestFactory
        fun `should subtract`() = testEach(
            days(2) to JavaDuration.ofDays(2),
            hours(3) to JavaDuration.ofHours(3),
            minutes(4) to JavaDuration.ofMinutes(4),
            seconds(5) to JavaDuration.ofSeconds(5),
            nanoseconds(6) to JavaDuration.ofNanos(6),
        ) { (kotlinDuration, javaDuration) ->
            listOf(
                Now.instant,
                Now.localTime,
                Now.localDateTime,
                Now.zonedDateTime,
                Now.offsetDateTime,
                Now.offsetTime,
            ).forEach { time ->
                expecting("from ${time::class.simpleName}") {
                    time - kotlinDuration
                } that {
                    isA<Temporal>().isEqualTo(time.minus(javaDuration))
                }
            }

            val fileTime = Now.fileTime
            expecting("from FileTime") {
                fileTime - kotlinDuration
            } that {
                isA<FileTime>().isEqualTo(fileTime.toInstant().minus(javaDuration).toFileTime())
            }
        }

        @TestFactory
        fun `should throw if differ in conceptual days`() = testEach(
            Now.localDate,
            Now.yearMonth,
            Now.year,
        ) { time ->
            expectThrows<UnsupportedTemporalTypeException> { time - days(2) }
        }
    }

    @Test
    fun `should return FileType`() {
        val now = Now.instant
        expectThat(now.toFileTime()).isEqualTo(FileTime.from(now))
    }
}
