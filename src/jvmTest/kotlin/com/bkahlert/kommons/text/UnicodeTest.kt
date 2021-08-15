package com.bkahlert.kommons.text

import com.bkahlert.kommons.math.RoundingMode.CEILING
import com.bkahlert.kommons.math.RoundingMode.FLOOR
import com.bkahlert.kommons.math.RoundingMode.HALF_UP
import com.bkahlert.kommons.test.testEach
import com.bkahlert.kommons.test.toStringIsEqualTo
import com.bkahlert.kommons.text.Unicode.Emojis.Emoji
import com.bkahlert.kommons.text.Unicode.NEXT_LINE
import com.bkahlert.kommons.text.Unicode.TAB
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.TestFactory
import strikt.assertions.isEqualTo
import strikt.assertions.isTrue
import strikt.assertions.startsWith
import java.time.Instant

class UnicodeTest {

    @Nested
    inner class Get {

        @TestFactory
        fun `should return code point`() = testEach(
            133 to NEXT_LINE.toString(),
            119594 to Unicode.DivinationSymbols.Tetragrams.Purity.toString(),
            containerNamePattern = "\"{}\" ？⃔ \"{}\"") { (codePoint, expected) ->
            expecting { Unicode[codePoint] } that { toStringIsEqualTo(expected) }
        }
    }

    @TestFactory
    fun `should have valid unicode blocks`() = testEach(
        Unicode.BoxDrawings to ("╿" to """
            ─${TAB}BOX DRAWINGS LIGHT HORIZONTAL
            ━${TAB}BOX DRAWINGS HEAVY HORIZONTAL
            │${TAB}BOX DRAWINGS LIGHT VERTICAL
            ┃${TAB}BOX DRAWINGS HEAVY VERTICAL
        """.trimIndent()),
        Unicode.CombiningDiacriticalMarks to ("ͯ" to """
             ̀${TAB}COMBINING GRAVE ACCENT
             ́${TAB}COMBINING ACUTE ACCENT
             ̂${TAB}COMBINING CIRCUMFLEX ACCENT
             ̃${TAB}COMBINING TILDE
        """.trimIndent()),
        Unicode.DivinationSymbols.Tetragrams to ("𝍖" to """
            𝌆${TAB}TETRAGRAM FOR CENTRE
            𝌇${TAB}TETRAGRAM FOR FULL CIRCLE
            𝌈${TAB}TETRAGRAM FOR MIRED
            𝌉${TAB}TETRAGRAM FOR BARRIER
        """.trimIndent()),
    ) { (unicodeBlockMeta, expectations) ->
        expecting("should be valid") { unicodeBlockMeta.isValid } that { isTrue() }
        expecting("should map code point") { unicodeBlockMeta.unicodeBlock.range.last.string } that { isEqualTo(expectations.first) }
        expecting("should provide code point table") { unicodeBlockMeta.asTable() } that { startsWith(expectations.second) }
    }


    @Nested
    inner class Emojis {

        @TestFactory
        fun `maps hours`() = testEach(
            listOf(-12, 0, 12, 24) to listOf(Emoji("🕛"), Emoji("🕧")),
            listOf(-8, 4, 16) to listOf(Emoji("🕓"), Emoji("🕟")),
        ) { (hours, expectations) ->
            hours.forEach { hour ->
                expecting("$hour:00 ➜ ${expectations[0]}") { Unicode.Emojis.FullHoursDictionary[hour] } that { isEqualTo(expectations[0]) }
                expecting("$hour:30 ➜ ${expectations[1]}") { Unicode.Emojis.HalfHoursDictionary[hour] } that { isEqualTo(expectations[1]) }
            }
        }

        @TestFactory
        fun `maps instants`() = testEach(
            Instant.parse("2020-02-02T02:02:02Z") to listOf(Emoji("🕝"), Emoji("🕑"), Emoji("🕑")),
            Instant.parse("2020-02-02T22:32:02Z") to listOf(Emoji("🕚"), Emoji("🕥"), Emoji("🕥")),
        ) { (instant, expectations) ->
            expecting { instant.asEmoji(CEILING) } that { isEqualTo(expectations[0]) }
            expecting { instant.asEmoji(FLOOR) } that { isEqualTo(expectations[1]) }
            expecting { instant.asEmoji(HALF_UP) } that { isEqualTo(expectations[2]) }
        }
    }
}
