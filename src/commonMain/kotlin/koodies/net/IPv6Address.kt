package koodies.net

import com.ionspin.kotlin.bignum.integer.BigInteger
import com.ionspin.kotlin.bignum.integer.toBigInteger
import koodies.net.IPv6Notation.format
import koodies.number.bigIntegerOf
import koodies.number.bigIntegerOfHexadecimalString
import koodies.number.padStart
import koodies.number.toHexadecimalString
import koodies.number.toUBytes
import koodies.number.trim
import koodies.regex.countMatches
import koodies.unit.bits

/**
 * [Internet Protocol version 6](https://en.wikipedia.org/wiki/IPv6) address
 */
class IPv6Address private constructor(override val value: BigInteger, override val bytes: UByteArray) : IPAddress {
    constructor(value: BigInteger) : this(value, value.toUByteArray().trim().padStart(IPv6Address.byteCount))
    constructor(value: UInt) : this(value.toBigInteger(), value.toUBytes().trim().padStart(IPv6Address.byteCount))
    constructor(bytes: UByteArray) : this(bigIntegerOf(bytes), bytes.trim().padStart(IPv6Address.byteCount))

    init {
        require(value in MIN_VALUE..MAX_VALUE) {
            "$value must be between $MIN_VALUE and $MAX_VALUE."
        }
    }

    override val version: IPAddress.Version = IPv6Address
    operator fun rangeTo(endInclusive: IPv6Address): IPv6Range = IPv6Range(value, endInclusive.value)

    /**
     * Returns the [Notation.compressedRepresentation] of this IPv6 address, e.g. `::ffff:c0a8:1001`.
     *
     * @see Notation.fullRepresentation
     * @see Notation.conventionalRepresentation
     */
    override fun toString(): String = format(value)

    override fun equals(other: Any?): Boolean = isEqual(other)
    override fun hashCode(): Int = hash()

    companion object : IPAddress.Version by VersionImpl(6, 128.bits) {
        private const val sizeHextets = 8

        val MIN_VALUE: BigInteger = BigInteger.ZERO
        val MAX_VALUE: BigInteger = bigIntegerOfHexadecimalString("FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFF")

        val DEFAULT_ROOT = IPv6Address(MIN_VALUE)
        val LOOPBACK = IPv6Address(MIN_VALUE + 1)
        val RANGE = DEFAULT_ROOT..IPv6Address(MAX_VALUE)

        fun parse(ipAddress: String): IPv6Address {
            val mixedStrings = ipAddress.split(":")
            val hextetStrings = mixedStrings.takeUnless { it.last().contains(".") }
                ?: mixedStrings.take(mixedStrings.size - 1) + IPv4Address.parse(mixedStrings.last())
                    .bytes.toHexadecimalString().padStart(8, '0').chunkedSequence(4).toList()
            val compressions = Regex.fromLiteral("::").countMatches(ipAddress)

            require(compressions < 2) { "$ipAddress must not use more than 1 compression (::) but $compressions were found." }
            require(compressions == 1 || hextetStrings.size == sizeHextets) { "$ipAddress must consist of $sizeHextets hextets but ${hextetStrings.size} were provided." }
            val eightHextetStrings = hextetStrings.takeUnless { it.any { it.isBlank() } } ?: hextetStrings.let {
                val (start, end) = it.takeWhile { it.isNotBlank() } to it.takeLastWhile { it.isNotBlank() }
                val missing = (sizeHextets - (start.size + end.size)).coerceAtLeast(0)
                start + MutableList(missing) { "0" } + end
            }

            require(eightHextetStrings.size == 8) { "$sizeHextets hextets expected but only ${eightHextetStrings.size} found." }
            require(eightHextetStrings.none { it.length > 4 }) {
                val invalid = eightHextetStrings.filter { it.length > 4 }.joinToString(", ")
                "Each hextet must consist of at most 4 hexadecimal digits (0-F). Found $invalid"
            }
            val bytes = eightHextetStrings.flatMap { hextetString ->
                hextetString.toUInt(16)
                    .also { require(it in 0u..65535u) { "$it must be between 0 and FFFF." } }
                    .toUBytes(trim = false).takeLast(2)
            }
            return IPv6Address(bytes.toUByteArray())
        }
    }
}

fun String.toIPv6(): IPv6Address = IPv6Address.parse(this)
fun ip6Of(value: String): IPv6Address = value.toIPv6()

fun BigInteger.toIPv6(): IPv6Address = IPv6Address(this)
fun ip6Of(value: BigInteger): IPv6Address = value.toIPv6()
