@file:Suppress("RemoveRedundantQualifierName")

package koodies.net

import com.ionspin.kotlin.bignum.integer.BigInteger
import com.ionspin.kotlin.bignum.integer.toBigInteger
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
    override val bitCount: Int = addressLength.bits.intValue()
    override val byteCount: Int = addressLength.bytes.intValue()
}

fun String.toIP(): IPAddress = when {
    contains(":") -> IPv6Address.parse(this)
    contains(".") -> IPv4Address.parse(this)
    else -> throw NumberFormatException("$this is no valid IP address.")
}

fun ipOf(value: String): IPAddress = value.toIP()

inline fun <reified IP : IPAddress> BigInteger.toIP(): IP = when (IP::class) {
    IPv6Address::class -> IPv6Address(this)
    IPv4Address::class -> IPv4Address(this)
    else -> throw NumberFormatException("$this is no valid IP address.")
}.let { it as? IP } ?: error("IP $this is no ${IP::class.simpleName}")

inline fun <reified IP : IPAddress> UInt.toIP(): IP = toBigInteger().toIP()
inline fun <reified IP : IPAddress> Int.toIP(): IP = toBigInteger().toIP()

inline fun <reified IP : IPAddress> ipOf(value: BigInteger): IP = value.toIP()
inline fun <reified IP : IPAddress> ipOf(value: UInt): IP = value.toBigInteger().toIP()
inline fun <reified IP : IPAddress> ipOf(value: Int): IP = value.toBigInteger().toIP()
