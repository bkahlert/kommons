package com.bkahlert.kommons.net

import com.bkahlert.kommons.math.BigInteger
import com.bkahlert.kommons.math.minus
import com.bkahlert.kommons.math.plus
import com.bkahlert.kommons.math.toUByteArray
import com.bkahlert.kommons.math.FFu
import com.bkahlert.kommons.math.OO
import com.bkahlert.kommons.math.OOu
import com.bkahlert.kommons.math.bigIntegerOf
import com.bkahlert.kommons.math.shl
import com.bkahlert.kommons.math.trim
import com.bkahlert.kommons.ranges.map

/**
 * Implementations of this interface provide means to map
 * instances of [IPv4Address] to instances of [IPv6Address] and
 * vice versa.
 */
public interface IPv4toIPv6Mapping {
    public fun IPv4Address.toIPv6Address(): IPv6Address
    public fun IPv6Address.toIPv4Address(): IPv4Address
}

/**
 * Default implementation that maps instances of [IPv4Address] to instances of [IPv6Address]
 * by adding respectively instances of [IPv6Address] to [IPv4Address] by subtracting
 * the specified fixed [offset] to the [IPAddress.value] of the address to be mapped.
 */
public open class OffsetIPv4toIPv6Mapping(public val offset: BigInteger) : IPv4toIPv6Mapping {
    public val range: IPv6Range = IPv4Address.RANGE
        .map { value + offset }
        .let { IPv6Address(it.start.toUByteArray().trim())..IPv6Address(it.endInclusive.toUByteArray().trim()) }

    override fun IPv4Address.toIPv6Address(): IPv6Address =
        IPv6Address(value + offset)

    override fun IPv6Address.toIPv4Address(): IPv4Address {
        require(this in range) { "$this cannot be mapped to IPv4 as it's not in the range $range" }
        return IPv4Address(value - offset)
    }
}

/**
 * [Stateless IP translation](https://en.wikipedia.org/wiki/IPv6_transition_mechanism#Stateless_IP/ICMP_Translation) of [IPv4Address] to [IPv6Address] and vice versa.
 */
public object DefaultIPv4toIPv6Mapping : OffsetIPv4toIPv6Mapping(bigIntegerOf(ubyteArrayOf(
    UByte.OOu, UByte.OOu, UByte.OOu, UByte.OOu,
    UByte.OOu, UByte.OOu, UByte.OOu, UByte.OOu,
    UByte.OOu, UByte.OOu, UByte.FFu, UByte.FFu,
)) shl IPv4Address.bitCount)

/**
 * [NAT64](https://en.wikipedia.org/wiki/NAT64) based mapping of [IPv4Address] to [IPv6Address] and vice versa.
 */
public object Nat64IPv4toIPv6Mapping : OffsetIPv4toIPv6Mapping(bigIntegerOf(ubyteArrayOf(
    UByte.OOu, 0x64u, UByte.FFu, 0x9Bu,
    UByte.OOu, UByte.OOu, UByte.OO, UByte.OOu,
    UByte.OOu, UByte.OOu, UByte.OO, UByte.OOu,
)) shl IPv4Address.bitCount)

public fun IPv4Address.toIPv6Address(mapping: IPv4toIPv6Mapping = DefaultIPv4toIPv6Mapping): IPv6Address = with(mapping) { toIPv6Address() }
public fun IPv6Address.toIPv4Address(mapping: IPv4toIPv6Mapping = DefaultIPv4toIPv6Mapping): IPv4Address = with(mapping) { toIPv4Address() }