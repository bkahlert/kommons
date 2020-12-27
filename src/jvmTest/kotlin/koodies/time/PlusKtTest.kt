package koodies.time

import org.junit.jupiter.api.DynamicContainer.dynamicContainer
import org.junit.jupiter.api.DynamicTest.dynamicTest
import org.junit.jupiter.api.TestFactory
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode.CONCURRENT
import strikt.api.expectCatching
import strikt.api.expectThat
import strikt.assertions.isA
import strikt.assertions.isEqualTo
import strikt.assertions.isFailure
import java.nio.file.attribute.FileTime
import java.time.Duration
import java.time.temporal.Temporal
import java.time.temporal.UnsupportedTemporalTypeException
import kotlin.time.days
import kotlin.time.hours
import kotlin.time.minutes
import kotlin.time.nanoseconds
import kotlin.time.seconds

@Execution(CONCURRENT)
class PlusKtTest {

    @TestFactory
    fun `should add`() = listOf(
        2.days to Duration.ofDays(2),
        3.hours to Duration.ofHours(3),
        4.minutes to Duration.ofMinutes(4),
        5.seconds to Duration.ofSeconds(5),
        6.nanoseconds to Duration.ofNanos(6),
    ).map { (kotlinDuration, javaDuration) ->
        dynamicContainer("$kotlinDuration", listOf(
            Now.instant,
            Now.localTime,
            Now.localDateTime,
            Now.zonedDateTime,
            Now.offsetDateTime,
            Now.offsetTime,
        ).map { time ->
            dynamicTest("from ${time::class.simpleName}") {
                expectThat(time + kotlinDuration).isA<Temporal>().isEqualTo(time.plus(javaDuration))
            }
        } + dynamicTest("from FileTime") {
            val fileTime = Now.fileTime
            expectThat(fileTime + kotlinDuration).isA<FileTime>().isEqualTo(fileTime.toInstant().plus(javaDuration).toFileTime())
        })
    }

    @TestFactory
    fun `should throw if differ in conceptual days`() = listOf(
        Now.localDate,
        Now.yearMonth,
        Now.year,
    ).map { time ->
        dynamicTest("from ${time::class.simpleName}") {
            expectCatching { time + 2.days }.isFailure().isA<UnsupportedTemporalTypeException>()
        }
    }
}

