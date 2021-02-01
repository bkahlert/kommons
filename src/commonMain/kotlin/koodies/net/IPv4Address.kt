package koodies.net

import com.ionspin.kotlin.bignum.integer.BigInteger
import com.ionspin.kotlin.bignum.integer.toBigInteger
import koodies.net.IPv4Notation.format
import koodies.number.padStart
import koodies.number.toUBytes
import koodies.number.toUInt
import koodies.number.trim
import koodies.unit.bits

/**
 * [Internet Protocol version 4](https://en.wikipedia.org/wiki/IPv4) address
 */
class IPv4Address private constructor(val intValue: UInt, override val bytes: UByteArray) : IPAddress {
    constructor(value: BigInteger) : this(value.uintValue(), value.toUByteArray().trim().padStart(IPv4Address.byteCount))
    constructor(value: UInt) : this(value, value.toUBytes().trim().padStart(IPv4Address.byteCount))
    constructor(bytes: UByteArray) : this(bytes.toUInt(), bytes.trim().padStart(IPv4Address.byteCount))

    init {
        require(intValue in IPv4Address.MIN_VALUE..IPv4Address.MAX_VALUE) {
            "$value must be between ${IPv6Address.MIN_VALUE} and ${IPv6Address.MAX_VALUE}."
        }
    }

    override val value: BigInteger get() = intValue.toBigInteger()
    override val version: IPAddress.Version = IPv4Address

    fun rangeTo(endInclusive: IPv4Address): ClosedRange<IPv4Address> = object : ClosedRange<IPv4Address> {
        override val start: IPv4Address = this@IPv4Address
        override val endInclusive: IPv4Address = endInclusive
    }

    /**
     * Returns the [Notation.compressedRepresentation] of this IPv4 address, e.g. `192.168.0.1`.
     */
    override fun toString(): String = format(value)

    override fun compareTo(other: IPAddress): Int = value.compareTo(other.value)
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as IPv4Address

        if (intValue != other.intValue) return false
        if (version != other.version) return false

        return true
    }

    override fun hashCode(): Int {
        var result = intValue.hashCode()
        result = 31 * result + version.hashCode()
        return result
    }

    companion object : IPAddress.Version by VersionImpl(4, 32.bits) {
        val MIN_VALUE: UInt = UInt.MIN_VALUE
        val MAX_VALUE: UInt = UInt.MAX_VALUE
        val RANGE = parse("0.0.0.0")..parse("255.255.255.255")
        val LOOPBACK = parse("127.0.0.1")
        val RFC1918_24block = parse("10.0.0.0")..parse("10.255.255.255")
        val RFC1918_20block = parse("172.16.0.0")..parse("172.31.255.255")
        val RFC1918_16block = parse("192.168.0.0")..parse("192.168.255.255")

        fun parse(ipAddress: String): IPv4Address {
            val byteStrings = ipAddress.split(".")
            require(byteStrings.size == byteCount.toInt()) { "IP address must consist of $byteCount bytes but ${byteStrings.size} were provided." }
            val bytes = byteStrings.map { byteString ->
                byteString.toInt()
                    .also { require(it in 0..255) { "$it must be between 0 and 255." } }
                    .toUByte()
            }
            return IPv4Address(bytes.toUByteArray())
        }
    }
}
