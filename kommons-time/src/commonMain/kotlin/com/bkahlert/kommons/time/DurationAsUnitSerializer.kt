package com.bkahlert.kommons.time

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlin.time.Duration
import kotlin.time.DurationUnit
import kotlin.time.toDuration

public abstract class DurationAsUnitSerializer(
    public val unit: DurationUnit,
) : KSerializer<Duration> {

    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor(
        "com.bkahlert.kommons.time.DurationAsUnitSerializer", PrimitiveKind.LONG
    )

    override fun serialize(encoder: Encoder, value: Duration) {
        encoder.encodeLong(value.toLong(unit))
    }

    override fun deserialize(decoder: Decoder): Duration =
        decoder.decodeLong().toDuration(unit)
}

public object DurationAsNanosecondsSerializer : DurationAsUnitSerializer(DurationUnit.NANOSECONDS)
public object DurationAsMicrosecondsSerializer : DurationAsUnitSerializer(DurationUnit.MICROSECONDS)
public object DurationAsMillisecondsSerializer : DurationAsUnitSerializer(DurationUnit.MILLISECONDS)
public object DurationAsSecondsSerializer : DurationAsUnitSerializer(DurationUnit.SECONDS)
public object DurationAsMinutesSerializer : DurationAsUnitSerializer(DurationUnit.MINUTES)
public object DurationAsHoursSerializer : DurationAsUnitSerializer(DurationUnit.HOURS)
public object DurationAsDaysSerializer : DurationAsUnitSerializer(DurationUnit.DAYS)

public typealias DurationAsNanoseconds = @Serializable(DurationAsNanosecondsSerializer::class) Duration
public typealias DurationAsMicroseconds = @Serializable(DurationAsMicrosecondsSerializer::class) Duration
public typealias DurationAsMilliseconds = @Serializable(DurationAsMillisecondsSerializer::class) Duration
public typealias DurationAsSeconds = @Serializable(DurationAsSecondsSerializer::class) Duration
public typealias DurationAsMinutes = @Serializable(DurationAsMinutesSerializer::class) Duration
public typealias DurationAsHours = @Serializable(DurationAsHoursSerializer::class) Duration
public typealias DurationAsDays = @Serializable(DurationAsDaysSerializer::class) Duration
