package koodies.text

import kotlin.streams.asSequence

/**
 * Contains the name of this code point
 */
val CodePoint.unicodeName: String get() = Unicode[codePoint.toLong()]

/**
 * The Unicode name, e.g. `LINE SEPARATOR`
 */
val CodePoint.formattedName: String get() = "❲$unicodeName❳"


/**
 * Contains the character pointed to and represented by a [CharArray].
 */
val CodePoint.chars: CharArray get() = Character.toChars(codePoint)

/**
 * Contains the number of [Char] values needed to represent this code point.
 */
val CodePoint.charCount: Int get() = Character.charCount(codePoint)

/**
 * Determines if this code point is alphanumeric, that is,
 * if it is a [Unicode Letter](https://www.unicode.org/glossary/#letter) or
 * a [Unicode Digit](http://www.unicode.org/glossary/#digits).
 *
 * @return `true` if this code point is a letter or digit
 * @see isLetterOrDigit
 */
val CodePoint.isAlphanumeric: Boolean get() = Character.isLetterOrDigit(codePoint)


operator fun String.minus(amount: Int): String =
    asCodePointSequence().map { it - amount }.joinToString("")

/**
 * Returns a sequence containing the [CodePoint] instances this string consists of.
 */
fun CharSequence.asCodePointSequence(): Sequence<CodePoint> =
    codePoints().mapToObj { CodePoint(it) }.asSequence()

/**
 * Returns a sequence containing the [CodePoint] instances this string consists of.
 */
fun String.asCodePointSequence(): Sequence<CodePoint> =
    (this as CharSequence).asCodePointSequence()

/**
 * Returns a list containing the results of applying the given [transform] function
 * to each [CodePoint] of this string.
 */
fun <R> String.mapCodePoints(transform: (CodePoint) -> R): List<R> =
    asCodePointSequence().map(transform).toList()

