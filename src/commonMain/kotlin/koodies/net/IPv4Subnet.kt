package koodies.net

import com.ionspin.kotlin.bignum.integer.BigInteger

public class IPv4Subnet private constructor(override val value: BigInteger, override val prefixLength: Int) : IPSubnet<IPv4Address> {
    override val maxLength: Int = IPv4Address.bitCount

    init {
        requireValid()
    }

    override val usable: IPv4Range get() = IPv4Range(network.inc().coerceAtMost(broadcast), broadcast.dec().coerceAtLeast(network))
    override fun address(value: BigInteger): IPv4Address = IPv4Address(value)
    override fun toString(): String = representation
    override fun equals(other: Any?): Boolean = isEqual(other)
    override fun hashCode(): Int = hash

    public companion object : IPSubnet.Factory<IPv4Address> {
        override fun address(value: BigInteger): IPv4Address = IPv4Address(value)
        override fun from(value: BigInteger, prefixLength: Int): IPv4Subnet = IPv4Subnet(value, prefixLength)
        override fun from(ip: IPv4Address, prefixLength: Int): IPv4Subnet = from(ip.value, prefixLength)
        override fun getSmallestCommonSubnet(range: ClosedRange<IPv4Address>): IPv4Subnet =
            super.getSmallestCommonSubnet(range).run { IPv4Subnet(start.value, prefixLength) }

        override fun parse(ipSubnet: String): IPv4Subnet =
            super.parse(ipSubnet).run { IPv4Subnet(start.value, prefixLength) }
    }
}

public operator fun IPv4Address.div(prefixLength: Int): IPv4Subnet = IPv4Subnet.from(this, prefixLength)
public val ClosedRange<IPv4Address>.smallestCommonSubnet: IPv4Subnet get() = IPv4Subnet.getSmallestCommonSubnet(this)

public fun String.toIPv4Subnet(): IPv4Subnet = IPv4Subnet.parse(this)
public fun ip4SubnetOf(value: String): IPv4Subnet = value.toIPv4Subnet()
