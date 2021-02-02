package koodies.net

import com.ionspin.kotlin.bignum.integer.BigInteger

class IPv6Range(start: BigInteger, endInclusive: BigInteger) : IPRange<IPv6Address> {
    override val start: IPv6Address = IPv6Address(start)
    override val endInclusive: IPv6Address = IPv6Address(endInclusive)

    init {
        requireValid()
    }

    override val smallestCommonSubnet: IPSubnet<IPv6Address> = IPv6Subnet.from(start, smallestCommonPrefixLength)
    override fun toString(): String = representation
    override fun equals(other: Any?): Boolean = isEqual(other)
    override fun hashCode(): Int = hash

    companion object : IPRange.Factory<IPv6Address> {
        override fun from(start: BigInteger, endInclusive: BigInteger): IPRange<IPv6Address> = IPv6Range(start, endInclusive)
    }
}

inline fun String.toIPv6Range(): IPRange<IPv6Address> = IPv6Range.parse(this)
inline fun ip6RangeOf(value: String): IPRange<IPv6Address> = value.toIPv6Range()
