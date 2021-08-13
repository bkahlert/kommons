package koodies.net

import koodies.math.BigInteger
import koodies.math.dec
import koodies.math.inc

public class IPv6Subnet private constructor(override val value: BigInteger, override val prefixLength: Int) : IPSubnet<IPv6Address> {
    override val maxLength: Int = IPv6Address.bitCount

    init {
        requireValid()
    }

    override val usable: IPv6Range get() = IPv6Range(network.inc().coerceAtMost(broadcast), broadcast.dec().coerceAtLeast(network))
    override fun address(value: BigInteger): IPv6Address = IPv6Address(value)
    override fun toString(): String = representation
    override fun equals(other: Any?): Boolean = isEqual(other)
    override fun hashCode(): Int = hash

    public companion object : IPSubnet.Factory<IPv6Address> {
        override fun address(value: BigInteger): IPv6Address = IPv6Address(value)
        override fun from(value: BigInteger, prefixLength: Int): IPv6Subnet = IPv6Subnet(value, prefixLength)
        override fun from(ip: IPv6Address, prefixLength: Int): IPv6Subnet = from(ip.value, prefixLength)
        override fun getSmallestCommonSubnet(range: ClosedRange<IPv6Address>): IPv6Subnet =
            super.getSmallestCommonSubnet(range).run { IPv6Subnet(start.value, prefixLength) }

        override fun parse(ipSubnet: String): IPv6Subnet =
            super.parse(ipSubnet).run { IPv6Subnet(start.value, prefixLength) }
    }
}

public operator fun IPv6Address.div(prefixLength: Int): IPv6Subnet = IPv6Subnet.from(this, prefixLength)
public val ClosedRange<IPv6Address>.smallestCommonSubnet: IPv6Subnet get() = IPv6Subnet.getSmallestCommonSubnet(this)

public fun String.toIPv6Subnet(): IPv6Subnet = IPv6Subnet.parse(this)
public fun ip6SubnetOf(value: String): IPv6Subnet = value.toIPv6Subnet()
