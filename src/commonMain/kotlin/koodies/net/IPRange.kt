package koodies.net

import koodies.math.BigInteger
import koodies.math.toString
import koodies.math.xor

public interface IPRange<IP : IPAddress> : ClosedRange<IP> {

    public val valid: Boolean get() = start <= endInclusive
    public fun requireValid(): Unit = require(valid) { "$start must be less or equal to $endInclusive." }

    override val start: IP
    override val endInclusive: IP
    public val smallestCommonPrefixLength: Int get() = smallestCommonPrefixLength(start.version.bitCount, start.value, endInclusive.value)
    public val smallestCommonSubnet: IPSubnet<IP>

    public val representation: String get() = "$start..$endInclusive"

    public fun isEqual(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as IPRange<*>

        if (start != other.start) return false
        if (endInclusive != other.endInclusive) return false

        return true
    }

    public val hash: Int get() = 31 * start.hashCode() + endInclusive.hashCode()

    public companion object {
        public fun smallestCommonPrefixLength(length: Int, value1: BigInteger, value2: BigInteger): Int =
            length.minus((value2 xor value1).toString(2).takeUnless { it == "0" }?.length ?: 0)
    }

    public interface Factory<IP : IPAddress> {
        public fun from(start: BigInteger, endInclusive: BigInteger): IPRange<IP>
        public fun from(start: IP, endInclusive: IP): IPRange<IP> = from(start.value, endInclusive.value)
        public fun parse(ipRange: String): IPRange<IP> = ipRange.split("..").map { it.trim() }.run {
            require(size == 2) { "Expected format <IP1>..<IP2>" }
            from(first().toIP().value, last().toIP().value)
        }
    }
}

public fun String.toIPRange(): IPRange<out IPAddress> =
    runCatching { toIPv4Range() }.recoverCatching { toIPv6Range() }.getOrThrow()

public fun ipRangeOf(value: String): IPRange<out IPAddress> = value.toIPRange()
