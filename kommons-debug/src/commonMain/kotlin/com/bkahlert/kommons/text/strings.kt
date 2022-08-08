package com.bkahlert.kommons.text

import com.bkahlert.kommons.debug.Compression.Always
import com.bkahlert.kommons.debug.Typing.Untyped
import com.bkahlert.kommons.debug.getOrNull
import com.bkahlert.kommons.debug.properties
import com.bkahlert.kommons.debug.render
import com.bkahlert.kommons.debug.renderType
import com.bkahlert.kommons.text.CodePoint.Companion.codePoints
import com.bkahlert.kommons.text.Text.Companion.asText
import com.bkahlert.kommons.text.Text.Companion.mapText
import kotlin.random.Random
import kotlin.reflect.KProperty

// String companion extensions -----------------------------------------------------------------------------------------

private const val EMPTY_STRING: String = ""

/** An empty string. */
public val String.Companion.EMPTY: String get() = EMPTY_STRING


// get -----------------------------------------------------------------------------------------------------------------

/**
 * Returns the character at the specified [range] in this character sequence.
 *
 * @throws [IllegalArgumentException] if the [IntRange.first] isn't equal to [IntRange.last].
 * @throws [IndexOutOfBoundsException] if the [range] is out of bounds of this character sequence.
 */
public operator fun CharSequence.get(range: IntRange): kotlin.Char {
    require(range.first == range.last) { "The requested range $range is not suitable to get a single character." }
    checkBoundsIndexes(length, range.first, range.last + 1)
    return this[range.first]
}


// containsAny ---------------------------------------------------------------------------------------------------------

/**
 * Returns `true` if this character sequence contains any of the specified [others] as a substring.
 *
 * @param ignoreCase `true` to ignore the character case when comparing strings. By default `false`.
 */
public fun <T : CharSequence> CharSequence.containsAny(others: Iterable<T>, ignoreCase: Boolean = false): Boolean =
    others.any { contains(it, ignoreCase = ignoreCase) }

/**
 * Returns `true` if this character sequence contains any of the specified [others] as a substring.
 *
 * @param ignoreCase `true` to ignore the character case when comparing strings. By default `false`.
 */
public fun <T : CharSequence> CharSequence.containsAny(vararg others: T, ignoreCase: Boolean = false): Boolean =
    others.any { contains(it, ignoreCase = ignoreCase) }


// checkBoundsIndex ----------------------------------------------------------------------------------------------------

/**
 * Returns the specified [index] if it in the specified [range].
 * Otherwise, throws an [IndexOutOfBoundsException].
 */
@Suppress("NOTHING_TO_INLINE")
public inline fun checkBoundsIndex(range: IntRange, index: Int): Int {
    if (range.contains(index)) return index
    throw IndexOutOfBoundsException("index out of range: $index")
}

// checkBoundsIndexes --------------------------------------------------------------------------------------------------

/**
 * Returns a new range from [startIndex] until [endIndex] if:
 * 1) [startIndex] is less than [endIndex],
 * 2) [startIndex] is greater than or equal to `0`, and
 * 3) [endIndex] is less than the specified [length].
 *
 * Otherwise, throws an [IndexOutOfBoundsException].
 */
@Suppress("NOTHING_TO_INLINE")
public inline fun checkBoundsIndexes(length: Int, startIndex: Int, endIndex: Int): IntRange {
    if (startIndex in 0..endIndex && endIndex <= length) return startIndex until endIndex
    throw IndexOutOfBoundsException("begin $startIndex, end $endIndex, length $length")
}

/**
 * Returns the specified [range] if:
 * 1) it's not empty,
 * 2) [IntRange.first] is greater than or equal to `0`, and
 * 3) [IntRange.last] is less than the specified [length].
 *
 * Otherwise, throws an [IndexOutOfBoundsException].
 */
@Suppress("NOTHING_TO_INLINE")
public inline fun checkBoundsIndexes(length: Int, range: IntRange): IntRange =
    checkBoundsIndexes(length, range.first, range.last + 1)


// requireNotEmpty -----------------------------------------------------------------------------------------------------

/** Throws an [IllegalArgumentException] if the specified [charSequence] [isEmpty]. */
@Suppress("NOTHING_TO_INLINE")
public inline fun requireNotEmpty(charSequence: CharSequence): CharSequence = charSequence.also { require(it.isNotEmpty()) }

/** Throws an [IllegalArgumentException] if the specified [string] [isEmpty]. */
@Suppress("NOTHING_TO_INLINE")
public inline fun requireNotEmpty(string: String): String = string.also { require(it.isNotEmpty()) }

/** Throws an [IllegalArgumentException] with the result of calling [lazyMessage] if the specified [charSequence] [isEmpty]. */
public inline fun requireNotEmpty(charSequence: CharSequence, lazyMessage: () -> Any): CharSequence =
    charSequence.also { require(it.isNotEmpty(), lazyMessage) }

/** Throws an [IllegalArgumentException] with the result of calling [lazyMessage] if the specified [string] [isEmpty]. */
public inline fun requireNotEmpty(string: String, lazyMessage: () -> Any): String = string.also { require(it.isNotEmpty(), lazyMessage) }


// requireNotBlank -----------------------------------------------------------------------------------------------------

/** Throws an [IllegalArgumentException] if the specified [charSequence] [isBlank]. */
@Suppress("NOTHING_TO_INLINE")
public inline fun requireNotBlank(charSequence: CharSequence): CharSequence = charSequence.also { require(charSequence.isNotBlank()) }

/** Throws an [IllegalArgumentException] if the specified [string] [isBlank]. */
@Suppress("NOTHING_TO_INLINE")
public inline fun requireNotBlank(string: String): String = string.also { require(string.isNotBlank()) }

/** Throws an [IllegalArgumentException] with the result of calling [lazyMessage] if the specified [charSequence] [isBlank]. */
public inline fun requireNotBlank(charSequence: CharSequence, lazyMessage: () -> Any): CharSequence =
    charSequence.also { require(it.isNotBlank(), lazyMessage) }

/** Throws an [IllegalArgumentException] with the result of calling [lazyMessage] if the specified [string] [isBlank]. */
public inline fun requireNotBlank(string: String, lazyMessage: () -> Any): String = string.also { require(it.isNotBlank(), lazyMessage) }


// checkNotEmpty -------------------------------------------------------------------------------------------------------
/** Throws an [IllegalStateException] if the specified [charSequence] [isEmpty]. */
@Suppress("NOTHING_TO_INLINE")
public inline fun checkNotEmpty(charSequence: CharSequence): CharSequence = charSequence.also { check(it.isNotEmpty()) }

/** Throws an [IllegalStateException] if the specified [string] [isEmpty]. */
@Suppress("NOTHING_TO_INLINE")
public inline fun checkNotEmpty(string: String): String = string.also { check(it.isNotEmpty()) }

/** Throws an [IllegalStateException] with the result of calling [lazyMessage] if the specified [charSequence] [isEmpty]. */
public inline fun checkNotEmpty(charSequence: CharSequence, lazyMessage: () -> Any): CharSequence = charSequence.also { check(it.isNotEmpty(), lazyMessage) }

/** Throws an [IllegalStateException] with the result of calling [lazyMessage] if the specified [string] [isEmpty]. */
public inline fun checkNotEmpty(string: String, lazyMessage: () -> Any): String = string.also { check(it.isNotEmpty(), lazyMessage) }


// checkNotBlank -------------------------------------------------------------------------------------------------------

/** Throws an [IllegalStateException] if the specified [charSequence] [isBlank]. */
@Suppress("NOTHING_TO_INLINE")
public inline fun checkNotBlank(charSequence: CharSequence): CharSequence = charSequence.also { check(it.isNotBlank()) }

/** Throws an [IllegalStateException] if the specified [string] [isBlank]. */
@Suppress("NOTHING_TO_INLINE")
public inline fun checkNotBlank(string: String): String = string.also { check(it.isNotBlank()) }

/** Throws an [IllegalStateException] with the result of calling [lazyMessage] if the specified [charSequence] [isBlank]. */
public inline fun checkNotBlank(charSequence: CharSequence, lazyMessage: () -> Any): CharSequence = charSequence.also { check(it.isNotBlank(), lazyMessage) }

/** Throws an [IllegalStateException] with the result of calling [lazyMessage] if the specified [string] [isBlank]. */
public inline fun checkNotBlank(string: String, lazyMessage: () -> Any): String = string.also { check(it.isNotBlank(), lazyMessage) }


// takeIfNotEmpty ------------------------------------------------------------------------------------------------------

/** Returns this character sequence if it [isNotEmpty] or `null`, if it is. */
@Suppress("NOTHING_TO_INLINE")
public inline fun CharSequence.takeIfNotEmpty(): CharSequence? = takeIf { it.isNotEmpty() }

/** Returns this string if it [isNotEmpty] or `null`, if it is. */
@Suppress("NOTHING_TO_INLINE")
public inline fun String.takeIfNotEmpty(): String? = takeIf { it.isNotEmpty() }


// takeIfNotBlank ------------------------------------------------------------------------------------------------------

/** Returns this character sequence if it [isNotBlank] or `null`, if it is. */
@Suppress("NOTHING_TO_INLINE")
public inline fun CharSequence.takeIfNotBlank(): CharSequence? = takeIf { it.isNotBlank() }

/** Returns this string if it [isNotBlank] or `null`, if it is. */
@Suppress("NOTHING_TO_INLINE")
public inline fun String.takeIfNotBlank(): String? = takeIf { it.isNotBlank() }


// takeUnlessEmpty -----------------------------------------------------------------------------------------------------

/** Returns this character sequence if it [isNotEmpty] or `null`, if it is. */
@Suppress("NOTHING_TO_INLINE")
public inline fun CharSequence.takeUnlessEmpty(): CharSequence? = takeUnless { it.isEmpty() }

/** Returns this string if it [isNotEmpty] or `null`, if it is. */
@Suppress("NOTHING_TO_INLINE")
public inline fun String.takeUnlessEmpty(): String? = takeUnless { it.isEmpty() }


// takeUnlessBlank -----------------------------------------------------------------------------------------------------

/** Returns this character sequence if it [isNotBlank] or `null`, if it is. */
@Suppress("NOTHING_TO_INLINE")
public inline fun CharSequence.takeUnlessBlank(): CharSequence? = takeUnless { it.isBlank() }

/** Returns this string if it [isNotBlank] or `null`, if it is. */
@Suppress("NOTHING_TO_INLINE")
public inline fun String.takeUnlessBlank(): String? = takeUnless { it.isBlank() }


// ansiContained / ansiRemoved -----------------------------------------------------------------------------------------

private val ansiPatterns = listOf(
    @Suppress("RegExpRedundantEscape") // otherwise "lone quantifier brackets in JS"
    "\\u001B\\]\\d*;[^\\u001B]*\\u001B\\\\".toRegex(), // OSC (operating system command) escape sequences
    "\\u001B[@-Z\\-_]".toRegex(),            // Fe escape sequences
    "\\u001B[ -/][@-~]".toRegex(),           // 2-byte sequences
    "\\u001B\\[[0-?]*[ -/]*[@-~]".toRegex(), // CSI (control sequence intro) escape sequences
)

private val ansiPattern: Regex = ansiPatterns.joinToString("|") { it.pattern }.toRegex()

/** Whether this character sequence contains [ANSI escape codes](https://en.wikipedia.org/wiki/ANSI_escape_code).*/
public val CharSequence.ansiContained: Boolean
    get() = ansiPattern.containsMatchIn(this)

/** This character sequence with all [ANSI escape codes](https://en.wikipedia.org/wiki/ANSI_escape_code) removed. */
public val CharSequence.ansiRemoved: CharSequence
    get() = if (ansiContained) ansiPattern.replace(this, String.EMPTY) else this

/** This character sequence with all [ANSI escape codes](https://en.wikipedia.org/wiki/ANSI_escape_code) removed. */
public val String.ansiRemoved: String
    get() = if (ansiContained) ansiPattern.replace(this, String.EMPTY) else this


// spaced / startSpaced / endSpaced ------------------------------------------------------------------------------------

/** Returns this char with a space added to each side if not already present, or an empty string otherwise. */
public inline val kotlin.Char?.spaced: String get() = startSpaced.endSpaced

/** Returns this char with a space added to the beginning if not already present, or an empty string otherwise. */
public inline val kotlin.Char?.startSpaced: String get() = this?.run { withPrefix(" ") } ?: String.EMPTY

/** Returns this char with a space added to the end if not already present, or an empty string otherwise. */
public inline val kotlin.Char?.endSpaced: String get() = this?.run { withSuffix(" ") } ?: String.EMPTY

/** Returns this character sequence with a space added to each side if not already present, or an empty string otherwise. */
public inline val CharSequence?.spaced: CharSequence get() = startSpaced.endSpaced

/** Returns this character sequence with a space added to the beginning if not already present, or an empty string otherwise. */
public inline val CharSequence?.startSpaced: CharSequence get() = if (this == null) String.EMPTY else if (isBlank()) this else withPrefix(" ")

/** Returns this character sequence with a space added to the end if not already present, or an empty string otherwise. */
public inline val CharSequence?.endSpaced: CharSequence get() = if (this == null) String.EMPTY else if (isBlank()) this else withSuffix(" ")

/** Returns this string with a space added to each side if not already present, or an empty string otherwise. */
public inline val String?.spaced: String get() = startSpaced.endSpaced

/** Returns this string with a space added to the beginning if not already present, or an empty string otherwise. */
public inline val String?.startSpaced: String get() = if (this == null) String.EMPTY else if (isBlank()) this else withPrefix(" ")

/** Returns this string with a space added to the end if not already present, or an empty string otherwise. */
public inline val String?.endSpaced: String get() = if (this == null) String.EMPTY else if (isBlank()) this else withSuffix(" ")


// truncate ------------------------------------------------------------------------------------------------------------

/**
 * Returns this character sequence truncated from the center to up to 15 code points
 * including the [marker] (default: " … ").
 */
public fun CharSequence.truncate(marker: CharSequence = Unicode.ELLIPSIS.spaced): CharSequence = truncate(15.codePoints, marker)

/**
 * Returns this string truncated from the center to up to 15 code points
 * including the [marker] (default: " … ").
 */
public fun String.truncate(marker: CharSequence = Unicode.ELLIPSIS.spaced): String = truncate(15.codePoints, marker)


/**
 * Returns this character sequence truncated from the center to the specified [length] (default: 15)
 * including the [marker] (default: " … ").
 */
public fun CharSequence.truncate(
    length: TextLength<*>,
    marker: CharSequence = Unicode.ELLIPSIS.spaced,
): CharSequence = mapText(length.unit) {
    it.truncate(length.value, marker.asText(length.unit))
}

/**
 * Returns this string truncated from the center to the specified [length] (default: 15)
 * including the [marker] (default: " … ").
 */
public fun String.truncate(
    length: TextLength<*>,
    marker: CharSequence = Unicode.ELLIPSIS.spaced,
): String = mapText(length.unit) { it.truncate(length.value, marker.asText(length.unit)) }

/**
 * Returns this character sequence truncated from the start to up to 15 code points
 * including the [marker] (default: "… ").
 */
public fun CharSequence.truncateStart(marker: CharSequence = Unicode.ELLIPSIS.endSpaced): CharSequence =
    truncateStart(15.codePoints, marker)

/**
 * Returns this string truncated from the start to up to 15 code points
 * including the [marker] (default: "… ").
 */
public fun String.truncateStart(marker: CharSequence = Unicode.ELLIPSIS.endSpaced): String =
    truncateStart(15.codePoints, marker)

/**
 * Returns this character sequence truncated from the start to the specified [length] (default: 15)
 * including the [marker] (default: "… ").
 */
public fun CharSequence.truncateStart(
    length: TextLength<*>,
    marker: CharSequence = Unicode.ELLIPSIS.endSpaced,
): CharSequence = mapText(length.unit) { it.truncateStart(length.value, marker.asText(length.unit)) }

/**
 * Returns this string truncated from the start to the specified [length] (default: 15)
 * including the [marker] (default: "… ").
 */
public fun String.truncateStart(
    length: TextLength<*>,
    marker: CharSequence = Unicode.ELLIPSIS.endSpaced,
): String = mapText(length.unit) { it.truncateStart(length.value, marker.asText(length.unit)) }

/**
 * Returns this character sequence truncated from the end to up to 15 code points
 * including the [marker] (default: " …").
 */
public fun CharSequence.truncateEnd(marker: CharSequence = Unicode.ELLIPSIS.startSpaced): CharSequence =
    truncateEnd(15.codePoints, marker)

/**
 * Returns this string truncated from the end to up to 15 code points
 * including the [marker] (default: " …").
 */
public fun String.truncateEnd(marker: CharSequence = Unicode.ELLIPSIS.startSpaced): CharSequence =
    truncateEnd(15.codePoints, marker)

/**
 * Returns this character sequence truncated from the end to the specified [length] (default: 15)
 * including the [marker] (default: " …").
 */
public fun CharSequence.truncateEnd(
    length: TextLength<*>,
    marker: CharSequence = Unicode.ELLIPSIS.startSpaced,
): CharSequence = mapText(length.unit) { it.truncateEnd(length.value, marker.asText(length.unit)) }

/**
 * Returns this string truncated from the end to the specified [length] (default: 15)
 * including the [marker] (default: " …").
 */
public fun String.truncateEnd(
    length: TextLength<*>,
    marker: CharSequence = Unicode.ELLIPSIS.startSpaced,
): String = mapText(length.unit) { it.truncateEnd(length.value, marker.asText(length.unit)) }


// withPrefix / withSuffix ---------------------------------------------------------------------------------------------

/** Returns this char with the [prefix] prepended if it's not already present. */
public fun kotlin.Char.withPrefix(prefix: CharSequence): String =
    toString().withPrefix(prefix)

/** Returns this char with the [suffix] appended if it's not already present. */
public fun kotlin.Char.withSuffix(suffix: CharSequence): String =
    toString().withSuffix(suffix)

/** Returns this character sequence with the [prefix] prepended if it's not already present. */
public fun CharSequence.withPrefix(prefix: CharSequence): CharSequence =
    if (startsWith(prefix)) this else buildString { append(prefix); append(this@withPrefix) }

/** Returns this character sequence with the [suffix] appended if it's not already present. */
public fun CharSequence.withSuffix(suffix: CharSequence): CharSequence =
    if (endsWith(suffix)) this else buildString { append(this@withSuffix);append(suffix) }

/** Returns this string with the [prefix] prepended if it's not already present. */
public fun String.withPrefix(prefix: CharSequence): String =
    if (startsWith(prefix)) this else buildString { append(prefix); append(this@withPrefix) }

/** Returns this string with the [suffix] appended if it's not already present. */
public fun String.withSuffix(suffix: CharSequence): String =
    if (endsWith(suffix)) this else buildString { append(this@withSuffix);append(suffix) }


// withRandomSuffix ----------------------------------------------------------------------------------------------------

private const val randomSuffixLength = 4
private const val randomSuffixSeparator = "--"

@Suppress("RegExpSimplifiable")
private val randomSuffixMatcher: Regex = Regex(".*$randomSuffixSeparator[\\da-zA-Z]{$randomSuffixLength}\$")

/** Returns this char with a random suffix of two dashes and four alphanumeric characters. */
public fun kotlin.Char.withRandomSuffix(): String =
    toString().withRandomSuffix()

/** Returns this character sequence with a random suffix of two dashes and four alphanumeric characters. */
public fun CharSequence.withRandomSuffix(): CharSequence =
    if (randomSuffixMatcher.matches(this)) this
    else buildString { append(this@withRandomSuffix); append(randomSuffixSeparator); append(randomString(length = randomSuffixLength)) }

/** Returns this string with a random suffix of two dashes and four alphanumeric characters. */
public fun String.withRandomSuffix(): String =
    if (randomSuffixMatcher.matches(this)) this
    else buildString { append(this@withRandomSuffix); append(randomSuffixSeparator); append(randomString(length = randomSuffixLength)) }


// randomString --------------------------------------------------------------------------------------------------------

/** Creates a random string of the specified [length] made up of the specified [allowedCharacters]. */
public fun randomString(length: Int = 16, vararg allowedCharacters: kotlin.Char = (('0'..'9') + ('a'..'z') + ('A'..'Z')).toCharArray()): String =
    buildString(length) { repeat(length) { append(allowedCharacters[Random.nextInt(0, allowedCharacters.size)]) } }


// repeat --------------------------------------------------------------------------------------------------------------

/** Returns a string containing this char repeated [n] times. */
public fun kotlin.Char.repeat(n: Int): String = toString().repeat(n)


// indexOfOrNull -------------------------------------------------------------------------------------------------------

/**
 * Returns the index within this string of the first occurrence of the specified character,
 * starting from the specified [startIndex].
 *
 * @param ignoreCase `true` to ignore the character case when matching a character. By default `false`.
 * @return An index of the first occurrence of [char] or `null` if none is found.
 */
public fun CharSequence.indexOfOrNull(char: kotlin.Char, startIndex: Int = 0, ignoreCase: Boolean = false): Int? =
    indexOf(char, startIndex, ignoreCase).takeIf { it >= 0 }

/**
 * Returns the index within this character sequence of the first occurrence of the specified [string],
 * starting from the specified [startIndex].
 *
 * @param ignoreCase `true` to ignore the character case when matching a string. By default `false`.
 * @return An index of the first occurrence of [string] or `null` if none is found.
 */
public fun CharSequence.indexOfOrNull(string: String, startIndex: Int = 0, ignoreCase: Boolean = false): Int? =
    indexOf(string, startIndex, ignoreCase).takeIf { it >= 0 }


// lastIndexOfOrNull ---------------------------------------------------------------------------------------------------

/**
 * Returns the index within this character sequence of the last occurrence of the specified character,
 * starting from the specified [startIndex].
 *
 * @param startIndex The index of character to start searching at. The search proceeds backward toward the beginning of the string.
 * @param ignoreCase `true` to ignore the character case when matching a character. By default `false`.
 * @return An index of the last occurrence of [char] or `null` if none is found.
 */
public fun CharSequence.lastIndexOfOrNull(char: kotlin.Char, startIndex: Int = lastIndex, ignoreCase: Boolean = false): Int? =
    lastIndexOf(char, startIndex, ignoreCase).takeIf { it >= 0 }

/**
 * Returns the index within this character sequence of the last occurrence of the specified [string],
 * starting from the specified [startIndex].
 *
 * @param startIndex The index of character to start searching at. The search proceeds backward toward the beginning of the string.
 * @param ignoreCase `true` to ignore the character case when matching a string. By default `false`.
 * @return An index of the last occurrence of [string] or `null` if none is found.
 */
public fun CharSequence.lastIndexOfOrNull(string: String, startIndex: Int = lastIndex, ignoreCase: Boolean = false): Int? =
    lastIndexOf(string, startIndex, ignoreCase).takeIf { it >= 0 }


// asString ------------------------------------------------------------------------------------------------------------

/**
 * Returns a string representing this object
 * and the properties specified by [include] (default: all)
 * with properties excluded as specified by [excludeNullValues]
 * and [exclude].
 */
public fun <T : Any> T.asString(
    vararg include: KProperty<*>,
    excludeNullValues: Boolean = true,
    exclude: Iterable<KProperty<*>> = emptyList(),
): String {
    val receiver = this
    return asString(excludeNullValues, exclude.map { it.name }) {
        if (include.isEmpty()) putAll(receiver.properties)
        else include.forEach { prop ->
            prop.getOrNull(receiver)?.also { put(prop.name, it) }
        }
    }
}

/**
 * Returns a string representing this object
 * and the properties specified by [include] (default: all)
 * with properties excluded as specified by [excludeNullValues]
 * and [exclude].
 */
public fun <T : Any> T.asString(
    excludeNullValues: Boolean = true,
    exclude: Iterable<String> = emptyList(),
    include: MutableMap<Any, Any?>.() -> Unit,
): String {
    val properties = buildMap(include).mapKeys { (key, _) ->
        when (key) {
            is CharSequence -> key.quoted.removeSurrounding("\"")
            is KProperty<*> -> key.name
            else -> key.render { compression = Always }
        }
    }
    val renderedType = renderType()
    val rendered = properties.render {
        typing = Untyped
        filterProperties { receiver, prop ->
            (!excludeNullValues || receiver != null) && !exclude.contains(prop)
        }
    }
    return buildString {
        append(renderedType)
        append(" ")
        append(rendered.removePrefix(renderedType))
    }
}


// splitMap ------------------------------------------------------------------------------------------------------------

/**
 * Splits this strings using the specified [delimiter] applies [transform]
 * to each substrings and joins back the mapped substrings to a string using the same [delimiter].
 */
public fun CharSequence.splitMap(
    delimiter: String,
    ignoreCase: Boolean = false,
    limit: Int = 0,
    transform: (String) -> String
): String = split(delimiter, ignoreCase = ignoreCase, limit = limit).joinToString(delimiter) { transform(it) }


// splitToSequence -----------------------------------------------------------------------------------------------------

/**
 * Splits this character sequence to a sequence of strings around occurrences of the specified [delimiters].
 *
 * @param delimiters One or more strings to be used as delimiters.
 * @param keepDelimiters `true` to have strings end with its corresponding delimiter.
 * @param ignoreCase `true` to ignore the character case when matching a delimiter. By default `false`.
 * @param limit The maximum number of substrings to return. Zero by default means no limit is set.
 *
 * To avoid ambiguous results when strings in [delimiters] have characters in common, this method proceeds from
 * the beginning to the end of this string, and finds at each position the first element in [delimiters]
 * that matches this string at that position.
 */
public fun CharSequence.splitToSequence(
    vararg delimiters: String,
    keepDelimiters: Boolean = false,
    ignoreCase: Boolean = false,
    limit: Int = 0,
): Sequence<String> =
    rangesDelimitedBy(delimiters = delimiters, ignoreCase = ignoreCase, limit = limit)
        .run {
            if (!keepDelimiters) map { substring(it) }
            else windowed(size = 2, step = 1, partialWindows = true) { ranges ->
                substring(if (ranges.size == 2) ranges[0].first until ranges[1].first else ranges[0])
            }
        }

/**
 * Returns a sequence of index ranges of substrings in this character sequence around occurrences of the specified [delimiters].
 *
 * @param delimiters One or more strings to be used as delimiters.
 * @param startIndex The index to start searching delimiters from.
 *  No range having its start value less than [startIndex] is returned.
 *  [startIndex] is coerced to be non-negative and not greater than length of this string.
 * @param ignoreCase `true` to ignore the character case when matching a delimiter. By default `false`.
 * @param limit The maximum number of substrings to return. Zero by default means no limit is set.
 *
 * To avoid ambiguous results when strings in [delimiters] have characters in common, this method proceeds from
 * the beginning to the end of this string, and finds at each position the first element in [delimiters]
 * that matches this string at that position.
 */
private fun CharSequence.rangesDelimitedBy(
    delimiters: Array<out String>,
    startIndex: Int = 0,
    ignoreCase: Boolean = false,
    limit: Int = 0,
): Sequence<IntRange> {
    require(limit >= 0) { "Limit must be non-negative, but was $limit." }
    val delimiterList = delimiters.asList()

    return DelimitedRangesSequence(this, startIndex, limit) { currentIndex ->
        findAnyOf(
            strings = delimiterList,
            startIndex = currentIndex,
            ignoreCase = ignoreCase,
            last = false
        )?.let { it.first to it.second.length }
    }
}

private fun CharSequence.findAnyOf(strings: Collection<String>, startIndex: Int, ignoreCase: Boolean, last: Boolean): Pair<Int, String>? {
    if (!ignoreCase && strings.size == 1) {
        val string = strings.single()
        val index = if (!last) indexOf(string, startIndex) else lastIndexOf(string, startIndex)
        return if (index < 0) null else index to string
    }

    val indices = if (!last) startIndex.coerceAtLeast(0)..length else startIndex.coerceAtMost(lastIndex) downTo 0

    if (this is String) {
        for (index in indices) {
            val matchingString = strings.firstOrNull { it.regionMatches(0, this, index, it.length, ignoreCase) }
            if (matchingString != null)
                return index to matchingString
        }
    } else {
        for (index in indices) {
            val matchingString = strings.firstOrNull { it.regionMatchesImpl(0, this, index, it.length, ignoreCase) }
            if (matchingString != null)
                return index to matchingString
        }
    }

    return null
}

/**
 * Implementation of [regionMatches] for CharSequences.
 * Invoked when it's already known that arguments aren't strings, so that no extra type checks are performed.
 */
private fun CharSequence.regionMatchesImpl(thisOffset: Int, other: CharSequence, otherOffset: Int, length: Int, ignoreCase: Boolean): Boolean =
    if ((otherOffset < 0) || (thisOffset < 0) || (thisOffset > this.length - length) || (otherOffset > other.length - length)) false
    else (0 until length).all {
        this[thisOffset + it].equals(other[otherOffset + it], ignoreCase)
    }

private class DelimitedRangesSequence(
    private val input: CharSequence,
    private val startIndex: Int,
    private val limit: Int,
    private val getNextMatch: CharSequence.(currentIndex: Int) -> Pair<Int, Int>?,
) : Sequence<IntRange> {

    override fun iterator(): Iterator<IntRange> = object : Iterator<IntRange> {
        var nextState: Int = -1 // -1 for unknown, 0 for done, 1 for continue
        var currentStartIndex: Int = startIndex.coerceIn(0, input.length)
        var nextSearchIndex: Int = currentStartIndex
        var nextItem: IntRange? = null
        var counter: Int = 0

        private fun calcNext() {
            if (nextSearchIndex < 0) {
                nextState = 0
                nextItem = null
            } else {
                if (limit > 0 && ++counter >= limit || nextSearchIndex > input.length) {
                    nextItem = currentStartIndex..input.lastIndex
                    nextSearchIndex = -1
                } else {
                    val match = input.getNextMatch(nextSearchIndex)
                    if (match == null) {
                        nextItem = currentStartIndex..input.lastIndex
                        nextSearchIndex = -1
                    } else {
                        val (index, length) = match
                        nextItem = currentStartIndex until index
                        currentStartIndex = index + length
                        nextSearchIndex = currentStartIndex + if (length == 0) 1 else 0
                    }
                }
                nextState = 1
            }
        }

        override fun next(): IntRange {
            if (nextState == -1)
                calcNext()
            if (nextState == 0)
                throw NoSuchElementException()
            val result = nextItem as IntRange
            // Clean next to avoid keeping reference on yielded instance
            nextItem = null
            nextState = -1
            return result
        }

        override fun hasNext(): Boolean {
            if (nextState == -1)
                calcNext()
            return nextState == 1
        }
    }
}
