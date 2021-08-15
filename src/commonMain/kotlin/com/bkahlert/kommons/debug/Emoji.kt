@file:Suppress("ClassName")

package com.bkahlert.kommons.debug

import com.bkahlert.kommons.text.Semantics.Symbols

/**
 * Emoji representation of this value.
 *
 * @sample Samples.emoji.trueValue
 * @sample Samples.emoji.falseValue
 * @sample Samples.emoji.nullValue
 * @sample Samples.emoji.nonNullValue
 */
public val Any?.asEmoji: String
    inline get() = when (this) {
        true -> "✅"
        false -> "❌"
        null -> Symbols.Null
        else -> "🔣"
    }

private object Samples {
    object emoji {
        fun trueValue() {
            true.asEmoji
        }

        fun falseValue() {
            false.asEmoji
        }

        fun nullValue() {
            null.asEmoji
        }

        fun nonNullValue() {
            "Any".asEmoji
        }
    }
}
