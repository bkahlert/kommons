package com.bkahlert.kommons.text

import com.ibm.icu.text.BreakIterator as IcuBreakIterator

/** An [Iterator] that iterates [Grapheme] positions. */
public actual class GraphemeBreakIterator actual constructor(
    text: CharSequence,
) : BreakIterator, AbstractIterator<Int>() {

    private val iterator: IcuBreakIterator = IcuBreakIterator.getCharacterInstance()

    init {
        iterator.setText(text)
    }

    override fun computeNext() {
        when (val breakIndex = iterator.next()) {
            IcuBreakIterator.DONE -> done()
            else -> setNext(breakIndex)
        }
    }
}
