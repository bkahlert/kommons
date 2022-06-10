package com.bkahlert.kommons.net

import com.bkahlert.kommons.bigIntegerOfBinaryString
import com.bkahlert.kommons.math.BigInteger
import com.bkahlert.kommons.math.BigIntegerConstants
import com.bkahlert.kommons.math.and
import com.bkahlert.kommons.math.dec
import com.bkahlert.kommons.math.inc
import com.bkahlert.kommons.math.plus
import com.bkahlert.kommons.math.shl

public interface IPSubnet<IP : IPAddress> : ClosedRange<IP> {

    public val value: BigInteger
    public val prefixLength: Int
    public val maxLength: Int
    public fun address(value: BigInteger): IP

    public val suffixLength: Int get() = maxLength - prefixLength

    public val mask: BigInteger get() = bigIntegerOfBinaryString("1".repeat(prefixLength).padEnd(maxLength, '0'))
    public val network: BigInteger get() = value and mask
    public val networkAddress: IP get() = address(network)
    public val hostCount: BigInteger get() = (BigIntegerConstants.TWO shl suffixLength.dec()).coerceAtLeast(BigIntegerConstants.ONE)
    public val usableHostCount: BigInteger get() = hostCount.dec().dec().coerceAtLeast(BigIntegerConstants.ONE)
    public val broadcast: BigInteger get() = network + hostCount.dec()
    public val broadcastAddress: IP get() = address(broadcast)

    public val valid: Boolean get() = prefixLength in 0..maxLength
    public fun requireValid(): Unit = require(valid) { "$prefixLength must be between 0 and $maxLength." }

    override val start: IP get() = networkAddress
    override val endInclusive: IP get() = broadcastAddress

    public val firstUsableHost: IP get() = address(network.inc().coerceAtMost(broadcast))
    public val lastUsableHost: IP get() = address(broadcast.dec().coerceAtLeast(network))
    public val usable: ClosedRange<IP> get() = firstUsableHost..lastUsableHost

    public val representation: String get() = "$networkAddress/$prefixLength"

    public fun isEqual(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as IPSubnet<*>

        if (mask != other.mask) return false
        if (networkAddress != other.networkAddress) return false

        return true
    }

    public val hash: Int get() = 31 * mask.hashCode() + networkAddress.hashCode()

    public interface Factory<IP : IPAddress> {
        public fun address(value: BigInteger): IP
        public fun from(value: BigInteger, prefixLength: Int): IPSubnet<IP>
        public fun from(ip: IP, prefixLength: Int): IPSubnet<IP> = from(ip.value, prefixLength)
        public fun getSmallestCommonSubnet(range: ClosedRange<IP>): IPSubnet<IP> =
            from(range.start, IPRange.smallestCommonPrefixLength(range.start.version.bitCount, range.start.value, range.endInclusive.value))

        public fun parseCidrVariant(ipSubnet: String): IPSubnet<IP>? = ipSubnet.split("/").map { it.trim() }.run {
            if (size == 2) from(first().toIP().value, last().toInt())
            else null
        }

        public fun parseRangeVariant(ipRange: String): IPSubnet<IP>? = kotlin.runCatching {
            val range: IPRange<out IPAddress> = ipRange.toIPRange()
            from(range.start.value, range.smallestCommonPrefixLength)
        }.getOrNull()

        public fun parse(ipSubnet: String): IPSubnet<IP> = parseCidrVariant(ipSubnet)
            ?: parseRangeVariant(ipSubnet)
            ?: error("Subnet expected to be either in format <IP1>..<IP2> or <IP>/<CIDR>")
    }
}

public operator fun IPAddress.div(prefixLength: Int): IPSubnet<out IPAddress> = when (this) {
    is IPv6Address -> this / prefixLength
    is IPv4Address -> this / prefixLength
    else -> throw NumberFormatException("$this no recognized.")
}

public fun String.toIPSubnet(): IPSubnet<out IPAddress> = runCatching { toIPv4Subnet() }.recoverCatching { toIPv6Subnet() }.getOrThrow()
public fun ipSubnetOf(value: String): IPSubnet<out IPAddress> = value.toIPSubnet()
