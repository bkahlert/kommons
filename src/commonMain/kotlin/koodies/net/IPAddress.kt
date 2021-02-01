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

fun String.toAnyIp(): IPAddress = when {
    contains(":") -> IPv6Address.parse(this)
    contains(".") -> IPv4Address.parse(this)
    else -> throw NumberFormatException("$this is no valid IP address.")
}

inline fun <reified IP : IPAddress> String.toIp(): IP {
    val ipAddress = toAnyIp()
    return (ipAddress as? IP) ?: error("IP $ipAddress is no ${IP::class.simpleName}")
}

inline fun <reified IP : IPAddress> ipOf(value: String): IP = value.toIp()
