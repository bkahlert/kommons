package koodies.text

import koodies.collections.zipWithDefault
import koodies.text.AnsiString.Companion.asAnsiString
import koodies.text.LineSeparators.LF
import koodies.text.LineSeparators.lineSequence

/**
 * Returns a string that consists of two columns.
 * 1) This char sequence as the first column.
 * 2) The other char sequence as the second column.
 *
 * **Example**
 * ```
 * Line 1
 * Line 1.1
 * Line 2
 * ```
 * and
 * ```
 * Line a
 * Line a.b
 * Line c
 * Line d
 * ```
 * will result in
 * ```
 * Line 1       Line 1
 * Line 1.1     Line a.b
 * Line 2       Line c
 *              Line d
 * ```
 */
public fun AnsiString.addColumn(column: AnsiString, columnWidth: Int = maxLength(), paddingCharacter: Char = ' ', paddingWidth: Int = 5): AnsiString =
    lineSequence()
        .zipWithDefault(column.lineSequence(), "" to "") { leftLine: String, rightLine: String ->
            val leftColumn = leftLine.asAnsiString().padEnd(columnWidth + paddingWidth, padChar = paddingCharacter)
            "$leftColumn$rightLine"
        }
        .joinToString(LF)
        .asAnsiString()

/**
 * Returns a string that consists of two columns.
 * 1) This char sequence as the first column.
 * 2) The other char sequence as the second column.
 *
 * **Example**
 * ```
 * Line 1
 * Line 1.1
 * Line 2
 * ```
 * and
 * ```
 * Line a
 * Line a.b
 * Line c
 * Line d
 * ```
 * will result in
 * ```
 * Line 1       Line 1
 * Line 1.1     Line a.b
 * Line 2       Line c
 *              Line d
 * ```
 */
public fun CharSequence.addColumn(
    column: CharSequence,
    columnWidth: Int = asAnsiString().maxLength(),
    paddingCharacter: Char = ' ',
    paddingWidth: Int = 5,
): String =
    asAnsiString().addColumn(
        column = column.asAnsiString(),
        columnWidth = columnWidth,
        paddingCharacter = paddingCharacter,
        paddingWidth = paddingWidth,
    ).toString()
