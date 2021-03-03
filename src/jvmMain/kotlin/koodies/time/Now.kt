package koodies.time

import java.nio.file.attribute.FileTime
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.OffsetDateTime
import java.time.OffsetTime
import java.time.Year
import java.time.YearMonth
import java.time.ZonedDateTime
import kotlin.time.Duration
import kotlin.time.milliseconds

object Now {
    val instant: Instant get() = Instant.now()
    val localTime: LocalTime get() = LocalTime.now()
    val localDate: LocalDate get() = LocalDate.now()
    val localDateTime: LocalDateTime get() = LocalDateTime.now()
    val zonedDateTime: ZonedDateTime get() = ZonedDateTime.now()
    val offsetDateTime: OffsetDateTime get() = OffsetDateTime.now()
    val offsetTime: OffsetTime get() = OffsetTime.now()
    val yearMonth: YearMonth get() = YearMonth.now()
    val year: Year get() = Year.now()

    val fileTime: FileTime get() = FileTime.from(instant)

    val millis: Long get() = instant.toEpochMilli()

    public fun passedSince(start: Long): Duration = (System.currentTimeMillis() - start).milliseconds

    public operator fun plus(duration: Duration): Instant = instant.plusMillis(duration.toLongMilliseconds())

    public operator fun minus(duration: Duration): Instant = instant.minusMillis(duration.toLongMilliseconds())

    public override fun toString(): String = "$instant"
}
