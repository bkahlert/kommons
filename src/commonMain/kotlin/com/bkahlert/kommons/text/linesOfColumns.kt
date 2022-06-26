package com.bkahlert.kommons.text

import com.bkahlert.kommons.LineSeparators
import com.bkahlert.kommons.LineSeparators.lineSequence


/**
 * Returns a sequence of lines of which none occupies more than given [maxColumns].
 */
public fun CharSequence?.linesOfColumnsSequence(maxColumns: Int): Sequence<CharSequence> {
    if (this == null) return emptySequence()
    val lines = lineSequence()
    return lines.flatMap { line: String ->
        val iterator = line.chunkedByColumnsSequence(maxColumns) { it }.iterator()
        if (iterator.hasNext()) iterator.asSequence() else sequenceOf("")
    }
}

/**
 * Returns a list of lines of which none occupies more than given [maxColumns].
 */
public fun CharSequence?.linesOfColumns(maxColumns: Int): List<CharSequence> =
    linesOfColumnsSequence(maxColumns).toList()

/**
 * Returns a string consisting of lines of which each occupies exactly the given number of [columns].
 *
 * The last line is filled with whitespaces if necessary.
 */
public fun CharSequence?.wrapLines(columns: Int): CharSequence =
    this?.linesOfColumnsSequence(columns)?.joinToString(LineSeparators.LF) {
        val missingColumns = columns - it.columns
        it.toString() + " ".repeat(missingColumns)
    } ?: ""
