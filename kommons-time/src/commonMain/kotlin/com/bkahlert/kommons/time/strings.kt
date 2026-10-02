package com.bkahlert.kommons.time

import kotlin.time.Instant

/** Returns an Emoji representation of this value. */
public fun Any?.asEmoji(): String = when (this) {
    null -> "❔"
    true -> "✅"
    false -> "❌"
    is Instant -> asEmoji()
    else -> "🔣"
}

/** Returns an Emoji representation of this [Instant] and the specified [roundingMode]. */
public fun Instant.asEmoji(): String = when (utcMinutes) {
    in 0..14 -> fullHourClocks[utcHours.mod(12)]
    in 15..44 -> halfHourClocks[utcHours.mod(12)]
    else -> fullHourClocks[(utcHours + 1).mod(12)]
}

private val fullHourClocks = listOf("🕛", "🕐", "🕑", "🕒", "🕓", "🕔", "🕕", "🕖", "🕗", "🕘", "🕙", "🕚")
private val halfHourClocks = listOf("🕧", "🕜", "🕝", "🕞", "🕟", "🕠", "🕡", "🕢", "🕣", "🕤", "🕥", "🕦")
