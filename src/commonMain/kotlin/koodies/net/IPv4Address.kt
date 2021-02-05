package koodies.net

import com.ionspin.kotlin.bignum.integer.BigInteger
import com.ionspin.kotlin.bignum.integer.toBigInteger
import koodies.net.IPv4Notation.format
import koodies.number.bigIntegerOf
import koodies.number.padStart
import koodies.number.toUBytes
import koodies.number.trim
import koodies.unit.bits

/**
 * [Internet Protocol version 4](https://en.wikipedia.org/wiki/IPv4) address
 */
class IPv4Address private constructor(override val value: BigInteger, override val bytes: UByteArray) : IPAddress {
    constructor(value: BigInteger) : this(value, value.toUByteArray().trim().padStart(IPv4Address.byteCount))
    constructor(value: UInt) : this(value.toBigInteger(), value.toUBytes().trim().padStart(IPv4Address.byteCount))
    constructor(bytes: UByteArray) : this(bigIntegerOf(bytes), bytes.trim().padStart(IPv4Address.byteCount))

    init {
        require(value in MIN_VALUE..MAX_VALUE) {
            "$value must be between $MIN_VALUE and $MAX_VALUE."
        }
    }

    override val version: IPAddress.Version = IPv4Address
    operator fun rangeTo(endInclusive: IPv4Address): IPv4Range = IPv4Range(value, endInclusive.value)

    /**
     * Returns the [Notation.compressedRepresentation] of this IPv4 address, e.g. `192.168.0.1`.
     */
    override fun toString(): String = format(value)

    override fun equals(other: Any?): Boolean = isEqual(other)
    override fun hashCode(): Int = hash()

    companion object : IPAddress.Version by VersionImpl(4, 32.bits) {
        val MIN_VALUE: BigInteger = UInt.MIN_VALUE.toBigInteger()
        val MAX_VALUE: BigInteger = UInt.MAX_VALUE.toBigInteger()
        val RANGE = parse("0.0.0.0")..parse("255.255.255.255")
        val LOOPBACK = parse("127.0.0.1")
        val RFC1918_24block = parse("10.0.0.0")..parse("10.255.255.255")
        val RFC1918_20block = parse("172.16.0.0")..parse("172.31.255.255")
        val RFC1918_16block = parse("192.168.0.0")..parse("192.168.255.255")

        fun parse(ipAddress: String): IPv4Address {
            val byteStrings = ipAddress.split(".")
            require(byteStrings.size == byteCount) { "IP address must consist of $byteCount bytes but ${byteStrings.size} were provided." }
            val bytes = byteStrings.map { byteString ->
                byteString.toInt()
                    .also { require(it in 0..255) { "$it must be between 0 and 255." } }
                    .toUByte()
            }
            return IPv4Address(bytes.toUByteArray())
        }
    }
}

fun String.toIPv4(): IPv4Address = IPv4Address.parse(this)
fun ip4Of(value: String): IPv4Address = value.toIPv4()

fun BigInteger.toIPv4(): IPv4Address = IPv4Address(this)
fun ip4Of(value: BigInteger): IPv4Address = value.toIPv4()
