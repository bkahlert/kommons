package com.bkahlert.kommons_deprecated.text

import com.bkahlert.kommons.ansiRemoved
import com.bkahlert.kommons.text.LineSeparators.lineSequence

/**
 * Splits this character sequence into its lines and returns the length
 * of the longest of them.
 */
public fun CharSequence.maxLength(): Int =
    lineSequence().maxLength()

/**
 * Returns the length of the longest character sequence.
 */
public fun Iterable<CharSequence>.maxLength(): Int =
    asSequence().maxLength()

/**
 * Returns the length of the longest character sequence.
 */
public fun Sequence<CharSequence>.maxLength(): Int =
    maxOf { it.ansiRemoved.length }
