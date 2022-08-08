package com.bkahlert.kommons.debug

import com.bkahlert.kommons.Instant
import com.bkahlert.kommons.RoundingMode
import com.bkahlert.kommons.RoundingMode.Ceiling
import com.bkahlert.kommons.utcHours
import com.bkahlert.kommons.utcMinutes
import com.bkahlert.kommons.withNegativeIndices

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
