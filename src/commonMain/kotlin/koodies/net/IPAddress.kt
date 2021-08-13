@file:Suppress("RemoveRedundantQualifierName")

package koodies.net

import koodies.math.BigInteger
import koodies.math.toBigInteger
import koodies.unit.Size

/**
 * [Internet Protocol](https://en.wikipedia.org/wiki/Internet_Protocol) address
 */
public interface IPAddress : Comparable<IPAddress> {
    public val version: Version
    public val value: BigInteger
    public val bytes: UByteArray

    override fun compareTo(other: IPAddress): Int = value.compareTo(other.value)
    public fun isEqual(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as IPAddress

        if (value != other.value) return false
        if (version != other.version) return false

        return true
    }

    public fun hash(): Int = 31 * value.hashCode() + version.hashCode()

    public interface Version {
        public val major: Int
        public val addressLength: Size
        public val bitCount: Int
        public val byteCount: Int
    }
}

internal data class VersionImpl(
    override val major: Int,
    override val addressLength: Size,
) : IPAddress.Version {
    override val bitCount: Int = addressLength.bits.toInt()
    override val byteCount: Int = addressLength.bytes.toInt()
}

public fun String.toIP(): IPAddress = when {
    contains(":") -> IPv6Address.parse(this)
    contains(".") -> IPv4Address.parse(this)
    else -> throw NumberFormatException("$this is no valid IP address.")
}

public fun ipOf(value: String): IPAddress = value.toIP()

public inline fun <reified IP : IPAddress> BigInteger.toIP(): IP = when (IP::class) {
    IPv6Address::class -> IPv6Address(this)
    IPv4Address::class -> IPv4Address(this)
    else -> throw NumberFormatException("$this is no valid IP address.")
}.let { it as? IP } ?: error("IP $this is no ${IP::class.simpleName}")

public inline fun <reified IP : IPAddress> UInt.toIP(): IP = toBigInteger().toIP()
public inline fun <reified IP : IPAddress> Int.toIP(): IP = toBigInteger().toIP()

public inline fun <reified IP : IPAddress> ipOf(value: BigInteger): IP = value.toIP()
public inline fun <reified IP : IPAddress> ipOf(value: UInt): IP = value.toBigInteger().toIP()
public inline fun <reified IP : IPAddress> ipOf(value: Int): IP = value.toBigInteger().toIP()
