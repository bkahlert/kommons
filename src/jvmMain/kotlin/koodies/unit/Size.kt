package koodies.unit

import koodies.io.path.requireExists
import koodies.number.BigDecimalConstants
import koodies.number.formatToExactDecimals
import koodies.number.scientificFormat
import koodies.number.toBigDecimal
import koodies.text.quoted
import java.io.File
import java.math.BigDecimal
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.isDirectory
import kotlin.io.path.isSymbolicLink
import kotlin.reflect.KClass

/**
 * Amount of bytes representable in with the decimal [SI prefixes](https://en.wikipedia.org/wiki/Metric_prefix)
 * [Yotta], [Zetta], [Exa], [Peta], [Tera], [Giga], [Mega], [kilo] as well as with the binary prefixes as defined by
 * [ISO/IEC 80000](https://en.wikipedia.org/wiki/ISO/IEC_80000) [Yobi], [Zebi], [Exbi], [Pebi], [Tebi], [Gibi], [Mebi] and [Kibi].
 */
inline class Size(val bytes: BigDecimal) : Comparable<Size> {

    companion object {
        val ZERO: Size = Size(BigDecimal.ZERO)
        val supportedPrefixes: Map<KClass<out UnitPrefix>, List<UnitPrefix>> = mapOf(
            BinaryPrefix::class to listOf(
                BinaryPrefix.Yobi,
                BinaryPrefix.Zebi,
                BinaryPrefix.Exbi,
                BinaryPrefix.Pebi,
                BinaryPrefix.Tebi,
                BinaryPrefix.Gibi,
                BinaryPrefix.Mebi,
                BinaryPrefix.Kibi,
            ),
            DecimalPrefix::class to listOf(
                DecimalPrefix.Yotta,
                DecimalPrefix.Zetta,
                DecimalPrefix.Exa,
                DecimalPrefix.Peta,
                DecimalPrefix.Tera,
                DecimalPrefix.Giga,
                DecimalPrefix.Mega,
                DecimalPrefix.kilo,
            )
        )
        const val SYMBOL = "B"

        fun precision(value: BigDecimal, unit: UnitPrefix?): Int = when (unit) {
            null -> 0
            else -> when {
                value < BigDecimal.ONE -> 3
                value < BigDecimal.TEN -> 2
                value < BigDecimalConstants.HUNDRED -> 1
                else -> 0
            }
        }

        fun CharSequence.toSize(): Size {
            val trimmed = trim()
            val unitString = trimmed.takeLastWhile { it.isLetter() }
            val valueString = trimmed.dropLast(unitString.length).trim().toString()
            val value = valueString.toBigDecimal()
            return unitString.removeSuffix(SYMBOL).let { it ->
                when {
                    it.isBlank() -> value.bytes
                    it == "K" -> (value * BinaryPrefix.Kibi.factor).bytes
                    else -> supportedPrefixes.flatMap { prefix -> prefix.value }.find { unit -> unit.symbol == it }?.let { (value * it.factor).bytes }
                }
            } ?: throw IllegalArgumentException("${unitString.quoted} is no valid size unit like MB or GiB.")
        }

        val Number.bytes: Size get() = if (this == 0) ZERO else Size(toBigDecimal())

        val Path.size: Size
            get() {
                requireExists()
                return if (!isDirectory()) Files.size(toAbsolutePath()).bytes
                else (toFile().listFiles() ?: return ZERO) // TODO remove toFile
                    .asSequence()
                    .map(File::toPath)
                    .filterNot { it.isSymbolicLink() }
                    .fold(ZERO) { size, path -> size + path.size }
            }
    }

    object FileSizeComparator : (Path, Path) -> Int {
        override fun invoke(path1: Path, path2: Path): Int = path1.size.compareTo(path2.size)
    }

    /**
     * Returns a string representation of this size value expressed in the unit
     * with the [UnitPrefix] which yields the most compact and readable number value.
     *
     * Special cases:
     *  - zero size is formatted as `"0 B"`
     *  - fraction sizes are formatted as `"0 B"`
     *  - very big sizes (more than a million [Yotta]byte/[Yobi]byte) are expressed
     *  in [Yotta]byte/[Yobi]byte and formatted in scientific notation
     *
     * @return the value of size in the automatically determined [UnitPrefix], e.g. 42.2 MB.
     */
    override fun toString(): String = toString<DecimalPrefix>()

    /**
     * Returns a string representation of this size value expressed in the unit
     * with the [UnitPrefix] which yields the most compact and readable number value.
     *
     * Special cases:
     *  - zero size is formatted as `"0 B"`
     *  - fraction sizes are formatted as `"0 B"`
     *  - very big sizes (more than a million [Yotta]byte/[Yobi]byte) are expressed
     *  in [Yotta]byte/[Yobi]byte and formatted in scientific notation
     *
     * @return the value of size in the automatically determined [UnitPrefix], e.g. 42.2 MB.
     */
    inline fun <reified T : UnitPrefix> toString(prefixType: KClass<out UnitPrefix> = T::class, decimals: Int? = null): String {
        val prefixes: List<UnitPrefix>? = supportedPrefixes[prefixType]
        require(prefixes != null) { "$prefixType is not supported. Valid options are: " + supportedPrefixes.keys }
        return when (bytes) {
            BigDecimal.ZERO -> "0 $SYMBOL"
            else -> {
                val absNs = bytes.abs()
                var scientific = false
                val index = prefixes.dropLastWhile { absNs >= it.factor }.size
                val millionish = prefixes.first().basis.toBigDecimal().pow(2 * prefixes.first().baseExponent)
                if (index == 0 && absNs >= prefixes.first().factor * millionish) scientific = true
                val prefix = prefixes.getOrNull(index)
                val value = bytes.divide(prefix.factor)
                val formattedValue = when {
                    scientific -> value.scientificFormat
                    else -> {
                        val decimals = decimals ?: precision(value.abs(), prefix)
                        value.formatToExactDecimals(decimals)
                    }
                }
                "$formattedValue ${prefix.getSymbol<Size>()}$SYMBOL"
            }
        }
    }

    /**
     * Returns a string representation of this size value expressed with the given [unitPrefix]
     * and formatted with the specified [decimals] number of digits after decimal point.
     *
     * @return the value of duration in the specified [unitPrefix]
     *
     * @throws IllegalArgumentException if [decimals] is less than zero.
     */
    fun toString(unitPrefix: UnitPrefix, decimals: Int = 0): String {
        require(decimals >= 0) { "decimals must be not negative, but was $decimals" }
        val number = bytes.divide(unitPrefix.factor)
        val upperDetailLimit = 1e14.toBigDecimal()
        return when {
            number.abs() < upperDetailLimit -> number.formatToExactDecimals(decimals.coerceAtMost(12))
            else -> number.scientificFormat
        } + " " + unitPrefix.getSymbol<Size>() + SYMBOL
    }

    override fun compareTo(other: Size): Int = this.bytes.compareTo(other.bytes)
    operator fun plus(other: Size): Size = Size(bytes + other.bytes)
    operator fun plus(otherBytes: Long): Size = Size(bytes + BigDecimal.valueOf(otherBytes))
    operator fun plus(otherBytes: Int): Size = Size(bytes + BigDecimal.valueOf(otherBytes.toLong()))
    operator fun minus(other: Size): Size = Size(bytes - other.bytes)
    operator fun minus(otherBytes: Long): Size = Size(bytes - BigDecimal.valueOf(otherBytes))
    operator fun minus(otherBytes: Int): Size = Size(bytes - BigDecimal.valueOf(otherBytes.toLong()))
    operator fun unaryMinus(): Size = ZERO - this
    operator fun div(other: Size): Double = (bytes.div(other.bytes)).toDouble()
    operator fun times(factor: Number): Size = (factor.toBigDecimal() * bytes).bytes
    fun toZeroFilledByteArray(): ByteArray = ByteArray(bytes.toInt())

}
