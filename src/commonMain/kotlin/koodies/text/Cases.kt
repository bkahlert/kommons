@file:Suppress("EnumEntryName")

package koodies.text

import koodies.regex.RegularExpressions.camelCaseRegex
import koodies.regex.RegularExpressions.kebabCaseRegex
import koodies.regex.RegularExpressions.screamingSnakeCaseRegex
import koodies.regex.namedGroups

public enum class Cases(
    public val splitter: (CharSequence) -> List<CharSequence>,
    public val joiner: ((List<CharSequence>) -> CharSequence),
) {
    camelCase({
        mutableListOf<Int>().apply {
            add(0)
            camelCaseRegex
                .findAll(it.toString())
                .let { matches -> matches.forEach { add(it.range.first + 1) } }
            add(it.length)
        }.windowed(2) { indices -> it.subSequence(indices[0], indices[1]).decapitalize() }
    }, {
        it.joinToString("") { part -> part.capitalize() }.decapitalize()
    }),
    PascalCase({
        mutableListOf<Int>().apply {
            add(0)
            camelCaseRegex
                .findAll(it.toString().decapitalize())
                .let { matches -> matches.forEach { add(it.range.first + 1) } }
            add(it.length)
        }.windowed(2) { indices -> it.subSequence(indices[0], indices[1]).decapitalize() }
    }, {
        it.joinToString("") { part -> part.capitalize() }
    }),
    SCREAMING_SNAKE_CASE({
        mutableListOf<Int>().apply {
            add(-1)
            screamingSnakeCaseRegex
                .findAll(it.toString())
                .let { matches -> matches.forEach { add(it.range.first + 1) } }
            add(it.length)
        }.windowed(2) { indices -> it.subSequence(indices[0] + 1, indices[1]).toLowerCase() }
    }, {
        it.joinToString("_") { part -> part.toUpperCase() }
    }),
    kebabcase({
        mutableListOf<Int>().apply {
            add(-1)
            kebabCaseRegex
                .findAll(it.toString())
                .let { matches -> matches.forEach { add(it.range.first + 1) } }
            add(it.length)
        }.windowed(2) { indices -> it.subSequence(indices[0] + 1, indices[1]).decapitalize() }
    }, {
        it.joinToString("-")
    })
}

public infix fun CharSequence.splitCase(case: Cases): List<String> = case.splitter(this).map { it.toString() }

public fun CharSequence.splitCamelCase(): List<CharSequence> = this splitCase Cases.camelCase
public fun CharSequence.splitPascalCase(): List<CharSequence> = this splitCase Cases.PascalCase
public fun CharSequence.splitScreamingSnakeCase(): List<CharSequence> = this splitCase Cases.SCREAMING_SNAKE_CASE
public fun CharSequence.splitKebabCase(): List<CharSequence> = this splitCase Cases.kebabcase

public fun String.splitCamelCase(): List<String> = this splitCase Cases.camelCase
public fun String.splitPascalCase(): List<String> = this splitCase Cases.PascalCase
public fun String.splitScreamingSnakeCase(): List<String> = this splitCase Cases.SCREAMING_SNAKE_CASE
public fun String.splitKebabCase(): List<String> = this splitCase Cases.kebabcase

public infix fun List<CharSequence>.joinToCase(case: Cases): String = case.joiner(this).toString()

public fun List<CharSequence>.joinToCamelCase(): CharSequence = this joinToCase Cases.camelCase
public fun List<CharSequence>.joinToPascalCase(): CharSequence = this joinToCase Cases.PascalCase
public fun List<CharSequence>.joinToScreamingSnakeCase(): CharSequence = this joinToCase Cases.SCREAMING_SNAKE_CASE
public fun List<CharSequence>.joinToKebabCase(): CharSequence = this joinToCase Cases.kebabcase

public fun List<String>.joinToCamelCase(): String = this joinToCase Cases.camelCase
public fun List<String>.joinToPascalCase(): String = this joinToCase Cases.PascalCase
public fun List<String>.joinToScreamingSnakeCase(): String = this joinToCase Cases.SCREAMING_SNAKE_CASE
public fun List<String>.joinToKebabCase(): String = this joinToCase Cases.kebabcase

/**
 * Given this string is using `kebab-case` this method will return the string converted to `camelCase`.
 */
public fun CharSequence.convertKebabCaseToCamelCase(): String {
    val hyphenMerger = { previousEnd: Int, hyphenMatch: MatchResult ->
        substring(previousEnd,
            hyphenMatch.range.first) + hyphenMatch.namedGroups["leftChar"]?.value + hyphenMatch.namedGroups["rightChar"]?.value?.toUpperCase()
    }
    return Regex("(?<leftChar>[a-z0-9]?)-(?<rightChar>[a-z0-9])").findAll(this).let { matches ->
        matches.zipWithNext { previousMatch, currentMatch -> hyphenMerger.invoke(previousMatch.range.last + 1, currentMatch) }.joinToString(
            prefix = matches.firstOrNull()?.let { hyphenMerger.invoke(0, matches.first()) } ?: this,
            postfix = matches.lastOrNull()?.let { subSequence(it.range.last + 1, length) } ?: "",
            separator = "", transform = { it })
    }
}

private fun CharSequence.convertCamelCase(separator: Char, transformator: (String) -> String): String =
    camelCaseRegex
        .replace(this.toString().decapitalize(), "\${lowerLeftChar}$separator\${upperRightChar}")
        .let(transformator)

/**
 * Given this string is using `camelCase` this method will return the string converted to `kebab-case`.
 */
public fun CharSequence.convertCamelCaseToKebabCase(): String = convertCamelCase('-', String::toLowerCase)

/**
 * Given this string is using `SCREAMING_SNAKE_CASE` this method will return the string converted to `kebab-case`.
 */
public fun CharSequence.convertScreamingSnakeCaseToKebabCase(): String = toString().toLowerCase().replace('_', '-')

/**
 * Given this string is using `kebab-case` this method will return the string converted to `SCREAMING_SNAKE_CASE`.
 */
public fun CharSequence.convertKebabCaseToScreamingSnakeCase(): String = toString().toUpperCase().replace('-', '_')

/**
 * Returns this enum constant's name in `kebab-case`. Both Kotlin coding conventions allowed (`SCREAMING_SNAKE_CASE` and `PascalCase`) are supported.
 *
 * @see <a href="https://kotlinlang.org/docs/reference/coding-conventions.html#property-names">Naming rules: property names</a>
 */
public fun Enum<*>.kebabCaseName(): String {
    val screamingSnakeCaseName =
        name.takeUnless { it.asSequence().any { char -> char.isLowerCase() } } ?: name.convertCamelCase('_',
            String::toUpperCase)
    return screamingSnakeCaseName.convertScreamingSnakeCaseToKebabCase()
}

/**
 * Returns this enum constant from `kebab-case`.
 *
 * @see <a href="https://kotlinlang.org/docs/reference/coding-conventions.html#property-names">Naming rules: property names</a>
 */
public inline fun <reified T : Enum<T>> CharSequence.valueOfKebabCaseName(): T {
    return enumValueOf(this.convertKebabCaseToScreamingSnakeCase())
}

/**
 * Returns `true` if it contains at least one upper and one lower case character.
 */
public fun CharSequence.isMixedCase(): Boolean = containsUpperCase() && containsLowerCase()

/**
 * Returns `true` if it contains at least one upper case character.
 */
public fun CharSequence.containsUpperCase(): Boolean = any { it.isUpperCase() }

/**
 * Returns `true` if it contains at least one lower case character.
 */
public fun CharSequence.containsLowerCase(): Boolean = any { it.isLowerCase() }


public fun CharSequence.capitalize(): CharSequence = object : CharSequence {
    override val length: Int get() = this@capitalize.length
    override fun get(index: Int): Char = this@capitalize[index].let { if (index == 0) it.uppercaseChar() else it }
    override fun subSequence(startIndex: Int, endIndex: Int): CharSequence =
        this@capitalize.subSequence(startIndex, endIndex).capitalize()

    override fun toString(): String = StringBuilder(this).toString()
}

public fun CharSequence.decapitalize(): CharSequence = object : CharSequence {
    override val length: Int get() = this@decapitalize.length
    override fun get(index: Int): Char = this@decapitalize[index].let { if (index == 0) it.lowercaseChar() else it }
    override fun subSequence(startIndex: Int, endIndex: Int): CharSequence =
        this@decapitalize.subSequence(startIndex, endIndex).decapitalize()

    override fun toString(): String = StringBuilder(this).toString()
}


public fun CharSequence.toUpperCase(): CharSequence = object : CharSequence {
    override val length: Int get() = this@toUpperCase.length
    override fun get(index: Int): Char = this@toUpperCase[index].uppercaseChar()
    override fun subSequence(startIndex: Int, endIndex: Int): CharSequence =
        this@toUpperCase.subSequence(startIndex, endIndex).toUpperCase()

    override fun toString(): String = StringBuilder(this).toString()
}

public fun CharSequence.toLowerCase(): CharSequence = object : CharSequence {
    override val length: Int get() = this@toLowerCase.length
    override fun get(index: Int): Char = this@toLowerCase[index].lowercaseChar()
    override fun subSequence(startIndex: Int, endIndex: Int): CharSequence =
        this@toLowerCase.subSequence(startIndex, endIndex).toLowerCase()

    override fun toString(): String = StringBuilder(this).toString()
}


public fun String.toUpperCase(): String = (this as CharSequence).toUpperCase().toString()
public fun String.toLowerCase(): String = (this as CharSequence).toLowerCase().toString()

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
