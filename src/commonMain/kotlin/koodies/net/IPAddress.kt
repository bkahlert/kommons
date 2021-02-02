@file:Suppress("RemoveRedundantQualifierName")

package koodies.net

import com.ionspin.kotlin.bignum.integer.BigInteger
import koodies.number.toInt
import koodies.unit.Size

/**
 * [Internet Protocol](https://en.wikipedia.org/wiki/Internet_Protocol) address
 */
interface IPAddress : Comparable<IPAddress> {
    val version: Version
    val value: BigInteger
    val bytes: UByteArray

    override fun compareTo(other: IPAddress): Int = value.compareTo(other.value)
    fun isEqual(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as IPAddress

        if (value != other.value) return false
        if (version != other.version) return false

        return true
    }

    fun hash(): Int = 31 * value.hashCode() + version.hashCode()

    interface Version {
        val major: Int
        val addressLength: Size
        val bitCount: Int
        val byteCount: Int
    }
}

internal data class VersionImpl(
    override val major: Int,
    override val addressLength: Size,
) : IPAddress.Version {
    override val bitCount: Int = addressLength.bits.toInt()
    override val byteCount: Int = addressLength.bytes.toInt()
}

inline fun <reified IP : IPAddress> String.toIP(): IP {
    val ipAddress = toAnyIP()
    return (ipAddress as? IP) ?: error("IP $ipAddress is no ${IP::class.simpleName}")
}

inline fun <reified IP : IPAddress> ipOf(value: String): IP = value.toIP()
inline fun <reified IP : IPAddress> ipOf(value: BigInteger): IP =
    (IPv6Address(value) as? IP) ?: (IPv4Address(value) as? IP) ?: error("Failed to create IP address from $value")

fun String.toAnyIP(): IPAddress = when {
    contains(":") -> IPv6Address.parse(this)
    contains(".") -> IPv4Address.parse(this)
    else -> throw NumberFormatException("$this is no valid IP address.")
}
