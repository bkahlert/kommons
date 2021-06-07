package koodies.text

import koodies.collections.too
import koodies.test.expectThrows
import koodies.test.expecting
import koodies.test.testEach
import koodies.test.tests
import koodies.test.toStringIsEqualTo
import koodies.text.ANSI.Text.Companion.ansi
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory
import strikt.api.Assertion.Builder
import strikt.assertions.containsExactly
import strikt.assertions.isEqualTo

class TextWidthKtTest {

    @Test
    fun `should calculate zero columns for empty string`() {
        expecting { "".columns } that { isEqualTo(0) }
    }

    @TestFactory
    fun `should calc one column for basic letters and digits`() = testEach("A", "a", "Ö", "ß", "4") {
        expecting { it.columns } that { isEqualTo(1) }
    }

    @TestFactory
    fun `should calc one column for small width code points`() = testEach(Whitespaces.HAIR_SPACE, Whitespaces.THIN_SPACE) {
        expecting { it.columns } that { isEqualTo(1) }
    }

    @TestFactory
    fun `should calc two columns for wide characters`() = testEach("한", "글", "⮕", "😀") { // TODO "👩‍👩‍👧‍👧"
        expecting { it.columns } that { isEqualTo(2) }
    }

    @TestFactory
    fun `should calc zero columns for no-space characters`() = testEach(Unicode.zeroWidthSpace, Unicode.zeroWidthJoiner) {
        expecting { it.columns } that { isEqualTo(0) }
    }

    @TestFactory
    fun `should calc 0 columns for line separators`() = testEach(*LineSeparators.toTypedArray()) {
        expecting { it.first().columns } that { isEqualTo(0) }
        expecting { it.columns } that { isEqualTo(0) }
        expecting { "XXX${it}XX".columns } that { isEqualTo(5) }
    }

    @TestFactory
    fun `should calculate width for complex text`() = tests {
        expecting { "‾͟͟͞(((ꎤ ✧曲✧)̂—̳͟͞͞O HIT!".columns } that { isEqualTo(18) }
        expecting { "text        ".columns } that { isEqualTo(12) }
        expecting { "🟥🟧🟨🟩🟦🟪".columns } that { isEqualTo(12) }
        expecting { "text                                                ".columns } that { isEqualTo(52) }
        expecting { "🟥🟧🟨🟩🟦🟪                                        ".columns } that { isEqualTo(52) }
        expecting { "ℹ".columns } that { isEqualTo(1) }
    }

    @Test
    fun `should ignore ANSI`() {
        expecting { "red".ansi.red.columns } that { isEqualTo(3) }
    }

    @TestFactory
    fun `should provide columns for code points`() = tests {
        expecting { "A".asCodePoint()!!.columns } that { isEqualTo(1) }
        expecting { "⮕".asCodePoint()!!.columns } that { isEqualTo(2) }
    }

    @Nested
    inner class FindIndex {

        @TestFactory
        fun `should find index`() = testEach(
            0 to 0, // ""
            1 to 4, // "‾͟͟͞"
            2 to 5, // "‾͟͟͞("
            3 to 6, // "‾͟͟͞(("
            4 to 7, // "‾͟͟͞((("
            5 to 8, // "‾͟͟͞(((ꎤ"
            6 to 9, // "‾͟͟͞(((ꎤ "
            7 to 10, // "‾͟͟͞(((ꎤ ✧"
            8 to 10, // "‾͟͟͞(((ꎤ ✧曲"
            9 to 11, // "‾͟͟͞(((ꎤ ✧曲"
            10 to 12, // "‾͟͟͞(((ꎤ ✧曲✧"
            11 to 14, // "‾͟͟͞(((ꎤ ✧曲✧)̂"
            12 to 19, // "‾͟͟͞(((ꎤ ✧曲✧)̂—̳͟͞͞"
            13 to 20, // "‾͟͟͞(((ꎤ ✧曲✧)̂—̳͟͞͞O"
            14 to 21, // "‾͟͟͞(((ꎤ ✧曲✧)̂—̳͟͞͞O "
            15 to 22, // "‾͟͟͞(((ꎤ ✧曲✧)̂—̳͟͞͞O H"
            16 to 23, // "‾͟͟͞(((ꎤ ✧曲✧)̂—̳͟͞͞O HI"
            17 to 24, // "‾͟͟͞(((ꎤ ✧曲✧)̂—̳͟͞͞O HIT"
            18 to 25, // "‾͟͟͞(((ꎤ ✧曲✧)̂—̳͟͞͞O HIT!"
            19 to null, // "‾͟͟͞(((ꎤ ✧曲✧)̂—̳͟͞͞O HIT!"
        ) { (column, expectedIndex) ->
            expecting { "‾͟͟͞(((ꎤ ✧曲✧)̂—̳͟͞͞O HIT!".findIndexByColumns(column) } that { isEqualTo(expectedIndex) }
        }

        @Test
        fun `should throw on negative column`() {
            expectThrows<IllegalArgumentException> { "‾͟͟͞(((ꎤ ✧曲✧)̂—̳͟͞͞O HIT!".findIndexByColumns(-1) }
        }
    }

    @Nested
    inner class ColumnsSubSequence {

        @TestFactory
        fun `should work with proper assumptions`() = testEach(
            0 to 0 too "",
            0 to 1 too "‾",
            0 to 2 too "‾͟",
            0 to 3 too "‾͟͟",
            0 to 4 too "‾͟͟͞",
            0 to 5 too "‾͟͟͞(",
            0 to 6 too "‾͟͟͞((",
            0 to 7 too "‾͟͟͞(((",
            0 to 8 too "‾͟͟͞(((ꎤ",
            0 to 9 too "‾͟͟͞(((ꎤ ",
            0 to 10 too "‾͟͟͞(((ꎤ ✧",
            0 to 11 too "‾͟͟͞(((ꎤ ✧曲",
            0 to 12 too "‾͟͟͞(((ꎤ ✧曲✧",
            0 to 13 too "‾͟͟͞(((ꎤ ✧曲✧)",
            0 to 14 too "‾͟͟͞(((ꎤ ✧曲✧)̂",
            0 to 15 too "‾͟͟͞(((ꎤ ✧曲✧)̂—",
            0 to 16 too "‾͟͟͞(((ꎤ ✧曲✧)̂—̳",
            0 to 17 too "‾͟͟͞(((ꎤ ✧曲✧)̂—̳͟",
            0 to 18 too "‾͟͟͞(((ꎤ ✧曲✧)̂—̳͟͞",
            0 to 19 too "‾͟͟͞(((ꎤ ✧曲✧)̂—̳͟͞͞",
            0 to 20 too "‾͟͟͞(((ꎤ ✧曲✧)̂—̳͟͞͞O",
            0 to 21 too "‾͟͟͞(((ꎤ ✧曲✧)̂—̳͟͞͞O ",
            0 to 22 too "‾͟͟͞(((ꎤ ✧曲✧)̂—̳͟͞͞O H",
            0 to 23 too "‾͟͟͞(((ꎤ ✧曲✧)̂—̳͟͞͞O HI",
            0 to 24 too "‾͟͟͞(((ꎤ ✧曲✧)̂—̳͟͞͞O HIT",
            0 to 25 too "‾͟͟͞(((ꎤ ✧曲✧)̂—̳͟͞͞O HIT!",
        ) { (startColumn: Int, endColumn: Int, expected) ->
            expecting { "‾͟͟͞(((ꎤ ✧曲✧)̂—̳͟͞͞O HIT!".subSequence(startColumn, endColumn) } that { isEqualTo(expected) }
        }

        @TestFactory
        fun `should return columns sub sequence ⧸ substring`() = testEach(
            0 to 0 too "",
            0 to 1 too "‾͟͟͞", // combining characters
            0 to 2 too "‾͟͟͞(",
            0 to 3 too "‾͟͟͞((",
            0 to 4 too "‾͟͟͞(((",
            0 to 5 too "‾͟͟͞(((ꎤ",
            0 to 6 too "‾͟͟͞(((ꎤ ",
            0 to 7 too "‾͟͟͞(((ꎤ ✧",
            0 to 8 too "‾͟͟͞(((ꎤ ✧",
            0 to 9 too "‾͟͟͞(((ꎤ ✧曲", // wide character
            0 to 10 too "‾͟͟͞(((ꎤ ✧曲✧",
            0 to 11 too "‾͟͟͞(((ꎤ ✧曲✧)̂",
            0 to 12 too "‾͟͟͞(((ꎤ ✧曲✧)̂—̳͟͞͞",
            0 to 13 too "‾͟͟͞(((ꎤ ✧曲✧)̂—̳͟͞͞O",
            0 to 14 too "‾͟͟͞(((ꎤ ✧曲✧)̂—̳͟͞͞O ",
            0 to 15 too "‾͟͟͞(((ꎤ ✧曲✧)̂—̳͟͞͞O H",
            0 to 16 too "‾͟͟͞(((ꎤ ✧曲✧)̂—̳͟͞͞O HI",
            0 to 17 too "‾͟͟͞(((ꎤ ✧曲✧)̂—̳͟͞͞O HIT",
            0 to 18 too "‾͟͟͞(((ꎤ ✧曲✧)̂—̳͟͞͞O HIT!",
        ) { (startColumn: Int, endColumn: Int, expected) ->
            expecting { "‾͟͟͞(((ꎤ ✧曲✧)̂—̳͟͞͞O HIT!".subSequenceByColumns(startColumn, endColumn) } that { isEqualTo(expected) }
            expecting { "‾͟͟͞(((ꎤ ✧曲✧)̂—̳͟͞͞O HIT!".substringByColumns(startColumn, endColumn) } that { isEqualTo(expected) }
        }

        @Test
        fun `should return last column by default`() {
            expecting { "‾͟͟͞(((ꎤ ✧曲✧)̂—̳͟͞͞O HIT!".substringByColumns(10) } that { isEqualTo(")̂—̳͟͞͞O HIT!") }
        }

        @TestFactory
        fun `should throw on out of bounds column`() = tests {
            expectThrows<IllegalArgumentException> { "‾͟͟͞(((ꎤ ✧曲✧)̂—̳͟͞͞O HIT!".subSequenceByColumns(19, 20) }
            expectThrows<IllegalArgumentException> { "‾͟͟͞(((ꎤ ✧曲✧)̂—̳͟͞͞O HIT!".substringByColumns(19, 20) }
        }

        @TestFactory
        fun `should throw on negative column`() = tests {
            expectThrows<IllegalArgumentException> { "‾͟͟͞(((ꎤ ✧曲✧)̂—̳͟͞͞O HIT!".subSequenceByColumns(-1, 10) }
            expectThrows<IllegalArgumentException> { "‾͟͟͞(((ꎤ ✧曲✧)̂—̳͟͞͞O HIT!".substringByColumns(-1) }
        }
    }

    @Nested
    inner class DropColumns {

        @TestFactory
        fun `should drop columns`() = testEach(
            0 to "‾͟͟͞(((ꎤ ✧曲✧)̂—̳͟͞͞O HIT!",
            1 to "(((ꎤ ✧曲✧)̂—̳͟͞͞O HIT!",
            2 to "((ꎤ ✧曲✧)̂—̳͟͞͞O HIT!",
            3 to "(ꎤ ✧曲✧)̂—̳͟͞͞O HIT!",
            4 to "ꎤ ✧曲✧)̂—̳͟͞͞O HIT!",
            5 to " ✧曲✧)̂—̳͟͞͞O HIT!",
            6 to "✧曲✧)̂—̳͟͞͞O HIT!",
            7 to "曲✧)̂—̳͟͞͞O HIT!",
            8 to "曲✧)̂—̳͟͞͞O HIT!",
            9 to "✧)̂—̳͟͞͞O HIT!",
            10 to ")̂—̳͟͞͞O HIT!",
            11 to "—̳͟͞͞O HIT!",
            12 to "O HIT!",
            13 to " HIT!",
            14 to "HIT!",
            15 to "IT!",
            16 to "T!",
            17 to "!",
            18 to "",
            19 to "",
        ) { (columns, expected) ->
            expecting { ("‾͟͟͞(((ꎤ ✧曲✧)̂—̳͟͞͞O HIT!" as CharSequence).dropColumns(columns) } that { isEqualTo(expected) }
            expecting { "‾͟͟͞(((ꎤ ✧曲✧)̂—̳͟͞͞O HIT!".dropColumns(columns) } that { isEqualTo(expected) }
        }

        @TestFactory
        fun `should throw on negative column`() = tests {
            expectThrows<IllegalArgumentException> { ("‾͟͟͞(((ꎤ ✧曲✧)̂—̳͟͞͞O HIT!" as CharSequence).dropColumns(-1) }
            expectThrows<IllegalArgumentException> { "‾͟͟͞(((ꎤ ✧曲✧)̂—̳͟͞͞O HIT!".dropColumns(-1) }
        }
    }

    @Nested
    inner class DropLastColumns {

        @TestFactory
        fun `should drop last columns`() = testEach(
            0 to "‾͟͟͞(((ꎤ ✧曲✧)̂—̳͟͞͞O HIT!",
            1 to "‾͟͟͞(((ꎤ ✧曲✧)̂—̳͟͞͞O HIT",
            2 to "‾͟͟͞(((ꎤ ✧曲✧)̂—̳͟͞͞O HI",
            3 to "‾͟͟͞(((ꎤ ✧曲✧)̂—̳͟͞͞O H",
            4 to "‾͟͟͞(((ꎤ ✧曲✧)̂—̳͟͞͞O ",
            5 to "‾͟͟͞(((ꎤ ✧曲✧)̂—̳͟͞͞O",
            6 to "‾͟͟͞(((ꎤ ✧曲✧)̂—̳͟͞͞",
            7 to "‾͟͟͞(((ꎤ ✧曲✧)̂",
            8 to "‾͟͟͞(((ꎤ ✧曲✧",
            9 to "‾͟͟͞(((ꎤ ✧曲",
            10 to "‾͟͟͞(((ꎤ ✧",
            11 to "‾͟͟͞(((ꎤ ✧",
            12 to "‾͟͟͞(((ꎤ ",
            13 to "‾͟͟͞(((ꎤ",
            14 to "‾͟͟͞(((",
            15 to "‾͟͟͞((",
            16 to "‾͟͟͞(",
            17 to "‾͟͟͞",
            18 to "",
            19 to "",
        ) { (columns, expected) ->
            expecting { ("‾͟͟͞(((ꎤ ✧曲✧)̂—̳͟͞͞O HIT!" as CharSequence).dropLastColumns(columns) } that { isEqualTo(expected) }
            expecting { "‾͟͟͞(((ꎤ ✧曲✧)̂—̳͟͞͞O HIT!".dropLastColumns(columns) } that { isEqualTo(expected) }
        }

        @TestFactory
        fun `should throw on negative column`() = tests {
            expectThrows<IllegalArgumentException> { ("‾͟͟͞(((ꎤ ✧曲✧)̂—̳͟͞͞O HIT!" as CharSequence).dropLastColumns(-1) }
            expectThrows<IllegalArgumentException> { "‾͟͟͞(((ꎤ ✧曲✧)̂—̳͟͞͞O HIT!".dropLastColumns(-1) }
        }
    }

    @Nested
    inner class TakeColumns {

        @TestFactory
        fun `should take columns`() = testEach(
            0 to "",
            1 to "‾͟͟͞",
            2 to "‾͟͟͞(",
            3 to "‾͟͟͞((",
            4 to "‾͟͟͞(((",
            5 to "‾͟͟͞(((ꎤ",
            6 to "‾͟͟͞(((ꎤ ",
            7 to "‾͟͟͞(((ꎤ ✧",
            8 to "‾͟͟͞(((ꎤ ✧",
            9 to "‾͟͟͞(((ꎤ ✧曲",
            10 to "‾͟͟͞(((ꎤ ✧曲✧",
            11 to "‾͟͟͞(((ꎤ ✧曲✧)̂",
            12 to "‾͟͟͞(((ꎤ ✧曲✧)̂—̳͟͞͞",
            13 to "‾͟͟͞(((ꎤ ✧曲✧)̂—̳͟͞͞O",
            14 to "‾͟͟͞(((ꎤ ✧曲✧)̂—̳͟͞͞O ",
            15 to "‾͟͟͞(((ꎤ ✧曲✧)̂—̳͟͞͞O H",
            16 to "‾͟͟͞(((ꎤ ✧曲✧)̂—̳͟͞͞O HI",
            17 to "‾͟͟͞(((ꎤ ✧曲✧)̂—̳͟͞͞O HIT",
            18 to "‾͟͟͞(((ꎤ ✧曲✧)̂—̳͟͞͞O HIT!",
            19 to "‾͟͟͞(((ꎤ ✧曲✧)̂—̳͟͞͞O HIT!",
        ) { (columns, expected) ->
            expecting { ("‾͟͟͞(((ꎤ ✧曲✧)̂—̳͟͞͞O HIT!" as CharSequence).takeColumns(columns) } that { isEqualTo(expected) }
            expecting { "‾͟͟͞(((ꎤ ✧曲✧)̂—̳͟͞͞O HIT!".takeColumns(columns) } that { isEqualTo(expected) }
        }

        @TestFactory
        fun `should throw on negative column`() = tests {
            expectThrows<IllegalArgumentException> { ("‾͟͟͞(((ꎤ ✧曲✧)̂—̳͟͞͞O HIT!" as CharSequence).takeColumns(-1) }
            expectThrows<IllegalArgumentException> { "‾͟͟͞(((ꎤ ✧曲✧)̂—̳͟͞͞O HIT!".takeColumns(-1) }
        }
    }

    @Nested
    inner class TakeLastColumns {

        @TestFactory
        fun `should take last columns`() = testEach(
            0 to "",
            1 to "!",
            2 to "T!",
            3 to "IT!",
            4 to "HIT!",
            5 to " HIT!",
            6 to "O HIT!",
            7 to "—̳͟͞͞O HIT!",
            8 to ")̂—̳͟͞͞O HIT!",
            9 to "✧)̂—̳͟͞͞O HIT!",
            10 to "曲✧)̂—̳͟͞͞O HIT!",
            11 to "曲✧)̂—̳͟͞͞O HIT!",
            12 to "✧曲✧)̂—̳͟͞͞O HIT!",
            13 to " ✧曲✧)̂—̳͟͞͞O HIT!",
            14 to "ꎤ ✧曲✧)̂—̳͟͞͞O HIT!",
            15 to "(ꎤ ✧曲✧)̂—̳͟͞͞O HIT!",
            16 to "((ꎤ ✧曲✧)̂—̳͟͞͞O HIT!",
            17 to "(((ꎤ ✧曲✧)̂—̳͟͞͞O HIT!",
            18 to "‾͟͟͞(((ꎤ ✧曲✧)̂—̳͟͞͞O HIT!",
            19 to "‾͟͟͞(((ꎤ ✧曲✧)̂—̳͟͞͞O HIT!",
        ) { (columns, expected) ->
            expecting { ("‾͟͟͞(((ꎤ ✧曲✧)̂—̳͟͞͞O HIT!" as CharSequence).takeLastColumns(columns) } that { isEqualTo(expected) }
            expecting { "‾͟͟͞(((ꎤ ✧曲✧)̂—̳͟͞͞O HIT!".takeLastColumns(columns) } that { isEqualTo(expected) }
        }

        @TestFactory
        fun `should throw on negative column`() = tests {
            expectThrows<IllegalArgumentException> { ("‾͟͟͞(((ꎤ ✧曲✧)̂—̳͟͞͞O HIT!" as CharSequence).takeLastColumns(-1) }
            expectThrows<IllegalArgumentException> { "‾͟͟͞(((ꎤ ✧曲✧)̂—̳͟͞͞O HIT!".takeLastColumns(-1) }
        }
    }

    @Nested
    inner class ChunkedByColumns {

        @TestFactory
        fun `should return chunked`() = testEach<CharSequence.(Int) -> List<String>>(
            { chunkedByColumns(it) },
            { chunkedByColumnsSequence(it).toList() },
        ) { chunkFn ->
            expecting { "‾͟͟͞(((ꎤ ✧曲✧)̂—̳͟͞͞O HIT!".chunkFn(4) } that {
                containsExactly("‾͟͟͞(((", "ꎤ ✧", "曲✧)̂", "—̳͟͞͞O H", "IT!")
            }
        }

        @TestFactory
        fun `should return transformed`() = testEach<CharSequence.(Int, (CharSequence) -> String) -> List<String>>(
            { columns, transform -> chunkedByColumns(columns, transform) },
            { columns, transform -> chunkedByColumnsSequence(columns, transform).toList() },
        ) { chunkFn ->
            expecting { "‾͟͟͞(((ꎤ ✧曲✧)̂—̳͟͞͞O HIT!".chunkFn(4) { it.padEnd(4, 'X').toString() } } that {
                containsExactly("‾͟͟͞(((", "ꎤ ✧X", "曲✧)̂", "—̳͟͞͞O H", "IT!X")
            }
        }
    }

    @Nested
    inner class PadStartByColumns {

        @TestFactory
        fun `should pad`() = testEach<CharSequence.(Int) -> CharSequence>(
            { padStartByColumns(it) },
            { toString().padStartByColumns(it) },
        ) { fn ->
            expecting { "‾͟͟͞(((ꎤ ✧曲✧)̂—̳͟͞͞O HIT!".fn(20) } that {
                toStringIsEqualTo("  ‾͟͟͞(((ꎤ ✧曲✧)̂—̳͟͞͞O HIT!")
                columns.isEqualTo(20)
            }
        }

        @TestFactory
        fun `should pad control characters`() = tests {
            val string = "a\u0003b".repeat(5)
            expecting { string.padStartByColumns(1) } that { isEqualTo(string) }
            expecting { string.padStartByColumns(11) } that { isEqualTo(" $string") }
        }

        @TestFactory
        fun `should pad with custom pad char`() = testEach<CharSequence.(Int, Char) -> CharSequence>(
            { columns, padChar -> padStartByColumns(columns, padChar) },
            { columns, padChar -> toString().padStartByColumns(columns, padChar) },
        ) { fn ->
            expecting { "‾͟͟͞(((ꎤ ✧曲✧)̂—̳͟͞͞O HIT!".fn(20, '⮕') } that {
                toStringIsEqualTo("⮕‾͟͟͞(((ꎤ ✧曲✧)̂—̳͟͞͞O HIT!")
                columns.isEqualTo(20)
            }
            expecting { "‾͟͟͞(((ꎤ ✧曲✧)̂—̳͟͞͞O HIT!".fn(21, '⮕') } that {
                toStringIsEqualTo(" ⮕‾͟͟͞(((ꎤ ✧曲✧)̂—̳͟͞͞O HIT!")
                columns.isEqualTo(21)
            }
        }

        @TestFactory
        fun `should not pad if wide enough already`() = testEach<CharSequence.(Int) -> CharSequence>(
            { padStartByColumns(it) },
            { toString().padStartByColumns(it) },
        ) { fn ->
            expecting { "‾͟͟͞(((ꎤ ✧曲✧)̂—̳͟͞͞O HIT!".fn(10) } that {
                toStringIsEqualTo("‾͟͟͞(((ꎤ ✧曲✧)̂—̳͟͞͞O HIT!")
                columns.isEqualTo(18)
            }
        }

        @TestFactory
        fun `should throw on negative columns`() = testEach<CharSequence.(Int) -> CharSequence>(
            { padStartByColumns(it) },
            { toString().padStartByColumns(it) },
        ) { fn ->
            expectThrows<IllegalArgumentException> { "‾͟͟͞(((ꎤ ✧曲✧)̂—̳͟͞͞O HIT!".fn(-1) }
        }

        @TestFactory
        fun `should throw in pad character with zero columns`() = testEach<CharSequence.(Char) -> CharSequence>(
            { padStartByColumns(10, it) },
            { toString().padStartByColumns(10, it) },
        ) { fn ->
            (0 until 32).forEach {
                expectThrows<IllegalArgumentException> { "‾͟͟͞(((ꎤ ✧曲✧)̂—̳͟͞͞O HIT!".fn(Unicode[it].char!!) }
            }
        }
    }

    @Nested
    inner class PadEndByColumns {

        @TestFactory
        fun `should pad`() = testEach<CharSequence.(Int) -> CharSequence>(
            { padEndByColumns(it) },
            { toString().padEndByColumns(it) },
        ) { fn ->
            expecting { "‾͟͟͞(((ꎤ ✧曲✧)̂—̳͟͞͞O HIT!".fn(20) } that {
                toStringIsEqualTo("‾͟͟͞(((ꎤ ✧曲✧)̂—̳͟͞͞O HIT!  ")
                columns.isEqualTo(20)
            }
        }

        @TestFactory
        fun `should pad control characters`() = tests {
            val string = "a\u0003b".repeat(5)
            expecting { string.padEndByColumns(1) } that { isEqualTo(string) }
            expecting { string.padEndByColumns(11) } that { isEqualTo("$string ") }
        }

        @TestFactory
        fun `should pad with custom pad char`() = testEach<CharSequence.(Int, Char) -> CharSequence>(
            { columns, padChar -> padEndByColumns(columns, padChar) },
            { columns, padChar -> toString().padEndByColumns(columns, padChar) },
        ) { fn ->
            expecting { "‾͟͟͞(((ꎤ ✧曲✧)̂—̳͟͞͞O HIT!".fn(20, '⮕') } that {
                toStringIsEqualTo("‾͟͟͞(((ꎤ ✧曲✧)̂—̳͟͞͞O HIT!⮕")
                columns.isEqualTo(20)
            }
            expecting { "‾͟͟͞(((ꎤ ✧曲✧)̂—̳͟͞͞O HIT!".fn(21, '⮕') } that {
                toStringIsEqualTo("‾͟͟͞(((ꎤ ✧曲✧)̂—̳͟͞͞O HIT!⮕ ")
                columns.isEqualTo(21)
            }
        }

        @TestFactory
        fun `should not pad if wide enough already`() = testEach<CharSequence.(Int) -> CharSequence>(
            { padEndByColumns(it) },
            { toString().padEndByColumns(it) },
        ) { fn ->
            expecting { "‾͟͟͞(((ꎤ ✧曲✧)̂—̳͟͞͞O HIT!".fn(10) } that {
                toStringIsEqualTo("‾͟͟͞(((ꎤ ✧曲✧)̂—̳͟͞͞O HIT!")
                columns.isEqualTo(18)
            }
        }

        @TestFactory
        fun `should throw on negative columns`() = testEach<CharSequence.(Int) -> CharSequence>(
            { padEndByColumns(it) },
            { toString().padEndByColumns(it) },
        ) { fn ->
            expectThrows<IllegalArgumentException> { "‾͟͟͞(((ꎤ ✧曲✧)̂—̳͟͞͞O HIT!".fn(-1) }
        }

        @TestFactory
        fun `should throw in pad character with zero columns`() = testEach<CharSequence.(Char) -> CharSequence>(
            { padEndByColumns(10, it) },
            { toString().padEndByColumns(10, it) },
        ) { fn ->
            (0 until 32).forEach {
                expectThrows<IllegalArgumentException> { "‾͟͟͞(((ꎤ ✧曲✧)̂—̳͟͞͞O HIT!".fn(Unicode[it].char!!) }
            }
        }
    }
}

val Builder<CharSequence>.columns
    get() = get("columns") { columns }
