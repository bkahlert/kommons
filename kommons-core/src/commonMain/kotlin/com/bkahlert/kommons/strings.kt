package com.bkahlert.kommons

import com.bkahlert.kommons.RoundingMode.Ceiling
import kotlinx.datetime.Instant
import kotlin.random.Random

private const val EMPTY_STRING: String = ""

/** An empty string. */
public val String.Companion.EMPTY: String get() = EMPTY_STRING

/** Creates a random string of the specified [length] made up of the specified [allowedCharacters]. */
public fun randomString(length: Int = 16, vararg allowedCharacters: kotlin.Char = (('0'..'9') + ('a'..'z') + ('A'..'Z')).toCharArray()): String =
    buildString(length) { repeat(length) { append(allowedCharacters[Random.nextInt(0, allowedCharacters.size)]) } }

/** Returns an Emoji representation of this value. */
public fun Any?.asEmoji(): String = when (this) {
    null -> "❔"
    true -> "✅"
    false -> "❌"
    is Instant -> asEmoji()
    else -> "🔣"
}

/** Returns an Emoji representation of this [Instant] and the specified [roundingMode]. */
public fun Instant.asEmoji(roundingMode: RoundingMode = Ceiling): String {
    val hour = utcHours
    val minute = utcMinutes
    return when ((roundingMode(minute.toDouble(), 30.0) / 30.0).toInt()) {
        0 -> fullHourClocks[hour]
        1 -> halfHourClocks[hour]
        else -> fullHourClocks[hour + 1]
    }
}

private val fullHourClocks = listOf("🕛", "🕐", "🕑", "🕒", "🕓", "🕔", "🕕", "🕖", "🕗", "🕘", "🕙", "🕚").withNegativeIndices()
private val halfHourClocks = listOf("🕧", "🕜", "🕝", "🕞", "🕟", "🕠", "🕡", "🕢", "🕣", "🕤", "🕥", "🕦").withNegativeIndices()
