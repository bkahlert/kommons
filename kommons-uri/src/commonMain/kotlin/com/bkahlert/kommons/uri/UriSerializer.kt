package com.bkahlert.kommons.uri

import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationException
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

public object UriSerializer : KSerializer<Uri> {

    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor(
        "com.bkahlert.kommons.uri.UriSerializer", PrimitiveKind.STRING
    )

    override fun serialize(encoder: Encoder, value: Uri) {
        encoder.encodeString(value.toString())
    }

    override fun deserialize(decoder: Decoder): Uri {
        return Uri.parseOrNull(decoder.decodeString()) ?: throw SerializationException()
    }
}


internal object GenericUriSerializer : KSerializer<GenericUri> {

    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor(
        "com.bkahlert.kommons.uri.UriSerializer", PrimitiveKind.STRING
    )

    override fun serialize(encoder: Encoder, value: GenericUri) {
        encoder.encodeString(value.toString())
    }

    override fun deserialize(decoder: Decoder): GenericUri = when (val uri = Uri.parseOrNull(decoder.decodeString()) ?: throw SerializationException()) {
        is GenericUri -> uri
        else -> GenericUri(uri.scheme, uri.authority, uri.path, uri.query, uri.fragment)
    }
}
