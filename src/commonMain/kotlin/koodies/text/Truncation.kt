package koodies.text

import koodies.math.ceilDiv
import koodies.math.floorDiv
import koodies.regex.repeat
import koodies.text.Semantics.formattedAs
import koodies.text.Whitespaces.trailingWhitespaces

/**
 * Creates a truncated string from selected elements separated using [separator] and using the given [prefix] and [postfix] if supplied.
 *
 * If the collection could be huge, you can specify a non-negative value of [startLimit], in which case at most the first [startLimit]
 * elements and the [endLimit] last elements will be appended, leaving out the elements in between using the [truncated] string (which defaults to "…").
 */
public fun <T> Collection<T>.joinToTruncatedString(
    separator: CharSequence = ", ",
    prefix: CharSequence = "",
    postfix: CharSequence = "",
    startLimit: Int = 2,
    endLimit: Int = 1,
    truncated: CharSequence = "…",
    transform: ((T) -> CharSequence)? = null,
    transformEnd: ((T) -> CharSequence)? = null,
): String {
    val limit = startLimit + endLimit
    if (size <= limit) return joinToString(separator, prefix, postfix, limit, truncated, transform)

    val list: List<T> = filterIndexed { index, _ -> index <= startLimit + 1 || index > size - endLimit }
    var index = 0
    return list.joinTo(StringBuilder(), separator, prefix, postfix, size, "") { element ->
        kotlin.run {
            when (index) {
                in (0 until startLimit) -> transform?.invoke(element) ?: element.toString()
                startLimit -> truncated
                else -> transformEnd?.invoke(element) ?: transform?.invoke(element) ?: element.toString()
            }
        }.also { index++ }
    }.toString()
}

private fun requirePositiveColumns(maxColumns: Int) {
    require(maxColumns > 0) {
        "maxColumns ${maxColumns.formattedAs.input} must be positive."
    }
}

private fun targetColumns(maxColumns: Int = 15, marker: String = "…"): Int {
    requirePositiveColumns(maxColumns)
    val markerColumns = marker.columns
    require(maxColumns >= markerColumns) {
        "maxColumns ${maxColumns.formattedAs.input} must not be less than ${markerColumns.formattedAs.input}/${marker.formattedAs.input}"
    }
    return maxColumns - markerColumns
}

/**
 * Returns `this` string truncated from the center to [maxColumns] including the [marker].
 */
public fun String.truncate(maxColumns: Int = 15, marker: String = "…"): String {
    requirePositiveColumns(maxColumns)
    return if ((length > 2 * maxColumns) || columns > maxColumns) {
        val targetColumns = targetColumns(maxColumns, marker)
        val left = truncateEnd(targetColumns ceilDiv 2, "")
        val right = truncateStart(targetColumns floorDiv 2, "")
        "$left$marker$right"
    } else {
        this
    }
}

/**
 * Returns `this` character sequence truncated from the center to [maxColumns] including the [marker].
 */
public fun CharSequence.truncate(maxColumns: Int = 15, marker: String = "…"): CharSequence =
    if (length > 2 * maxColumns || columns > maxColumns) toString().truncate(maxColumns, marker) else this

/**
 * Returns `this` string truncated from the start to [maxColumns] including the [marker].
 */
public fun String.truncateStart(maxColumns: Int = 15, marker: String = "…"): String {
    requirePositiveColumns(maxColumns)
    return when {
        length > 2 * maxColumns -> { // save CPU by trashing obviously too much text
            takeLast(2 * maxColumns).truncateStart(maxColumns, marker)
        }
        columns > maxColumns -> {
            val targetColumns = targetColumns(maxColumns, marker)
            for (i in 0 until length) {
                val truncated = subSequence(i, length)
                if (truncated.columns <= targetColumns) return "$marker$truncated"
            }
            marker
        }
        else -> {
            this
        }
    }
}

/**
 * Returns `this` character sequence truncated from the start to [maxColumns] including the [marker].
 */
public fun CharSequence.truncateStart(maxColumns: Int = 15, marker: String = "…"): CharSequence =
    if (length > 2 * maxColumns || columns > maxColumns) toString().truncateStart(maxColumns, marker) else this

/**
 * Returns `this` string truncated from the end to [maxColumns] including the [marker].
 */
public fun String.truncateEnd(maxColumns: Int = 15, marker: String = "…"): String {
    requirePositiveColumns(maxColumns)
    return when {
        length > 2 * (maxColumns + marker.length) -> { // save CPU by trashing obviously too much text
            take(2 * maxColumns).truncateEnd(maxColumns, marker)
        }
        columns > maxColumns -> {
            val targetColumns = targetColumns(maxColumns, marker)
            for (i in length downTo 0) {
                val truncated = subSequence(0, i)
                if (truncated.columns <= targetColumns) return "$truncated$marker"
            }
            marker
        }
        else -> {
            this
        }
    }
}

/**
 * Returns `this` character sequence truncated from the end to [maxColumns] including the [marker].
 */
public fun CharSequence.truncateEnd(maxColumns: Int = 15, marker: String = "…"): CharSequence =
    if (length > 2 * maxColumns || columns > maxColumns) toString().truncateEnd(maxColumns, marker) else this


/**
 * Truncates `this` character sequence by [numberOfWhitespaces] by strategically removing whitespaces.
 *
 * The algorithm guarantees that word borders are respected, that is, two words never become one
 * (unless [minWhitespaceLength] is set to 0).
 * Therefore the truncated string might not be fully truncated than envisioned.
 */
public fun CharSequence.truncateBy(numberOfWhitespaces: Int, startIndex: Int = 0, minWhitespaceLength: Int = 1): String =
    toString().run { "${truncateTo(length - numberOfWhitespaces, startIndex, minWhitespaceLength)}" }

/**
 * Truncates `this` string to [maxLength] by strategically removing whitespaces.
 *
 * The algorithm guarantees that word borders are respected, that is, two words never become one
 * (unless [minWhitespaceLength] is set to 0).
 * Therefore the truncated string might not be fully truncated than envisioned.
 */
public fun CharSequence.truncateTo(maxLength: Int, startIndex: Int = 0, minWhitespaceLength: Int = 1): CharSequence {
    val difference = length - maxLength
    if (difference <= 0) return this
    val trailingWhitespaces = trailingWhitespaces
    if (trailingWhitespaces.isNotEmpty()) {
        val trimmed = this.take(length - trailingWhitespaces.length.coerceAtMost(difference))
        return if (trimmed.length <= maxLength) trimmed else trimmed.truncateTo(maxLength, startIndex, minWhitespaceLength)
    }
    val regex = Regex("[${Regex.fromLiteral(Unicode.whitespaces.joinToString(""))}]").repeat(minWhitespaceLength + 1)
    val longestWhitespace = regex.findAll(this, startIndex).toList().reversed().maxByOrNull { it.value.length } ?: return this
    val whitespaceStart = longestWhitespace.range.first
    val truncated = replaceRange(whitespaceStart, whitespaceStart + 2, " ")
    if (truncated.length >= length) return truncated
    return truncated.truncateTo(maxLength, startIndex, minWhitespaceLength).toString()
}

/**
 * Returns `this` character sequence truncated to [length] and if necessary padded from the start.
 */
public fun CharSequence.padStartFixedLength(
    length: Int = 15,
    marker: String = "…",
    padChar: Char = ' ',
): String = toString().truncate(length, marker).padStart(length, padChar)

/**
 * Returns `this` character sequence truncated to [length] and if necessary padded from the end.
 */
public fun CharSequence.padEndFixedLength(
    length: Int = 15,
    marker: String = "…",
    padChar: Char = ' ',
): String = toString().truncate(length, marker).padEnd(length, padChar)
