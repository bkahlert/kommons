package koodies.unit

import koodies.math.toBigDecimal
import koodies.math.toBigInteger
import koodies.test.tests
import org.junit.jupiter.api.TestFactory
import strikt.assertions.isEqualTo
import kotlin.time.Duration

class DurationKtTest {

    @TestFactory
    fun `should return seconds`() = tests {
        expecting { 42.seconds } that { isEqualTo(Duration.seconds(42)) }
        expecting { 42L.seconds } that { isEqualTo(Duration.seconds(42L)) }
        expecting { 42.0.seconds } that { isEqualTo(Duration.seconds(42.0)) }
        expecting { 42.toBigDecimal().seconds } that { isEqualTo(Duration.seconds(42.0)) }
        expecting { 42.toBigInteger().seconds } that { isEqualTo(Duration.seconds(42L)) }
    }

    @TestFactory
    fun `should return minutes`() = tests {
        expecting { 42.minutes } that { isEqualTo(Duration.minutes(42)) }
        expecting { 42L.minutes } that { isEqualTo(Duration.minutes(42L)) }
        expecting { 42.0.minutes } that { isEqualTo(Duration.minutes(42.0)) }
        expecting { 42.toBigDecimal().minutes } that { isEqualTo(Duration.minutes(42.0)) }
        expecting { 42.toBigInteger().minutes } that { isEqualTo(Duration.minutes(42L)) }
    }

    @TestFactory
    fun `should return hours`() = tests {
        expecting { 42.hours } that { isEqualTo(Duration.hours(42)) }
        expecting { 42L.hours } that { isEqualTo(Duration.hours(42L)) }
        expecting { 42.0.hours } that { isEqualTo(Duration.hours(42.0)) }
        expecting { 42.toBigDecimal().hours } that { isEqualTo(Duration.hours(42.0)) }
        expecting { 42.toBigInteger().hours } that { isEqualTo(Duration.hours(42L)) }
    }

    @TestFactory
    fun `should return days`() = tests {
        expecting { 42.days } that { isEqualTo(Duration.days(42)) }
        expecting { 42L.days } that { isEqualTo(Duration.days(42L)) }
        expecting { 42.0.days } that { isEqualTo(Duration.days(42.0)) }
        expecting { 42.toBigDecimal().days } that { isEqualTo(Duration.days(42.0)) }
        expecting { 42.toBigInteger().days } that { isEqualTo(Duration.days(42L)) }
    }

    @TestFactory
    fun `should support binary prefixes`() = tests {
        expecting { 42.mibi.seconds.inWholeMilliseconds } that { isEqualTo(41) }
        expecting { 42.mubi.seconds.inWholeMicroseconds } that { isEqualTo(40) }
        expecting { 42.nabi.seconds.inWholeNanoseconds } that { isEqualTo(39) }
    }

    @TestFactory
    fun `should support decimal prefixes`() = tests {
        expecting { 42.milli.seconds } that { isEqualTo(Duration.milliseconds(42)) }
        expecting { 42.micro.seconds } that { isEqualTo(Duration.microseconds(42)) }
        expecting { 42.nano.seconds } that { isEqualTo(Duration.nanoseconds(42)) }
    }
}
