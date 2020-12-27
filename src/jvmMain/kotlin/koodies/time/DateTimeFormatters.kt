package koodies.time

import java.nio.file.Path
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.time.temporal.TemporalAccessor

object DateTimeFormatters {
    @Suppress("SpellCheckingInspection")
    val ISO8601_INSTANT = DateTimeFormatter(
        DateTimeFormatter.ISO_INSTANT,
        Path::class to DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH-mm-ss").withZone(ZoneOffset.UTC)
    )
    val ISO8601_LOCAL_DATE = DateTimeFormatter(
        DateTimeFormatter.ISO_LOCAL_DATE,
        Path::class to DateTimeFormatter.ofPattern("yyyy-MM-dd").withZone(ZoneOffset.UTC)
    )
}

@Throws(DateTimeParseException::class)
inline fun <reified TEMPORAL : TemporalAccessor, reified TYPE : Any> String.parseInstant() =
    DateTimeFormatters.ISO8601_INSTANT.parse<TEMPORAL>(this, TYPE::class)

@Throws(DateTimeParseException::class)
inline fun <reified TYPE : Any> String.parseableInstant(): Boolean =
    kotlin.runCatching { DateTimeFormatters.ISO8601_INSTANT.parse<Instant>(this, TYPE::class) }.isSuccess

@Throws(DateTimeParseException::class)
inline fun <reified TEMPORAL : TemporalAccessor, reified TYPE : Any> String.parseLocalDate() =
    DateTimeFormatters.ISO8601_LOCAL_DATE.parse<TEMPORAL>(this, TYPE::class)

@Throws(DateTimeParseException::class)
inline fun <reified TYPE : Any> String.parseableLocalDate(): Boolean =
    kotlin.runCatching { DateTimeFormatters.ISO8601_LOCAL_DATE.parse<LocalDate>(this, TYPE::class) }.isSuccess

fun LocalDate.toInstant(): Instant = this.atStartOfDay().atZone(ZoneOffset.UTC).toInstant()
fun Instant.toLocalDate(): LocalDate = LocalDateTime.ofInstant(this, ZoneOffset.UTC).toLocalDate()


fun Instant.format(): String = DateTimeFormatters.ISO8601_INSTANT.format(this)
fun LocalDate.format(): String = DateTimeFormatters.ISO8601_LOCAL_DATE.format(this)

@JvmName("typeSpecificInstantFormat")
inline fun <reified TYPE : Any> Instant.format(): String = DateTimeFormatters.ISO8601_INSTANT.format<TYPE>(this)

@JvmName("typeSpecificLocalDateFormat")
inline fun <reified TYPE : Any> LocalDate.format(): String = DateTimeFormatters.ISO8601_LOCAL_DATE.format<TYPE>(this)

