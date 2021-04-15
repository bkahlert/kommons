package koodies.text

import koodies.text.AnsiString
import koodies.text.AnsiString.Companion.asAnsiString
import koodies.text.LineSeparators.lineSequence

/**
 * Returns a sequence of lines of which none is longer than [maxLineLength].
 */
public fun CharSequence.linesOfLengthSequence(maxLineLength: Int, ignoreTrailingSeparator: Boolean = false): Sequence<CharSequence> {
    val ansiString = this is AnsiString
    val lines = lineSequence(ignoreTrailingSeparator = ignoreTrailingSeparator)
    return lines.flatMap { line: String ->
        if (ansiString) {
            val seq: Sequence<AnsiString> = line.asAnsiString().chunkedSequence(maxLineLength)
            if (ignoreTrailingSeparator) seq
            else seq.iterator().run { if (!hasNext()) sequenceOf(AnsiString.EMPTY) else asSequence() }
        } else {
            val seq = line.chunkedSequence(maxLineLength)
            if (ignoreTrailingSeparator) seq
            else seq.iterator().run { if (!hasNext()) sequenceOf("") else asSequence() }
        }
    }
}

/**
 * Returns a list of lines of which none is longer than [maxLineLength].
 */
public fun CharSequence.linesOfLength(maxLineLength: Int, ignoreTrailingSeparator: Boolean = false): List<CharSequence> =
    linesOfLengthSequence(maxLineLength, ignoreTrailingSeparator).toList()
