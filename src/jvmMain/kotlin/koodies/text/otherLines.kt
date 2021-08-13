package koodies.text

import koodies.collections.tail
import koodies.text.LineSeparators.lines

/**
 * If this character sequence is made up of multiple lines of text,
 * this property contains all but the first.
 *
 * @see CharSequence.firstLine
 */
public val CharSequence.otherLines: List<String>
    get() = lines().tail
