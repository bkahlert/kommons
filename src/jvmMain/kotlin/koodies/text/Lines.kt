package koodies.text

import koodies.terminal.AnsiCode.Companion.removeEscapeSequences
import koodies.text.LineSeparators.lineSequence

/**
 * Splits this char sequence into its lines and returns the length
 * of the longest of them.
 */
public fun <T : CharSequence> T.maxLength(): Int =
    lineSequence().maxLength<CharSequence>()

/**
 * Returns the length of the longest char sequence.
 */
public fun <T : CharSequence> Iterable<T>.maxLength(): Int =
    asSequence().maxLength()

/**
 * Returns the length of the longest char sequence.
 */
public fun <T : CharSequence> Sequence<T>.maxLength(): Int =
    maxOf { it.removeEscapeSequences().length }

