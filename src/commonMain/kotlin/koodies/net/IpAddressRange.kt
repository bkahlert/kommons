package koodies.net

import com.ionspin.kotlin.bignum.integer.BigInteger

open class IpAddressRange<T : IpAddress<T>>(
    final override val start: T,
    final override val endInclusive: T,
    factory: (BigInteger) -> T,
) : ClosedRange<T> {
    init {
        require(start <= endInclusive) { "$start must be less or equal to $endInclusive" }
    }

    val subnet by lazy { Subnet(start, endInclusive, factory) }
    val usable by lazy { maxOf(start, subnet.firstHost)..minOf(endInclusive, subnet.lastHost) }
    val firstUsableHost by lazy { usable.start }
    val lastUsableHost by lazy { usable.endInclusive }

    private val string by lazy { "${start}..$endInclusive" }
    override fun toString(): String = string

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as IpAddressRange<*>

        if (endInclusive != other.endInclusive) return false
        if (start != other.start) return false

        return true
    }

    override fun hashCode(): Int {
        var result = endInclusive.hashCode()
        result = 31 * result + start.hashCode()
        return result
    }
}
