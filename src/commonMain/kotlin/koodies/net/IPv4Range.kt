package koodies.net

import com.ionspin.kotlin.bignum.integer.BigInteger

class IPv4Range(start: BigInteger, endInclusive: BigInteger) : IPRange<IPv4Address> {
    override val start: IPv4Address = IPv4Address(start)
    override val endInclusive: IPv4Address = IPv4Address(endInclusive)

    init {
        requireValid()
    }

    override val smallestCommonSubnet: IPv4Subnet = IPv4Subnet.from(start, smallestCommonPrefixLength)
    override fun toString(): String = representation
    override fun equals(other: Any?): Boolean = isEqual(other)
    override fun hashCode(): Int = hash

    companion object : IPRange.Factory<IPv4Address> {
        override fun from(start: BigInteger, endInclusive: BigInteger): IPv4Range = IPv4Range(start, endInclusive)
        override fun from(start: IPv4Address, endInclusive: IPv4Address): IPv4Range = from(start.value, endInclusive.value)
        override fun parse(ipRange: String): IPv4Range = super.parse(ipRange).run { IPv4Range(start.value, endInclusive.value) }
    }
}

fun String.toIPv4Range(): IPv4Range = IPv4Range.parse(this)
fun ip4RangeOf(value: String): IPv4Range = value.toIPv4Range()
