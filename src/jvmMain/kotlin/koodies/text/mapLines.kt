package koodies.text

import koodies.text.LineSeparators.hasTrailingLineSeparator
import koodies.text.LineSeparators.lines

/**
 * Maps each line of this char sequence using [transform].
 *
 * If this char sequence consists of but a single line this line is mapped.
 *
 * If this char sequence has a trailing line that trailing line is left unchanged.
 */
fun CharSequence.mapLines(ignoreTrailingSeparator: Boolean = true, transform: (CharSequence) -> CharSequence): String =
    (hasTrailingLineSeparator && ignoreTrailingSeparator).let { trailingLineSeparator ->
        lines().map(transform)
            .let { if (trailingLineSeparator) it.dropLast(1) else it }
            .joinToString("\n")
            .let { if (trailingLineSeparator) it + "\n" else it }
    }

/**
 * Maps each line of this string using [transform].
 *
 * If this string consists of but a single line this line is mapped.
 *
 * If this string has a trailing line that trailing line is left unchanged.
 */
fun String.mapLines(ignoreTrailingSeparator: Boolean = true, transform: (CharSequence) -> CharSequence): String =
    (this as CharSequence).mapLines(ignoreTrailingSeparator, transform)
