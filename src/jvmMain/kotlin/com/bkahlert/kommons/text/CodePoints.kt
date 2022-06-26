package com.bkahlert.kommons.text

import com.bkahlert.kommons.CodePoint
import com.bkahlert.kommons.asCodePointSequence


public operator fun String.minus(amount: Int): String =
    asCodePointSequence().map { CodePoint(it.index - amount) }.joinToString("")
