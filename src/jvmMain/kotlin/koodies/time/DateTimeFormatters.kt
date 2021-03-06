package koodies.time

import java.nio.file.Path
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.time.temporal.TemporalAccessor

public object DateTimeFormatters {
    @Suppress("SpellCheckingInspection")
    public val ISO8601_INSTANT: koodies.time.DateTimeFormatter = DateTimeFormatter(
        DateTimeFormatter.ISO_INSTANT,
        Path::class to DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH-mm-ss").withZone(ZoneOffset.UTC)
    )
    public val ISO8601_LOCAL_DATE: koodies.time.DateTimeFormatter = DateTimeFormatter(
        DateTimeFormatter.ISO_LOCAL_DATE,
        Path::class to DateTimeFormatter.ofPattern("yyyy-MM-dd").withZone(ZoneOffset.UTC)
    )
}

@Throws(DateTimeParseException::class)
public inline fun <reified TEMPORAL : TemporalAccessor, reified TYPE : Any> String.parseInstant(): TEMPORAL =
    DateTimeFormatters.ISO8601_INSTANT.parse<TEMPORAL>(this, TYPE::class)

@Throws(DateTimeParseException::class)
public inline fun <reified TYPE : Any> String.parseableInstant(): Boolean =
    kotlin.runCatching { DateTimeFormatters.ISO8601_INSTANT.parse<Instant>(this, TYPE::class) }.isSuccess

@Throws(DateTimeParseException::class)
public inline fun <reified TEMPORAL : TemporalAccessor, reified TYPE : Any> String.parseLocalDate(): TEMPORAL =
    DateTimeFormatters.ISO8601_LOCAL_DATE.parse<TEMPORAL>(this, TYPE::class)

@Throws(DateTimeParseException::class)
public inline fun <reified TYPE : Any> String.parseableLocalDate(): Boolean =
    kotlin.runCatching { DateTimeFormatters.ISO8601_LOCAL_DATE.parse<LocalDate>(this, TYPE::class) }.isSuccess

public fun LocalDate.toInstant(): Instant = this.atStartOfDay().atZone(ZoneOffset.UTC).toInstant()
public fun Instant.toLocalDate(): LocalDate = LocalDateTime.ofInstant(this, ZoneOffset.UTC).toLocalDate()
public fun Instant.format(): String = DateTimeFormatters.ISO8601_INSTANT.format(this)
public fun LocalDate.format(): String = DateTimeFormatters.ISO8601_LOCAL_DATE.format(this)

@JvmName("typeSpecificInstantFormat")
public inline fun <reified TYPE : Any> Instant.format(): String = DateTimeFormatters.ISO8601_INSTANT.format<TYPE>(this)

@JvmName("typeSpecificLocalDateFormat")
public inline fun <reified TYPE : Any> LocalDate.format(): String = DateTimeFormatters.ISO8601_LOCAL_DATE.format<TYPE>(this)

