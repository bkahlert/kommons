package koodies.text

/**
 * Creates a string from all the elements separated using [Unicode.lineFeed] (`\n`) and using the given [prefix] and [postfix] if supplied.
 *
 * If the collection could be huge, you can specify a non-negative value of [limit], in which case only the first [limit]
 * elements will be appended, followed by the [truncated] string (which defaults to "...").
 */
public fun <T : Any> Sequence<T>.joinLinesToString(
    prefix: CharSequence = "",
    postfix: CharSequence = "",
    limit: Int = -1,
    truncated: CharSequence = "...",
    transform: ((T) -> CharSequence)? = null,
): String = joinToString(
    separator = LineSeparators.LF,
    prefix = prefix,
    postfix = postfix,
    limit = limit,
    truncated = truncated,
    transform = transform
)

/**
 * Creates a string from all the elements separated using [Unicode.lineFeed] (`\n`) and using the given [prefix] and [postfix] if supplied.
 *
 * If the collection could be huge, you can specify a non-negative value of [limit], in which case only the first [limit]
 * elements will be appended, followed by the [truncated] string (which defaults to "...").
 */
public fun <T : Any> Iterable<T>.joinLinesToString(
    prefix: CharSequence = "",
    postfix: CharSequence = "",
    limit: Int = -1,
    truncated: CharSequence = "...",
    transform: ((T) -> CharSequence)? = null,
): String = joinToString(
    separator = LineSeparators.LF,
    prefix = prefix,
    postfix = postfix,
    limit = limit,
    truncated = truncated,
    transform = transform
)
