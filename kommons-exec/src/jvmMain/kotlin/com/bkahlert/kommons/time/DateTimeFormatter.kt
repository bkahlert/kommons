package com.bkahlert.kommons.time

import java.time.Instant
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.time.temporal.Temporal
import java.time.temporal.TemporalAccessor
import kotlin.reflect.KClass

/**
 * A [DateTimeFormatter] can [format] temporal objects like [Instant]
 * depending on the use case expressed by an additionally provided or interfered type.
 */
public class DateTimeFormatter private constructor(
    private val fallbackDateTimeFormatter: DateTimeFormatter,
    private val specificFormatters: Map<KClass<*>, DateTimeFormatter>,
) {
    public constructor(
        fallbackDateTimeFormatter: DateTimeFormatter,
        vararg specificFormatters: Pair<KClass<*>, DateTimeFormatter>,
    ) : this(fallbackDateTimeFormatter, specificFormatters.toMap())

    public fun forType(type: KClass<*>): DateTimeFormatter =
        specificFormatters.filterKeys { it == type }.let { filtered ->
            when (filtered.size) {
                0 -> fallbackDateTimeFormatter
                1 -> filtered.values.single()
                else -> error("Implementation was changed so more than one formatter is applicable. Needs adaption.")
            }
        }

    public fun format(temporal: Temporal = Instant.now(), type: KClass<*>): String = forType(type).format(temporal)
    public fun format(temporal: Temporal = Instant.now(), instance: Any): String = format(temporal, instance::class)

    @JvmName("formatReified")
    public inline fun <reified TYPE : Any> format(temporal: Temporal = Instant.now()): String = format(temporal, TYPE::class)

    @JvmName("formatAny")
    public fun format(temporal: Temporal = Instant.now()): String = format(temporal, Any::class)

    @Throws(DateTimeParseException::class)
    public inline fun <reified TEMPORAL : TemporalAccessor> parse(text: String, type: KClass<*>): TEMPORAL {
        val formatter = forType(type)
        val temporal: TemporalAccessor = formatter.parse(text)
        val staticFrom = TEMPORAL::class.java.methods.single {
            it.name == "from" && it.parameterTypes.size == 1
        }
        return staticFrom.invoke(null, temporal) as TEMPORAL
    }

    @Throws(DateTimeParseException::class)
    public inline fun <reified TEMPORAL : TemporalAccessor> parseAny(text: String): TEMPORAL = parse(text, TEMPORAL::class)

    @Throws(DateTimeParseException::class)
    public inline fun <reified TEMPORAL : TemporalAccessor, reified TYPE : Any> parse(text: String): TEMPORAL = parse(text, TYPE::class)
}
