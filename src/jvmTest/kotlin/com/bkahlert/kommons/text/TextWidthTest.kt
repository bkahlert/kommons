@file:Suppress("NonAsciiCharacters")

package com.bkahlert.kommons.text

import com.bkahlert.kommons.CodePoint
import com.bkahlert.kommons.Grapheme
import com.bkahlert.kommons.LineSeparators
import com.bkahlert.kommons.Unicode
import com.bkahlert.kommons.asCodePointSequence
import com.bkahlert.kommons.test.expectThrows
import com.bkahlert.kommons.test.expecting
import com.bkahlert.kommons.test.testEachOld
import com.bkahlert.kommons.test.testsOld
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
    fun `should calc smaller width for one-column string than two-column string`() = testEachOld("⮕", "😀", "👨🏾", "👩‍👩‍👧‍👧") {
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
        fun `should calc one column for basic letters and digits`() = testEachOld("A", "a", "Ö", "ß", "4") {
            expecting { it.columns } that { isEqualTo(1) }
        }

        @TextWidthRequiring @TestFactory
        fun `should calc one for combined letter`() = testEachOld("a̳") {
            expecting { it.columns } that { isEqualTo(1) }
        }

        @TestFactory
        fun `should calc one column for small width code points`() = testEachOld(Whitespaces.HAIR_SPACE, Whitespaces.THIN_SPACE) {
            expecting { it.columns } that { isEqualTo(1) }
        }

        @TextWidthRequiring @TestFactory
        fun `should calc two columns for wide characters`() = testEachOld("⮕", "😀", "👨🏾", "👩‍👩‍👧‍👧") {
            expecting { it.columns } that { isEqualTo(2) }
        }

        @TestFactory
        fun `should calc zero columns for no-space characters`() = testEachOld(
            Unicode.ZERO_WIDTH_SPACE,
            Unicode.ZERO_WIDTH_JOINER,
// TODO            Unicode.ZERO_WIDTH_NO_BREAK_SPACE,
        ) {
            expecting { it.columns } that { isEqualTo(0) }
        }

        @TestFactory
        fun `should calc 0 columns for line separators`() = testEachOld(*LineSeparators.Unicode) {
            expecting { it.first().columns } that { isEqualTo(0) }
            expecting { it.columns } that { isEqualTo(0) }
            expecting { "XXX${it}XX".columns } that { isEqualTo(5) }
        }

        @TextWidthRequiring @TestFactory
        fun `should calc columns for complex text`() = testsOld {
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
        fun `should provide columns for code points`() = testsOld {
            expecting { "A".asCodePointSequence().single().columns } that { isEqualTo(1) }
            expecting { "😀".asCodePointSequence().single().columns } that { isEqualTo(2) }
        }

        @TextWidthRequiring @TestFactory
        fun `should provide columns for grapheme cluster`() = testsOld {
            expecting { Grapheme("A").columns } that { isEqualTo(1) }
            expecting { Grapheme("👩‍👩‍👧‍👧").columns } that { isEqualTo(2) }
        }
    }

    @Nested
    inner class FindIndex {

        @TestFactory
        fun `should find index by index`() = testEachOld(
            0 to 0, // ""
            1 to 0, // ""
            2 to 2, // "😀"
            3 to 3, // "😀o"
        ) { (column, expectedIndex) ->
            expecting { "😀o".findIndexByColumns(column) } that { isEqualTo(expectedIndex) }
        }

        @Test
        fun `should throw on negative column`() {
            expectThrows<IllegalArgumentException> { "😀o".findIndexByColumns(-1) }
        }
    }

    @Nested
    inner class ColumnsSubSequence {

        @TestFactory
        fun `should return columns sub sequence ⧸ substring`() = testEachOld(
            Triple(0, 0, ""),
            Triple(0, 1, ""),
            Triple(0, 2, "😀"),
            Triple(0, 3, "😀o"),
        ) { (startColumn: Int, endColumn: Int, expected) ->
            expecting { "😀o".subSequenceByColumns(startColumn, endColumn) } that { isEqualTo(expected) }
            expecting { "😀o".substringByColumns(startColumn, endColumn) } that { isEqualTo(expected) }
        }

        @Test
        fun `should return till end of string by default`() {
            expecting { "😀o".substringByColumns(2) } that { isEqualTo("o") }
        }

        @TestFactory
        fun `should throw on out of bounds column`() = testsOld {
            expectThrows<IllegalArgumentException> { "😀o".subSequenceByColumns(3, 5) }
            expectThrows<IllegalArgumentException> { "😀o".substringByColumns(3, 5) }
        }

        @TestFactory
        fun `should throw on negative column`() = testsOld {
            expectThrows<IllegalArgumentException> { "😀o".subSequenceByColumns(-1, 3) }
            expectThrows<IllegalArgumentException> { "😀o".substringByColumns(-1) }
        }
    }

    @Nested
    inner class DropColumns {

        @TestFactory
        fun `should drop columns`() = testEachOld(
            0 to "😀o",
            1 to "😀o",
            2 to "o",
            3 to "",
        ) { (columns, expected) ->
            expecting { ("😀o" as CharSequence).dropColumns(columns) } that { isEqualTo(expected) }
            expecting { "😀o".dropColumns(columns) } that { isEqualTo(expected) }
        }

        @TestFactory
        fun `should throw on negative column`() = testsOld {
            expectThrows<IllegalArgumentException> { ("😀o" as CharSequence).dropColumns(-1) }
            expectThrows<IllegalArgumentException> { "😀o".dropColumns(-1) }
        }
    }

    @Nested
    inner class DropLastColumns {

        @TestFactory
        fun `should drop last columns`() = testEachOld(
            0 to "😀o",
            1 to "😀",
            2 to "",
            3 to "",
        ) { (columns, expected) ->
            expecting { ("😀o" as CharSequence).dropLastColumns(columns) } that { isEqualTo(expected) }
            expecting { "😀o".dropLastColumns(columns) } that { isEqualTo(expected) }
        }

        @TestFactory
        fun `should throw on negative column`() = testsOld {
            expectThrows<IllegalArgumentException> { ("😀o" as CharSequence).dropLastColumns(-1) }
            expectThrows<IllegalArgumentException> { "😀o".dropLastColumns(-1) }
        }
    }

    @Nested
    inner class TakeColumns {

        @TestFactory
        fun `should take columns`() = testEachOld(
            0 to "",
            1 to "",
            2 to "😀",
            3 to "😀o",
        ) { (columns, expected) ->
            expecting { ("😀o" as CharSequence).takeColumns(columns) } that { isEqualTo(expected) }
            expecting { "😀o".takeColumns(columns) } that { isEqualTo(expected) }
        }

        @TestFactory
        fun `should throw on negative column`() = testsOld {
            expectThrows<IllegalArgumentException> { ("😀o" as CharSequence).takeColumns(-1) }
            expectThrows<IllegalArgumentException> { "😀o".takeColumns(-1) }
        }
    }

    @Nested
    inner class TakeLastColumns {

        @TestFactory
        fun `should take last columns`() = testEachOld(
            0 to "",
            1 to "o",
            2 to "😀o",
            3 to "😀o",
        ) { (columns, expected) ->
            expecting { ("😀o" as CharSequence).takeLastColumns(columns) } that { isEqualTo(expected) }
            expecting { "😀o".takeLastColumns(columns) } that { isEqualTo(expected) }
        }

        @TestFactory
        fun `should throw on negative column`() = testsOld {
            expectThrows<IllegalArgumentException> { ("😀o" as CharSequence).takeLastColumns(-1) }
            expectThrows<IllegalArgumentException> { "😀o".takeLastColumns(-1) }
        }
    }

    @Nested
    inner class ChunkedByColumns {

        @TestFactory
        fun `should return chunked`() = testEachOld<CharSequence.(Int) -> List<String>>(
            { chunkedByColumns(it) },
            { chunkedByColumnsSequence(it).toList() },
        ) { chunkFn ->
            expecting { "😀😀😀".chunkFn(5) } that {
                containsExactly("😀😀", "😀")
            }
        }

        @TestFactory
        fun `should return transformed`() = testEachOld<CharSequence.(Int, (CharSequence) -> String) -> List<String>>(
            { columns, transform -> chunkedByColumns(columns, transform) },
            { columns, transform -> chunkedByColumnsSequence(columns, transform).toList() },
        ) { chunkFn ->
            expecting { "😀😀😀".chunkFn(5) { it.padEnd(3, 'X').toString() } } that {
                containsExactly("😀😀", "😀X")
            }
        }
    }

    @Nested
    inner class PadStartByColumns {

        @TestFactory
        fun `should pad`() = testEachOld<CharSequence.(Int) -> CharSequence>(
            { padStartByColumns(it) },
            { toString().padStartByColumns(it) },
        ) { fn ->
            expecting { "😀o".fn(5) } that {
                toStringIsEqualTo("  😀o")
                columns.isEqualTo(5)
            }
        }

        @TestFactory
        fun `should pad control characters`() = testsOld {
            val string = "a\u0003b".repeat(5)
            expecting { string.padStartByColumns(1) } that { isEqualTo(string) }
            expecting { string.padStartByColumns(11) } that { isEqualTo(" $string") }
        }

        @TestFactory
        fun `should pad with custom pad char`() = testEachOld<CharSequence.(Int, String) -> CharSequence>(
            { columns, padChar -> padStartByColumns(columns, padChar) },
            { columns, padChar -> toString().padStartByColumns(columns, padChar) },
        ) { fn ->
            expecting { "😀o".fn(4, "😀") } that {
                toStringIsEqualTo("😀o")
                columns.isEqualTo(3)
            }
            expecting { "😀o".fn(5, "😀") } that {
                toStringIsEqualTo("😀😀o")
                columns.isEqualTo(5)
            }
            expecting { "😀o".fn(6, "😀") } that {
                toStringIsEqualTo("😀😀o")
                columns.isEqualTo(5)
            }
        }

        @TestFactory
        fun `should not pad if wide enough already`() = testEachOld<CharSequence.(Int) -> CharSequence>(
            { padStartByColumns(it) },
            { toString().padStartByColumns(it) },
        ) { fn ->
            expecting { "😀o".fn(2) } that {
                toStringIsEqualTo("😀o")
            }
        }

        @TestFactory
        fun `should throw on negative columns`() = testEachOld<CharSequence.(Int) -> CharSequence>(
            { padStartByColumns(it) },
            { toString().padStartByColumns(it) },
        ) { fn ->
            expectThrows<IllegalArgumentException> { "😀o".fn(-1) }
        }

        @TestFactory
        fun `should throw on pad character with zero columns`() = testEachOld<CharSequence.(Char) -> CharSequence>(
            { padStartByColumns(10, it.toString()) },
            { toString().padStartByColumns(10, it.toString()) },
        ) { fn ->
            (0 until 32).forEach {
                expectThrows<IllegalArgumentException> { "😀o".fn(CodePoint(it).char!!) }
            }
        }
    }

    @Nested
    inner class PadEndByColumns {

        @TestFactory
        fun `should pad`() = testEachOld<CharSequence.(Int) -> CharSequence>(
            { padEndByColumns(it) },
            { toString().padEndByColumns(it) },
        ) { fn ->
            expecting { "😀o".fn(5) } that {
                toStringIsEqualTo("😀o  ")
                columns.isEqualTo(5)
            }
        }

        @TestFactory
        fun `should pad control characters`() = testsOld {
            val string = "a\u0003b".repeat(5)
            expecting { string.padEndByColumns(1) } that { isEqualTo(string) }
            expecting { string.padEndByColumns(11) } that { isEqualTo("$string ") }
        }

        @TestFactory
        fun `should pad with custom pad char`() = testEachOld<CharSequence.(Int, String) -> CharSequence>(
            { columns, padChar -> padEndByColumns(columns, padChar) },
            { columns, padChar -> toString().padEndByColumns(columns, padChar) },
        ) { fn ->
            expecting { "😀o".fn(4, "😀") } that {
                toStringIsEqualTo("😀o")
                columns.isEqualTo(3)
            }
            expecting { "😀o".fn(5, "😀") } that {
                toStringIsEqualTo("😀o😀")
                columns.isEqualTo(5)
            }
            expecting { "😀o".fn(6, "😀") } that {
                toStringIsEqualTo("😀o😀")
                columns.isEqualTo(5)
            }
        }

        @TestFactory
        fun `should not pad if wide enough already`() = testEachOld<CharSequence.(Int) -> CharSequence>(
            { padEndByColumns(it) },
            { toString().padEndByColumns(it) },
        ) { fn ->
            expecting { "😀o".fn(2) } that {
                toStringIsEqualTo("😀o")
            }
        }

        @TestFactory
        fun `should throw on negative columns`() = testEachOld<CharSequence.(Int) -> CharSequence>(
            { padEndByColumns(it) },
            { toString().padEndByColumns(it) },
        ) { fn ->
            expectThrows<IllegalArgumentException> { "😀o".fn(-1) }
        }

        @TestFactory
        fun `should throw on pad character with zero columns`() = testEachOld<CharSequence.(Char) -> CharSequence>(
            { padEndByColumns(10, it.toString()) },
            { toString().padEndByColumns(10, it.toString()) },
        ) { fn ->
            (0 until 32).forEach {
                expectThrows<IllegalArgumentException> { "😀o".fn(CodePoint(it).char!!) }
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
