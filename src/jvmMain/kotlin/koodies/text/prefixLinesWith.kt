package koodies.text

/**
 * Returns the [String] of what all lines of text are prefixed with the given [prefix].
 */
fun CharSequence.prefixLinesWith(prefix: CharSequence, ignoreTrailingSeparator: Boolean = true): String = mapLines(ignoreTrailingSeparator) { "$prefix$it" }
