package com.bkahlert.kommons.net

import com.bkahlert.kommons.bigIntegerOf
import com.bkahlert.kommons.math.BigInteger
import com.bkahlert.kommons.math.toBigInteger
import com.bkahlert.kommons.math.toUByteArray
import com.bkahlert.kommons.net.IPv4Notation.format
import com.bkahlert.kommons.padStart
import com.bkahlert.kommons.toUByteArray
import com.bkahlert.kommons.trim
import com.bkahlert.kommons.unit.bits

/**
 * [Internet Protocol version 4](https://en.wikipedia.org/wiki/IPv4) address
 */
public class IPv4Address private constructor(override val value: BigInteger, override val bytes: UByteArray) : IPAddress {
    public constructor(value: BigInteger) : this(value, value.toUByteArray().trim().padStart(IPv4Address.byteCount))
    public constructor(value: UInt) : this(value.toBigInteger(), value.toUByteArray().trim().padStart(IPv4Address.byteCount))
    public constructor(bytes: UByteArray) : this(bigIntegerOf(bytes), bytes.trim().padStart(IPv4Address.byteCount))

    init {
        require(value in MIN_VALUE..MAX_VALUE) {
            "$value must be between $MIN_VALUE and $MAX_VALUE."
        }
    }

    override val version: IPAddress.Version = IPv4Address
    public operator fun rangeTo(endInclusive: IPv4Address): IPv4Range = IPv4Range(value, endInclusive.value)

    /**
     * Returns the [Notation.compressedRepresentation] of this IPv4 address, e.g. `192.168.0.1`.
     */
    override fun toString(): String = format(value)

    override fun equals(other: Any?): Boolean = isEqual(other)
    override fun hashCode(): Int = hash()

    public companion object : IPAddress.Version by VersionImpl(4, 32.bits) {
        public val MIN_VALUE: BigInteger = UInt.MIN_VALUE.toBigInteger()
        public val MAX_VALUE: BigInteger = UInt.MAX_VALUE.toBigInteger()
        public val RANGE: IPv4Range = parse("0.0.0.0")..parse("255.255.255.255")
        public val LOOPBACK: IPv4Address = parse("127.0.0.1")
        public val RFC1918_24block: IPv4Range = parse("10.0.0.0")..parse("10.255.255.255")
        public val RFC1918_20block: IPv4Range = parse("172.16.0.0")..parse("172.31.255.255")
        public val RFC1918_16block: IPv4Range = parse("192.168.0.0")..parse("192.168.255.255")

        public fun parse(ipAddress: String): IPv4Address {
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

public fun String.toIPv4(): IPv4Address = IPv4Address.parse(this)
public fun ip4Of(value: String): IPv4Address = value.toIPv4()

public fun BigInteger.toIPv4(): IPv4Address = IPv4Address(this)
public fun UInt.toIPv4(): IPv4Address = IPv4Address(this.toBigInteger())
public fun Int.toIPv4(): IPv4Address = IPv4Address(this.toBigInteger())
public fun ip4Of(value: BigInteger): IPv4Address = value.toIPv4()
public fun ip4Of(value: UInt): IPv4Address = value.toBigInteger().toIPv4()
public fun ip4Of(value: Int): IPv4Address = value.toBigInteger().toIPv4()
