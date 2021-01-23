package koodies.number

val Byte.Companion.ZERO: Byte get() = 0x0
fun Byte.toPositiveInt() = toInt() and 0xFF
fun Byte.toDecString() = toPositiveInt().toString()

fun Int.toBytes(trim: Boolean = true): ByteArray =
    0.until(Int.SIZE_BYTES)
        .map { i -> (shr((Int.SIZE_BYTES - i - 1) * Byte.SIZE_BITS) and 0xFF).toByte() }
        .let { if (trim) it.dropWhile { byte -> byte == 0.toByte() } else it }
        .toByteArray()

fun UInt.toUBytes(trim: Boolean = true): UByteArray =
    0.until(Int.SIZE_BYTES)
        .map { i -> shr((UInt.SIZE_BYTES - i - 1) * UByte.SIZE_BITS).toUByte() }
        .let { if (trim) it.dropWhile { byte -> byte == 0.toUByte() } else it }
        .toUByteArray()

fun Iterable<Byte>.toInt() = toList().toByteArray().toInt()
fun Iterable<UByte>.toUInt() = toList().toUByteArray().toUInt()

fun ByteArray.toInt(): Int {
    require(size <= Int.SIZE_BYTES) { "The byte array must not consist of more than ${Int.SIZE_BYTES} bytes." }
    return fold(0) { value, byte -> value.shl(Byte.SIZE_BITS) + byte.toPositiveInt() }
}

fun ByteArray.toUInt(): UInt = toInt().toUInt()

fun UByteArray.toUInt(): UInt {
    require(count() <= UInt.SIZE_BYTES) { "The byte array must not consist of more than ${UInt.SIZE_BYTES} bytes." }
    return fold(0u) { value, byte -> value.shl(UByte.SIZE_BITS) + byte }
}

fun UByteArray.toInt(): Int = toUInt().toInt()
