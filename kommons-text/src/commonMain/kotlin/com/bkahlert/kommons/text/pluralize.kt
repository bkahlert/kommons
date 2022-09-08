package com.bkahlert.kommons.text

/**
 * Computes the plural of this English word
 * using generic rules and returns it.
 *
 * Limitations:
 * - This implementation treats every input as one word.
 * - This implementation isn't idempotent.
 * - Only English is supported.
 * - Only singular nouns are supported.
 * - Only lower-case nouns are supported.
 * - Nouns with an irregular plural are treated as if they had a regular plural.
 */
public fun CharSequence.pluralize(): String =
    when {
        endsWith("s") -> "${this}es"
        endsWith("x") -> "${this}es"
        endsWith("z") -> "${this}es"
        endsWith("ch") -> "${this}es"
        endsWith("sh") -> "${this}es"
        endsWith("y") && length > 1 && get(length - 2) !in vocals -> "${dropLast(1)}ies"
        else -> "${this}s"
    }

private val vocals = arrayOf('a', 'e', 'i', 'o', 'u')
