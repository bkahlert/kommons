@file:Suppress("NonAsciiCharacters")

package com.bkahlert.kommons.text

import com.bkahlert.kommons.collections.too
import com.bkahlert.kommons.test.expectThrows
import com.bkahlert.kommons.test.expecting
import com.bkahlert.kommons.test.testEach
import com.bkahlert.kommons.test.tests
import com.bkahlert.kommons.test.toStringIsEqualTo
import com.bkahlert.kommons.text.ANSI.Text.Companion.ansi
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory
import org.junit.platform.launcher.TestExecutionListener
import org.junit.platform.launcher.TestPlan
import strikt.api.Assertion.Builder
import strikt.api.expectThat
import strikt.assertions.containsExactly
import strikt.assertions.isEqualTo
import strikt.assertions.isGreaterThan
import strikt.assertions.isLessThan

class TextWidthKtTest {

    class AwtInitializer : TestExecutionListener {
        override fun testPlanExecutionStarted(testPlan: TestPlan) {
            TextWidth.calculateWidth("implicit AWT initialization")
        }
    }

    @Test
    fun `should have positive x width`() {
        expecting { TextWidth.X_WIDTH } that { isGreaterThan(0) }
    }

    @Test
    fun `should have slack smaller x width`() {
        expecting { TextWidth.COLUMN_SLACK } that { isLessThan(TextWidth.X_WIDTH) }
    }

    @TextWidthRequiring @TestFactory
    fun `should calc smaller width for one-column string than two-column string`() = testEach("⮕", "😀", "👨🏾", "👩‍👩‍👧‍👧") {
        expecting { TextWidth.calculateWidth("X") } that { isLessThan(TextWidth.calculateWidth(it)) }
        expecting { TextWidth.calculateWidth("—") } that { isLessThan(TextWidth.calculateWidth(it)) }
    }

    @Nested
    inner class Columns {

        @Test
        fun `should calculate zero columns for empty string`() {
            expecting { "".columns } that { isEqualTo(0) }
        }

        @TestFactory
        fun `should calc one column for basic letters and digits`() = testEach("A", "a", "Ö", "ß", "4") {
            expecting { it.columns } that { isEqualTo(1) }
        }

        @TestFactory
        fun `should calc one for combined letter`() = testEach("a̳") {
            expecting { it.columns } that { isEqualTo(1) }
        }

        @TestFactory
        fun `should calc one column for small width code points`() = testEach(Whitespaces.HAIR_SPACE, Whitespaces.THIN_SPACE) {
            expecting { it.columns } that { isEqualTo(1) }
        }

        @TextWidthRequiring @TestFactory
        fun `should calc two columns for wide characters`() = testEach("⮕", "😀", "👨🏾", "👩‍👩‍👧‍👧") {
            expecting { it.columns } that { isEqualTo(2) }
        }

        @TestFactory
        fun `should calc zero columns for no-space characters`() = testEach(
            Unicode.ZERO_WIDTH_SPACE,
            Unicode.ZERO_WIDTH_JOINER,
// TODO            Unicode.ZERO_WIDTH_NO_BREAK_SPACE,
        ) {
            expecting { it.columns } that { isEqualTo(0) }
        }

        @TestFactory
        fun `should calc 0 columns for line separators`() = testEach(*LineSeparators.toTypedArray()) {
            expecting { it.first().columns } that { isEqualTo(0) }
            expecting { it.columns } that { isEqualTo(0) }
            expecting { "XXX${it}XX".columns } that { isEqualTo(5) }
        }

        @TestFactory
        fun `should calc columns for complex text`() = tests {
            expecting { "a̳o".columns } that { isEqualTo(2) }
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
            expecting { "😀".asCodePoint()!!.columns } that { isEqualTo(2) }
        }

        @TestFactory
        fun `should provide columns for grapheme cluster`() = tests {
            expecting { "A".asGraphemeCluster()!!.columns } that { isEqualTo(1) }
            expecting { "👩‍👩‍👧‍👧".asGraphemeCluster()!!.columns } that { isEqualTo(2) }
        }
    }

    @Nested
    inner class FindIndex {

        @TestFactory
        fun `should find index`() = testEach(
            0 to 0, // ""
            1 to 2, // ""
            2 to 3, // "a̳"
            3 to null, // "a̳o"
        ) { (column, expectedIndex) ->
            expecting { "a̳o".findIndexByColumns(column) } that { isEqualTo(expectedIndex) }
        }

        @Test
        fun `should throw on negative column`() {
            expectThrows<IllegalArgumentException> { "a̳o".findIndexByColumns(-1) }
        }
    }

    @Nested
    inner class ColumnsSubSequence {

        @TestFactory
        fun `should work with proper assumptions`() = testEach(
            0 to 0 too "",
            0 to 1 too "a",
            0 to 2 too "a̳",
            0 to 3 too "a̳o",
        ) { (startColumn: Int, endColumn: Int, expected) ->
            expecting { "a̳o".subSequence(startColumn, endColumn) } that { isEqualTo(expected) }
        }

        @TestFactory
        fun `should return columns sub sequence ⧸ substring`() = testEach(
            0 to 0 too "",
            0 to 1 too "a̳",
            0 to 2 too "a̳o",
        ) { (startColumn: Int, endColumn: Int, expected) ->
            expecting { "a̳o".subSequenceByColumns(startColumn, endColumn) } that { isEqualTo(expected) }
            expecting { "a̳o".substringByColumns(startColumn, endColumn) } that { isEqualTo(expected) }
        }

        @Test
        fun `should return till end of string by default`() {
            expecting { "a̳o".substringByColumns(1) } that { isEqualTo("o") }
        }

        @TestFactory
        fun `should throw on out of bounds column`() = tests {
            expectThrows<IllegalArgumentException> { "a̳o".subSequenceByColumns(3, 5) }
            expectThrows<IllegalArgumentException> { "a̳o".substringByColumns(3, 5) }
        }

        @TestFactory
        fun `should throw on negative column`() = tests {
            expectThrows<IllegalArgumentException> { "a̳o".subSequenceByColumns(-1, 3) }
            expectThrows<IllegalArgumentException> { "a̳o".substringByColumns(-1) }
        }
    }

    @Nested
    inner class DropColumns {

        @TestFactory
        fun `should drop columns`() = testEach(
            0 to "a̳o",
            1 to "o",
            2 to "",
        ) { (columns, expected) ->
            expecting { ("a̳o" as CharSequence).dropColumns(columns) } that { isEqualTo(expected) }
            expecting { "a̳o".dropColumns(columns) } that { isEqualTo(expected) }
        }

        @TestFactory
        fun `should throw on negative column`() = tests {
            expectThrows<IllegalArgumentException> { ("a̳o" as CharSequence).dropColumns(-1) }
            expectThrows<IllegalArgumentException> { "a̳o".dropColumns(-1) }
        }
    }

    @Nested
    inner class DropLastColumns {

        @TestFactory
        fun `should drop last columns`() = testEach(
            0 to "a̳o",
            1 to "a̳",
            2 to "",
        ) { (columns, expected) ->
            expecting { ("a̳o" as CharSequence).dropLastColumns(columns) } that { isEqualTo(expected) }
            expecting { "a̳o".dropLastColumns(columns) } that { isEqualTo(expected) }
        }

        @TestFactory
        fun `should throw on negative column`() = tests {
            expectThrows<IllegalArgumentException> { ("a̳o" as CharSequence).dropLastColumns(-1) }
            expectThrows<IllegalArgumentException> { "a̳o".dropLastColumns(-1) }
        }
    }

    @Nested
    inner class TakeColumns {

        @TestFactory
        fun `should take columns`() = testEach(
            0 to "",
            1 to "a̳",
            2 to "a̳o",
        ) { (columns, expected) ->
            expecting { ("a̳o" as CharSequence).takeColumns(columns) } that { isEqualTo(expected) }
            expecting { "a̳o".takeColumns(columns) } that { isEqualTo(expected) }
        }

        @TestFactory
        fun `should throw on negative column`() = tests {
            expectThrows<IllegalArgumentException> { ("a̳o" as CharSequence).takeColumns(-1) }
            expectThrows<IllegalArgumentException> { "a̳o".takeColumns(-1) }
        }
    }

    @Nested
    inner class TakeLastColumns {

        @TestFactory
        fun `should take last columns`() = testEach(
            0 to "",
            1 to "o",
            2 to "a̳o",
        ) { (columns, expected) ->
            expecting { ("a̳o" as CharSequence).takeLastColumns(columns) } that { isEqualTo(expected) }
            expecting { "a̳o".takeLastColumns(columns) } that { isEqualTo(expected) }
        }

        @TestFactory
        fun `should throw on negative column`() = tests {
            expectThrows<IllegalArgumentException> { ("a̳o" as CharSequence).takeLastColumns(-1) }
            expectThrows<IllegalArgumentException> { "a̳o".takeLastColumns(-1) }
        }
    }

    @Nested
    inner class ChunkedByColumns {

        @TestFactory
        fun `should return chunked`() = testEach<CharSequence.(Int) -> List<String>>(
            { chunkedByColumns(it) },
            { chunkedByColumnsSequence(it).toList() },
        ) { chunkFn ->
            expecting { "a̳a̳a̳".chunkFn(2) } that {
                containsExactly("a̳a̳", "a̳")
            }
        }

        @TestFactory
        fun `should return transformed`() = testEach<CharSequence.(Int, (CharSequence) -> String) -> List<String>>(
            { columns, transform -> chunkedByColumns(columns, transform) },
            { columns, transform -> chunkedByColumnsSequence(columns, transform).toList() },
        ) { chunkFn ->
            expecting { "a̳a̳a̳".chunkFn(2) { it.padEnd(3, 'X').toString() } } that {
                containsExactly("a̳a̳", "a̳X")
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
            expecting { "a̳o".fn(5) } that {
                toStringIsEqualTo("   a̳o")
                columns.isEqualTo(5)
            }
        }

        @TestFactory
        fun `should pad control characters`() = tests {
            val string = "a\u0003b".repeat(5)
            expecting { string.padStartByColumns(1) } that { isEqualTo(string) }
            expecting { string.padStartByColumns(11) } that { isEqualTo(" $string") }
        }

        @TestFactory
        fun `should pad with custom pad char`() = testEach<CharSequence.(Int, String) -> CharSequence>(
            { columns, padChar -> padStartByColumns(columns, padChar) },
            { columns, padChar -> toString().padStartByColumns(columns, padChar) },
        ) { fn ->
            expecting { "a̳o".fn(4, "😀") } that {
                toStringIsEqualTo("😀a̳o")
                columns.isEqualTo(4)
            }
            expecting { "a̳o".fn(5, "😀") } that {
                toStringIsEqualTo("😀a̳o")
                columns.isEqualTo(4)
            }
            expecting { "a̳o".fn(6, "😀") } that {
                toStringIsEqualTo("😀😀a̳o")
                columns.isEqualTo(6)
            }
        }

        @TestFactory
        fun `should not pad if wide enough already`() = testEach<CharSequence.(Int) -> CharSequence>(
            { padStartByColumns(it) },
            { toString().padStartByColumns(it) },
        ) { fn ->
            expecting { "a̳o".fn(2) } that {
                toStringIsEqualTo("a̳o")
                columns.isEqualTo(2)
            }
        }

        @TestFactory
        fun `should throw on negative columns`() = testEach<CharSequence.(Int) -> CharSequence>(
            { padStartByColumns(it) },
            { toString().padStartByColumns(it) },
        ) { fn ->
            expectThrows<IllegalArgumentException> { "a̳o".fn(-1) }
        }

        @TestFactory
        fun `should throw on pad character with zero columns`() = testEach<CharSequence.(Char) -> CharSequence>(
            { padStartByColumns(10, it.toString()) },
            { toString().padStartByColumns(10, it.toString()) },
        ) { fn ->
            (0 until 32).forEach {
                expectThrows<IllegalArgumentException> { "a̳o".fn(Unicode[it].char!!) }
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
            expecting { "a̳o".fn(5) } that {
                toStringIsEqualTo("a̳o   ")
                columns.isEqualTo(5)
            }
        }

        @TestFactory
        fun `should pad control characters`() = tests {
            val string = "a\u0003b".repeat(5)
            expecting { string.padEndByColumns(1) } that { isEqualTo(string) }
            expecting { string.padEndByColumns(11) } that { isEqualTo("$string ") }
        }

        @TestFactory
        fun `should pad with custom pad char`() = testEach<CharSequence.(Int, String) -> CharSequence>(
            { columns, padChar -> padEndByColumns(columns, padChar) },
            { columns, padChar -> toString().padEndByColumns(columns, padChar) },
        ) { fn ->
            expecting { "a̳o".fn(4, "😀") } that {
                toStringIsEqualTo("a̳o😀")
                columns.isEqualTo(4)
            }
            expecting { "a̳o".fn(5, "😀") } that {
                toStringIsEqualTo("a̳o😀")
                columns.isEqualTo(4)
            }
            expecting { "a̳o".fn(6, "😀") } that {
                toStringIsEqualTo("a̳o😀😀")
                columns.isEqualTo(6)
            }
        }

        @TestFactory
        fun `should not pad if wide enough already`() = testEach<CharSequence.(Int) -> CharSequence>(
            { padEndByColumns(it) },
            { toString().padEndByColumns(it) },
        ) { fn ->
            expecting { "a̳o".fn(2) } that {
                toStringIsEqualTo("a̳o")
                columns.isEqualTo(2)
            }
        }

        @TestFactory
        fun `should throw on negative columns`() = testEach<CharSequence.(Int) -> CharSequence>(
            { padEndByColumns(it) },
            { toString().padEndByColumns(it) },
        ) { fn ->
            expectThrows<IllegalArgumentException> { "a̳o".fn(-1) }
        }

        @TestFactory
        fun `should throw on pad character with zero columns`() = testEach<CharSequence.(Char) -> CharSequence>(
            { padEndByColumns(10, it.toString()) },
            { toString().padEndByColumns(10, it.toString()) },
        ) { fn ->
            (0 until 32).forEach {
                expectThrows<IllegalArgumentException> { "a̳o".fn(Unicode[it].char!!) }
            }
        }
    }


    private val longText = "1234567890".repeat(1000)

    @Nested
    inner class TruncateByColumns {

        @Test
        fun `should truncate from center`() {
            expectThat("12345678901234567890".truncateByColumns()).isEqualTo("123456 … 567890")
        }

        @Test
        fun `should truncate using columns`() {
            expectThat("😀😀😀😀😀😀😀😀😀😀😀😀".truncateByColumns()).isEqualTo("😀😀😀 … 😀😀😀")
        }

        @Test
        fun `should not truncate if not necessary`() {
            expectThat("1234567890".truncateByColumns()).toStringIsEqualTo("1234567890")
        }

        @Test
        fun `should truncate using custom marker`() {
            expectThat("12345678901234567890".truncateByColumns(marker = "...")).isEqualTo("123456...567890")
        }

        @Test
        fun `should truncate long text`() {
            expectThat(longText.truncateByColumns()).isEqualTo("123456 … 567890")
        }

        @Test
        fun `should throw if marker is wider than max length`() {
            expectThrows<IllegalArgumentException> {
                "1234567890".truncateByColumns(maxColumns = 1, marker = "XX")
            }
        }
    }

    @Nested
    inner class TruncateStart {

        @Test
        fun `should truncate from start`() {
            expectThat("12345678901234567890".truncateStartByColumns()).isEqualTo(" … 901234567890")
        }

        @Test
        fun `should truncate using columns`() {
            expectThat("😀😀😀😀😀😀😀😀😀😀😀".truncateStartByColumns()).isEqualTo(" … 😀😀😀😀😀😀")
        }

        @Test
        fun `should not truncate if not necessary`() {
            expectThat("1234567890".truncateStartByColumns()).toStringIsEqualTo("1234567890")
        }

        @Test
        fun `should truncate using custom marker`() {
            expectThat("12345678901234567890".truncateStartByColumns(marker = "...")).isEqualTo("...901234567890")
        }

        @Test
        fun `should truncate long text`() {
            expectThat(longText.truncateStartByColumns()).isEqualTo(" … 901234567890")
        }

        @Test
        fun `should throw if marker is wider than max length`() {
            expectThrows<IllegalArgumentException> {
                "1234567890".truncateStartByColumns(maxColumns = 1, marker = "XX")
            }
        }
    }

    @Nested
    inner class TruncateEnd {

        @Test
        fun `should truncate from end`() {
            expectThat("12345678901234567890".truncateEndByColumns()).isEqualTo("123456789012 … ")
        }

        @Test
        fun `should truncate using columns`() {
            expectThat("😀😀😀😀😀😀😀😀😀😀".truncateEndByColumns()).isEqualTo("😀😀😀😀😀😀 … ")
        }

        @Test
        fun `should not truncate if not necessary`() {
            expectThat("1234567890".truncateEndByColumns()).toStringIsEqualTo("1234567890")
        }

        @Test
        fun `should truncate using custom marker`() {
            expectThat("12345678901234567890".truncateEndByColumns(marker = "...")).isEqualTo("123456789012...")
        }

        @Test
        fun `should truncate long text`() {
            expectThat(longText.truncateEndByColumns()).isEqualTo("123456789012 … ")
        }

        @Test
        fun `should throw if marker is wider than max length`() {
            expectThrows<IllegalArgumentException> {
                "1234567890".truncateEndByColumns(maxColumns = 1, marker = "XX")
            }
        }
    }
}

val Builder<CharSequence>.columns
    get() = get("columns") { columns }
