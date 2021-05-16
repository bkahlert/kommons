package koodies.text

import koodies.math.ceilDiv
import koodies.math.floorDiv
import koodies.regex.repeat
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

/**
 * Different approaches on how to conduct the [truncate] operation.
 */
public enum class TruncationStrategy(private val implementation: (CharSequence).(Int, CharSequence) -> CharSequence) {

    /**
     * If the character sequence has more characters than specified,
     * leading characters will be truncated.
     */
    START({ maxLength, marker ->
        "$marker${subSequence(length - (maxLength - marker.length), length)}"
    }),

    /**
     * If the character sequence has more characters than specified,
     * centric characters will be truncated.
     */
    MIDDLE({ maxLength, marker ->
        val maxX: Int = maxLength - marker.length
        val left = subSequence(0, maxX ceilDiv 2)
        val right = subSequence(length - (maxX floorDiv 2), length)
        "$left$marker$right"
    }),

    /**
     * If the character sequence has more characters than specified,
     * trailing characters will be truncated.
     */
    END({ maxLength, marker ->
        "${subSequence(0, maxLength - marker.length)}$marker"
    });

    /**
     * Returns the given [text] truncated to [maxLength] characters including the [marker].
     */
    public fun truncate(text: String, maxLength: Int, marker: String = "…"): String =
        if (text.length > maxLength) implementation(text, maxLength, marker).toString() else text

    /**
     * Returns the given [text] truncated to [maxLength] characters including the [marker].
     */
    public fun truncate(text: CharSequence, maxLength: Int, marker: String = "…"): CharSequence =
        if (text.length > maxLength) implementation(text, maxLength, marker) else text
}

/**
 * Returns `this` string truncated to [maxLength] characters including the [marker].
 */
public fun String.truncate(maxLength: Int = 15, strategy: TruncationStrategy = TruncationStrategy.END, marker: String = "…"): String =
    strategy.truncate(this, maxLength, marker)

/**
 * Returns `this` character sequence truncated to [maxLength] characters including the [marker].
 */
public fun CharSequence.truncate(maxLength: Int = 15, strategy: TruncationStrategy = TruncationStrategy.END, marker: String = "…"): CharSequence =
    strategy.truncate(this, maxLength, marker)


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
 * Returns this [CharSequence] truncated to [length] and if necessary padded from the start.
 */
public fun CharSequence.padStartFixedLength(
    length: Int = 15,
    strategy: TruncationStrategy = TruncationStrategy.END,
    marker: String = "…",
    padChar: Char = ' ',
): CharSequence =
    strategy.truncate(toString(), length, marker).padStart(length, padChar)

/**
 * Returns this [CharSequence] truncated to [length] and if necessary padded from the end.
 */
public fun CharSequence.padEndFixedLength(
    length: Int = 15,
    strategy: TruncationStrategy = TruncationStrategy.END,
    marker: String = "…",
    padChar: Char = ' ',
): CharSequence =
    strategy.truncate(toString(), length, marker).padEnd(length, padChar)
