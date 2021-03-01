package koodies.text

import kotlin.random.Random

object CharRanges {
    val Numeric: CharRange = ('0'..'9')
    val LowerCaseAtoZ: CharRange = ('a'..'z')
    val UpperCaseAtoZ: CharRange = ('A'..'Z')

    val Alphanumeric: CharArray = (Numeric + LowerCaseAtoZ + UpperCaseAtoZ).toCharArray()
    val UpperCaseAlphanumeric: CharArray = (Numeric + UpperCaseAtoZ).toCharArray()
}


/**
 * Creates a random string of the specified [length] made up of the specified [allowedCharacters].
 */
fun randomString(length: Int = 16, allowedCharacters: CharArray = CharRanges.Alphanumeric): String =
    StringBuilder().apply {
        repeat(length) { append(allowedCharacters[Random.nextInt(0, allowedCharacters.size)]) }
    }.toString()


/**
 * Returns this [CharSequence] with the [prefix] prepended if it is not already there.
 */
fun CharSequence.withPrefix(prefix: String): String =
    if (startsWith(prefix)) toString() else prefix + toString()

/**
 * Returns this [CharSequence] with a random suffix of one dash and four alpha-numeric characters.
 */
fun CharSequence.withRandomSuffix(): String {
    if (randomSuffixMatcher.matches(this)) return this.toString()
    return "$this-${randomString(length = randomSuffixLength, allowedCharacters = CharRanges.Alphanumeric)}"
}

private const val randomSuffixLength = 4
private val randomSuffixMatcher: Regex = Regex(".*-[0-9a-zA-Z]{$randomSuffixLength}\$")


/**
 * If this char sequence starts with the given [prefix], returns a new char sequence
 * with the prefix removed. Otherwise, returns a new char sequence with the same characters.
 */
fun CharSequence.withoutPrefix(prefix: CharSequence, ignoreCase: Boolean = false): CharSequence {
    if (startsWith(prefix, ignoreCase = ignoreCase)) {
        return subSequence(prefix.length, length)
    }
    return subSequence(0, length)
}

/**
 * If this string starts with the given [prefix], returns a copy of this string
 * with the prefix removed. Otherwise, returns this string.
 */
fun String.withoutPrefix(prefix: CharSequence, ignoreCase: Boolean = false): String {
    if (startsWith(prefix, ignoreCase = ignoreCase)) {
        return substring(prefix.length)
    }
    return this
}


/**
 * Returns this [CharSequence] with the [suffix] appended if it is not already there.
 */
fun CharSequence.withSuffix(suffix: String): String =
    if (endsWith(suffix)) toString() else toString() + suffix


/**
 * If this char sequence ends with the given [suffix], returns a new char sequence
 * with the suffix removed. Otherwise, returns a new char sequence with the same characters.
 */
fun CharSequence.withoutSuffix(suffix: CharSequence, ignoreCase: Boolean = false): CharSequence {
    if (endsWith(suffix, ignoreCase = ignoreCase)) {
        return subSequence(0, length - suffix.length)
    }
    return subSequence(0, length)
}

/**
 * If this string ends with the given [suffix], returns a copy of this string
 * with the suffix removed. Otherwise, returns this string.
 */
fun String.withoutSuffix(suffix: CharSequence, ignoreCase: Boolean = false): String {
    if (endsWith(suffix, ignoreCase = ignoreCase)) {
        return substring(0, length - suffix.length)
    }
    return this
}
