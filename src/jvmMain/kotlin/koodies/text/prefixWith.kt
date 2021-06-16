package koodies.text

/**
 * Returns the [String] prefixed with the given [prefix].
 */
public fun String.prefixWith(prefix: String): String =
    if (prefix.isNotEmpty()) prefix + this else this

/**
 * Returns the [CharSequence] prefixed with the given [prefix].
 */
public fun CharSequence.prefixWith(prefix: String): CharSequence =
    if (prefix.isNotEmpty()) prefix + this else this
