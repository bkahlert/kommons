package com.bkahlert.kommons.text

import com.bkahlert.kommons.math.ceilDiv
import com.bkahlert.kommons.math.floorDiv
import com.bkahlert.kommons.regex.repeat
import com.bkahlert.kommons.text.Semantics.formattedAs
import com.bkahlert.kommons.text.Whitespaces.trailingWhitespaces

/**
 * Creates a truncated string from selected elements separated using [separator] and using the given [prefix] and [postfix] if supplied.
 *
 * If the collection could be huge, you can specify a non-negative value of [startLimit], in which case at most the first [startLimit]
 * elements and the [endLimit] last elements will be appended, leaving out the elements in between using the [truncated] string (which defaults to " … ").
 */
public fun <T> Collection<T>.joinToTruncatedString(
    separator: CharSequence = ", ",
    prefix: CharSequence = "",
    postfix: CharSequence = "",
    startLimit: Int = 2,
    endLimit: Int = 1,
    truncated: CharSequence = Unicode.ELLIPSIS.spaced,
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


private fun requirePositiveCodePoints(maxCodePoints: Int) {
    require(maxCodePoints > 0) {
        "maxCodePoints ${maxCodePoints.formattedAs.input} must be positive."
    }
}

private fun targetCodePoints(maxCodePoints: Int, marker: String): Int {
    requirePositiveCodePoints(maxCodePoints)
    val markerCodePointCount = marker.codePointCount
    require(maxCodePoints >= markerCodePointCount) {
        "maxCodePoints ${maxCodePoints.formattedAs.input} must not be less than ${markerCodePointCount.formattedAs.input}/${marker.formattedAs.input}"
    }
    return maxCodePoints - markerCodePointCount
}

/**
 * Returns this string truncated from the center to [maxCodePoints] including the [marker].
 */
public fun String.truncate(maxCodePoints: Int = 15, marker: String = Unicode.ELLIPSIS.spaced): String {
    requirePositiveCodePoints(maxCodePoints)
    return if (length > 2 * maxCodePoints || codePointCount > maxCodePoints) {
        val targetCodePoints = targetCodePoints(maxCodePoints, marker)
        val left = truncateEnd(targetCodePoints ceilDiv 2, "")
        val right = truncateStart(targetCodePoints floorDiv 2, "")
        "$left$marker$right"
    } else {
        this
    }
}

/**
 * Returns this character sequence truncated from the center to [maxCodePoints] including the [marker].
 */
public fun CharSequence.truncate(maxCodePoints: Int = 15, marker: String = Unicode.ELLIPSIS.spaced): CharSequence =
    if (codePointCount > maxCodePoints) toString().truncate(maxCodePoints, marker) else this

/**
 * Returns this string truncated from the start to [maxCodePoints] including the [marker].
 */
public fun String.truncateStart(maxCodePoints: Int = 15, marker: String = Unicode.ELLIPSIS.spaced): String {
    requirePositiveCodePoints(maxCodePoints)
    if (codePointCount <= maxCodePoints) return this

    val targetCodePoints = targetCodePoints(maxCodePoints, marker)
    val codePoints = toCodePointList().takeLast(targetCodePoints)
    return "$marker${codePoints.joinToString("")}"
}

/**
 * Returns this character sequence truncated from the start to [maxCodePoints] including the [marker].
 */
public fun CharSequence.truncateStart(maxCodePoints: Int = 15, marker: String = Unicode.ELLIPSIS.spaced): CharSequence =
    if (codePointCount > maxCodePoints) toString().truncateStart(maxCodePoints, marker) else this

/**
 * Returns this string truncated from the end to [maxCodePoints] including the [marker].
 */
public fun String.truncateEnd(maxCodePoints: Int = 15, marker: String = Unicode.ELLIPSIS.spaced): String {
    requirePositiveCodePoints(maxCodePoints)
    if (codePointCount <= maxCodePoints) return this

    val targetCodePoints = targetCodePoints(maxCodePoints, marker)
    val codePoints = toCodePointList().take(targetCodePoints)
    return "${codePoints.joinToString("")}$marker"
}

/**
 * Returns this character sequence truncated from the end to [maxCodePoints] including the [marker].
 */
public fun CharSequence.truncateEnd(maxCodePoints: Int = 15, marker: String = Unicode.ELLIPSIS.spaced): CharSequence =
    if (length > 2 * (maxCodePoints + 1) || length > maxCodePoints) toString().truncateEnd(maxCodePoints, marker) else this


/**
 * Truncates this character sequence by [numberOfWhitespaces] by strategically removing whitespaces.
 *
 * The algorithm guarantees that word borders are respected, that is, two words never become one
 * (unless [minWhitespaceLength] is set to 0).
 * Therefore the truncated string might not be fully truncated than envisioned.
 */
public fun CharSequence.truncateBy(numberOfWhitespaces: Int, startIndex: Int = 0, minWhitespaceLength: Int = 1): String =
    toString().run { "${truncateTo(length - numberOfWhitespaces, startIndex, minWhitespaceLength)}" }

/**
 * Truncates this string to [maxLength] by strategically removing whitespaces.
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
 * Returns this character sequence truncated to [length] and if necessary padded from the start.
 */
public fun CharSequence.padStartFixedLength(
    length: Int = 15,
    marker: String = Unicode.ELLIPSIS.spaced,
    padChar: Char = ' ',
): String = toString().truncate(length, marker).padStart(length, padChar)

/**
 * Returns this character sequence truncated to [length] and if necessary padded from the end.
 */
public fun CharSequence.padEndFixedLength(
    length: Int = 15,
    marker: String = Unicode.ELLIPSIS.spaced,
    padChar: Char = ' ',
): String = toString().truncate(length, marker).padEnd(length, padChar)
