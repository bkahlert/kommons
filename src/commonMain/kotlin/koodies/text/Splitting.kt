package koodies.text

/**
 * Splits this strings using the specified [delimiter] applies [transform]
 * to each substrings and joins back the mapped substrings to a string using the same [delimiter].
 */
fun String.splitAndMap(delimiter: String, transform: String.() -> String): String =
    split(delimiter).map { transform(it) }.joinToString(delimiter)

/**
 * Splits this char sequence to a sequence of strings around occurrences of the specified [delimiters].
 *
 * @param delimiters One or more strings to be used as delimiters.
 * @param keepDelimiters `true` to have string end with its corresponding delimiter.
 * @param ignoreCase `true` to ignore character case when matching a delimiter. By default `false`.
 * @param limit The maximum number of substrings to return. Zero by default means no limit is set.
 *
 * To avoid ambiguous results when strings in [delimiters] have characters in common, this method proceeds from
 * the beginning to the end of this string, and finds at each position the first element in [delimiters]
 * that matches this string at that position.
 */
fun CharSequence.splitToSequence(
    delimiters: Array<String>,
    keepDelimiters: Boolean = false,
    ignoreTrailingSeparator: Boolean = false,
    ignoreCase: Boolean = false,
    limit: Int = 0,
): Sequence<String> =
    rangesDelimitedBy(delimiters = delimiters, ignoreCase = ignoreCase, limit = limit)
        .run {
            if (!keepDelimiters) map { substring(it) }
            else windowed(size = 2, step = 1, partialWindows = true) { ranges ->
                substring(if (ranges.size == 2) ranges[0].first until ranges[1].first else ranges[0])
            }
        }
        .run {
            if (!ignoreTrailingSeparator) this
            else iterator().run {
                generateSequence {
                    val current = if (hasNext()) next() else null
                    current?.takeIf { hasNext() || current.isNotEmpty() }
                }
            }
        }

/**
 * Returns a sequence of index ranges of substrings in this char sequence around occurrences of the specified [delimiters].
 *
 * @param delimiters One or more strings to be used as delimiters.
 * @param startIndex The index to start searching delimiters from.
 *  No range having its start value less than [startIndex] is returned.
 *  [startIndex] is coerced to be non-negative and not greater than length of this string.
 * @param ignoreCase `true` to ignore character case when matching a delimiter. By default `false`.
 * @param limit The maximum number of substrings to return. Zero by default means no limit is set.
 *
 * To avoid ambiguous results when strings in [delimiters] have characters in common, this method proceeds from
 * the beginning to the end of this string, and finds at each position the first element in [delimiters]
 * that matches this string at that position.
 */
private fun CharSequence.rangesDelimitedBy(
    delimiters: Array<out String>,
    startIndex: Int = 0,
    ignoreCase: Boolean = false,
    limit: Int = 0,
): Sequence<IntRange> {
    require(limit >= 0) { "Limit must be non-negative, but was $limit." }
    val delimitersList = delimiters.asList()

    return DelimitedRangesSequence(this, startIndex, limit) { currentIndex ->
        findAnyOf(strings = delimitersList,
            startIndex = currentIndex,
            ignoreCase = ignoreCase,
            last = false)?.let { it.first to it.second.length }
    }
}

private fun CharSequence.findAnyOf(strings: Collection<String>, startIndex: Int, ignoreCase: Boolean, last: Boolean): Pair<Int, String>? {
    if (!ignoreCase && strings.size == 1) {
        val string = strings.single()
        val index = if (!last) indexOf(string, startIndex) else lastIndexOf(string, startIndex)
        return if (index < 0) null else index to string
    }

    val indices = if (!last) startIndex.coerceAtLeast(0)..length else startIndex.coerceAtMost(lastIndex) downTo 0

    if (this is String) {
        for (index in indices) {
            val matchingString = strings.firstOrNull { it.regionMatches(0, this, index, it.length, ignoreCase) }
            if (matchingString != null)
                return index to matchingString
        }
    } else {
        for (index in indices) {
            val matchingString = strings.firstOrNull { it.regionMatchesImpl(0, this, index, it.length, ignoreCase) }
            if (matchingString != null)
                return index to matchingString
        }
    }

    return null
}

/**
 * Implementation of [regionMatches] for CharSequences.
 * Invoked when it's already known that arguments are not Strings, so that no additional type checks are performed.
 */
private fun CharSequence.regionMatchesImpl(thisOffset: Int, other: CharSequence, otherOffset: Int, length: Int, ignoreCase: Boolean): Boolean =
    if ((otherOffset < 0) || (thisOffset < 0) || (thisOffset > this.length - length) || (otherOffset > other.length - length)) false
    else (0 until length).all {
        this[thisOffset + it].equals(other[otherOffset + it], ignoreCase)
    }

private class DelimitedRangesSequence(
    private val input: CharSequence,
    private val startIndex: Int,
    private val limit: Int,
    private val getNextMatch: CharSequence.(currentIndex: Int) -> Pair<Int, Int>?,
) : Sequence<IntRange> {

    override fun iterator(): Iterator<IntRange> = object : Iterator<IntRange> {
        var nextState: Int = -1 // -1 for unknown, 0 for done, 1 for continue
        var currentStartIndex: Int = startIndex.coerceIn(0, input.length)
        var nextSearchIndex: Int = currentStartIndex
        var nextItem: IntRange? = null
        var counter: Int = 0

        private fun calcNext() {
            if (nextSearchIndex < 0) {
                nextState = 0
                nextItem = null
            } else {
                if (limit > 0 && ++counter >= limit || nextSearchIndex > input.length) {
                    nextItem = currentStartIndex..input.lastIndex
                    nextSearchIndex = -1
                } else {
                    val match = input.getNextMatch(nextSearchIndex)
                    if (match == null) {
                        nextItem = currentStartIndex..input.lastIndex
                        nextSearchIndex = -1
                    } else {
                        val (index, length) = match
                        nextItem = currentStartIndex until index
                        currentStartIndex = index + length
                        nextSearchIndex = currentStartIndex + if (length == 0) 1 else 0
                    }
                }
                nextState = 1
            }
        }

        override fun next(): IntRange {
            if (nextState == -1)
                calcNext()
            if (nextState == 0)
                throw NoSuchElementException()
            val result = nextItem as IntRange
            // Clean next to avoid keeping reference on yielded instance
            nextItem = null
            nextState = -1
            return result
        }

        override fun hasNext(): Boolean {
            if (nextState == -1)
                calcNext()
            return nextState == 1
        }
    }
}
