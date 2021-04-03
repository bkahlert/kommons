package koodies.text

import koodies.test.test
import koodies.test.testEach
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
import strikt.assertions.isEqualTo
import strikt.assertions.matches
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
    fun `take if empty`() = testEach(
        "" to "",
        "abc" to null,
    ) { (string, expected) ->
        expect { string.takeIfEmpty() }.that { isEqualTo(expected) }
        expect { (string as CharSequence).takeIfEmpty() }.that { isEqualTo(expected) }
    }

    @TestFactory
    fun `take if blank`() = testEach(
        "" to "",
        " " to " ",
        *Unicode.whitespaces.map { it.toString() to it.toString() }.toTypedArray(),
        "abc" to null,
    ) { (string, expected) ->
        expect { string.takeIfBlank() }.that { isEqualTo(expected) }
        expect { (string as CharSequence).takeIfBlank() }.that { isEqualTo(expected) }
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
        *Unicode.whitespaces.map { it.toString() to null }.toTypedArray(),
        "abc" to "abc",
    ) { (string, expected) ->
        expect { string.takeUnlessBlank() }.that { isEqualTo(expected) }
        expect { (string as CharSequence).takeUnlessBlank() }.that { isEqualTo(expected) }
    }
}

fun <T : CharSequence> Assertion.Builder<T>.containsOnlyCharacters(chars: CharArray) =
    assert("contains only the characters " + chars.joinToString(", ")) {
        val unexpectedCharacters: CharSequence = it.filter { char: Char -> !chars.contains(char) }
        when (unexpectedCharacters.isEmpty()) {
            true -> pass()
            else -> fail("contained unexpected characters: " + unexpectedCharacters.toList().joinToString(", "))
        }
    }
