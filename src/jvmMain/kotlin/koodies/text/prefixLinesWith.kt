package koodies.text

/**
 * Returns the [String] of what all lines of text are prefixed with the given [prefix].
 */
fun CharSequence.prefixLinesWith(ignoreTrailingSeparator: Boolean = true, prefix: CharSequence): String = mapLines(ignoreTrailingSeparator) { "$prefix$it" }
