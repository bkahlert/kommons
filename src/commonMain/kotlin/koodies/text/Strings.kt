package koodies.text

import koodies.Exceptions
import koodies.text.ANSI.ansiRemoved
import koodies.text.LineSeparators.LF
import kotlin.random.Random

public object CharRanges {
    public val Numeric: CharRange = ('0'..'9')
    public val LowerCaseAtoZ: CharRange = ('a'..'z')
    public val UpperCaseAtoZ: CharRange = ('A'..'Z')

    public val Alphanumeric: CharArray = (Numeric + LowerCaseAtoZ + UpperCaseAtoZ).toCharArray()
    public val UpperCaseAlphanumeric: CharArray = (Numeric + UpperCaseAtoZ).toCharArray()
}

/**
 * Returns the length of this character sequence.
 * @param ignoreAnsi whether to ignore [ANSI escape codes](https://en.wikipedia.org/wiki/ANSI_escape_code). Ignored by default.
 * @return number of characters
 */
public fun CharSequence.length(ignoreAnsi: Boolean = true): Int =
    if (ignoreAnsi) ansiRemoved.length else length

/**
 * Creates a random string of the specified [length] made up of the specified [allowedCharacters].
 */
public fun randomString(length: Int = 16, allowedCharacters: CharArray = CharRanges.Alphanumeric): String =
    StringBuilder().apply {
        repeat(length) { append(allowedCharacters[Random.nextInt(0, allowedCharacters.size)]) }
    }.toString()

/**
 * Returns this [CharSequence] with the [prefix] prepended if it is not already there.
 */
public fun CharSequence.withPrefix(prefix: String): String =
    if (startsWith(prefix)) toString() else prefix + toString()

/**
 * Returns this [CharSequence] with a random suffix of two dashes dash and four alpha-numeric characters.
 */
public fun CharSequence.withRandomSuffix(): String {
    if (randomSuffixMatcher.matches(this)) return this.toString()
    return "$this--${randomString(length = randomSuffixLength, allowedCharacters = CharRanges.Alphanumeric)}"
}

private const val randomSuffixLength = 4
private val randomSuffixMatcher: Regex = Regex(".*--[0-9a-zA-Z]{$randomSuffixLength}\$")


/**
 * If this character sequence starts with the given [prefix], returns a new character sequence
 * with the prefix removed. Otherwise, returns a new character sequence with the same characters.
 */
public fun CharSequence.withoutPrefix(prefix: CharSequence, ignoreCase: Boolean = false): CharSequence {
    if (startsWith(prefix, ignoreCase = ignoreCase)) {
        return subSequence(prefix.length, length)
    }
    return subSequence(0, length)
}

/**
 * If this string starts with the given [prefix], returns a copy of this string
 * with the prefix removed. Otherwise, returns this string.
 */
public fun String.withoutPrefix(prefix: CharSequence, ignoreCase: Boolean = false): String {
    if (startsWith(prefix, ignoreCase = ignoreCase)) {
        return substring(prefix.length)
    }
    return this
}


/**
 * Returns this [CharSequence] with the [suffix] appended if it is not already there.
 */
public fun CharSequence.withSuffix(suffix: String): String =
    if (endsWith(suffix)) toString() else toString() + suffix


/**
 * If this character sequence ends with the given [suffix], returns a new character sequence
 * with the suffix removed. Otherwise, returns a new character sequence with the same characters.
 */
public fun CharSequence.withoutSuffix(suffix: CharSequence, ignoreCase: Boolean = false): CharSequence {
    if (endsWith(suffix, ignoreCase = ignoreCase)) {
        return subSequence(0, length - suffix.length)
    }
    return subSequence(0, length)
}

/**
 * If this string ends with the given [suffix], returns a copy of this string
 * with the suffix removed. Otherwise, returns this string.
 */
public fun String.withoutSuffix(suffix: CharSequence, ignoreCase: Boolean = false): String {
    if (endsWith(suffix, ignoreCase = ignoreCase)) {
        return substring(0, length - suffix.length)
    }
    return this
}

/**
 * Removes from a string both the given [Pair.first] and [Pair.second] if and only if
 * it starts with the [Pair.first] and ends with the [Pair.second].
 * Otherwise returns this string unchanged.
 */
public fun String.removeSurrounding(prefixSuffixPair: Pair<CharSequence, CharSequence>): String {
    val (prefix, suffix) = prefixSuffixPair
    if ((length >= prefix.length + suffix.length) && startsWith(prefix) && endsWith(suffix)) {
        return substring(prefix.length, length - suffix.length)
    }
    return this
}


/**
 * Returns a character sequence with content of this character sequence padded at the beginning
 * to the specified [length] with the specified character or space.
 *
 * @param length the desired string length.
 * @param padChar the character to pad string with, if it has length less than the [length] specified. Space is used by default.
 * @param ignoreAnsi whether to ignore [ANSI escape codes](https://en.wikipedia.org/wiki/ANSI_escape_code). Ignored by default.
 * @return Returns a character sequence of length at least [length] consisting of `this` character sequence prepended with [padChar] as many times
 * as are necessary to reach that length.
 */
public fun CharSequence.padStart(length: Int, padChar: Char = ' ', ignoreAnsi: Boolean = true): CharSequence {
    if (length < 0)
        throw IllegalArgumentException("Desired length $length is less than zero.")
    if (length <= this.length(ignoreAnsi))
        return this.subSequence(0, this.length(ignoreAnsi))

    val sb = StringBuilder(length)
    for (i in 1..(length - this.length(ignoreAnsi)))
        sb.append(padChar)
    sb.append(this)
    return sb
}

/**
 * Pads the string to the specified [length] at the beginning with the specified character or space.
 *
 * @param length the desired string length.
 * @param padChar the character to pad string with, if it has length less than the [length] specified. Space is used by default.
 * @param ignoreAnsi whether to ignore [ANSI escape codes](https://en.wikipedia.org/wiki/ANSI_escape_code). Ignored by default.
 * @return Returns a string of length at least [length] consisting of `this` string prepended with [padChar] as many times
 * as are necessary to reach that length.
 */
public fun String.padStart(length: Int, padChar: Char = ' ', ignoreAnsi: Boolean = true): String =
    (this as CharSequence).padStart(length, padChar, ignoreAnsi).toString()

/**
 * Returns a character sequence with content of this character sequence padded at the end
 * to the specified [length] with the specified character or space.
 *
 * @param length the desired string length.
 * @param padChar the character to pad string with, if it has length less than the [length] specified. Space is used by default.
 * @param ignoreAnsi whether to ignore [ANSI escape codes](https://en.wikipedia.org/wiki/ANSI_escape_code). Ignored by default.
 * @return Returns a character sequence of length at least [length] consisting of `this` character sequence appended with [padChar] as many times
 * as are necessary to reach that length.
 */
public fun CharSequence.padEnd(length: Int, padChar: Char = ' ', ignoreAnsi: Boolean = true): CharSequence {
    if (length < 0)
        throw IllegalArgumentException("Desired length $length is less than zero.")
    if (length <= this.length(ignoreAnsi))
        return this.subSequence(0, this.length(ignoreAnsi))

    val sb = StringBuilder(length)
    sb.append(this)
    for (i in 1..(length - this.length(ignoreAnsi)))
        sb.append(padChar)
    return sb
}

/**
 * Pads the string to the specified [length] at the end with the specified character or space.
 *
 * @param length the desired string length.
 * @param padChar the character to pad string with, if it has length less than the [length] specified. Space is used by default.
 * @param ignoreAnsi whether to ignore [ANSI escape codes](https://en.wikipedia.org/wiki/ANSI_escape_code). Ignored by default.
 * @return Returns a string of length at least [length] consisting of `this` string appended with [padChar] as many times
 * as are necessary to reach that length.
 */
public fun String.padEnd(length: Int, padChar: Char = ' ', ignoreAnsi: Boolean = true): String =
    (this as CharSequence).padEnd(length, padChar, ignoreAnsi).toString()

/**
 * Wraps this character sequence with the given [prefix] and [suffix].
 * - a line feed (`\n`) is added to separate the [prefix] from this character sequence
 * - a line feed (`\n`) is added to separate this character sequence from the [suffix].
 * - the indentation of [prefix] and [suffix] is trimmed using [String.trimIndent]
 */
public fun CharSequence.wrapMultiline(prefix: CharSequence, suffix: CharSequence): String =
    "${prefix.toString().trimIndent()}$LF$this$LF${suffix.toString().trimIndent()}"

/**
 * Returns `this` character sequence if it [isNotEmpty] or `null`, if it is.
 */
public fun CharSequence.takeIfNotEmpty(): CharSequence? = takeIf { it.isNotEmpty() }

/**
 * Returns `this` character sequence if it [isNotEmpty] or `null`, if it is.
 */
public fun String.takeIfNotEmpty(): String? = takeIf { it.isNotEmpty() }


/**
 * Returns `this` character sequence if it [isNotBlank] or `null`, if it is.
 */
public fun CharSequence.takeIfNotBlank(): CharSequence? = takeIf { it.isNotBlank() }

/**
 * Returns `this` character sequence if it [isNotBlank] or `null`, if it is.
 */
public fun String.takeIfNotBlank(): String? = takeIf { it.isNotBlank() }

/**
 * Returns `this` character sequence if it [isNotEmpty] or `null`, if it is.
 */
public fun CharSequence.takeUnlessEmpty(): CharSequence? = takeUnless { it.isEmpty() }

/**
 * Returns `this` character sequence if it [isNotEmpty] or `null`, if it is.
 */
public fun String.takeUnlessEmpty(): String? = takeUnless { it.isEmpty() }


/**
 * Returns `this` character sequence if it [isNotBlank] or `null`, if it is.
 */
public fun CharSequence.takeUnlessBlank(): CharSequence? = takeUnless { it.isBlank() }

/**
 * Returns `this` character sequence if it [isNotBlank] or `null`, if it is.
 */
public fun String.takeUnlessBlank(): String? = takeUnless { it.isBlank() }

/**
 * Splits this character sequence into at most [limit] columns
 * and returns the two columns [index1] and [index2] mapped
 * using [transform]. If not enough columns exist, `null` is returned.
 *
 * ***Note:** Columns start counting at 1*.
 *
 * @param index1 index of the first column
 * @param index2 index of the second column
 * @param delimiter string to be used to split this character sequence into columns
 * @param limit maximum number of columns to split into, before selecting the ones to process (default: no limit)
 * @param removeAnsi whether to remove ANSI escape sequences (default: true)
 * @param transform function used to transform the two columns with indices [index1] and [index2]
 *
 * @return transform applied to columns specified by [index1] and [index2]; `null` if not enough columns exist
 */
public inline fun <T> CharSequence.mapColumnsOrNull(
    index1: Int = 1,
    index2: Int = 2,
    delimiter: String = "\t",
    limit: Int? = null,
    removeAnsi: Boolean = true,
    transform: (String, String) -> T,
): T? {
    listOf(index1, index2).filter { it <= 0 }.joinToString(" and ") { "index $it" }.takeIfNotBlank()
        ?.let { throw Exceptions.IAE("$it must be greater than or equal to 1") }
    require(limit?.let { it >= 2 } != false) { "Limit $limit must be greater than or equal to 2." }
    return (takeUnless { removeAnsi } ?: ansiRemoved)
        .split(delimiter, limit = limit ?: 0).takeIf { it.size >= maxOf(index1, index2) }
        ?.let { transform(it[index1 - 1], it[index2 - 1]) }
}

/**
 * Splits this character sequence into at most [limit] columns
 * and returns the two columns [index1] and [index2] mapped
 * using [transform].
 *
 * ***Note:** Columns start counting at 1*.
 *
 * @param index1 index of the first column
 * @param index2 index of the second column
 * @param delimiter string to be used to split this character sequence into columns
 * @param limit maximum number of columns to split into, before selecting the ones to process (default: no limit)
 * @param removeAnsi whether to remove ANSI escape sequences (default: true)
 * @param transform function used to transform the two columns with indices [index1] and [index2]
 *
 * @return transform applied to columns specified by [index1] and [index2]
 * @throws NoSuchElementException if not enough columns exist
 *
 */
public inline fun <T> CharSequence.mapColumns(
    index1: Int = 1,
    index2: Int = 2,
    delimiter: String = "\t",
    limit: Int? = null,
    removeAnsi: Boolean = true,
    transform: (String, String) -> T,
): T = mapColumnsOrNull(index1, index2, delimiter, limit, removeAnsi, transform) ?: throw NoSuchElementException("No enough columns exist.")

/**
 * Splits this character sequence into at most [limit] columns
 * and returns the three columns [index1], [index2] and [index3] mapped
 * using [transform]. If not enough columns exist, `null` is returned.
 *
 * ***Note:** Columns start counting at 1*.
 *
 * @param index1 index of the first column
 * @param index2 index of the second column
 * @param index3 index of the third column
 * @param delimiter string to be used to split this character sequence into columns
 * @param limit maximum number of columns to split into, before selecting the ones to process (default: no limit)
 * @param removeAnsi whether to remove ANSI escape sequences (default: true)
 * @param transform function used to transform the two columns with indices [index1], [index2] and [index3]
 *
 * @return transform applied to columns specified by [index1], [index2] and [index3]; `null` if not enough columns exist
 */
public inline fun <T> CharSequence.mapColumnsOrNull(
    index1: Int = 1,
    index2: Int = 2,
    index3: Int = 3,
    delimiter: String = "\t",
    limit: Int? = null,
    removeAnsi: Boolean = true,
    transform: (String, String, String) -> T,
): T? {
    listOf(index1, index2, index3).filter { it <= 0 }.joinToString(" and ") { "index $it" }.takeIfNotBlank()
        ?.let { throw Exceptions.IAE("$it must be greater than or equal to 1") }
    require(limit?.let { it >= 3 } != false) { "Limit $limit must be greater than or equal to 3." }
    return (takeUnless { removeAnsi } ?: ansiRemoved)
        .split(delimiter, limit = limit ?: 0).takeIf { it.size >= maxOf(index1, index2, index3) }
        ?.let { transform(it[index1 - 1], it[index2 - 1], it[index3 - 1]) }
}

/**
 * Splits this character sequence into at most [limit] columns
 * and returns the three columns [index1], [index2] and [index3] mapped
 * using [transform].
 *
 * ***Note:** Columns start counting at 1*.
 *
 * @param index1 index of the first column
 * @param index2 index of the second column
 * @param index3 index of the third column
 * @param delimiter string to be used to split this character sequence into columns
 * @param limit maximum number of columns to split into, before selecting the ones to process (default: no limit)
 * @param removeAnsi whether to remove ANSI escape sequences (default: true)
 * @param transform function used to transform the two columns with indices [index1], [index2] and [index3]
 *
 * @return transform applied to columns specified by [index1], [index2] and [index3]
 * @throws NoSuchElementException if not enough columns exist
 */
public inline fun <T> CharSequence.mapColumns(
    index1: Int = 1,
    index2: Int = 2,
    index3: Int = 3,
    delimiter: String = "\t",
    limit: Int? = null,
    removeAnsi: Boolean = true,
    transform: (String, String, String) -> T,
): T = mapColumnsOrNull(index1, index2, index3, delimiter, limit, removeAnsi, transform) ?: throw NoSuchElementException("No enough columns exist.")
