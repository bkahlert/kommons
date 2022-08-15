package com.bkahlert.kommons.text

import com.bkahlert.kommons.EMPTY
import com.bkahlert.kommons.text.CaseStyle.PascalCase
import com.bkahlert.kommons.text.CaseStyle.SCREAMING_SNAKE_CASE
import com.bkahlert.kommons.text.CaseStyle.`Title Case`
import com.bkahlert.kommons.text.CaseStyle.camelCase
import com.bkahlert.kommons.text.CaseStyle.`kebab-case`
import kotlin.js.JsName
import kotlin.reflect.KClass

/** Typical strategies to join words to a phrase, respectively split a phrase to words. */
public enum class CaseStyle {

    /** Strategy that separates words with a capitalized letter, and the first word starting with lowercase. */
    @Suppress("EnumEntryName")
    camelCase {
        override fun matches(phrase: CharSequence, isUpperCase: (kotlin.Char) -> Boolean): Boolean {
            if (phrase.isEmpty()) return true
            if (isUpperCase(phrase.first())) return false
            if (phrase.containsAny("_", "-", " ")) return false
            return true
        }

        override fun split(phrase: CharSequence, isUpperCase: (kotlin.Char) -> Boolean): List<String> = PascalCase.split(phrase, isUpperCase)

        override fun join(words: Iterable<CharSequence>): String =
            buildString {
                words.firstOrNull()?.also { append(it.toString().lowercase()) }
                append(PascalCase.join(words.drop(1)))
            }
    },

    /** Strategy that separates words with a capitalized letter, and the first word starting with uppercase. */
    PascalCase {
        override fun matches(phrase: CharSequence, isUpperCase: (kotlin.Char) -> Boolean): Boolean {
            if (phrase.isEmpty()) return true
            if (!isUpperCase(phrase.first())) return false
            if (phrase.containsAny("_", "-", " ")) return false
            return true
        }

        override fun split(phrase: CharSequence, isUpperCase: (kotlin.Char) -> Boolean): List<String> {
            var rem = phrase
            val words = mutableListOf<String>()
            while (rem.isNotEmpty()) {
                rem.takeWord(isUpperCase).also { (word, newRem) ->
                    words.add(word)
                    rem = newRem
                }
            }
            return words
        }

        override fun join(words: Iterable<CharSequence>): String =
            words.joinToString(String.EMPTY) { word ->
                when (word.length) {
                    1 -> word.toString().uppercase()
                    2 -> word.toString().uppercase()
                    else -> word.capitalize()
                }
            }

        private fun CharSequence.takeWord(isUpperCase: (kotlin.Char) -> Boolean): Pair<String, CharSequence> = when (length) {
            1 -> first().lowercase() to String.EMPTY
            2 -> toString().lowercase() to String.EMPTY
            else -> {
                if (isUpperCase(get(2))) {
                    val word = substring(0, 2)
                    word.lowercase() to subSequence(word.length until length)
                } else if (isUpperCase(get(1))) {
                    val word = substring(0, 1)
                    word.lowercase() to subSequence(word.length until length)
                } else {
                    val word = buildString {
                        append(this@takeWord.first())
                        append(this@takeWord.subSequence(1 until this@takeWord.length).let { rem ->
                            val index = rem.indexOfFirst { isUpperCase(it) }
                            if (index > 0) rem.subSequence(0, index)
                            else rem.subSequence(rem.indices)
                        })
                    }
                    word.lowercase() to subSequence(word.length until length)
                }
            }
        }
    },

    /** Strategy that separates words with uppercase letters, and underscores instead of spaces. */
    SCREAMING_SNAKE_CASE {
        override fun matches(phrase: CharSequence, isUpperCase: (kotlin.Char) -> Boolean): Boolean {
            if (phrase.isEmpty()) return true
            if (phrase.any { !isUpperCase(it) }) return false
            if (phrase.containsAny("-", " ")) return false
            return true
        }

        override fun split(phrase: CharSequence, isUpperCase: (kotlin.Char) -> Boolean): List<String> =
            if (phrase.isEmpty()) emptyList() else phrase.split('_').map { it.lowercase() }

        override fun join(words: Iterable<CharSequence>): String = words.joinToString("_") { word -> word.toString().uppercase() }
    },

    /** Strategy that separates words with lowercase letters, and dashes instead of spaces. */
    @JsName("kebab_case")
    @Suppress("EnumEntryName")
    `kebab-case` {
        override fun matches(phrase: CharSequence, isUpperCase: (kotlin.Char) -> Boolean): Boolean {
            if (phrase.isEmpty()) return true
            if (phrase.any { it != '-' && isUpperCase(it) }) return false
            if (phrase.containsAny("_", " ")) return false
            return true
        }

        override fun split(phrase: CharSequence, isUpperCase: (kotlin.Char) -> Boolean): List<String> = if (phrase.isEmpty()) emptyList() else phrase.split('-')
        override fun join(words: Iterable<CharSequence>): String = words.joinToString("-")
    },

    /** Strategy that separates words capitalized. */
    @JsName("Title_Case")
    @Suppress("EnumEntryName")
    `Title Case` {
        override fun matches(phrase: CharSequence, isUpperCase: (kotlin.Char) -> Boolean): Boolean {
            if (phrase.isEmpty()) return true
            if (!isUpperCase(phrase.first())) return false
            if (phrase.containsAny("_", "-")) return false
            return phrase.padEnd(3, ' ').windowed(3).all {
                if (it[1] == ' ') true
                else if (it[0] == ' ') isUpperCase(it[1])
                else if (!isUpperCase(it[0])) !isUpperCase(it[1])
                else if (isUpperCase(it[0])) it[2] == ' ' || !isUpperCase(it[2])
                else true
            }
        }

        override fun split(phrase: CharSequence, isUpperCase: (kotlin.Char) -> Boolean): List<String> =
            if (phrase.isEmpty()) emptyList() else phrase.split(' ').map { it.lowercase() }

        override fun join(words: Iterable<CharSequence>): String =
            words.joinToString(" ") { word ->
                when (word.length) {
                    1 -> word.toString().uppercase()
                    2 -> word.toString().uppercase()
                    else -> word.capitalize()
                }
            }
    },
    ;

    /** Returns `true` if the specified [phrase] matches this [CaseStyle]. */
    public abstract fun matches(phrase: CharSequence, isUpperCase: (kotlin.Char) -> Boolean = { !it.isLowerCase() }): Boolean

    /** Splits the specified [phrase] with this [CaseStyle] into a list of words. */
    public abstract fun split(phrase: CharSequence, isUpperCase: (kotlin.Char) -> Boolean = { !it.isLowerCase() }): List<String>

    /** Joins the specified [words] to a phrase with this [CaseStyle]. */
    public abstract fun join(words: Iterable<CharSequence>): String

    /** Joins the specified [words] to a phrase with this [CaseStyle]. */
    public fun join(vararg words: CharSequence): String = join(words.asList())

    public companion object {
        /** Returns all matching case styles for the specified [phrase]. */
        public fun findByMatching(phrase: CharSequence): List<CaseStyle> = values().filter { it.matches(phrase) }
    }
}

/**
 * Returns a string representation of this object with the specified [caseStyle] applied,
 * or the string representation with no case style applied if the original case style can't be determined—either
 * automatically or by providing a [hint].
 */
public fun Any.toCasedString(caseStyle: CaseStyle, hint: CaseStyle? = null): String {
    val phrase = toString()
    val originalCaseStyle = if (hint == null) {
        val matchingCaseStyles = CaseStyle.findByMatching(phrase)
        if (matchingCaseStyles.isEmpty()) return phrase
        matchingCaseStyles.first()
    } else {
        hint
    }
    return originalCaseStyle.split(phrase).let { caseStyle.join(it) }
}

/**
 * Returns a string representation of this object with [CaseStyle.camelCase] applied,
 * or the string representation with no case style applied if the original case style can't be determined—either
 * automatically or by providing a [hint].
 */
public fun Any.toCamelCasedString(hint: CaseStyle? = null): String = toCasedString(camelCase, hint)

/**
 * Returns a string representation of this object with [CaseStyle.PascalCase] applied,
 * or the string representation with no case style applied if the original case style can't be determined—either
 * automatically or by providing a [hint].
 */
public fun Any.toPascalCasedString(hint: CaseStyle? = null): String = toCasedString(PascalCase, hint)

/**
 * Returns a string representation of this object with [CaseStyle.SCREAMING_SNAKE_CASE] applied,
 * or the string representation with no case style applied if the original case style can't be determined—either
 * automatically or by providing a [hint].
 */
public fun Any.toScreamingSnakeCasedString(hint: CaseStyle? = null): String = toCasedString(SCREAMING_SNAKE_CASE, hint)

/**
 * Returns a string representation of this object with [CaseStyle.kebab-case] applied,
 * or the string representation with no case style applied if the original case style can't be determined—either
 * automatically or by providing a [hint].
 */
public fun Any.toKebabCasedString(hint: CaseStyle? = null): String = toCasedString(`kebab-case`, hint)

/**
 * Returns a string representation of this object with [CaseStyle.`Title Case`] applied,
 * or the string representation with no case style applied if the original case style can't be determined—either
 * automatically or by providing a [hint].
 */
public fun Any.toTitleCasedString(hint: CaseStyle? = null): String = toCasedString(`Title Case`, hint)


/**
 * The [KClass.simpleName] of this class with [CaseStyle.camelCase] applied,
 * or the simple name with no case style applied if the original case style can't be determined.
 */
public val KClass<*>.simpleCamelCasedName: String? get() = simpleName?.toCamelCasedString()

/**
 * The [KClass.simpleName] of this class with [CaseStyle.PascalCase] applied,
 * or the simple name with no case style applied if the original case style can't be determined.
 */
public val KClass<*>.simplePascalCasedName: String? get() = simpleName?.toPascalCasedString()

/**
 * The [KClass.simpleName] of this class with [CaseStyle.SCREAMING_SNAKE_CASE] applied,
 * or the simple name with no case style applied if the original case style can't be determined.
 */
public val KClass<*>.simpleScreamingSnakeCasedName: String? get() = simpleName?.toScreamingSnakeCasedString()

/**
 * The [KClass.simpleName] of this class with [CaseStyle.kebab-case] applied,
 * or the simple name with no case style applied if the original case style can't be determined.
 */
public val KClass<*>.simpleKebabCasedName: String? get() = simpleName?.toKebabCasedString()

/**
 * The [KClass.simpleName] of this class with [CaseStyle.`Title Case`] applied,
 * or the simple name with no case style applied if the original case style can't be determined.
 */
public val KClass<*>.simpleTitleCasedName: String? get() = simpleName?.toTitleCasedString()


/**
 * The [Enum.name] of this enum constant with [CaseStyle.camelCase] applied,
 * or the enum constant name with no case style applied if the original case style can't be determined.
 */
public val Enum<*>.camelCasedName: String get() = name.toCamelCasedString()

/**
 * The [Enum.name] of this enum constant with [CaseStyle.PascalCase] applied,
 * or the enum constant name with no case style applied if the original case style can't be determined.
 */
public val Enum<*>.pascalCasedName: String get() = name.toPascalCasedString()

/**
 * The [Enum.name] of this enum constant with [CaseStyle.SCREAMING_SNAKE_CASE] applied,
 * or the enum constant name with no case style applied if the original case style can't be determined.
 */
public val Enum<*>.screamingSnakeCasedName: String get() = name.toScreamingSnakeCasedString()

/**
 * The [Enum.name] of this enum constant with [CaseStyle.kebab-case] applied,
 * or the enum constant name with no case style applied if the original case style can't be determined.
 */
public val Enum<*>.kebabCasedName: String get() = name.toKebabCasedString()

/**
 * The [Enum.name] of this enum constant with [CaseStyle.`Title Case`] applied,
 * or the enum constant name with no case style applied if the original case style can't be determined.
 */
public val Enum<*>.titleCasedName: String get() = name.toTitleCasedString()
