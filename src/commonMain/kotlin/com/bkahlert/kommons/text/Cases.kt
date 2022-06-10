@file:Suppress("EnumEntryName")

package com.bkahlert.kommons.text

import com.bkahlert.kommons.regex.RegularExpressions.camelCaseRegex
import com.bkahlert.kommons.regex.RegularExpressions.kebabCaseRegex
import com.bkahlert.kommons.regex.RegularExpressions.screamingSnakeCaseRegex
import kotlin.js.JsName

// TODO migrate
public enum class Cases(
    public val splitter: (String) -> List<String>,
    public val joiner: ((List<String>) -> String),
) {
    // TODO `sentence case`
    camelCase({
        buildList {
            add(0)
            camelCaseRegex
                .findAll(it)
                .let { matches -> matches.forEach { add(it.range.first + 1) } }
            add(it.length)
        }.windowed(2) { indices -> it.substring(indices[0], indices[1]).decapitalize() }
    }, {
        it.joinToString("") { part -> part.capitalize() }.decapitalize()
    }),
    PascalCase({
        buildList {
            add(0)
            camelCaseRegex
                .findAll(it.toString().decapitalize())
                .let { matches -> matches.forEach { add(it.range.first + 1) } }
            add(it.length)
        }.windowed(2) { indices -> it.substring(indices[0], indices[1]).decapitalize() }
    }, {
        it.joinToString("") { part -> part.capitalize() }
    }),
    SCREAMING_SNAKE_CASE({
        buildList {
            add(-1)
            screamingSnakeCaseRegex
                .findAll(it.toString())
                .let { matches -> matches.forEach { add(it.range.first + 1) } }
            add(it.length)
        }.windowed(2) { indices -> it.substring(indices[0] + 1, indices[1]).lowercase() }
    }, {
        it.joinToString("_") { part -> part.uppercase() }
    }),

    @JsName("kebab_case")
    `kebab-case`({
        buildList {
            add(-1)
            kebabCaseRegex
                .findAll(it.toString())
                .let { matches -> matches.forEach { add(it.range.first + 1) } }
            add(it.length)
        }.windowed(2) { indices -> it.substring(indices[0] + 1, indices[1]).decapitalize() }
    }, {
        it.joinToString("-")
    })
}

public infix fun String.splitCase(case: Cases): List<String> = case.splitter(this)
public fun String.splitCamelCase(): List<String> = this splitCase Cases.camelCase
public fun String.splitPascalCase(): List<String> = this splitCase Cases.PascalCase
public fun String.splitScreamingSnakeCase(): List<String> = this splitCase Cases.SCREAMING_SNAKE_CASE
public fun String.splitKebabCase(): List<String> = this splitCase Cases.`kebab-case`

public infix fun List<String>.joinToCase(case: Cases): String = case.joiner(this)
public fun List<String>.joinToCamelCase(): String = this joinToCase Cases.camelCase
public fun List<String>.joinToPascalCase(): String = this joinToCase Cases.PascalCase
public fun List<String>.joinToScreamingSnakeCase(): String = this joinToCase Cases.SCREAMING_SNAKE_CASE
public fun List<String>.joinToKebabCase(): String = this joinToCase Cases.`kebab-case`

/**
 * Given this string is using `kebab-case` this method will return the string converted to `camelCase`.
 */
public fun String.convertKebabCaseToCamelCase(): String {
    val hyphenMerger = { previousEnd: Int, hyphenMatch: MatchResult ->
        substring(previousEnd, hyphenMatch.range.first) + hyphenMatch.groupValues[1] + hyphenMatch.groupValues[2].uppercase()
    }
    val regex = Regex("(?<leftChar>[a-z\\d]?)-(?<rightChar>[a-z\\d])")
    val matches = regex.findAll(this)
    val words = matches.zipWithNext { previousMatch, currentMatch -> hyphenMerger(previousMatch.range.last + 1, currentMatch) }
    return words.joinToString(
        separator = "",
        prefix = matches.firstOrNull()?.let { hyphenMerger(0, matches.first()) } ?: this,
        postfix = matches.lastOrNull()?.let { subSequence(it.range.last + 1, length) } ?: "",
        truncated = Unicode.ELLIPSIS.toString())
}

private fun String.convertCamelCase(separator: Char, transform: (String) -> String): String =
    camelCaseRegex
        .replace(decapitalize(), "\${lowerLeftChar}$separator\${upperRightChar}")
        .let(transform)

/**
 * Given this string is using `camelCase` this method will return the string converted to `kebab-case`.
 */
public fun String.convertCamelCaseToKebabCase(): String = convertCamelCase('-') { it.lowercase() }

/**
 * Given this string is using `SCREAMING_SNAKE_CASE` this method will return the string converted to `kebab-case`.
 */
public fun String.convertScreamingSnakeCaseToKebabCase(): String = lowercase().replace('_', '-')

/**
 * Given this string is using `kebab-case` this method will return the string converted to `SCREAMING_SNAKE_CASE`.
 */
public fun String.convertKebabCaseToScreamingSnakeCase(): String = uppercase().replace('-', '_')

/**
 * Returns this enum constant's name in `kebab-case`. Both Kotlin coding conventions allowed (`SCREAMING_SNAKE_CASE` and `PascalCase`) are supported.
 *
 * @see <a href="https://kotlinlang.org/docs/reference/coding-conventions.html#property-names">Naming rules: property names</a>
 */
public fun Enum<*>.kebabCaseName(): String {
    val screamingSnakeCaseName =
        name.takeUnless { it.asSequence().any { char -> char.isLowerCase() } } ?: name.convertCamelCase(
            '_'
        ) { it.uppercase() }
    return screamingSnakeCaseName.convertScreamingSnakeCaseToKebabCase()
}

/**
 * Returns this enum constant from `kebab-case`.
 *
 * @see <a href="https://kotlinlang.org/docs/reference/coding-conventions.html#property-names">Naming rules: property names</a>
 */
public inline fun <reified T : Enum<T>> String.valueOfKebabCaseName(): T = enumValueOf(convertKebabCaseToScreamingSnakeCase())

/**
 * Returns `true` if it contains at least one upper and one lower case character.
 */
public fun String.isMixedCase(): Boolean = containsUpperCase() && containsLowerCase()

/**
 * Returns `true` if it contains at least one upper case character.
 */
public fun String.containsUpperCase(): Boolean = any { it.isUpperCase() }

/**
 * Returns `true` if it contains at least one lower case character.
 */
public fun CharSequence.containsLowerCase(): Boolean = any { it.isLowerCase() }

public fun String.capitalize(): String = if (isNotEmpty() && first().isLowerCase()) first().uppercaseChar() + substring(1) else this
public fun String.decapitalize(): String = if (isNotEmpty() && first().isUpperCase()) first().lowercaseChar() + substring(1) else this

/**
 * Returns `true` if this character is upper case.
 *
 * @see isLowerCase
 */
public fun Char.isUpperCase(): Boolean = this == uppercaseChar() && this != lowercaseChar()

/**
 * Returns `true` if this character is lower case.
 *
 * @see isUpperCase
 */
public fun Char.isLowerCase(): Boolean = this == lowercaseChar() && this != uppercaseChar()
