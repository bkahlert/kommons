package koodies.text

import koodies.collections.head
import koodies.text.LineSeparators.lines

/**
 * If this character sequence is made up of multiple lines of text
 * this property contains the first one. Otherwise it simply contains
 * this character sequence.
 *
 * @see CharSequence.otherLines
 */
public val CharSequence.firstLine: String
    get() = lines().head
