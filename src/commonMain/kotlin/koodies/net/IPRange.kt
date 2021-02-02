package koodies.net

import com.ionspin.kotlin.bignum.integer.BigInteger

interface IPRange<IP : IPAddress> : ClosedRange<IP> {

    val valid: Boolean get() = start <= endInclusive
    fun requireValid() = require(valid) { "$start must be less or equal to $endInclusive." }

    override val start: IP
    override val endInclusive: IP
    val smallestCommonPrefixLength: Int get() = smallestCommonPrefixLength(start.version.bitCount, start.value, endInclusive.value)
    val smallestCommonSubnet: IPSubnet<IP>

    val representation: String get() = "$start..$endInclusive"

    fun isEqual(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as IPRange<*>

        if (start != other.start) return false
        if (endInclusive != other.endInclusive) return false

        return true
    }

    val hash: Int get() = 31 * start.hashCode() + endInclusive.hashCode()

    companion object {
        fun smallestCommonPrefixLength(length: Int, value1: BigInteger, value2: BigInteger): Int =
            length.minus((value2 xor value1).toString(2).takeUnless { it == "0" }?.length ?: 0)
    }

    interface Factory<IP : IPAddress> {
        fun from(start: BigInteger, endInclusive: BigInteger): IPRange<IP>
        fun from(start: IP, endInclusive: IP): IPRange<IP> = from(start.value, endInclusive.value)
        fun parse(ipRange: String): IPRange<IP> = ipRange.split("..").map { it.trim() }.run {
            require(size == 2) { "Expected format <IP1>..<IP2>" }
            from(first().toAnyIP().value, last().toAnyIP().value)
        }
    }
}

fun String.toIPRange(): IPRange<out IPAddress> =
    runCatching { toIPv4Range() }.recoverCatching { toIPv6Range() }.getOrThrow()

fun ipRangeOf(value: String): IPRange<out IPAddress> = value.toIPRange()
