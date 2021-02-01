package koodies.net

import com.ionspin.kotlin.bignum.integer.BigInteger

class IPv4Subnet private constructor(override val value: BigInteger, override val prefixLength: Int) : IPSubnet<IPv4Address> {

    override val maxLength: Int = IPv4Address.bitCount
    override fun address(value: BigInteger): IPv4Address = IPv4Address(value)
    override fun toString(): String = representation
    override fun equals(other: Any?): Boolean = isEqual(other)
    override fun hashCode(): Int = hash

    companion object : IPSubnet.Factory<IPv4Address> {
        override fun address(value: BigInteger): IPv4Address = IPv4Address(value)
        override fun from(value: BigInteger, prefixLength: Int): IPSubnet<IPv4Address> = IPv4Subnet(value, prefixLength)
    }
}
