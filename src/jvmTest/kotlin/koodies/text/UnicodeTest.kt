package koodies.text

import koodies.number.ApproximationMode
import koodies.test.testEach
import koodies.test.toStringIsEqualTo
import koodies.text.Unicode.nextLine
import org.junit.jupiter.api.DynamicContainer.dynamicContainer
import org.junit.jupiter.api.DynamicTest.dynamicTest
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.TestFactory
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import strikt.assertions.isTrue
import strikt.assertions.startsWith
import java.time.Instant


class UnicodeTest {

    @Nested
    inner class Get {

        @TestFactory
        fun `should return code point`() = listOf(
            133 to nextLine.toString(),
            119594 to Unicode.DivinationSymbols.Tetragrams.Purity.toString(),
        ).testEach("\"{}\" ？⃔ \"{}\"") { (codePoint, expected) ->
            val actual: CodePoint = Unicode[codePoint]
            expecting { actual } that { toStringIsEqualTo(expected) }
        }
    }

    @TestFactory
    fun `should have valid unicode blocks`() = listOf(
        Unicode.BoxDrawings to ("╿" to """
            ─	BOX DRAWINGS LIGHT HORIZONTAL
            ━	BOX DRAWINGS HEAVY HORIZONTAL
            │	BOX DRAWINGS LIGHT VERTICAL
            ┃	BOX DRAWINGS HEAVY VERTICAL
        """.trimIndent()),
        Unicode.CombiningDiacriticalMarks to ("ͯ" to """
             ̀	COMBINING GRAVE ACCENT
             ́	COMBINING ACUTE ACCENT
             ̂	COMBINING CIRCUMFLEX ACCENT
             ̃	COMBINING TILDE
        """.trimIndent()),
        Unicode.DivinationSymbols.Tetragrams to ("𝍖" to """
            𝌆	TETRAGRAM FOR CENTRE
            𝌇	TETRAGRAM FOR FULL CIRCLE
            𝌈	TETRAGRAM FOR MIRED
            𝌉	TETRAGRAM FOR BARRIER
        """.trimIndent()),
    ).map { (unicodeBlockMeta, expectations) ->
        dynamicContainer(unicodeBlockMeta.name, listOf(
            dynamicTest("should be valid") {
                expectThat(unicodeBlockMeta.isValid).isTrue()
            },
            dynamicTest("should map code point") {
                expectThat(unicodeBlockMeta.unicodeBlock.range.last.string).isEqualTo(expectations.first)
            },
            dynamicTest("should provide code point table") {
                expectThat(unicodeBlockMeta.asTable()).startsWith(expectations.second)
            },
        ))
    }


    @Nested
    inner class Emojis {

        @TestFactory
        fun `maps hours`() = listOf(
            listOf(-12, 0, 12, 24) to listOf(Unicode.Emojis.Emoji("🕛"), Unicode.Emojis.Emoji("🕧")),
            listOf(-8, 4, 16) to listOf(Unicode.Emojis.Emoji("🕓"), Unicode.Emojis.Emoji("🕟")),
        ).flatMap { (hours, expectations) ->
            hours.flatMap { hour ->
                listOf(
                    dynamicTest("$hour:00 ➜ ${expectations[0]}") {
                        val actual = Unicode.Emojis.FullHoursDictionary[hour]
                        expectThat(actual).isEqualTo(expectations[0])
                    },
                    dynamicTest("$hour:30 ➜ ${expectations[1]}") {
                        val actual = Unicode.Emojis.HalfHoursDictionary[hour]
                        expectThat(actual).isEqualTo(expectations[1])
                    },
                )
            }
        }

        @TestFactory
        fun `maps instants`() = listOf(
            Instant.parse("2020-02-02T02:02:02Z") to listOf(Unicode.Emojis.Emoji("🕝"), Unicode.Emojis.Emoji("🕑"), Unicode.Emojis.Emoji("🕑")),
            Instant.parse("2020-02-02T22:32:02Z") to listOf(Unicode.Emojis.Emoji("🕚"), Unicode.Emojis.Emoji("🕥"), Unicode.Emojis.Emoji("🕥")),
        ).flatMap { (instant, expectations) ->
            listOf(
                dynamicTest("$instant rounded up to ${expectations[0]}") {
                    val actual = instant.asEmoji(ApproximationMode.Ceil)
                    expectThat(actual).isEqualTo(expectations[0])
                },
                dynamicTest("$instant rounded down to ${expectations[1]}") {
                    val actual = instant.asEmoji(ApproximationMode.Floor)
                    expectThat(actual).isEqualTo(expectations[1])
                },
                dynamicTest("$instant rounded to ${expectations[2]}") {
                    val actual = instant.asEmoji(ApproximationMode.Round)
                    expectThat(actual).isEqualTo(expectations[2])
                }
            )
        }
    }
}
