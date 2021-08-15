package com.bkahlert.kommons.text

import com.bkahlert.kommons.collections.tail
import com.bkahlert.kommons.text.LineSeparators.lines

/**
 * If this character sequence is made up of multiple lines of text,
 * this property contains all but the first.
 *
 * @see CharSequence.firstLine
 */
public val CharSequence.otherLines: List<String>
    get() = lines().tail
