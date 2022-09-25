package com.bkahlert.kommons_deprecated.text

import com.bkahlert.kommons.text.CodePoint.Companion.codePoints
import com.bkahlert.kommons.text.Unicode
import com.bkahlert.kommons.text.codePointCount
import com.bkahlert.kommons.text.spaced
import com.bkahlert.kommons.text.truncate
import com.bkahlert.kommons.text.truncateEnd
import com.bkahlert.kommons.text.truncateStart


/**
 * Returns this character sequence truncated from the center to [maxCodePoints] including the [marker].
 */
public fun CharSequence.truncateOld(maxCodePoints: Int = 15, marker: String = Unicode.ELLIPSIS.spaced): CharSequence =
    if (toString().codePointCount() > maxCodePoints) toString().truncate(maxCodePoints.codePoints, marker) else this

/**
 * Returns this character sequence truncated from the start to [maxCodePoints] including the [marker].
 */
public fun CharSequence.truncateStartOld(maxCodePoints: Int = 15, marker: String = Unicode.ELLIPSIS.spaced): CharSequence =
    if (toString().codePointCount() > maxCodePoints) toString().truncateStart(maxCodePoints.codePoints, marker) else this

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
    padChar: kotlin.Char = ' ',
): String = toString().truncate(length.codePoints, marker).padStart(length, padChar)

/**
 * Returns this character sequence truncated to [length] and if necessary padded from the end.
 */
public fun CharSequence.padEndFixedLength(
    length: Int = 15,
    marker: String = Unicode.ELLIPSIS.spaced,
    padChar: kotlin.Char = ' ',
): String = toString().truncate(length.codePoints, marker).padEnd(length, padChar)
