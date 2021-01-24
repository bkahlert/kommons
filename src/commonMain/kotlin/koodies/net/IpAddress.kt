package koodies.net

import com.ionspin.kotlin.bignum.integer.BigInteger
import com.ionspin.kotlin.bignum.integer.toBigInteger
import com.ionspin.kotlin.bignum.integer.util.fromTwosComplementByteArray
import koodies.number.FF
import koodies.number.OO
import koodies.number.ZERO
import koodies.number.padStart
import koodies.number.toBytes
import koodies.number.toPositiveInt
import koodies.number.toUBytes
import koodies.number.toUInt
import koodies.ranges.map
import koodies.ranges.size
import koodies.regex.countMatches

interface IpAddress : Comparable<IpAddress> {
    val bytes: ByteArray
    val value: BigInteger
}

class IPv4Address(byte0: Byte, byte1: Byte, byte2: Byte, byte3: Byte) : IpAddress, Comparable<IpAddress> {
    private constructor(bytes: ByteArray) : this(bytes[0], bytes[1], bytes[2], bytes[3])
    constructor(value: Int) : this(value.toBytes(trim = false))

    override val bytes: ByteArray = byteArrayOf(byte0, byte1, byte2, byte3)
    override val value by lazy { bytes.toUInt().toBigInteger() }
    private val uintValue by lazy { bytes.toUInt() }

    operator fun rangeTo(endInclusive: IPv4Address): Range = Range(endInclusive)

    open inner class Range(override val endInclusive: IPv4Address) : ClosedRange<IPv4Address> {
        init {
            require(this@IPv4Address <= endInclusive) { "${this@IPv4Address} must be less or equal to $endInclusive" }
        }

        override val start: IPv4Address = this@IPv4Address
        val subnet by lazy { Subnet(endInclusive) }
        val usable by lazy { maxOf(start, subnet.firstHost)..minOf(endInclusive, subnet.lastHost) }
        val firstUsableHost by lazy { usable.start }
        val lastUsableHost by lazy { usable.endInclusive }

        private val string by lazy { "${this@IPv4Address}..$endInclusive" }
        override fun toString(): String = string
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other == null || this::class != other::class) return false

            other as Range

            if (endInclusive != other.endInclusive) return false
            if (start != other.start) return false

            return true
        }

        override fun hashCode(): Int {
            var result = endInclusive.hashCode()
            result = 31 * result + start.hashCode()
            return result
        }
    }

    inner class Subnet(endInclusive: IPv4Address) {
        init {
            require(this@IPv4Address <= endInclusive) { "${this@IPv4Address} must be less or equal to $endInclusive" }
        }

        val bitCount: Int by lazy { endInclusive.uintValue.xor(uintValue).countLeadingZeroBits() }
        val wildcardBitCount: Int by lazy { SIZE_BITS - bitCount }

        val hostCount: BigInteger by lazy { BigInteger.TWO shl (wildcardBitCount.dec()) }
        val usableHostCount: BigInteger by lazy { hostCount - 2 }
        val _mask: BigInteger = "0".repeat((hostCount - 1).toString(2).length).padStart(IPv6Address.SIZE_BITS, '1').toBigInteger(2)
        val networkAddress by lazy { IPv4Address(uintValue.and(_mask.uintValue(false)).toInt()) }
        val broadcastAddress by lazy { IPv4Address(networkAddress.uintValue.or(hostCount.uintValue()).dec().toInt()) }
        val firstHost by lazy { IPv4Address(networkAddress.uintValue.inc().toInt()) }
        val lastHost by lazy { IPv4Address(broadcastAddress.uintValue.dec().toInt()) }

        val mask by lazy { IPv4Address(_mask.intValue(false)).toString() }
        private val string by lazy { "$networkAddress/$bitCount" }
        override fun toString(): String = string
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other == null || this::class != other::class) return false

            other as Subnet

            if (bitCount != other.bitCount) return false
            if (networkAddress != other.networkAddress) return false

            return true
        }

        override fun hashCode(): Int {
            var result = bitCount
            result = 31 * result + networkAddress.hashCode()
            return result
        }
    }

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

    override fun hashCode(): Int {
        return bytes.contentHashCode()
    }


    companion object {
        const val SIZE_BITS = 32
        const val SIZE_BYTES = 4
        val RANGE = parse("0.0.0.0")..parse("255.255.255.255")
        val RFC1918_24block = IPv4Address.parse("10.0.0.0")..IPv4Address.parse("10.255.255.255")
        val RFC1918_20block = IPv4Address.parse("172.16.0.0")..IPv4Address.parse("172.31.255.255")
        val RFC1918_16block = IPv4Address.parse("192.168.0.0")..IPv4Address.parse("192.168.255.255")

        fun parse(ipAddress: String): IPv4Address {
            val byteStrings = ipAddress.split(".")
            require(byteStrings.size == SIZE_BYTES) { "IP address must consist of $SIZE_BYTES bytes but ${byteStrings.size} were provided." }
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
) : IpAddress, Comparable<IpAddress> {
    private constructor(bytes: ByteArray) : this(
        bytes[0], bytes[1], bytes[2], bytes[3],
        bytes[4], bytes[5], bytes[6], bytes[7],
        bytes[8], bytes[9], bytes[10], bytes[11],
        bytes[12], bytes[13], bytes[14], bytes[15],
    )

    constructor(value: BigInteger) : this(value
        .also {
            require(!it.isNegative && it.numberOfWords <= 4) {
                "An IPv6 address cannot hold more than $SIZE_BITS bits."
            }
        }
        .toByteArray()
        .padStart(SIZE_BYTES))

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

    operator fun rangeTo(endInclusive: IPv6Address): Range = Range(endInclusive)

    open inner class Range(override val endInclusive: IPv6Address) : ClosedRange<IPv6Address> {
        init {
            require(this@IPv6Address <= endInclusive) { "${this@IPv6Address} must be less or equal to $endInclusive" }
        }

        override val start: IPv6Address = this@IPv6Address
        val subnet by lazy { Subnet(endInclusive) }
        val usable by lazy { maxOf(start, subnet.firstHost)..minOf(endInclusive, subnet.lastHost) }
        val firstUsableHost by lazy { usable.start }
        val lastUsableHost by lazy { usable.endInclusive }

        private val string by lazy { "${this@IPv6Address}..$endInclusive" }
        override fun toString(): String = string
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other == null || this::class != other::class) return false

            other as Range

            if (endInclusive != other.endInclusive) return false
            if (start != other.start) return false

            return true
        }

        override fun hashCode(): Int {
            var result = endInclusive.hashCode()
            result = 31 * result + start.hashCode()
            return result
        }
    }

    inner class Subnet(endInclusive: IPv6Address) {
        init {
            require(this@IPv6Address <= endInclusive) { "${this@IPv6Address} must be less or equal to $endInclusive" }
        }

        val bitCount: Int by lazy { SIZE_BITS - (endInclusive.value xor value).toString(2).length }
        val wildcardBitCount: Int by lazy { SIZE_BITS - bitCount }
        val hostCount: BigInteger by lazy { BigInteger.TWO shl (wildcardBitCount.dec()) }
        val usableHostCount: BigInteger by lazy { hostCount - 2 }
        private val _mask: BigInteger = "0".repeat((hostCount - 1).toString(2).length).padStart(SIZE_BITS, '1').toBigInteger(2)
        val networkAddress by lazy { IPv6Address(value and _mask) }
        val broadcastAddress by lazy { IPv6Address(networkAddress.value.or(hostCount).dec()) }
        val firstHost by lazy { IPv6Address(networkAddress.value.inc()) }
        val lastHost by lazy { IPv6Address(broadcastAddress.value.dec()) }
        val mask by lazy { IPv6Address(_mask).toString() }
        private val string by lazy { "$networkAddress/$bitCount" }
        override fun toString(): String = string
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other == null || this::class != other::class) return false

            other as Subnet

            if (bitCount != other.bitCount) return false
            if (networkAddress != other.networkAddress) return false

            return true
        }

        override fun hashCode(): Int {
            var result = bitCount
            result = 31 * result + networkAddress.hashCode()
            return result
        }
    }


    private val paddedBytes by lazy {
        bytes.map { it.toPositiveInt().toString(16).padStart(2, '0') }
            .windowed(2, 2) { it.joinToString("") }
    }

    /**
     * This representation consists of eight hextets each consisting of four
     * hexadecimal digits—leading zeros included.
     *
     * Example: `0000:0000:0000:0000:0000:ffff:c0a8:1001`
     *
     * @see completeRepresentation
     * @see abbreviatedRepresentation
     * @see <a href="https://tools.ietf.org/html/rfc5952">A Recommendation for IPv6 Address Text Representation</a>
     */
    val paddedRepresentation by lazy { paddedBytes.joinToString(":") }

    /**
     * This representation consists of eight hextets each consisting of
     * one to four hexadecimal digits—leading zeros removed.
     *
     * Example: `0:0:0:0:0:ffff:c0a8:1001`
     *
     * @see paddedRepresentation
     * @see abbreviatedRepresentation
     * @see <a href="https://tools.ietf.org/html/rfc5952">A Recommendation for IPv6 Address Text Representation</a>
     */
    val completeRepresentation by lazy {
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
     * @see paddedRepresentation
     * @see completeRepresentation
     * @see <a href="https://tools.ietf.org/html/rfc5952">A Recommendation for IPv6 Address Text Representation</a>
     */
    val abbreviatedRepresentation by lazy {
        completeRepresentation.abbreviate()
    }

    /**
     * Returns the abbreviated representation of this IPv6 address, e.g. `::ffff:c0a8:1001`.
     *
     * @see paddedRepresentation
     * @see completeRepresentation
     * @see abbreviatedRepresentation
     */
    override fun toString(): String = abbreviatedRepresentation

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


    companion object {
        const val SIZE_BITS = 128
        const val SIZE_BYTES = 16
        const val SIZE_HEXTETS = 8
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
            BigInteger.fromTwosComplementByteArray(IPv4_TO_IPv6_MAPPING_PREFIX) shl IPv4Address.SIZE_BITS
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
            BigInteger.fromTwosComplementByteArray(IPv4_TO_IPv6_NAT64_MAPPING_PREFIX) shl IPv4Address.SIZE_BITS
        }
        val IPv4_NAT64_MAPPED_RANGE by lazy {
            IPv4Address.RANGE
                .map { value + IPv4_TO_IPv6_NAT64_MAPPING_OFFSET }
                .let { IPv6Address(it.start)..IPv6Address(it.endInclusive) }
        }

        val ABBREVIATOR by lazy { Regex("0{1,4}(?::0{1,4})+") }
        val ABBREVIATION by lazy { Regex.fromLiteral("::") }

        private fun String.abbreviate() =
            if (abbreviated) this
            else ABBREVIATOR
                .findAll(this)
                .maxByOrNull { it.value.length }
                ?.let { if (it.range.size == length) "::" else replaceRange(it.range, ":") } ?: this

        private val String.abbreviated get() = ABBREVIATION.countMatches(this) == 1

        fun parse(ipAddress: String): IPv6Address {
            val mixedStrings = ipAddress.split(":")
            val hextetStrings = mixedStrings.takeUnless { it.last().contains(".") }
                ?: mixedStrings.take(mixedStrings.size - 1) + IPv4Address.parse(mixedStrings.last())
                    .value.toString(16).padStart(8, '0').chunkedSequence(4).toList()
            val abbreviations = ABBREVIATION.countMatches(ipAddress)
            require(abbreviations < 2) { "IP address must not use more than 1 abbreviation (::) but ${abbreviations} were found." }
            require(abbreviations == 1 || hextetStrings.size == SIZE_HEXTETS) { "IP address must consist of $SIZE_BYTES hextets but ${hextetStrings.size} were provided." }
            val bytes = hextetStrings.let {
                val endings = it.takeWhile { it.isNotBlank() } to it.takeLastWhile { it.isNotBlank() }
                val missing = (SIZE_HEXTETS - (endings.first.size + endings.second.size)).coerceAtLeast(0)
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

fun String.toIp() = when {
    contains(":") -> IPv6Address.parse(this)
    contains(".") -> IPv4Address.parse(this)
    else -> throw NumberFormatException("$this is not valid IP address.")
}

fun ipOf(value: String) = value.toIp()

fun IPv4Address.toIPv6Address() = IPv6Address(value + IPv6Address.IPv4_TO_IPv6_MAPPING_OFFSET)
fun IPv6Address.toIPv4Address(): IPv4Address {
    require(this in IPv6Address.IPv4_MAPPED_RANGE) { "$this cannot be mapped to IPv4 as it's not in the range ${IPv6Address.IPv4_MAPPED_RANGE}" }
    return IPv4Address((value - IPv6Address.IPv4_TO_IPv6_MAPPING_OFFSET).intValue(false))
}
