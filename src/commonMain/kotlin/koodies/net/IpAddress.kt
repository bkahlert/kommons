package koodies.net

import koodies.number.toBytes
import koodies.number.toPositiveInt
import koodies.number.toUInt

interface IpAddress

class Ip4Address(byte0: Byte, byte1: Byte, byte2: Byte, byte3: Byte) : IpAddress, Comparable<Ip4Address> {
    private constructor(bytes: ByteArray) : this(bytes[0], bytes[1], bytes[2], bytes[3])
    constructor(value: Int) : this(value.toBytes(trim = false))

    val bytes: ByteArray = byteArrayOf(byte0, byte1, byte2, byte3)
    private val uintValue by lazy { bytes.toUInt() }

    operator fun rangeTo(endInclusive: Ip4Address): Range = Range(endInclusive)

    open inner class Range(override val endInclusive: Ip4Address) : ClosedRange<Ip4Address> {
        init {
            require(this@Ip4Address <= endInclusive) { "${this@Ip4Address} must be less or equal to $endInclusive" }
        }

        override val start: Ip4Address = this@Ip4Address
        val subnet by lazy { Subnet(endInclusive) }
        val usable by lazy { maxOf(start, subnet.firstHost)..minOf(endInclusive, subnet.lastHost) }
        val firstUsableHost by lazy { usable.start }
        val lastUsableHost by lazy { usable.endInclusive }

        private val string by lazy { "${this@Ip4Address}..$endInclusive" }
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

    inner class Subnet(endInclusive: Ip4Address) {
        init {
            require(this@Ip4Address <= endInclusive) { "${this@Ip4Address} must be less or equal to $endInclusive" }
        }

        val bitCount: Int by lazy { endInclusive.uintValue.xor(uintValue).countLeadingZeroBits() }
        val wildcardBitCount: Int by lazy { 32 - bitCount }
        val hostCount: UInt by lazy { 2u shl (wildcardBitCount.dec()) }
        val usableHostCount: UInt by lazy { hostCount - 2u }
        private val _mask = (hostCount - 1u).inv()
        val networkAddress by lazy { Ip4Address(uintValue.and(_mask).toInt()) }
        val broadcastAddress by lazy { Ip4Address(networkAddress.uintValue.or(hostCount).dec().toInt()) }
        val firstHost by lazy { Ip4Address(networkAddress.uintValue.inc().toInt()) }
        val lastHost by lazy { Ip4Address(broadcastAddress.uintValue.dec().toInt()) }

        val mask by lazy { Ip4Address(_mask.toInt()).toString() }
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

    override fun compareTo(other: Ip4Address): Int = uintValue.compareTo(other.uintValue)
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as Ip4Address

        if (!bytes.contentEquals(other.bytes)) return false

        return true
    }

    override fun hashCode(): Int {
        return bytes.contentHashCode()
    }


    companion object {
        val RFC1918_24block = ipOf("10.0.0.0")..ipOf("10.255.255.255")
        val RFC1918_20block = ipOf("172.16.0.0")..ipOf("172.31.255.255")
        val RFC1918_16block = ipOf("192.168.0.0")..ipOf("192.168.255.255")

        fun parse(ipAddress: String): Ip4Address {
            val byteStrings = ipAddress.split(".")
            require(byteStrings.size == 4) { "IP address must consist of 4 bytes but ${byteStrings.size} were provided." }
            val bytes = byteStrings.map { byteString ->
                byteString.toInt()
                    .also { require(it in 0..255) { "$it must be between 0 and 255." } }
                    .toByte()
            }
            return Ip4Address(bytes[0], bytes[1], bytes[2], bytes[3])
        }
    }
}

fun ipOf(value: String) = Ip4Address.parse(value)
fun String.toIp() = Ip4Address.parse(this)
