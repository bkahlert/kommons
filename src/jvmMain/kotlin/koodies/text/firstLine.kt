package koodies.text

import koodies.collections.head
import koodies.text.LineSeparators.lines

/**
 * If this char sequence is made up of multiple lines of text
 * this property contains the first one. Otherwise it simply contains
 * this char sequence.
 *
 * @see CharSequence.otherLines
 */
val CharSequence.firstLine: String
    get() = lines().head
