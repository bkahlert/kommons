package koodies.net

import com.ionspin.kotlin.bignum.integer.BigInteger
import koodies.number.ZERO
import koodies.number.bigIntegerOf
import koodies.number.padStart
import koodies.number.trim
import koodies.ranges.size
import koodies.regex.countMatches
import koodies.unit.bytes
import kotlin.math.ceil

/**
 * Encapsulates the knowledge on how to format Internet Protocol addresses.
 *
 * All permutations of the offered dimensions are possible, that is, not only
 * - `192.168.16.1` (see [IPv4Notation.conventionalRepresentation]) or
 * - `::ffff:c0a8:1001` (see [IPv6Notation.compressedRepresentation]) but also
 * - `⌁⌁60⌁ag401` (IPv4 address `192.168.16.1` mapped to made up addresses
 * of 24 bytes in 8 groups of quadlets formatted to the base of 32 and
 * the longest consecutive groups of zeros replaced by `⌁⌁`, see [format]).
 */
interface Notation {

    /**
     * Level of detail of a Internet Protocol address representation.
     */
    enum class Verbosity {
        /**
         * Fully detailed representation
         */
        Full,

        /**
         * Typical representation where obviously redundant
         * information like leading zeros are left out.
         */
        Conventional,

        /**
         * Representation that applies the compression rule
         * of replacing the longest sequence of groups with value `0`
         * by two consecutive group separators.
         */
        Compressed
    }

    /**
     * Specifies how many bytes one address has (e.g. `16` bytes = `128` Bit for IPv6).
     */
    val byteCount: Int

    /**
     * Specifies how many bytes make up one group (IPv4: `1`, IPv6: `2`).
     */
    val groupSize: Int

    /**
     * Specifies how groups are separated.
     */
    val groupSeparator: Char

    /**
     * Specifies the base of the representation respectively how many states a single digit can have.
     */
    val base: Int

    /**
     * Specifies which [Verbosity] to use by default.
     */
    val defaultVerbosity: Verbosity

    /**
     * Formats the Internet Protocol address specified by its [value] using
     * this notations [byteCount], [groupSize], [groupSeparator], [base] and [defaultVerbosity].
     */
    fun format(value: BigInteger): String = format(value, defaultVerbosity)

    /**
     * Formats the Internet Protocol address specified by its [value] using
     * this notations [byteCount], [groupSize], [groupSeparator], [base] and
     * the specified [defaultVerbosity].
     */
    fun format(value: BigInteger, verbosity: Verbosity): String {
        require(byteCount > 0) { "Byte count must be positive." }
        require(value >= 0 && value <= BigInteger.TWO shl (byteCount * Byte.SIZE_BITS)) { "$value exceeds 2^${byteCount * Byte.SIZE_BITS}." }
        val conventional = value.toUByteArray().trim() // minimal bytes
            .padStart(ceil(byteCount.div(groupSize.toDouble())).times(groupSize).toInt(), UByte.ZERO) // all bytes
            .windowed(groupSize, groupSize) // groups
            .map { bigIntegerOf(it.toUByteArray()) }
            .map { it.toString(base) } // base

        return when (verbosity) {
            Verbosity.Full -> {
                val length = groupSize.bytes.maxLengthOfRepresentationToBaseOf(base)
                conventional.map { byte -> byte.padStart(length, '0') }.joinToString(groupSeparator.toString())
            }
            Verbosity.Compressed -> {
                val string = conventional.joinToString(groupSeparator.toString())
                if (Regex.fromLiteral("$groupSeparator$groupSeparator").countMatches(string) == 0) {
                    val pattern = Regex("(?:^|$groupSeparator)(0+(?:${groupSeparator}0+)+)")
                    pattern.findAll(string)
                        .maxByOrNull { it.value.length }?.run {
                            if (range.size == string.length) "$groupSeparator$groupSeparator"
                            else {
                                if (this.value.startsWith(groupSeparator)) {
                                    string.replaceRange((range.first + 1)..range.last, groupSeparator.toString())
                                } else {
                                    string.replaceRange(range, groupSeparator.toString())
                                }
                            }
                        } ?: string
                } else {
                    string
                }
            }
            Verbosity.Conventional -> {
                conventional.joinToString(groupSeparator.toString())
            }
        }
    }
}

object IPv4Notation : Notation {
    override val byteCount = IPv4Address.byteCount
    override val groupSize = 1
    override val groupSeparator = '.'
    override val base = 10
    override val defaultVerbosity = Notation.Verbosity.Conventional
}

/**
 * This representation consists of four octets each consisting of
 * one to three decimal digits—leading zeros removed.
 *
 * Example: `192.168.0.1`
 */
val IPv4Address.conventionalRepresentation get() = IPv4Notation.format(value)


object IPv6Notation : Notation {
    override val byteCount = IPv6Address.byteCount
    override val groupSize = 2
    override val groupSeparator = ':'
    override val base = 16
    override val defaultVerbosity = Notation.Verbosity.Compressed
}

/**
 * This representation consists of eight hextets each consisting of four
 * hexadecimal digits—leading zeros included.
 *
 * Example: `0000:0000:0000:0000:0000:ffff:c0a8:1001`
 *
 * @see conventionalRepresentation
 * @see compressedRepresentation
 * @see <a href="https://tools.ietf.org/html/rfc5952">A Recommendation for IPv6 Address Text Representation</a>
 */
val IPv6Address.fullRepresentation: String get() = IPv6Notation.format(value, Notation.Verbosity.Full)

/**
 * This representation consists of eight hextets each consisting of
 * one to four hexadecimal digits—leading zeros removed.
 *
 * Example: `0:0:0:0:0:ffff:c0a8:1001`
 *
 * @see fullRepresentation
 * @see compressedRepresentation
 * @see <a href="https://tools.ietf.org/html/rfc5952">A Recommendation for IPv6 Address Text Representation</a>
 */
val IPv6Address.conventionalRepresentation get() = IPv6Notation.format(value, Notation.Verbosity.Conventional)

/**
 * This representation consists of up to eight hextets each consisting of
 * one to four hexadecimal digits—leading zeros removed.
 *
 * This is the shortest representation as it removes the longest sequence
 * of `0` hextets—given such a sequences spans at least two hextets.
 *
 * Example: `::ffff:c0a8:1001`
 *
 * @see fullRepresentation
 * @see conventionalRepresentation
 * @see <a href="https://tools.ietf.org/html/rfc5952">A Recommendation for IPv6 Address Text Representation</a>
 */
val IPv6Address.compressedRepresentation get() = IPv6Notation.format(value, Notation.Verbosity.Compressed)
