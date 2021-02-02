package koodies.net

import com.ionspin.kotlin.bignum.integer.BigInteger

class IPv4Range(start: BigInteger, endInclusive: BigInteger) : IPRange<IPv4Address> {
    override val start: IPv4Address = IPv4Address(start)
    override val endInclusive: IPv4Address = IPv4Address(endInclusive)

    init {
        requireValid()
    }

    override val smallestCommonSubnet: IPSubnet<IPv4Address> = IPv4Subnet.from(start, smallestCommonPrefixLength)
    override fun toString(): String = representation
    override fun equals(other: Any?): Boolean = isEqual(other)
    override fun hashCode(): Int = hash

    companion object : IPRange.Factory<IPv4Address> {
        override fun from(start: BigInteger, endInclusive: BigInteger): IPRange<IPv4Address> = IPv4Range(start, endInclusive)
    }
}

inline fun String.toIPv4Range(): IPRange<IPv4Address> = IPv4Range.parse(this)
inline fun ip4RangeOf(value: String): IPRange<IPv4Address> = value.toIPv4Range()
