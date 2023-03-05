package com.bkahlert.kommons

import kotlin.random.Random

private const val EMPTY_STRING: String = ""

/** An empty string. */
public val String.Companion.EMPTY: String get() = EMPTY_STRING

/** Creates a random string of the specified [length] made up of the specified [allowedCharacters]. */
public fun randomString(length: Int = 16, vararg allowedCharacters: Char = (('0'..'9') + ('a'..'z') + ('A'..'Z')).toCharArray()): String =
    buildString(length) { repeat(length) { append(allowedCharacters[Random.nextInt(0, allowedCharacters.size)]) } }

/** Returns an Emoji representation of this value. */
public fun Any?.asEmoji(): String = when (this) {
    null -> "❔"
    true -> "✅"
    false -> "❌"
    else -> "🔣"
}
