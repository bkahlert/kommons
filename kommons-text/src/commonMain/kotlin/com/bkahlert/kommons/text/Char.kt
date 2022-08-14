package com.bkahlert.kommons.text

import kotlin.Char

/** Text unit for texts consisting of [Char] chunks. */
public object Char : ChunkingTextUnit<Char>("character") {
    override fun chunk(text: CharSequence): BreakIterator = text.indices.map { it + 1 }.iterator()
    override fun transform(text: CharSequence, range: IntRange): kotlin.Char = text[range]

    /** Returns a new [TextLength] equal to this number of characters. */
    public inline val Int.characters: TextLength<Char> get() = lengthOf(this)
}
