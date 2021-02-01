package koodies.net

import com.ionspin.kotlin.bignum.integer.BigInteger

class IPv6Subnet private constructor(override val value: BigInteger, override val prefixLength: Int) : IPSubnet<IPv6Address> {

    override val maxLength: Int = IPv6Address.bitCount
    override fun address(value: BigInteger): IPv6Address = IPv6Address(value)
    override fun toString(): String = representation
    override fun equals(other: Any?): Boolean = isEqual(other)
    override fun hashCode(): Int = hash

    companion object : IPSubnet.Factory<IPv6Address> {
        override fun address(value: BigInteger): IPv6Address = IPv6Address(value)
        override fun from(value: BigInteger, prefixLength: Int): IPSubnet<IPv6Address> = IPv6Subnet(value, prefixLength)
    }
}
