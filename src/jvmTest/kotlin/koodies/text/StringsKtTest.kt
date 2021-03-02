package koodies.text

import koodies.test.test
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

        @Test
        fun `should add 4 random characters`() {
            val string = "the-string"
            expectThat(string.withRandomSuffix()) {
                startsWith("the-string-")
                matches(Regex("the-string-[0-9a-zA-Z]{4}"))
            }
        }

        @Test
        fun `should not append to already existing random suffix`() {
            val string = "the-string"
            expectThat(string.withRandomSuffix().withRandomSuffix()) {
                startsWith("the-string-")
                matches(Regex("the-string-[0-9a-zA-Z]{4}"))
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
}

fun <T : CharSequence> Assertion.Builder<T>.containsOnlyCharacters(chars: CharArray) =
    assert("contains only the characters " + chars.joinToString(", ")) {
        val unexpectedCharacters: CharSequence = it.filter { char: Char -> !chars.contains(char) }
        when (unexpectedCharacters.isEmpty()) {
            true -> pass()
            else -> fail("contained unexpected characters: " + unexpectedCharacters.toList().joinToString(", "))
        }
    }
