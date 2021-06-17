package koodies

import koodies.text.CodePoint
import koodies.text.asCodePointSequence
import koodies.text.decapitalize
import koodies.text.md5

/**
 * Creates a base name suited to be used as an ID for various programs
 * or as a filename. In contrast to a random string the returned base name
 * will reflect `this` string as good as possible.
 *
 * The resulting string will:
 * - have at least [minLength] characters
 * - all possibly invalid characters replaced
 * - start with a letter
 * - always be the same for the same input
 */
public fun CharSequence?.toBaseName(minLength: Int = 8): String {
    var firstCharReplaced = false

    val sanitized = StringBuilder().also {
        (this ?: "").asCodePointSequence().withIndex().joinTo(it, "") { (index: Int, codePoint: CodePoint) ->
            if (index == 0 && !codePoint.isAtoz) {
                firstCharReplaced = true
                when (codePoint.char) {
                    '0' -> "O"
                    '1' -> "I"
                    '2' -> "Z"
                    '3' -> "B"
                    '4' -> "R"
                    '5' -> "P"
                    '6' -> "G"
                    '7' -> "Z"
                    '8' -> "O"
                    '9' -> "Y"
                    else -> "X"
                }
            } else when {
                codePoint.isAsciiAlphanumeric -> codePoint.string
                "._-".contains(codePoint.string) -> codePoint.string
                codePoint.isWhitespace -> "-"
                else -> "_"
            }
        }
    }

    val decapitalize = firstCharReplaced && sanitized.drop(1).hasMoreLowerCaseLetters()

    return sanitized
        .idempotentFillUp(minLength)
        .run { if (decapitalize) decapitalize().toString() else toString() }
}

private fun CharSequence.idempotentFillUp(target: Int): CharSequence {
    if (target <= length) return this
    return StringBuilder(this).apply {
        val missing = target - length
        val loopingHash = md5.asIterable().asLoopingIterator()
        for (i in 0 until missing) append(loopingHash.next())
    }
}

private fun CharSequence.hasMoreLowerCaseLetters() =
    asSequence()
        .filter { it.isLetter() }
        .partition { it.isUpperCase() }
        .let { (upperCaseCount, lowerCaseCount) -> lowerCaseCount.size > upperCaseCount.size }

private inline fun <reified T> Iterable<T>.asLoopingIterator(): Iterator<T> {
    var iterator: Iterator<T> = iterator()
    return object : Iterator<T> {
        override fun hasNext(): Boolean = true
        override fun next(): T {
            if (!iterator.hasNext()) iterator = this@asLoopingIterator.iterator()
            return iterator.next()
        }
    }
}
