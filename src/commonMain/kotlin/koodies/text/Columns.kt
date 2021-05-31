package koodies.text

import koodies.collections.zipWithDefault
import koodies.text.AnsiString.Companion.asAnsiString
import koodies.text.LineSeparators.LF
import koodies.text.LineSeparators.lineSequence

/**
 * Splits this character sequence into its lines and returns the columns
 * of the widest of them.
 */
public fun CharSequence.maxColumns(): Int =
    lineSequence().maxColumns()

/**
 * Returns the columns of the widest character sequence.
 */
public fun Iterable<CharSequence>.maxColumns(): Int =
    asSequence().maxColumns()

/**
 * Returns the columns of the widest character sequence.
 */
public fun Sequence<CharSequence>.maxColumns(): Int =
    maxOf { it.columns }

/**
 * Returns a string that consists of two columns.
 * 1) This character sequence as the first column.
 * 2) The other character sequence as the second column.
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
public fun AnsiString.addColumn(column: AnsiString, columnWidth: Int = maxColumns(), paddingCharacter: Char = ' ', paddingColumns: Int = 5): AnsiString =
    lineSequence()
        .zipWithDefault(column.lineSequence(), "" to "") { leftLine: String, rightLine: String ->
            val paddedLeft = leftLine.padEndByColumns(columnWidth + paddingColumns, padChar = paddingCharacter)
            "$paddedLeft$rightLine"
        }
        .joinToString(LF)
        .asAnsiString()

/**
 * Returns a string that consists of two columns.
 * 1) This character sequence as the first column.
 * 2) The other character sequence as the second column.
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
    columnWidth: Int = asAnsiString().maxColumns(),
    paddingCharacter: Char = ' ',
    paddingWidth: Int = 5,
): String =
    asAnsiString().addColumn(
        column = column.asAnsiString(),
        columnWidth = columnWidth,
        paddingCharacter = paddingCharacter,
        paddingColumns = paddingWidth,
    ).toString()
