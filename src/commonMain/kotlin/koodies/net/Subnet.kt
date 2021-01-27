package koodies.net

import com.ionspin.kotlin.bignum.integer.BigInteger
import com.ionspin.kotlin.bignum.integer.toBigInteger
import koodies.number.toInt

class Subnet<IP : IpAddress>(override val start: IP, override val endInclusive: IP) : ClosedRange<IP> {
    init {
        require(start <= endInclusive) { "$start must be less or equal to $endInclusive" }
    }

    private fun factory(value: BigInteger) = when (start) {
        is IPv4Address -> IPv4Address(value.toInt())
        is IPv6Address -> IPv6Address(value)
        else -> error("")
    } as? IP ?: error("even worse")

    val bitCount: Int by lazy { start.version.bitCount - (endInclusive.value xor start.value).toString(2).length }
    val wildcardBitCount: Int by lazy { start.version.bitCount - bitCount }
    val hostCount: BigInteger by lazy { BigInteger.TWO shl (wildcardBitCount.dec()) }
    val usableHostCount: BigInteger by lazy { hostCount - 2 }
    private val _mask: BigInteger = "0".repeat((hostCount - 1).toString(2).length).padStart(start.version.bitCount, '1').toBigInteger(2)
    val networkAddress: IP by lazy { factory(start.value and _mask) }
    val broadcastAddress: IP by lazy { factory(networkAddress.value.or(hostCount).dec()) }
    val firstHost: IP by lazy { factory(networkAddress.value.inc()) }
    val lastHost: IP by lazy { factory(broadcastAddress.value.dec()) }
    val mask: String by lazy { factory(_mask).toString() }

    private val string by lazy { "$networkAddress/$bitCount" }
    override fun toString(): String = string

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as Subnet<*>

        if (bitCount != other.bitCount) return false
        if (networkAddress != other.networkAddress) return false

        return true
    }

    override fun hashCode(): Int {
        var result = bitCount
        result = 31 * result + networkAddress.hashCode()
        return result
    }
}
