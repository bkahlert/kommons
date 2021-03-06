package koodies.text

import koodies.text.LineSeparators.hasTrailingLineSeparator

/**
 * Flat maps each line of this char sequence using [transform].
 *
 * If this char sequence consists of but a single line this line is mapped.
 *
 * If this char sequence has a trailing line that trailing line is left unchanged.
 */
public fun <T : CharSequence> T.flatMapLines(ignoreTrailingSeparator: Boolean = true, transform: (CharSequence) -> Iterable<T>): String =
    (hasTrailingLineSeparator && ignoreTrailingSeparator).let { trailingLineSeparator ->
        lines().map { line -> transform(line).joinToString("\n") }
            .let { if (trailingLineSeparator) it.dropLast(1) else it }
            .joinToString("\n")
            .let { if (trailingLineSeparator) it + "\n" else it }
    }
