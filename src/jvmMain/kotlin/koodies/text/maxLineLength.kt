package koodies.text

import koodies.terminal.AnsiCode.Companion.removeEscapeSequences
import koodies.text.LineSeparators.lineSequence

/**
 * Splits this char sequence into its lines and returns the length
 * of the longest of them.
 */
fun <T : CharSequence> T.maxLineLength(): Int =
    lineSequence().maxLineLength<CharSequence>()

/**
 * Returns the length of the longest char sequence.
 */
fun <T : CharSequence> Iterable<T>.maxLineLength(): Int =
    asSequence().maxLineLength()

/**
 * Returns the length of the longest char sequence.
 */
fun <T : CharSequence> Sequence<T>.maxLineLength(): Int =
    maxOf { it.removeEscapeSequences().length }
