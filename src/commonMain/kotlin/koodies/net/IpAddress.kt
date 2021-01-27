@file:Suppress("RemoveRedundantQualifierName")

package koodies.net

import com.ionspin.kotlin.bignum.integer.BigInteger
import com.ionspin.kotlin.bignum.integer.toBigInteger
import com.ionspin.kotlin.bignum.integer.util.fromTwosComplementByteArray
import koodies.number.FF
import koodies.number.OO
import koodies.number.ZERO
import koodies.number.padStart
import koodies.number.toBytes
import koodies.number.toInt
import koodies.number.toPositiveInt
import koodies.number.toUBytes
import koodies.number.toUInt
import koodies.ranges.map
import koodies.ranges.size
import koodies.regex.countMatches
import koodies.unit.Size
import koodies.unit.bits

sealed class IpAddress : Comparable<IpAddress> {
    abstract val version: Version
    abstract val bytes: ByteArray
    abstract val value: BigInteger

    interface Version {
        val major: Int
        val addressLength: Size
        val bitCount: Int
        val byteCount: Int
    }
}


internal data class VersionImpl(
    override val major: Int,
    override val addressLength: Size,
) : IpAddress.Version {
    override val bitCount: Int = addressLength.bits.toInt()
    override val byteCount: Int = addressLength.bytes.toInt()
}

class IPv4Address(byte0: Byte, byte1: Byte, byte2: Byte, byte3: Byte) : IpAddress() {
    private constructor(bytes: ByteArray) : this(bytes[0], bytes[1], bytes[2], bytes[3])
    constructor(value: Int) : this(value.toBytes(trim = false))

    override val version: IpAddress.Version = IPv4Address
    override val bytes: ByteArray = byteArrayOf(byte0, byte1, byte2, byte3)
    override val value by lazy { bytes.toUInt().toBigInteger() }

    fun rangeTo(endInclusive: IPv4Address): IpAddressRange<IPv4Address> = IpAddressRange(this, endInclusive)

    private val string by lazy { bytes.map { it.toPositiveInt() }.joinToString(".") }
    override fun toString(): String = string

    override fun compareTo(other: IpAddress): Int = value.compareTo(other.value)
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as IPv4Address

        if (!bytes.contentEquals(other.bytes)) return false

        return true
    }

    override fun hashCode(): Int = bytes.contentHashCode()

    companion object : IpAddress.Version by VersionImpl(4, 32.bits) {
        val RANGE = parse("0.0.0.0")..parse("255.255.255.255")
        val RFC1918_24block = parse("10.0.0.0")..parse("10.255.255.255")
        val RFC1918_20block = parse("172.16.0.0")..parse("172.31.255.255")
        val RFC1918_16block = parse("192.168.0.0")..parse("192.168.255.255")

        fun parse(ipAddress: String): IPv4Address {
            val byteStrings = ipAddress.split(".")
            require(byteStrings.size == byteCount) { "IP address must consist of $byteCount bytes but ${byteStrings.size} were provided." }
            val bytes = byteStrings.map { byteString ->
                byteString.toInt()
                    .also { require(it in 0..255) { "$it must be between 0 and 255." } }
                    .toByte()
            }
            return IPv4Address(bytes[0], bytes[1], bytes[2], bytes[3])
        }
    }
}

class IPv6Address(
    byte0: Byte, byte1: Byte, byte2: Byte, byte3: Byte,
    byte4: Byte, byte5: Byte, byte6: Byte, byte7: Byte,
    byte8: Byte, byte9: Byte, byte10: Byte, byte11: Byte,
    byte12: Byte, byte13: Byte, byte14: Byte, byte15: Byte,
) : IpAddress() {
    private constructor(bytes: ByteArray) : this(
        bytes[0], bytes[1], bytes[2], bytes[3],
        bytes[4], bytes[5], bytes[6], bytes[7],
        bytes[8], bytes[9], bytes[10], bytes[11],
        bytes[12], bytes[13], bytes[14], bytes[15],
    )

    constructor(value: BigInteger) : this(value
        .also {
            require(!it.isNegative && it.numberOfWords <= 4) {
                "An IPv6 address cannot hold more than $bitCount bits."
            }
        }
        .toByteArray()
        .padStart(byteCount))

    override val version: IpAddress.Version = IPv6Address

    override val bytes: ByteArray = byteArrayOf(
        byte0, byte1, byte2, byte3,
        byte4, byte5, byte6, byte7,
        byte8, byte9, byte10, byte11,
        byte12, byte13, byte14, byte15,
    )
    override val value by lazy {
        val hexString = bytes.dropWhile { it == Byte.ZERO }.joinToString("") { it.toUByte().toString(16).padStart(2, '0') }
        BigInteger.parseString(hexString.takeUnless { it.isBlank() } ?: "0", 16)
    }

    private val paddedBytes by lazy {
        bytes.map { it.toPositiveInt().toString(16).padStart(2, '0') }
            .windowed(2, 2) { it.joinToString("") }
    }

    fun rangeTo(endInclusive: IPv6Address): IpAddressRange<IPv6Address> = IpAddressRange(this, endInclusive)

    /**
     * This representation consists of eight hextets each consisting of four
     * hexadecimal digits—leading zeros included.
     *
     * Example: `0000:0000:0000:0000:0000:ffff:c0a8:1001`
     *
     * @see conventionalRepresentation
     * @see compressedRepresentation
     * @see <a href="https://tools.ietf.org/html/rfc5952">A Recommendation for IPv6 Address Text Representation</a>
     */
    val fullRepresentation by lazy { paddedBytes.joinToString(":") }

    /**
     * This representation consists of eight hextets each consisting of
     * one to four hexadecimal digits—leading zeros removed.
     *
     * Example: `0:0:0:0:0:ffff:c0a8:1001`
     *
     * @see fullRepresentation
     * @see compressedRepresentation
     * @see <a href="https://tools.ietf.org/html/rfc5952">A Recommendation for IPv6 Address Text Representation</a>
     */
    val conventionalRepresentation by lazy {
        paddedBytes.joinToString(":") { hextet ->
            hextet.dropWhile { it == '0' }.takeUnless { it.isEmpty() } ?: "0"
        }
    }

    /**
     * This representation consists of up to eight hextets each consisting of
     * one to four hexadecimal digits—leading zeros removed.
     *
     * This is the shortest representation as it removes the longest sequence
     * of `0` hextets—given such a sequences spans at least two hextets.
     *
     * Example: `::ffff:c0a8:1001`
     *
     * @see fullRepresentation
     * @see conventionalRepresentation
     * @see <a href="https://tools.ietf.org/html/rfc5952">A Recommendation for IPv6 Address Text Representation</a>
     */
    val compressedRepresentation by lazy {
        conventionalRepresentation.compress()
    }

    /**
     * Returns the [compressedRepresentation] of this IPv6 address, e.g. `::ffff:c0a8:1001`.
     *
     * @see fullRepresentation
     * @see conventionalRepresentation
     * @see compressedRepresentation
     */
    override fun toString(): String = compressedRepresentation

    override fun compareTo(other: IpAddress): Int = value.compareTo(other.value)

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as IPv6Address

        if (!bytes.contentEquals(other.bytes)) return false

        return true
    }

    override fun hashCode(): Int {
        return bytes.contentHashCode()
    }


    companion object : IpAddress.Version by VersionImpl(6, 128.bits) {
        const val sizeHextets = 8
        val DEFAULT_ROOT by lazy { IPv6Address(BigInteger.ZERO) }
        val LOOPBACK by lazy { IPv6Address(BigInteger.ONE) }
        val RANGE by lazy { DEFAULT_ROOT..parse("FFFF:FFFF:FFFF:FFFF:FFFF:FFFF:FFFF:FFFF") }

        val IPv4_TO_IPv6_MAPPING_PREFIX by lazy {
            byteArrayOf(
                Byte.OO, Byte.OO, Byte.OO, Byte.OO,
                Byte.OO, Byte.OO, Byte.OO, Byte.OO,
                Byte.OO, Byte.OO, Byte.FF, Byte.FF,
            )
        }
        val IPv4_TO_IPv6_MAPPING_OFFSET by lazy {
            BigInteger.fromTwosComplementByteArray(IPv4_TO_IPv6_MAPPING_PREFIX) shl IPv4Address.bitCount
        }
        val IPv4_MAPPED_RANGE by lazy {
            IPv4Address.RANGE
                .map { value + IPv4_TO_IPv6_MAPPING_OFFSET }
                .let { IPv6Address(it.start)..IPv6Address(it.endInclusive) }
        }

        val IPv4_TO_IPv6_NAT64_MAPPING_PREFIX by lazy {
            byteArrayOf(
                Byte.OO, 0x64, Byte.FF, 0x9Bu.toByte(),
                Byte.OO, Byte.OO, Byte.OO, Byte.OO,
                Byte.OO, Byte.OO, Byte.OO, Byte.OO,
            )
        }
        val IPv4_TO_IPv6_NAT64_MAPPING_OFFSET by lazy {
            BigInteger.fromTwosComplementByteArray(IPv4_TO_IPv6_NAT64_MAPPING_PREFIX) shl IPv4Address.bitCount
        }
        val IPv4_NAT64_MAPPED_RANGE by lazy {
            IPv4Address.RANGE
                .map { value + IPv4_TO_IPv6_NAT64_MAPPING_OFFSET }
                .let { IPv6Address(it.start)..IPv6Address(it.endInclusive) }
        }

        val COMPRESSOR_PATTERN by lazy { Regex("0{1,4}(?::0{1,4})+") }
        val COMPRESSION_PATTERN by lazy { Regex.fromLiteral("::") }

        private fun String.compress() =
            if (compressed) this
            else COMPRESSOR_PATTERN
                .findAll(this)
                .maxByOrNull { it.value.length }
                ?.let { if (it.range.size == length) "::" else replaceRange(it.range, ":") } ?: this

        private val String.compressed get() = COMPRESSION_PATTERN.countMatches(this) == 1

        fun parse(ipAddress: String): IPv6Address {
            val mixedStrings = ipAddress.split(":")
            val hextetStrings = mixedStrings.takeUnless { it.last().contains(".") }
                ?: mixedStrings.take(mixedStrings.size - 1) + IPv4Address.parse(mixedStrings.last())
                    .value.toString(16).padStart(8, '0').chunkedSequence(4).toList()
            val compressions = COMPRESSION_PATTERN.countMatches(ipAddress)
            require(compressions < 2) { "IP address must not use more than 1 compression (::) but $compressions were found." }
            require(compressions == 1 || hextetStrings.size == sizeHextets) { "IP address must consist of $byteCount hextets but ${hextetStrings.size} were provided." }
            val bytes = hextetStrings.let {
                val endings = it.takeWhile { it.isNotBlank() } to it.takeLastWhile { it.isNotBlank() }
                val missing = (sizeHextets - (endings.first.size + endings.second.size)).coerceAtLeast(0)
                endings.first + MutableList(missing) { "0" } + endings.second
            }.also { eightHextetStrings ->
                require(eightHextetStrings.none { it.length > 4 }) {
                    val invalid = eightHextetStrings.filter { it.length > 4 }.joinToString(", ")
                    "Each hextet must consist of at most 4 hexadecimal digits (0-F). Found $invalid"
                }
            }.flatMap { hextetString ->
                hextetString.toUInt(16)
                    .also { require(it in 0u..65535u) { "$it must be between 0 and FFFF." } }
                    .toUBytes(trim = false).takeLast(2)
            }.map { it.toByte() }
            return IPv6Address(
                bytes[0], bytes[1], bytes[2], bytes[3],
                bytes[4], bytes[5], bytes[6], bytes[7],
                bytes[8], bytes[9], bytes[10], bytes[11],
                bytes[12], bytes[13], bytes[14], bytes[15],
            )
        }
    }
}

fun String.toAnyIp(): IpAddress = when {
    contains(":") -> IPv6Address.parse(this)
    contains(".") -> IPv4Address.parse(this)
    else -> throw NumberFormatException("$this is not valid IP address.")
}

inline fun <reified IP : IpAddress> String.toIp(): IP {
    val ipAddress = toAnyIp()
    return (ipAddress as? IP) ?: error("IP $ipAddress is no ${IP::class.simpleName}")
}

inline fun <reified IP : IpAddress> ipOf(value: String): IP = value.toIp<IP>()

fun IPv4Address.toIPv6Address() = IPv6Address(value + IPv6Address.IPv4_TO_IPv6_MAPPING_OFFSET)
fun IPv6Address.toIPv4Address(): IPv4Address {
    require(this in IPv6Address.IPv4_MAPPED_RANGE) { "$this cannot be mapped to IPv4 as it's not in the range ${IPv6Address.IPv4_MAPPED_RANGE}" }
    return IPv4Address((value - IPv6Address.IPv4_TO_IPv6_MAPPING_OFFSET).intValue(false))
}
