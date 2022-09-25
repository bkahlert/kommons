package com.bkahlert.kommons_deprecated.text

import com.bkahlert.kommons_deprecated.test.AnsiRequiring
import com.bkahlert.kommons_deprecated.test.testOld
import com.bkahlert.kommons_deprecated.test.testsOld
import com.bkahlert.kommons_deprecated.test.toStringIsEqualTo
import com.bkahlert.kommons_deprecated.text.ANSI.Text.Companion.ansi
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.TestFactory
import strikt.api.Assertion
import strikt.assertions.isEqualTo
import strikt.assertions.isNotNull
import strikt.assertions.isNull
import strikt.assertions.matches
import strikt.assertions.message

class StringsKtTest {

    @AnsiRequiring @TestFactory
    fun `should return length`() = testsOld {
        expecting { "red".length() } that { isEqualTo(3) }
        expecting { "red".ansi.red.length() } that { isEqualTo(3) }
        expecting { "red".length(ansi = true) } that { isEqualTo(3) }
        expecting { "red".ansi.red.length(ansi = true) } that { isEqualTo(3) }
        expecting { "red".length(ansi = false) } that { isEqualTo(3) }
        expecting { "red".ansi.red.length(ansi = false) } that { isEqualTo(13) }
    }

    @Suppress("SpellCheckingInspection")
    @AnsiRequiring @TestFactory
    fun `should pad start`() = testsOld {
        expecting { "red".padStart(15, 'X') } that { toStringIsEqualTo("XXXXXXXXXXXXred") }
        expecting { "red".ansi.red.padStart(15, 'X') } that { toStringIsEqualTo("XXXXXXXXXXXXred") }
        expecting { "red".padStart(15, 'X', ansi = false) } that { toStringIsEqualTo("XXXXXXXXXXXXred") }
        expecting { "red".ansi.red.padStart(15, 'X', ansi = false) } that { toStringIsEqualTo("XXred") }
    }

    @Suppress("SpellCheckingInspection")
    @AnsiRequiring @TestFactory
    fun `should pad end`() = testsOld {
        expecting { "red".padEnd(15, 'X') } that { toStringIsEqualTo("redXXXXXXXXXXXX") }
        expecting { "red".ansi.red.padEnd(15, 'X') } that { toStringIsEqualTo("redXXXXXXXXXXXX") }
        expecting { "red".padEnd(15, 'X', ansi = false) } that { toStringIsEqualTo("redXXXXXXXXXXXX") }
        expecting { "red".ansi.red.padEnd(15, 'X', ansi = false) } that { toStringIsEqualTo("redXX") }
    }

    @TestFactory
    fun `wrap multiline`() = testOld("foo".wrapMultiline("  bar 1\n    bar 2", "  \n    baz 1\n    baz 2\n        ")) {
        asserting {
            isEqualTo(
                """
            bar 1
              bar 2
            foo
            baz 1
            baz 2
        """.trimIndent()
            )
        }
    }

    @Nested
    inner class MapColumns {
        private val Int.ansi get() = "column$this".ansi.red

        @AnsiRequiring @TestFactory
        fun `should map two columns`() = testsOld {
            val transform: (String, String) -> String = { c1, c2 -> "${c1.last()}-${c2.last()}" }
            expecting { "column1\tcolumn2".mapColumns(transform = transform) } that { isEqualTo("1-2") }
            expecting { "column1\ncolumn2".mapColumns(delimiter = "\n", transform = transform) } that { isEqualTo("1-2") }
            expecting { "column1\tcolumn2".mapColumns(2, 1, transform = transform) } that { isEqualTo("2-1") }
            expectThrows<IllegalArgumentException> {
                "column1\tcolumn2".mapColumns(0, -1, transform = transform)
            } that { message.isNotNull().removeAnsi.isEqualTo("index 0 and index -1 must be greater than or equal to 1") }
            expecting { "column1\tcolumn2\tcolumn3".mapColumns(limit = 2, transform = transform) } that { isEqualTo("1-3") }
            expectThrows<IllegalArgumentException> { "column1\tcolumn2".mapColumns(limit = 1, transform = transform) }
            expectThrows<NoSuchElementException> { "column1".mapColumns(transform = transform) }
            expecting { "column1".mapColumnsOrNull(transform = transform) } that { isNull() }
            expecting { "${1.ansi}\t${2.ansi}".mapColumns(transform = transform) } that { isEqualTo("1-2") }
            expecting { "${1.ansi}\t${2.ansi}".mapColumns(removeAnsi = false, transform = transform) } that { isEqualTo("m-m") }
        }

        @AnsiRequiring @TestFactory
        fun `should map three columns`() = testsOld {
            val transform: (String, String, String) -> String = { c1, c2, c3 -> "${c1.last()}-${c2.last()}-${c3.last()}" }
            expecting { "column1\tcolumn2\tcolumn3".mapColumns(transform = transform) } that { isEqualTo("1-2-3") }
            expecting { "column1\ncolumn2\ncolumn3".mapColumns(delimiter = "\n", transform = transform) } that { isEqualTo("1-2-3") }
            expecting { "column1\tcolumn2\tcolumn3".mapColumns(3, 1, 2, transform = transform) } that { isEqualTo("3-1-2") }
            expectThrows<IllegalArgumentException> {
                "column1\tcolumn2\tcolumn3".mapColumns(0, -1, -2, transform = transform)
            } that { message.isNotNull().removeAnsi.isEqualTo("index 0 and index -1 and index -2 must be greater than or equal to 1") }
            expecting { "column1\tcolumn2\tcolumn3\tcolumn4".mapColumns(limit = 3, transform = transform) } that { isEqualTo("1-2-4") }
            expectThrows<IllegalArgumentException> { "column1\tcolumn2\tcolumn3".mapColumns(limit = 2, transform = transform) }
            expectThrows<NoSuchElementException> { "column1\tcolumn2".mapColumns(transform = transform) }
            expecting { "column1\tcolumn2".mapColumnsOrNull(transform = transform) } that { isNull() }
            expecting { "${1.ansi}\t${2.ansi}\t${3.ansi}".mapColumns(transform = transform) } that { isEqualTo("1-2-3") }
            expecting { "${1.ansi}\t${2.ansi}\t${3.ansi}".mapColumns(removeAnsi = false, transform = transform) } that { isEqualTo("m-m-m") }
        }
    }
}

fun <T : CharSequence> Assertion.Builder<T>.containsOnlyCharacters(chars: CharArray) =
    assert("contains only the characters " + chars.joinToString(", ")) {
        val unexpectedCharacters: CharSequence = it.filter { char: kotlin.Char -> !chars.contains(char) }
        when (unexpectedCharacters.isEmpty()) {
            true -> pass()
            else -> fail("contained unexpected characters: " + unexpectedCharacters.toList().joinToString(", "))
        }
    }

fun <T : CharSequence> Assertion.Builder<T>.endsWithRandomSuffix(): Assertion.Builder<T> =
    assert("ends with random suffix") {
        matches(Regex(".*--[0-9a-zA-Z]{4}"))
    }
