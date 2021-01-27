package koodies.net

val <IP : IpAddress> ClosedRange<IP>.subnet get() = Subnet(start, endInclusive)
val <IP : IpAddress> ClosedRange<IP>.usable get() = maxOf(start, subnet.firstHost)..minOf(endInclusive, subnet.lastHost)
val <IP : IpAddress> ClosedRange<IP>.firstUsableHost get() = usable.start
val <IP : IpAddress> ClosedRange<IP>.lastUsableHost get() = usable.endInclusive

open class IpAddressRange<IP : IpAddress>(
    final override val start: IP,
    final override val endInclusive: IP,
) : ClosedRange<IP> {
    init {
        require(start <= endInclusive) { "$start must be less or equal to $endInclusive" }
    }

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

    companion object {
        inline fun <reified IP : IpAddress> parse(start: String, endInclusive: String): IpAddressRange<IP> {
            val startIp: IP = start.toIp()
            val endInclusiveIp: IP = endInclusive.toIp()
            return IpAddressRange(startIp, endInclusiveIp)
        }

        inline fun <reified IP : IpAddress> parse(range: String): IpAddressRange<IP> =
            range.split("..").run { parse(first(), last()) }
    }
}
