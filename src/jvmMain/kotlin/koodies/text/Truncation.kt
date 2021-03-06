package koodies.text


/**
 * Creates a truncated string from selected elements separated using [separator] and using the given [prefix] and [postfix] if supplied.
 *
 * If the collection could be huge, you can specify a non-negative value of [startLimit], in which case at most the first [startLimit]
 * elements and the [endLimit] last elements will be appended, leaving out the elements in between using the [truncated] string (which defaults to "...").
 */
public fun <T> List<T>.joinToTruncatedString(
    separator: CharSequence = ", ",
    prefix: CharSequence = "",
    postfix: CharSequence = "",
    startLimit: Int = 2,
    endLimit: Int = 1,
    truncated: CharSequence = "...",
    transform: ((T) -> CharSequence)? = null,
    transformEnd: ((T) -> CharSequence)? = null,
): String {
    val limit = startLimit + endLimit
    if (size <= limit) return joinToString(separator, prefix, postfix, limit, truncated, transform)

    val list: List<T> = this.filterIndexed { index, _ -> index <= startLimit + 1 || index > size - endLimit }
    var index = 0
    return list.joinTo(StringBuilder(), separator, prefix, postfix, size, "", { element ->
        kotlin.run {
            when (index) {
                in (0 until startLimit) -> transform?.invoke(element) ?: element.toString()
                startLimit -> truncated
                else -> transformEnd?.invoke(element) ?: transform?.invoke(element) ?: element.toString()
            }
        }.also { index++ }
    }).toString()
}

public enum class TruncationStrategy(private val implementation: (CharSequence).(Int, CharSequence) -> CharSequence) {
    START({ maxLength, marker ->
        "$marker${subSequence(length - (maxLength - marker.length), length)}"
    }),
    MIDDLE({ maxLength, marker ->
        ((maxLength - marker.length) / 2.0).let { halfMaxLength ->
            val left = subSequence(0, kotlin.math.ceil(halfMaxLength).toInt())
            val right = subSequence(length - kotlin.math.floor(halfMaxLength).toInt(), length)
            "$left$marker$right"
        }
    }),
    END({ maxLength, marker ->
        "${subSequence(0, maxLength - marker.length)}$marker"
    });

    public fun truncate(text: CharSequence, maxLength: Int, marker: String = "…"): CharSequence =
        if (text.length > maxLength) implementation(text, maxLength, marker) else text
}

/**
 * Returns the [String] truncated to [maxLength] characters including the [marker].
 */
public fun CharSequence.truncate(maxLength: Int = 15, strategy: TruncationStrategy = TruncationStrategy.END, marker: String = "…"): CharSequence =
    strategy.truncate(this, maxLength, marker)


/**
 * Truncates this char sequence by [numberOfWhitespaces] by strategically removing whitespaces.
 *
 * The algorithm guarantees that word borders are respected, that is, two words never become one
 * (unless [minWhitespaceLength] is set to 0).
 * Therefore the truncated string might not be fully truncated than envisioned.
 */
public fun CharSequence.truncateBy(numberOfWhitespaces: Int, startIndex: Int = 0, minWhitespaceLength: Int = 1): String =
    toString().truncateBy(numberOfWhitespaces, startIndex, minWhitespaceLength)

/**
 * Truncates this string by [numberOfWhitespaces] by strategically removing whitespaces.
 *
 * The algorithm guarantees that word borders are respected, that is, two words never become one
 * (unless [minWhitespaceLength] is set to 0).
 * Therefore the truncated string might not be fully truncated than envisioned.
 */
public fun String.truncateBy(numberOfWhitespaces: Int, startIndex: Int = 0, minWhitespaceLength: Int = 1): String =
    "${truncateTo(length - numberOfWhitespaces, startIndex, minWhitespaceLength)}"

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
    val regex = Regex("[${Regex.fromLiteral(Unicode.whitespaces.joinToString(""))}]{${minWhitespaceLength + 1},}")
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
    strategy.truncate(toString(), length, marker).padStart(length, padChar).toString()

/**
 * Returns this [CharSequence] truncated to [length] and if necessary padded from the end.
 */
public fun CharSequence.padEndFixedLength(
    length: Int = 15,
    strategy: TruncationStrategy = TruncationStrategy.END,
    marker: String = "…",
    padChar: Char = ' ',
): CharSequence =
    strategy.truncate(toString(), length, marker).padEnd(length, padChar).toString()
