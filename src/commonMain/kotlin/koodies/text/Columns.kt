package koodies.text

import koodies.collections.zipWithDefault
import koodies.text.AnsiString.Companion.toAnsiString
import koodies.text.LineSeparators.LF
import koodies.text.LineSeparators.lineSequence
import koodies.text.LineSeparators.wrapLines as lineSepWrapLines

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
        .joinLinesToString(LF)
        .toAnsiString()

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
    columnWidth: Int = toAnsiString().maxColumns(),
    paddingCharacter: Char = ' ',
    paddingWidth: Int = 5,
): String =
    toAnsiString().addColumn(
        column = column.toAnsiString(),
        columnWidth = columnWidth,
        paddingCharacter = paddingCharacter,
        paddingColumns = paddingWidth,
    ).toString()

/**
 * Returns a string that consists of the given [columns]
 * formatted next to each other. Each element specifies
 * the text to be formatted and the number of columns
 * to be used.
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
 * and
 * ```
 *    ɑ
 *   β
 *  ɣ
 * ```
 * will result in
 * ```
 * Line 1       Line 1          ɑ
 * Line 1.1     Line a.b       β
 * Line 2       Line c        ɣ
 *              Line d
 * ```
 */
public fun formatColumns(
    vararg columns: Pair<CharSequence?, Int>,
    paddingCharacter: Char = ' ',
    paddingColumns: Int = 5,
    wrapLines: CharSequence?.(Int) -> CharSequence = { lineSepWrapLines(it) },
): CharSequence {
    val columnsWithMaxColumns = columns.map { (text, maxColumns) ->
        when {
            text == null -> AnsiString.EMPTY
            text.columns <= maxColumns -> text.toAnsiString()
            else -> text.wrapLines(maxColumns).toAnsiString()
        } to maxColumns
    }
    return when (columnsWithMaxColumns.size) {
        0 -> AnsiString.EMPTY
        1 -> columnsWithMaxColumns.single().let { (text, maxColumns) -> text.wrapLines(maxColumns).toAnsiString() }
        else -> {
            var linedUp = AnsiString.EMPTY
            var summed = columns.first().second
            columnsWithMaxColumns.windowed(2, 1, false) { window ->
                val (leftText, _) = window.first()
                val (rightText, rightMaxColumns) = window.last()
                linedUp = (linedUp.takeUnless { it.isEmpty() } ?: leftText)
                    .addColumn(rightText, summed, paddingCharacter, paddingColumns)
                    .also { summed += rightMaxColumns + paddingColumns }
            }
            linedUp
        }
    }
}
