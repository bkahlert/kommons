package com.bkahlert.kommons.text

import com.bkahlert.kommons.TextLength.Companion.codePoints
import com.bkahlert.kommons.Unicode
import com.bkahlert.kommons.codePointCount
import com.bkahlert.kommons.spaced
import com.bkahlert.kommons.truncate
import com.bkahlert.kommons.truncateEnd
import com.bkahlert.kommons.truncateStart


/**
 * Returns this character sequence truncated from the center to [maxCodePoints] including the [marker].
 */
public fun CharSequence.truncateOld(maxCodePoints: Int = 15, marker: String = Unicode.ELLIPSIS.spaced): CharSequence =
    if (safeString.codePointCount() > maxCodePoints) toString().truncate(maxCodePoints.codePoints, marker) else this

/**
 * Returns this character sequence truncated from the start to [maxCodePoints] including the [marker].
 */
public fun CharSequence.truncateStartOld(maxCodePoints: Int = 15, marker: String = Unicode.ELLIPSIS.spaced): CharSequence =
    if (safeString.codePointCount() > maxCodePoints) toString().truncateStart(maxCodePoints.codePoints, marker) else this

/**
 * Returns this character sequence truncated from the end to [maxCodePoints] including the [marker].
 */
public fun CharSequence.truncateEndOld(maxCodePoints: Int = 15, marker: String = Unicode.ELLIPSIS.spaced): CharSequence =
    if (length > 2 * (maxCodePoints + 1) || length > maxCodePoints) toString().truncateEnd(maxCodePoints.codePoints, marker) else this


/**
 * Returns this character sequence truncated to [length] and if necessary padded from the start.
 */
public fun CharSequence.padStartFixedLength(
    length: Int = 15,
    marker: String = Unicode.ELLIPSIS.spaced,
    padChar: Char = ' ',
): String = toString().truncate(length.codePoints, marker).padStart(length, padChar)

/**
 * Returns this character sequence truncated to [length] and if necessary padded from the end.
 */
public fun CharSequence.padEndFixedLength(
    length: Int = 15,
    marker: String = Unicode.ELLIPSIS.spaced,
    padChar: Char = ' ',
): String = toString().truncate(length.codePoints, marker).padEnd(length, padChar)
