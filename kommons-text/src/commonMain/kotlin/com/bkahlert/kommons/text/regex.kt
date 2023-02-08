package com.bkahlert.kommons.text

import com.bkahlert.kommons.EMPTY

/** Whether this regular expression is a group—no matter if named, anonymous, or indexed. */
public val Regex.isGroup: Boolean
    get() {
        if (pattern.length < 2) return false
        val unescaped = pattern.replace(Regex("\\\\."), "X")
        if (unescaped[0] != '(' || unescaped[unescaped.lastIndex] != ')') return false

        var depth = 1
        for (cp in unescaped.toCodePointList().drop(1).dropLast(1)) {
            when (cp.char) {
                '(' -> depth++
                ')' -> depth--
            }
            if (depth <= 0) return false
        }
        return true
    }

/** Whether this regular expression is a named group. */
public val Regex.isNamedGroup: Boolean
    get() = isGroup && pattern.startsWith("(?<")

/** Whether this regular expression is an anonymous group. */
public val Regex.isAnonymousGroup: Boolean
    get() = isGroup && pattern.startsWith("(?:")

/** Whether this regular expression is an indexed group. */
public val Regex.isIndexedGroup: Boolean
    get() = isGroup && !pattern.startsWith("(?<") && !pattern.startsWith("(?:")

/** The contents of this regular expression's group if any, or this [Regex] otherwise. */
public val Regex.groupContents: Regex
    get() = if (isGroup) {
        if (pattern.startsWith("(?<")) Regex(pattern.substring(pattern.indexOf('>') + 1, pattern.lastIndex))
        else if (pattern.startsWith("(?:")) Regex(pattern.substring(3, pattern.lastIndex))
        else Regex(pattern.substring(1, pattern.lastIndex))
    } else this


/** Returns a [Regex] that matches this regular expression followed by the specified [other]. */
public operator fun Regex.plus(other: Regex): Regex = Regex("$pattern${other.pattern}")

/** Returns a [Regex] that matches this regular expression followed by the specified [pattern]. */
public operator fun Regex.plus(pattern: CharSequence): Regex = Regex("${this.pattern}$pattern")


/** Returns a [Regex] consisting of this and the specified [other] concatenated with `|`. */
public infix fun Regex.or(other: Regex): Regex = Regex("$pattern|${other.pattern}")

/** Returns a [Regex] consisting of this and the specified [otherPattern] concatenated with `|`. */
public infix fun Regex.or(otherPattern: CharSequence): Regex = Regex("$pattern|$otherPattern")


/** Returns a [Regex] that matches one of the specified [literals]. */
public fun Regex.Companion.fromLiteralAlternates(vararg literals: String): Regex =
    fromLiteralAlternates(literals.asList())

/** Returns a [Regex] that matches one of the specified [literals]. */
public fun Regex.Companion.fromLiteralAlternates(literals: Collection<String>): Regex =
    Regex(literals.joinToString("|") { escape(it) })

private const val anyCharacterPattern = "[\\s\\S]"
private const val anyNonLineSeparatorPattern = "."

/**
 * Returns a [Regex] that matches the same way the specified glob-like [pattern], using
 * the specified [wildcard] (default: `*`) to match within lines, and
 * the specified [multilineWildcard] (default: `**`) to match across lines,
 * would.
 *
 * The returned regular expression render all specified [lineSeparators] (default: [LineSeparators.Common])
 * matchable by any of the specified [lineSeparators].
 *
 * @see [CharSequence.matchesGlob]
 */
public fun Regex.Companion.fromGlob(
    pattern: CharSequence,
    wildcard: String = "*",
    multilineWildcard: String = "**",
    vararg lineSeparators: String = LineSeparators.Common,
): Regex {
    val anyLineSepPattern = Regex.fromLiteralAlternates(*lineSeparators).group(null).pattern
    val anyNumberLineSepPattern = "$anyLineSepPattern*"
    val multilineWildcardRegex = Regex("$anyNumberLineSepPattern${escape(multilineWildcard)}$anyNumberLineSepPattern")
    return pattern
        .split(multilineWildcardRegex)
        .joinToString("$anyCharacterPattern*") { multilineWildcardFenced ->
            multilineWildcardFenced.split(wildcard).joinToString("$anyNonLineSeparatorPattern*") { wildcardFenced ->
                wildcardFenced.splitToSequence(delimiters = lineSeparators).joinToString(anyLineSepPattern) {
                    escape(it)
                }
            }
        }.toRegex()
}

/**
 * Returns `true` if this character sequence matches the given
 * glob-like [pattern], using
 * the specified [wildcard] (default: `*`) to match within lines, and
 * the specified [multilineWildcard] (default: `**`) to match across lines.
 *
 * The specified [lineSeparators] (default: [LineSeparators.Common]) can
 * be matches by any line separator, i.e. [LineSeparators.LF] / `\n`.
 *
 * **Example 1: matching within lines with wildcard**
 * ```kotlin
 * "foo.bar()".matchesGlob("foo.*")  // ✅
 * ```
 *
 * **Example 2: matching across lines with multiline wildcard**
 * ```kotlin
 * """
 * foo
 *   .bar()
 *   .baz()
 * """.trimIndent().matchesGlob(
 *     """
 *     foo
 *       .**()
 *     """.trimIndent())             // ✅
 * ```
 *
 * **Example 3: wildcard not matching across lines**
 * ```kotlin
 * """
 * foo
 *   .bar()
 *   .baz()
 * """.trimIndent().matchesGlob(
 *     """
 *     foo
 *       .*()
 *     """.trimIndent())             // ❌ (* doesn't match across lines)
 * ```
 *
 * @see [Regex.Companion.fromGlob]
 */
public fun CharSequence.matchesGlob(
    pattern: CharSequence,
    wildcard: String = "*",
    multilineWildcard: String = "**",
    vararg lineSeparators: String = LineSeparators.Common,
): Boolean = Regex.fromGlob(pattern, wildcard, multilineWildcard, *lineSeparators).matches(this)

/**
 * Returns `true` if this character sequence matches the given
 * SLF4J / Logback style [pattern], using
 * `{}` to match within lines, and
 * `{{}}` to match across lines.
 *
 * The specified [lineSeparators] (default: [LineSeparators.Common]) can
 * be matches by any line separator, i.e. [LineSeparators.LF] / `\n`.
 *
 * **Example 1: matching within lines with `{}`**
 * ```kotlin
 * "foo.bar()".matchesGlob("foo.{}")  // ✅
 * ```
 *
 * **Example 2: matching across lines with `{{}}`**
 * ```kotlin
 * """
 * foo
 *   .bar()
 *   .baz()
 * """.trimIndent().matchesGlob(
 *     """
 *     foo
 *       .{{}}()
 *     """.trimIndent())             // ✅
 * ```
 *
 * **Example 3: `{}` not matching across lines**
 * ```kotlin
 * """
 * foo
 *   .bar()
 *   .baz()
 * """.trimIndent().matchesGlob(
 *     """
 *     foo
 *       .{}()
 *     """.trimIndent())             // ❌ ({} doesn't match across lines)
 * ```
 *
 * @see [Regex.Companion.fromGlob]
 */
public fun CharSequence.matchesCurly(
    pattern: CharSequence,
    vararg lineSeparators: String = LineSeparators.Common,
): Boolean = Regex.fromGlob(pattern, "{}", "{{}}", *lineSeparators).matches(this)


/**
 * Returns a [Regex] that groups this [Regex].
 *
 * If a [name] is specified, a named group, for example `(?<name>foo)`, is returned.
 *
 * If no [name] is specified **and** this regex isn't yet grouped,
 * an anonymous/non-capturing group (e.g. `(?:foo)`) is returned.
 *
 * In other words: no unnecessary brackets are added.
 */
public fun Regex.group(name: String? = null): Regex {
    return when (name) {
        null -> {
            @Suppress("RegExpUnnecessaryNonCapturingGroup")
            if (isGroup) this
            else Regex("(?:$pattern)")
        }

        else -> {
            requireValidGroupName(name)
            if (isGroup) {
                if (pattern.startsWith("(?<")) Regex("(?<$name>$pattern)")
                else if (pattern.startsWith("(?:")) Regex("(?<$name>${pattern.substring(3, pattern.lastIndex)})")
                else Regex("(?<$name>${pattern.substring(1, pattern.lastIndex)})")
            } else Regex("(?<$name>$pattern)")
        }
    }
}

/**
 * Returns this regular expression if it [isGroup] already or
 * this regular expression as an anonymous group otherwise.
 *
 * @see group
 */
public val Regex.grouped: Regex get() = group(null)

/** Returns the specified [name] if it's a valid group name or throws an [IllegalArgumentException] otherwise. */
private fun requireValidGroupName(name: String): String = name.apply {
    require(this.all { it in 'a'..'z' || it in 'A'..'Z' }) {
        "Group name $this must only consist of letters a..z and A..Z."
    }
}

/**
 * Returns a [Regex] that optionally matches this [Regex].
 *
 * Example: `foo` becomes `(?:foo)?`
 */
public fun Regex.optional(): Regex = Regex("${grouped.pattern}?")

/**
 * Returns a [Regex] that matches this [Regex] any number of times.
 *
 * Example: `foo` becomes `(?:foo)*`
 */
public fun Regex.repeatAny(): Regex = Regex("${grouped.pattern}*")

/**
 * Returns a [Regex] that matches this [Regex] at least once.
 *
 * Example: `foo` becomes `(?:foo)+`
 */
public fun Regex.repeatAtLeastOnce(): Regex = Regex("${grouped.pattern}+")

/**
 * Returns a [Regex] that matches this [Regex] between [min] and [max] times.
 *
 * Example: `foo` becomes `(?:foo){2,5}`
 */
public fun Regex.repeat(min: Int? = 0, max: Int? = null): Regex {
    if (min == 0 && max == 1) return optional()
    if (min == 0 && max == null) return repeatAny()
    if (min == 1 && max == null) return repeatAtLeastOnce()
    val minString = min?.toString() ?: String.EMPTY
    val maxString = max?.toString() ?: String.EMPTY
    @Suppress("RegExpSimplifiable")
    return Regex("${grouped.pattern}{$minString,$maxString}")
}


/**
 * Returns a named group with the specified [name].
 * @return An instance of [MatchGroup] if the group with the specified [name] was matched or `null` otherwise.
 * @throws IllegalArgumentException if there is no group with the specified [name] defined in the regular expression pattern.
 * @throws UnsupportedOperationException if this match group collection doesn't support getting match groups by name,
 * for example, when it's not supported by the current platform.
 */
public operator fun MatchGroupCollection.get(name: String): MatchGroup? {
    val named = (this as? MatchNamedGroupCollection)
        ?: throw UnsupportedOperationException("This match group collection doesn't support getting match groups by name.")
    return named[name]
}

/** Returns the value of the matched [MatchGroup] with the provided [index]. */
public fun MatchResult.groupValue(index: Int): String? = groups[index]?.value

/** Returns the value of the matched [MatchGroup] with the provided [name]. */
public fun MatchResult.groupValue(name: String): String? = groups[name]?.value

/**
 * Returns a sequence of all occurrences of this regular expression within
 * the [input] string, beginning at the specified [startIndex].
 *
 * @throws IndexOutOfBoundsException if [startIndex] is less than zero or
 *         greater than the length of the [input] character sequence.
 */
public fun Regex.findAllValues(input: CharSequence, startIndex: Int = 0): Sequence<String> =
    findAll(input, startIndex).map { it.value }


private val anyCharacterRegex = Regex(anyCharacterPattern)

/** A [Regex] that matches any character including line breaks. */
public val Regex.Companion.AnyCharacterRegex: Regex get() = anyCharacterRegex

@Deprecated("will be removed")
private val urlRegex = Regex("(?<schema>https?|ftp|file)://[-a-zA-Z\\d+&@#/%?=~_|!:,.;]*[-a-zA-Z\\d+&@#/%=~_|]")

@Deprecated("will be removed; use uri module")
private val uriRegex = Regex("\\w+:/?/?\\S+")

/** A [Regex] that matches URLs. */
@Deprecated("will be removed")
public val Regex.Companion.UrlRegex: Regex get() = urlRegex

/** A [Regex] that matches URIs. */
@Deprecated("will be removed; use uri module")
public val Regex.Companion.UriRegex: Regex get() = uriRegex
