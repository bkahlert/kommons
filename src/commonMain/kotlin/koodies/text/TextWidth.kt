package koodies.text

import koodies.text.AnsiString.Companion.asAnsiString
import koodies.text.Semantics.formattedAs
import kotlin.math.round

/**
 * Text width calculation.
 */
internal expect object TextWidth {

    /**
     * The width of an monospaced letter `X`.
     */
    val X_WIDTH: Int

    /**
     * Returns the width of the given [text].
     */
    fun calculateWidth(text: CharSequence): Int
}

/**
 * Number of columns needed to represent `this` character.
 */
public val Char.columns: Int get() = toString().columns

/**
 * Number of columns needed to represent the character described by this code point.
 */
public val CodePoint.columns: Int get() = string.columns

/**
 * Number of columns needed to represent `this` character sequence.
 */
public val CharSequence.columns: Int get() = round(TextWidth.calculateWidth(this).toDouble() / TextWidth.X_WIDTH).toInt()

/**
 * Returns the index where the given [column] starts.
 */
public fun CharSequence.findIndexByColumns(column: Int): Int? {
    require(column >= 0) { "Requested column ${column.formattedAs.input} is less than zero." }
    if (column == columns) return length
    for (i in 0 until length) {
        val currentColumns = subSequence(0, i).columns
        if (currentColumns == column) {
            var index = i
            while (subSequence(0, index + 1).columns == column) index++
            return index
        }
        if (currentColumns > column) {
            return i - 1
        }
    }
    return null
}

/**
 * Returns a new character sequence that is a subsequence of this character sequence,
 * starting at the specified [startColumn] and ending right before the specified [endColumn].
 *
 * @param startColumn the start column (inclusive).
 * @param endColumn the end column (exclusive).
 */
public fun CharSequence.subSequenceByColumns(startColumn: Int, endColumn: Int): CharSequence {
    val startIndex: Int = requireNotNull(findIndexByColumns(startColumn)) { "Requested start column ${startColumn.formattedAs.input} could not be found." }
    val endIndex: Int = requireNotNull(findIndexByColumns(endColumn)) { "Requested end column ${endColumn.formattedAs.input} could not be found." }
    return subSequence(startIndex, endIndex)
}

/**
 * Returns a substring of chars from a range of this char sequence starting at the [startColumn] and ending right before the [endColumn].
 *
 * @param startColumn the start column (inclusive).
 * @param endColumn the end column (exclusive). If not specified, the [columns] of the char sequence are used.
 */
public fun CharSequence.substringByColumns(startColumn: Int, endColumn: Int = columns): String =
    subSequenceByColumns(startColumn, endColumn).toString()

/**
 * Returns a subsequence of this char sequence occupying the specified number of [columns]
 * with the first characters removed.
 *
 * @throws IllegalArgumentException if [columns] is negative.
 */
public fun CharSequence.dropColumns(columns: Int): CharSequence {
    require(columns >= 0) { "Requested columns ${columns.formattedAs.input} is less than zero." }
    val limit = this.columns
    return subSequenceByColumns(columns.coerceAtMost(limit), limit)
}

/**
 * Returns a string occupying the specified number of [columns] with the first characters removed.
 *
 * @throws IllegalArgumentException if [columns] is negative.
 */
public fun String.dropColumns(columns: Int): String {
    require(columns >= 0) { "Requested columns ${columns.formattedAs.input} is less than zero." }
    val limit = this.columns
    return substringByColumns(columns.coerceAtMost(limit))
}

/**
 * Returns a subsequence of this char sequence with the specified number of last [columns] removed.
 *
 * @throws IllegalArgumentException if [columns] is negative.
 */
public fun CharSequence.dropLastColumns(columns: Int): CharSequence {
    require(columns >= 0) { "Requested columns ${columns.formattedAs.input} is less than zero." }
    return takeColumns((this.columns - columns).coerceAtLeast(0))
}

/**
 * Returns a string occupying the specified number of last [columns] removed.
 *
 * @throws IllegalArgumentException if [columns] is negative.
 */
public fun String.dropLastColumns(columns: Int): String {
    require(columns >= 0) { "Requested columns ${columns.formattedAs.input} is less than zero." }
    return takeColumns((this.columns - columns).coerceAtLeast(0))
}

/**
 * Returns a subsequence of this char sequence containing the first characters
 * together occupying the specified number of [columns].
 *
 * @throws IllegalArgumentException if [columns] is negative.
 */
public fun CharSequence.takeColumns(columns: Int): CharSequence {
    require(columns >= 0) { "Requested columns ${columns.formattedAs.input} is less than zero." }
    return subSequenceByColumns(0, columns.coerceAtMost(this.columns))
}

/**
 * Returns a string containing the first characters from this string
 * together occupying the specified number of [columns].
 *
 * @throws IllegalArgumentException if [columns] is negative.
 */
public fun String.takeColumns(columns: Int): String {
    require(columns >= 0) { "Requested columns ${columns.formattedAs.input} is less than zero." }
    return substringByColumns(0, columns.coerceAtMost(this.columns))
}

/**
 * Returns a subsequence of this char sequence containing the last [columns].
 *
 * @throws IllegalArgumentException if [columns] is negative.
 */
public fun CharSequence.takeLastColumns(columns: Int): CharSequence {
    require(columns >= 0) { "Requested columns ${columns.formattedAs.input} is less than zero." }
    val limit = this.columns
    return subSequenceByColumns(limit - columns.coerceAtMost(limit), limit)
}

/**
 * Returns a string containing the last [columns].
 *
 * @throws IllegalArgumentException if [columns] is negative.
 */
public fun String.takeLastColumns(columns: Int): String {
    require(columns >= 0) { "Requested columns ${columns.formattedAs.input} is less than zero." }
    val limit = this.columns
    return substringByColumns(limit - columns.coerceAtMost(limit))
}


/**
 * Splits this char sequence into a list of strings each not exceeding the given [columns].
 *
 * The last string in the resulting list may have fewer columns than the given [columns].
 */
public fun CharSequence.chunkedByColumns(columns: Int): List<String> =
    chunkedByColumnsSequence(columns) { it.toString() }.toList()

/**
 * Splits this char sequence into several char sequences each not exceeding the given [columns]
 * and applies the given [transform] function to an each.
 */
public fun <R> CharSequence.chunkedByColumns(columns: Int, transform: (CharSequence) -> R): List<R> =
    chunkedByColumnsSequence(columns, transform).toList()

/**
 * Splits this char sequence into a sequence of strings each not exceeding the given [columns].
 *
 * The last string in the resulting sequence may have fewer columns than the given [columns].
 */
public fun CharSequence.chunkedByColumnsSequence(columns: Int): Sequence<String> =
    chunkedByColumnsSequence(columns) { it.toString() }

/**
 * Splits this char sequence into several char sequences each not exceeding the given [columns]
 * and applies the given [transform] function to an each.
 */
public fun <R> CharSequence.chunkedByColumnsSequence(columns: Int, transform: (CharSequence) -> R): Sequence<R> {
    require(columns > 0) { "Requested columns ${columns.formattedAs.input} must be greater than zero." }
    var unprocessed = asAnsiString()
    return generateSequence {
        if (unprocessed.isEmpty()) {
            null
        } else {
            val chunk = unprocessed.takeColumns(columns)
            unprocessed = unprocessed.drop(chunk.length)
            transform(chunk)
        }
    }
}
