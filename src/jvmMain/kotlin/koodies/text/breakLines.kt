package koodies.text


/**
 * Breaks this char sequence to a sequence of strings of [maxLength].
 *
 * @param maxLength The maximum length of each returned line.
 */
fun CharSequence.breakLines(maxLength: Int, ignoreTrailingSeparator: Boolean = true): String {
    return flatMapLines(ignoreTrailingSeparator) { line ->
        line.chunked(maxLength)
    }
}
