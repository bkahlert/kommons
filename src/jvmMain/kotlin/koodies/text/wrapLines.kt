package koodies.text

/**
 * Returns a string consisting of lines of which none is longer than [maxLineLength].
 */
fun CharSequence.wrapLines(maxLineLength: Int): CharSequence =
    linesOfLength(maxLineLength).joinLinesToString("") { "$it" }
