package koodies.number

import java.math.BigDecimal
import java.math.RoundingMode
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale
import kotlin.concurrent.getOrSet

val Double.scientificFormat: String get() = privateFormatScientific(this)
val BigDecimal.scientificFormat: String get() = privateFormatScientific(this.toDouble())

fun Double.formatToExactDecimals(decimals: Int): String = privateFormatToExactDecimals(this, decimals)
fun BigDecimal.formatToExactDecimals(decimals: Int): String = privateFormatToExactDecimals(this.toDouble(), decimals)

fun Double.formatUpToDecimals(decimals: Int): String = privateFormatUpToDecimals(this, decimals)
fun BigDecimal.formatUpToDecimals(decimals: Int): String = privateFormatUpToDecimals(this.toDouble(), decimals)


private val rootNegativeExpFormatSymbols = DecimalFormatSymbols(Locale.ROOT).apply { exponentSeparator = "e" }
private val rootPositiveExpFormatSymbols = DecimalFormatSymbols(Locale.ROOT).apply { exponentSeparator = "e+" }


private val scientificFormat = ThreadLocal<DecimalFormat>()
private fun privateFormatScientific(value: Double): String =
    scientificFormat.getOrSet {
        DecimalFormat("0E0", rootNegativeExpFormatSymbols).apply { minimumFractionDigits = 2 }
    }.apply {
        decimalFormatSymbols = if (value >= 1 || value <= -1) rootPositiveExpFormatSymbols else rootNegativeExpFormatSymbols
    }.format(value)


private val precisionFormats = Array(4) { ThreadLocal<DecimalFormat>() }
private fun createFormatForDecimals(decimals: Int) = DecimalFormat("0", rootNegativeExpFormatSymbols).apply {
    if (decimals > 0) minimumFractionDigits = decimals
    roundingMode = RoundingMode.HALF_UP
}

private fun privateFormatToExactDecimals(value: Double, decimals: Int): String {
    val format = if (decimals < precisionFormats.size) {
        precisionFormats[decimals].getOrSet { createFormatForDecimals(decimals) }
    } else
        createFormatForDecimals(decimals)
    return format.format(value)
}

private fun privateFormatUpToDecimals(value: Double, decimals: Int): String =
    createFormatForDecimals(0)
        .apply { maximumFractionDigits = decimals }
        .format(value)
