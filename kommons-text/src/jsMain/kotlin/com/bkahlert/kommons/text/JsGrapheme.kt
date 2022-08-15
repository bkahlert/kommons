package com.bkahlert.kommons.text

private val nextGraphemeClusterBreak: (String, Int) -> Int = js("require('@stdlib/string-next-grapheme-cluster-break')").unsafeCast<(String, Int) -> Int>()

/** An [Iterator] that iterates [Grapheme] positions. */
public actual class GraphemeBreakIterator actual constructor(
    text: CharSequence,
) : BreakIterator by (text.toString().asGraphemeIndicesSequence().iterator())

private fun String.asGraphemeIndicesSequence(): Sequence<Int> {
    if (isEmpty()) return emptySequence()
    var index = 0
    return sequence {
        while (true) {
            val breakIndex = nextGraphemeClusterBreak(this@asGraphemeIndicesSequence, index)
            if (breakIndex == -1) {
                yield(this@asGraphemeIndicesSequence.length)
                break
            }
            yield(breakIndex)
            index = breakIndex
        }
    }
}
