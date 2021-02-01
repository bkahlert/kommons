package koodies.net

import com.ionspin.kotlin.bignum.integer.BigInteger
import koodies.number.bigIntegerOfBinaryString

interface IPSubnet<IP : IPAddress> : ClosedRange<IP> {

    val value: BigInteger
    val prefixLength: Int
    val maxLength: Int
    fun address(value: BigInteger): IP

    val suffixLength: Int get() = maxLength - prefixLength

    val mask: BigInteger get() = bigIntegerOfBinaryString("1".repeat(prefixLength).padEnd(maxLength, '0'))
    val networkAddress: IP get() = address(value and mask)
    val hostCount: BigInteger get() = (BigInteger.TWO shl suffixLength.dec()).coerceAtLeast(BigInteger.ONE)
    val usableHostCount: BigInteger get() = hostCount.dec().dec().coerceAtLeast(BigInteger.ONE)
    val broadcastAddress: IP get() = address(networkAddress.value + hostCount.dec())

    val valid: Boolean get() = prefixLength in 0..maxLength
    fun requireValid() = require(valid) { "$prefixLength must be between 0 and $maxLength." }

    override val start: IP get() = networkAddress
    override val endInclusive: IP get() = broadcastAddress

    val firstUsableHost: IP get() = address(networkAddress.value.inc().coerceAtMost(broadcastAddress.value))
    val lastUsableHost: IP get() = address(broadcastAddress.value.dec().coerceAtLeast(networkAddress.value))
    val usable: ClosedRange<IP> get() = firstUsableHost..lastUsableHost

    val representation: String get() = "$networkAddress/$prefixLength"

    fun isEqual(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as IPSubnet<*>

        if (mask != other.mask) return false
        if (networkAddress != other.networkAddress) return false

        return true
    }

    val hash: Int get() = 31 * mask.hashCode() + networkAddress.hashCode()

    interface Factory<IP : IPAddress> {
        fun address(value: BigInteger): IP
        fun from(value: BigInteger, prefixLength: Int): IPSubnet<IP>
        fun from(ip: IP, prefixLength: Int): IPSubnet<IP> = from(ip.value, prefixLength)
        operator fun IP.div(prefixLength: Int): IPSubnet<IP> = from(value, prefixLength)
        val ClosedRange<IP>.smallestCommonSubnet: IPSubnet<IP>
            get() {
                val smallestCommonPrefixLength = start.version.bitCount - (endInclusive.value xor start.value).toString(2).length
                return from(start, smallestCommonPrefixLength)
            }
    }
}
