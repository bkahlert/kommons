package com.bkahlert.kommons.text

import com.bkahlert.kommons.LineSeparators.lines
import com.bkahlert.kommons.tail

/**
 * If this character sequence is made up of multiple lines of text,
 * this property contains all but the first.
 *
 * @see CharSequence.firstLine
 */
public val CharSequence.otherLines: List<String>
    get() = lines().tail
