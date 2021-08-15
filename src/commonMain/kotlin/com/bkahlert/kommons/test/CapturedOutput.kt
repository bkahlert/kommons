package com.bkahlert.kommons.test

import com.bkahlert.kommons.collections.withNegativeIndices
import com.bkahlert.kommons.text.LineSeparators.lines

/**
 * Captured output providing access to the [out], [err]
 * and both through [all].
 */
public interface CapturedOutput {
    public val all: String
    public val out: String
    public val err: String

    public val allLines: List<String> get() = all.lines().dropLastWhile { it.isBlank() }.withNegativeIndices()
    public val outLines: List<String> get() = out.lines().dropLastWhile { it.isBlank() }.withNegativeIndices()
    public val errLines: List<String> get() = err.lines().dropLastWhile { it.isBlank() }.withNegativeIndices()
}
