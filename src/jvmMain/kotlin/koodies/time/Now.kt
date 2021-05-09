package koodies.time

import koodies.text.asEmoji
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

public object Now {
    public val instant: Instant get() = Instant.now()
    public val localTime: LocalTime get() = LocalTime.now()
    public val localDate: LocalDate get() = LocalDate.now()
    public val localDateTime: LocalDateTime get() = LocalDateTime.now()
    public val zonedDateTime: ZonedDateTime get() = ZonedDateTime.now()
    public val offsetDateTime: OffsetDateTime get() = OffsetDateTime.now()
    public val offsetTime: OffsetTime get() = OffsetTime.now()
    public val yearMonth: YearMonth get() = YearMonth.now()
    public val year: Year get() = Year.now()
    public val fileTime: FileTime get() = FileTime.from(instant)
    public val millis: Long get() = instant.toEpochMilli()
    public val emoji: String get() = instant.asEmoji().emojiVariant
    public fun passedSince(start: Long): Duration = Duration.milliseconds((System.currentTimeMillis() - start))

    public operator fun plus(duration: Duration): Instant = instant.plusMillis(duration.inWholeMilliseconds)

    public operator fun minus(duration: Duration): Instant = instant.minusMillis(duration.inWholeMilliseconds)
    public override fun toString(): String = "$instant"
}
