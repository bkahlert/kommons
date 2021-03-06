package koodies.text

/**
 * Indent (all whitespace characters) leading the first line of this [CharSequence].
 */
public val CharSequence.indent: CharSequence get() = indexOfFirst { !it.isWhitespace() }.let { if (it == -1) this else this.subSequence(0, it) }
