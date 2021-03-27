package koodies.text

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
 * Returns this [CharSequence] with a random suffix of one dash and four alpha-numeric characters.
 */
public fun CharSequence.withRandomSuffix(): String {
    if (randomSuffixMatcher.matches(this)) return this.toString()
    return "$this--${randomString(length = randomSuffixLength, allowedCharacters = CharRanges.Alphanumeric)}"
}

private const val randomSuffixLength = 4
private val randomSuffixMatcher: Regex = Regex(".*--[0-9a-zA-Z]{$randomSuffixLength}\$")


/**
 * If this char sequence starts with the given [prefix], returns a new char sequence
 * with the prefix removed. Otherwise, returns a new char sequence with the same characters.
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
 * If this char sequence ends with the given [suffix], returns a new char sequence
 * with the suffix removed. Otherwise, returns a new char sequence with the same characters.
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
 * Wraps this char sequence with the given [prefix] and [suffix].
 * - a line feed (`\n`) is added to separate the [prefix] from this char sequence
 * - a line feed (`\n`) is added to separate this char sequence from the [suffix].
 * - the indentation of [prefix] and [suffix] is trimmed using [String.trimIndent]
 */
public fun CharSequence.wrapMultiline(prefix: CharSequence, suffix: CharSequence): String =
    "${prefix.toString().trimIndent()}$LF$this$LF${suffix.toString().trimIndent()}"

/**
 * Returns `this` char sequence if it [isNotEmpty] or `null`, if it is.
 */
public fun CharSequence.takeUnlessEmpty(): CharSequence? = takeUnless { it.isEmpty() }

/**
 * Returns `this` char sequence if it [isNotEmpty] or `null`, if it is.
 */
public fun String.takeUnlessEmpty(): String? = takeUnless { it.isEmpty() }


/**
 * Returns `this` char sequence if it [isNotBlank] or `null`, if it is.
 */
public fun CharSequence.takeUnlessBlank(): CharSequence? = takeUnless { it.isBlank() }

/**
 * Returns `this` char sequence if it [isNotBlank] or `null`, if it is.
 */
public fun String.takeUnlessBlank(): String? = takeUnless { it.isBlank() }
