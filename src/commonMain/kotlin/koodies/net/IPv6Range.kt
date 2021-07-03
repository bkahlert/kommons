package koodies.net

import koodies.math.BigInteger

public class IPv6Range(start: BigInteger, endInclusive: BigInteger) : IPRange<IPv6Address> {
    override val start: IPv6Address = IPv6Address(start)
    override val endInclusive: IPv6Address = IPv6Address(endInclusive)

    init {
        requireValid()
    }

    override val smallestCommonSubnet: IPv6Subnet = IPv6Subnet.from(start, smallestCommonPrefixLength)
    override fun toString(): String = representation
    override fun equals(other: Any?): Boolean = isEqual(other)
    override fun hashCode(): Int = hash

    public companion object : IPRange.Factory<IPv6Address> {
        override fun from(start: BigInteger, endInclusive: BigInteger): IPv6Range = IPv6Range(start, endInclusive)
        override fun from(start: IPv6Address, endInclusive: IPv6Address): IPv6Range = from(start.value, endInclusive.value)
        override fun parse(ipRange: String): IPv6Range = super.parse(ipRange).run { IPv6Range(start.value, endInclusive.value) }
    }
}

public fun String.toIPv6Range(): IPv6Range = IPv6Range.parse(this)
public fun ip6RangeOf(value: String): IPv6Range = value.toIPv6Range()
