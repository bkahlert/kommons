package com.bkahlert.kommons.uri

import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationException
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

public object DataUriSerializer : KSerializer<DataUri> {

    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor(
        "com.bkahlert.kommons.uri.DataUriSerializer", PrimitiveKind.STRING
    )

    override fun serialize(encoder: Encoder, value: DataUri) {
        encoder.encodeString(value.toString())
    }

    override fun deserialize(decoder: Decoder): DataUri {
        return DataUri.parseOrNull(decoder.decodeString()) ?: throw SerializationException()
    }
}
