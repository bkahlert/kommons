package koodies.text

import koodies.test.test
import koodies.test.testEach
import koodies.text.ANSI.Text.Companion.ansi
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode.CONCURRENT
import strikt.api.Assertion
import strikt.api.expectThat
import strikt.assertions.doesNotContain
import strikt.assertions.hasLength
import strikt.assertions.hasSize
import strikt.assertions.isA
import strikt.assertions.isEqualTo
import strikt.assertions.isFailure
import strikt.assertions.isNotNull
import strikt.assertions.isNull
import strikt.assertions.matches
import strikt.assertions.message
import strikt.assertions.startsWith

@Execution(CONCURRENT)
class StringsKtTest {

    @Nested
    inner class RandomKtTest {

        @Test
        fun `should have 16 length by default`() {
            expectThat(randomString()).hasLength(16)
        }

        @Test
        fun `should have allow variable length`() {
            expectThat(randomString(7)).hasLength(7)
        }

        @Test
        fun `should only use alphanumeric characters by default`() {
            expectThat(randomString(10000)).containsOnlyCharacters(CharRanges.Alphanumeric)
        }

        @Test
        fun `should not easily produce the same string`() {
            val calculated = mutableListOf<String>()
            (0 until 1000).onEach {
                calculated += randomString(8).also {
                    expectThat(calculated).doesNotContain(it)
                }
            }
            expectThat(calculated).hasSize(1000)
        }

        @Test
        fun `should allow different character ranges`() {
            expectThat(randomString(1000, charArrayOf('A', 'B'))).containsOnlyCharacters(charArrayOf('A', 'B'))
        }
    }

    @Nested
    inner class WithPrefixKtTest {
        @Test
        fun `should prepend prefix if missing`() {
            expectThat("foo".withPrefix("bar")).isEqualTo("barfoo")
        }

        @Test
        fun `should fully prepend prefix if partially present`() {
            expectThat("rfoo".withPrefix("bar")).isEqualTo("barrfoo")
        }

        @Test
        fun `should not prepend prefix if present`() {
            expectThat("barfoo".withPrefix("bar")).isEqualTo("barfoo")
        }
    }

    @Nested
    inner class WithRandomSuffixKtTest {

        @TestFactory
        fun `should add 4 random characters`() = testEach("the-string", "four-char") {
            expect { it.withRandomSuffix() }.that {
                startsWith("$it-")
                matches(Regex("$it--[0-9a-zA-Z]{4}"))
            }
        }

        @TestFactory
        fun `should not append to already existing random suffix`() = testEach("the-string", "four-char") {
            expect { it.withRandomSuffix().withRandomSuffix() }.that {
                startsWith("$it-")
                matches(Regex("$it--[0-9a-zA-Z]{4}"))
                endsWithRandomSuffix()
            }
        }
    }

    @Nested
    inner class WithSuffixKtTest {
        @Test
        fun `should append suffix if missing`() {
            expectThat("foo".withSuffix("bar")).isEqualTo("foobar")
        }

        @Test
        fun `should fully append suffix if partially missing`() {
            expectThat("foob".withSuffix("bar")).isEqualTo("foobbar")
        }

        @Test
        fun `should not append suffix if present`() {
            expectThat("foobar".withSuffix("bar")).isEqualTo("foobar")
        }
    }

    @TestFactory
    fun `wrap multiline`() = test("foo".wrapMultiline("  bar 1\n    bar 2", "  \n    baz 1\n    baz 2\n        ")) {
        expect { this }.that {
            isEqualTo("""
            bar 1
              bar 2
            foo
            baz 1
            baz 2
        """.trimIndent())
        }
    }

    @TestFactory
    fun `take if not empty`() = testEach(
        "" to null,
        "abc" to "abc",
    ) { (string, expected) ->
        expect { string.takeIfNotEmpty() }.that { isEqualTo(expected) }
        expect { (string as CharSequence).takeIfNotEmpty() }.that { isEqualTo(expected) }
    }

    @TestFactory
    fun `take if not blank`() = testEach(
        "" to null,
        " " to null,
        *Whitespaces.map { it to null }.toTypedArray(),
        "abc" to "abc",
    ) { (string, expected) ->
        expect { string.takeIfNotBlank() }.that { isEqualTo(expected) }
        expect { (string as CharSequence).takeIfNotBlank() }.that { isEqualTo(expected) }
    }

    @TestFactory
    fun `take unless empty`() = testEach(
        "" to null,
        "abc" to "abc",
    ) { (string, expected) ->
        expect { string.takeUnlessEmpty() }.that { isEqualTo(expected) }
        expect { (string as CharSequence).takeUnlessEmpty() }.that { isEqualTo(expected) }
    }

    @TestFactory
    fun `take unless blank`() = testEach(
        "" to null,
        " " to null,
        *Whitespaces.map { it to null }.toTypedArray(),
        "abc" to "abc",
    ) { (string, expected) ->
        expect { string.takeUnlessBlank() }.that { isEqualTo(expected) }
        expect { (string as CharSequence).takeUnlessBlank() }.that { isEqualTo(expected) }
    }

    @Nested
    inner class MapColumns {
        private val Int.ansi get() = "column$this".ansi.red

        @TestFactory
        fun `should map two columns`() = test {
            val transform: (String, String) -> String = { c1, c2 -> "${c1.last()}-${c2.last()}" }
            expect { "column1\tcolumn2".mapColumns(transform = transform) }.that { isEqualTo("1-2") }
            expect { "column1\ncolumn2".mapColumns(delimiter = "\n", transform = transform) }.that { isEqualTo("1-2") }
            expect { "column1\tcolumn2".mapColumns(2, 1, transform = transform) }.that { isEqualTo("2-1") }
            expectThrowing { "column1\tcolumn2".mapColumns(0, -1, transform = transform) }
                .isFailure().message.isNotNull().ansiRemoved.isEqualTo("index 0 and index -1 must be greater than or equal to 1")
            expect { "column1\tcolumn2\tcolumn3".mapColumns(limit = 2, transform = transform) }.that { isEqualTo("1-3") }
            expectThrowing { "column1\tcolumn2".mapColumns(limit = 1, transform = transform) }.isFailure()
            expectThrowing { "column1".mapColumns(transform = transform) }.that { isFailure().isA<NoSuchElementException>() }
            expect { "column1".mapColumnsOrNull(transform = transform) }.that { isNull() }
            expect { "${1.ansi}\t${2.ansi}".mapColumns(transform = transform) }.that { isEqualTo("1-2") }
            expect { "${1.ansi}\t${2.ansi}".mapColumns(removeAnsi = false, transform = transform) }.that { isEqualTo("m-m") }
        }

        @TestFactory
        fun `should map three columns`() = test {
            val transform: (String, String, String) -> String = { c1, c2, c3 -> "${c1.last()}-${c2.last()}-${c3.last()}" }
            expect { "column1\tcolumn2\tcolumn3".mapColumns(transform = transform) }.that { isEqualTo("1-2-3") }
            expect { "column1\ncolumn2\ncolumn3".mapColumns(delimiter = "\n", transform = transform) }.that { isEqualTo("1-2-3") }
            expect { "column1\tcolumn2\tcolumn3".mapColumns(3, 1, 2, transform = transform) }.that { isEqualTo("3-1-2") }
            expectThrowing { "column1\tcolumn2\tcolumn3".mapColumns(0, -1, -2, transform = transform) }
                .isFailure().message.isNotNull().ansiRemoved.isEqualTo("index 0 and index -1 and index -2 must be greater than or equal to 1")
            expect { "column1\tcolumn2\tcolumn3\tcolumn4".mapColumns(limit = 3, transform = transform) }.that { isEqualTo("1-2-4") }
            expectThrowing { "column1\tcolumn2\tcolumn3".mapColumns(limit = 2, transform = transform) }.isFailure()
            expectThrowing { "column1\tcolumn2".mapColumns(transform = transform) }.that { isFailure().isA<NoSuchElementException>() }
            expect { "column1\tcolumn2".mapColumnsOrNull(transform = transform) }.that { isNull() }
            expect { "${1.ansi}\t${2.ansi}\t${3.ansi}".mapColumns(transform = transform) }.that { isEqualTo("1-2-3") }
            expect { "${1.ansi}\t${2.ansi}\t${3.ansi}".mapColumns(removeAnsi = false, transform = transform) }.that { isEqualTo("m-m-m") }
        }
    }
}

public fun <T : CharSequence> Assertion.Builder<T>.containsOnlyCharacters(chars: CharArray) =
    assert("contains only the characters " + chars.joinToString(", ")) {
        val unexpectedCharacters: CharSequence = it.filter { char: Char -> !chars.contains(char) }
        when (unexpectedCharacters.isEmpty()) {
            true -> pass()
            else -> fail("contained unexpected characters: " + unexpectedCharacters.toList().joinToString(", "))
        }
    }

public fun <T : CharSequence> Assertion.Builder<T>.endsWithRandomSuffix(): Assertion.Builder<T> {
    return assert("ends with random suffix") {
        matches(Regex(".*--[0-9a-zA-Z]{4}"))
    }
}
