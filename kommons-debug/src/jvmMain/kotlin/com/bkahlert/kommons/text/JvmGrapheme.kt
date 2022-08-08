package com.bkahlert.kommons.text

import com.ibm.icu.text.BreakIterator as IcuBreakIterator

/** An [Iterator] that iterates [Grapheme] positions. */
public actual class GraphemeBreakIterator actual constructor(
    text: CharSequence,
) : BreakIterator by (text.asGraphemeIndicesSequence().iterator())

/** Returns a sequence yielding the [Grapheme] instances this string consists of. */
private fun CharSequence.asGraphemeIndicesSequence(): Sequence<Int> {
    val iterator = IcuBreakIterator.getCharacterInstance().also { it.setText(this) }
    return sequence {
        while (true) {
            val breakIndex = iterator.next()
            if (breakIndex == IcuBreakIterator.DONE) break
            if (breakIndex > this@asGraphemeIndicesSequence.length) {
                yield(this@asGraphemeIndicesSequence.length)
                break
            }
            yield(breakIndex)
        }
    }
}
