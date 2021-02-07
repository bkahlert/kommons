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
    val network: BigInteger get() = value and mask
    val networkAddress: IP get() = address(network)
    val hostCount: BigInteger get() = (BigInteger.TWO shl suffixLength.dec()).coerceAtLeast(BigInteger.ONE)
    val usableHostCount: BigInteger get() = hostCount.dec().dec().coerceAtLeast(BigInteger.ONE)
    val broadcast: BigInteger get() = network + hostCount.dec()
    val broadcastAddress: IP get() = address(broadcast)

    val valid: Boolean get() = prefixLength in 0..maxLength
    fun requireValid() = require(valid) { "$prefixLength must be between 0 and $maxLength." }

    override val start: IP get() = networkAddress
    override val endInclusive: IP get() = broadcastAddress

    val firstUsableHost: IP get() = address(network.inc().coerceAtMost(broadcast))
    val lastUsableHost: IP get() = address(broadcast.dec().coerceAtLeast(network))
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
        fun getSmallestCommonSubnet(range: ClosedRange<IP>): IPSubnet<IP> =
            from(range.start, IPRange.smallestCommonPrefixLength(range.start.version.bitCount, range.start.value, range.endInclusive.value))

        fun parseCidrVariant(ipSubnet: String): IPSubnet<IP>? = ipSubnet.split("/").map { it.trim() }.run {
            if (size == 2) from(first().toIP().value, last().toInt())
            else null
        }

        fun parseRangeVariant(ipRange: String): IPSubnet<IP>? = kotlin.runCatching {
            val range: IPRange<out IPAddress> = ipRange.toIPRange()
            from(range.start.value, range.smallestCommonPrefixLength)
        }.getOrNull()

        fun parse(ipSubnet: String): IPSubnet<IP> = parseCidrVariant(ipSubnet)
            ?: parseRangeVariant(ipSubnet)
            ?: error("Subnet expected to be either in format <IP1>..<IP2> or <IP>/<CIDR>")
    }
}

operator fun IPAddress.div(prefixLength: Int): IPSubnet<out IPAddress> = when (this) {
    is IPv6Address -> this / prefixLength
    is IPv4Address -> this / prefixLength
    else -> throw NumberFormatException("$this no recognized.")
}

fun String.toIPSubnet(): IPSubnet<out IPAddress> = runCatching { toIPv4Subnet() }.recoverCatching { toIPv6Subnet() }.getOrThrow()
fun ipSubnetOf(value: String): IPSubnet<out IPAddress> = value.toIPSubnet()
