package com.bkahlert.kommons.text

import com.bkahlert.kommons.Platform
import com.bkahlert.kommons.Platform.Browser
import com.bkahlert.kommons.Platform.NodeJS
import com.bkahlert.kommons.debug.ClassWithCustomToString
import com.bkahlert.kommons.debug.ClassWithDefaultToString
import com.bkahlert.kommons.debug.OrdinaryClass
import com.bkahlert.kommons.debug.ThrowingClass
import com.bkahlert.kommons.test.testAll
import com.bkahlert.kommons.text.Char.characters
import com.bkahlert.kommons.text.CodePoint.Companion.codePoints
import com.bkahlert.kommons.text.Grapheme.Companion.graphemes
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.inspectors.forAll
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.sequences.shouldContainExactly
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldHaveLength
import io.kotest.matchers.string.shouldMatch
import io.kotest.matchers.string.shouldStartWith
import io.kotest.matchers.throwable.shouldHaveMessage
import io.kotest.matchers.types.shouldBeSameInstanceAs
import kotlin.test.Test

class StringsKtTest {

    @Test fun empty() = testAll {
        String.EMPTY shouldBe ""
    }

    @Test fun get() = testAll {
        shouldThrow<IndexOutOfBoundsException> { String.EMPTY[0..0] }.message shouldBe "begin 0, end 1, length 0"
        for (i in charSequence.indices) charSequence[i..i] shouldBe charSequence[i]
        shouldThrow<IllegalArgumentException> { charSequence[0..1] }.message shouldBe "The requested range 0..1 is not suitable to get a single character."
        shouldThrow<IndexOutOfBoundsException> { charSequence[charSequence.length..charSequence.length] }.message shouldBe "begin 13, end 14, length 13"
    }

    @Test fun contains_any() = testAll {
        "foo bar".containsAny("baz", "o b", "abc") shouldBe true
        "foo bar".containsAny("baz", "O B", "abc", ignoreCase = true) shouldBe true
        "foo bar".containsAny("baz", "O B", "abc") shouldBe false
        "foo bar".containsAny("baz", "---", "abc") shouldBe false
    }

    @Test fun check_bounds_index() = testAll {
        checkBoundsIndex(0..1, 0) shouldBe 0
        checkBoundsIndex(0..1, 1) shouldBe 1
        shouldThrow<IndexOutOfBoundsException> { checkBoundsIndex(0..1, -1) }.message shouldBe "index out of range: -1"
        shouldThrow<IndexOutOfBoundsException> { checkBoundsIndex(0..1, 2) }.message shouldBe "index out of range: 2"
    }

    @Suppress("EmptyRange")
    @Test fun check_bounds_indexes() = testAll {
        checkBoundsIndexes(2, 0, 0) shouldBe (0 until 0)
        checkBoundsIndexes(2, 0, 1) shouldBe (0 until 1)
        checkBoundsIndexes(2, 0, 2) shouldBe (0 until 2)
        checkBoundsIndexes(2, 1, 1) shouldBe (1 until 1)
        checkBoundsIndexes(2, 1, 2) shouldBe (1 until 2)
        checkBoundsIndexes(2, 2, 2) shouldBe (2 until 2)
        shouldThrow<IndexOutOfBoundsException> { checkBoundsIndexes(2, 2, 1) }.message shouldBe "begin 2, end 1, length 2"
        shouldThrow<IndexOutOfBoundsException> { checkBoundsIndexes(2, -1, 1) }.message shouldBe "begin -1, end 1, length 2"
        shouldThrow<IndexOutOfBoundsException> { checkBoundsIndexes(2, 1, 3) }.message shouldBe "begin 1, end 3, length 2"

        checkBoundsIndexes(2, 0 until 0) shouldBe (0 until 0)
        checkBoundsIndexes(2, 0 until 1) shouldBe (0 until 1)
        checkBoundsIndexes(2, 0 until 2) shouldBe (0 until 2)
        checkBoundsIndexes(2, 1 until 1) shouldBe (1 until 1)
        checkBoundsIndexes(2, 1 until 2) shouldBe (1 until 2)
        checkBoundsIndexes(2, 2 until 2) shouldBe (2 until 2)
        shouldThrow<IndexOutOfBoundsException> { checkBoundsIndexes(2, 2, 1) }.message shouldBe "begin 2, end 1, length 2"
        shouldThrow<IndexOutOfBoundsException> { checkBoundsIndexes(2, -1, 1) }.message shouldBe "begin -1, end 1, length 2"
        shouldThrow<IndexOutOfBoundsException> { checkBoundsIndexes(2, 1, 3) }.message shouldBe "begin 1, end 3, length 2"
    }

    @Test fun require_not_empty() = testAll {
        requireNotEmpty(charSequence) shouldBe charSequence
        requireNotEmpty(charSequence) { "error" } shouldBe charSequence
        requireNotEmpty(string) shouldBe string
        requireNotEmpty(string) { "error" } shouldBe string
        shouldThrow<IllegalArgumentException> { requireNotEmpty(emptyCharSequence) }
        shouldThrow<IllegalArgumentException> { requireNotEmpty(emptyCharSequence) { "error" } } shouldHaveMessage "error"
        shouldThrow<IllegalArgumentException> { requireNotEmpty(emptyString) }
        shouldThrow<IllegalArgumentException> { requireNotEmpty(emptyString) { "error" } } shouldHaveMessage "error"
        requireNotEmpty(blankCharSequence) shouldBe blankCharSequence
        requireNotEmpty(blankCharSequence) { "error" } shouldBe blankCharSequence
        requireNotEmpty(blankString) shouldBe blankString
        requireNotEmpty(blankString) { "error" } shouldBe blankString
    }

    @Test fun require_not_blank() = testAll {
        requireNotBlank(charSequence) shouldBe charSequence
        requireNotBlank(charSequence) { "error" } shouldBe charSequence
        requireNotBlank(string) shouldBe string
        requireNotBlank(string) { "error" } shouldBe string
        shouldThrow<IllegalArgumentException> { requireNotBlank(emptyCharSequence) }
        shouldThrow<IllegalArgumentException> { requireNotBlank(emptyCharSequence) { "error" } } shouldHaveMessage "error"
        shouldThrow<IllegalArgumentException> { requireNotBlank(emptyString) }
        shouldThrow<IllegalArgumentException> { requireNotBlank(emptyString) { "error" } } shouldHaveMessage "error"
        shouldThrow<IllegalArgumentException> { requireNotBlank(blankCharSequence) }
        shouldThrow<IllegalArgumentException> { requireNotBlank(blankCharSequence) { "error" } } shouldHaveMessage "error"
        shouldThrow<IllegalArgumentException> { requireNotBlank(blankString) }
        shouldThrow<IllegalArgumentException> { requireNotBlank(blankString) { "error" } } shouldHaveMessage "error"
    }

    @Test fun check_not_empty() = testAll {
        checkNotEmpty(charSequence) shouldBe charSequence
        checkNotEmpty(charSequence) { "error" } shouldBe charSequence
        checkNotEmpty(string) shouldBe string
        checkNotEmpty(string) { "error" } shouldBe string
        shouldThrow<IllegalStateException> { checkNotEmpty(emptyCharSequence) }
        shouldThrow<IllegalStateException> { checkNotEmpty(emptyCharSequence) { "error" } } shouldHaveMessage "error"
        shouldThrow<IllegalStateException> { checkNotEmpty(emptyString) }
        shouldThrow<IllegalStateException> { checkNotEmpty(emptyString) { "error" } } shouldHaveMessage "error"
        checkNotEmpty(blankCharSequence) shouldBe blankCharSequence
        checkNotEmpty(blankCharSequence) { "error" } shouldBe blankCharSequence
        checkNotEmpty(blankString) shouldBe blankString
        checkNotEmpty(blankString) { "error" } shouldBe blankString
    }

    @Test fun check_not_blank() = testAll {
        checkNotBlank(charSequence) shouldBe charSequence
        checkNotBlank(charSequence) { "error" } shouldBe charSequence
        checkNotBlank(string) shouldBe string
        checkNotBlank(string) { "error" } shouldBe string
        shouldThrow<IllegalStateException> { checkNotBlank(emptyCharSequence) }
        shouldThrow<IllegalStateException> { checkNotBlank(emptyCharSequence) { "error" } } shouldHaveMessage "error"
        shouldThrow<IllegalStateException> { checkNotBlank(emptyString) }
        shouldThrow<IllegalStateException> { checkNotBlank(emptyString) { "error" } } shouldHaveMessage "error"
        shouldThrow<IllegalStateException> { checkNotBlank(blankCharSequence) }
        shouldThrow<IllegalStateException> { checkNotBlank(blankCharSequence) { "error" } } shouldHaveMessage "error"
        shouldThrow<IllegalStateException> { checkNotBlank(blankString) }
        shouldThrow<IllegalStateException> { checkNotBlank(blankString) { "error" } } shouldHaveMessage "error"
    }

    @Test fun take_if_not_empty() = testAll {
        charSequence.takeIfNotEmpty() shouldBe charSequence
        string.takeIfNotEmpty() shouldBe string
        emptyCharSequence.takeIfNotEmpty() shouldBe null
        emptyString.takeIfNotEmpty() shouldBe null
        blankCharSequence.takeIfNotEmpty() shouldBe blankCharSequence
        blankString.takeIfNotEmpty() shouldBe blankString
    }

    @Test fun take_if_not_blank() = testAll {
        charSequence.takeIfNotBlank() shouldBe charSequence
        string.takeIfNotBlank() shouldBe string
        emptyCharSequence.takeIfNotBlank() shouldBe null
        emptyString.takeIfNotBlank() shouldBe null
        blankCharSequence.takeIfNotBlank() shouldBe null
        blankString.takeIfNotBlank() shouldBe null
    }

    @Test fun take_unless_empty() = testAll {
        charSequence.takeUnlessEmpty() shouldBe charSequence
        string.takeUnlessEmpty() shouldBe string
        emptyCharSequence.takeUnlessEmpty() shouldBe null
        emptyString.takeUnlessEmpty() shouldBe null
        blankCharSequence.takeUnlessEmpty() shouldBe blankCharSequence
        blankString.takeUnlessEmpty() shouldBe blankString
    }

    @Test fun take_unless_blank() = testAll {
        charSequence.takeUnlessBlank() shouldBe charSequence
        string.takeUnlessBlank() shouldBe string
        emptyCharSequence.takeUnlessBlank() shouldBe null
        emptyString.takeUnlessBlank() shouldBe null
        blankCharSequence.takeUnlessBlank() shouldBe null
        blankString.takeUnlessBlank() shouldBe null
    }


    @Test fun ansi_contained() = testAll {
        charSequence.ansiContained shouldBe false
        string.ansiContained shouldBe false
        emptyCharSequence.ansiContained shouldBe false
        emptyString.ansiContained shouldBe false
        blankCharSequence.ansiContained shouldBe false
        blankString.ansiContained shouldBe false
        ansiCsiCharSequence.ansiContained shouldBe true
        ansiCsiString.ansiContained shouldBe true
        ansiOscCharSequence.ansiContained shouldBe true
        ansiOscString.ansiContained shouldBe true
    }

    @Test fun ansi_removed() = testAll {
        charSequence.ansiRemoved shouldBe charSequence
        string.ansiRemoved shouldBe string
        emptyCharSequence.ansiRemoved shouldBe emptyCharSequence
        emptyString.ansiRemoved shouldBe emptyString
        blankCharSequence.ansiRemoved shouldBe blankCharSequence
        blankString.ansiRemoved shouldBe blankString
        ansiCsiCharSequence.ansiRemoved.toString() shouldBe "bold and blue"
        ansiCsiString.ansiRemoved shouldBe "bold and blue"
        ansiOscCharSequence.ansiRemoved.toString() shouldBe "↗ link"
        ansiOscString.ansiRemoved shouldBe "↗ link"
    }

    @Test fun spaced() = testAll {
        char.spaced shouldBe " $char "
        blankChar.spaced shouldBe " "
        nullChar.spaced shouldBeSameInstanceAs String.EMPTY
        char.startSpaced shouldBe " $char"
        blankChar.startSpaced shouldBe " "
        nullChar.startSpaced shouldBeSameInstanceAs String.EMPTY
        char.endSpaced shouldBe "$char "
        blankChar.endSpaced shouldBe " "
        nullChar.endSpaced shouldBeSameInstanceAs String.EMPTY

        charSequence.spaced shouldBe " $charSequence "
        emptyCharSequence.spaced shouldBeSameInstanceAs emptyCharSequence
        blankCharSequence.spaced shouldBeSameInstanceAs blankCharSequence
        nullCharSequence.spaced shouldBeSameInstanceAs String.EMPTY
        charSequence.startSpaced shouldBe " $charSequence"
        emptyCharSequence.startSpaced shouldBeSameInstanceAs emptyCharSequence
        blankCharSequence.startSpaced shouldBeSameInstanceAs blankCharSequence
        nullCharSequence.startSpaced shouldBeSameInstanceAs String.EMPTY
        charSequence.endSpaced shouldBe "$charSequence "
        emptyCharSequence.endSpaced shouldBeSameInstanceAs emptyCharSequence
        blankCharSequence.endSpaced shouldBeSameInstanceAs blankCharSequence
        nullCharSequence.endSpaced shouldBeSameInstanceAs String.EMPTY

        string.spaced shouldBe " $string "
        emptyString.spaced shouldBeSameInstanceAs emptyString
        blankString.spaced shouldBeSameInstanceAs blankString
        nullString.spaced shouldBeSameInstanceAs String.EMPTY
        string.startSpaced shouldBe " $string"
        emptyString.startSpaced shouldBeSameInstanceAs emptyString
        blankString.startSpaced shouldBeSameInstanceAs blankString
        nullString.startSpaced shouldBeSameInstanceAs String.EMPTY
        string.endSpaced shouldBe "$string "
        emptyString.endSpaced shouldBeSameInstanceAs emptyString
        blankString.endSpaced shouldBeSameInstanceAs blankString
        nullString.endSpaced shouldBeSameInstanceAs String.EMPTY
    }


    @Test fun truncate() = testAll(longString.cs, longString) {
        it.truncate() should { truncated ->
            truncated shouldBe it.truncate(length = 15.codePoints, marker = Unicode.ELLIPSIS.spaced)
            truncated.toString() shouldBe "a𝕓🫠🇩🇪👨 … ‍👩‍👦‍👦"
        }
        it.truncate(length = 100_000.graphemes) shouldBeSameInstanceAs it
        shouldThrow<IllegalArgumentException> { it.truncate(length = 7.characters, marker = "1234567890") }
            .message shouldBe "The specified length (7) must be greater or equal than the length of the marker \"1234567890\" (10)."
    }

    @Test fun truncate_start() = testAll(longString.cs, longString) {
        it.truncateStart() should { truncated ->
            truncated shouldBe it.truncateStart(length = 15.codePoints, marker = Unicode.ELLIPSIS.endSpaced)
            truncated.toString() shouldBe "… 🇩🇪👨🏾‍🦱👩‍👩‍👦‍👦"
        }
        it.truncateStart(length = 100_000.graphemes) shouldBeSameInstanceAs it
        shouldThrow<IllegalArgumentException> { it.truncateStart(length = 7.characters, marker = "1234567890") }
            .message shouldBe "The specified length (7) must be greater or equal than the length of the marker \"1234567890\" (10)."
    }

    @Test fun truncate_end() = testAll(longString.cs, longString) {
        it.truncateEnd() should { truncated ->
            truncated shouldBe it.truncateEnd(length = 15.codePoints, marker = Unicode.ELLIPSIS.startSpaced)
            truncated.toString() shouldBe "a𝕓🫠🇩🇪👨🏾‍🦱👩‍👩‍ …"
        }
        it.truncateEnd(length = 100_000.graphemes) shouldBeSameInstanceAs it
        shouldThrow<IllegalArgumentException> { it.truncateEnd(length = 7.characters, marker = "1234567890") }
            .message shouldBe "The specified length (7) must be greater or equal than the length of the marker \"1234567890\" (10)."
    }


    @Test fun with_prefix() = testAll {
        char.withPrefix("c") shouldBe "c"
        char.withPrefix("b") shouldBe "bc"
        char.withPrefix("bc") shouldBe "bcc"
        charSequence.withPrefix(charSequence) shouldBeSameInstanceAs charSequence
        charSequence.withPrefix("char") shouldBeSameInstanceAs charSequence
        charSequence.withPrefix("char-") shouldBe "char-$charSequence"
        string.withPrefix(string) shouldBeSameInstanceAs string
        string.withPrefix("str") shouldBeSameInstanceAs string
        string.withPrefix("str-") shouldBe "str-$string"
    }

    @Test fun with_suffix() = testAll {
        char.withSuffix("c") shouldBe "c"
        char.withSuffix("d") shouldBe "cd"
        char.withSuffix("cd") shouldBe "ccd"
        charSequence.withSuffix(charSequence) shouldBeSameInstanceAs charSequence
        charSequence.withSuffix("sequence") shouldBeSameInstanceAs charSequence
        charSequence.withSuffix("-sequence") shouldBe "$charSequence-sequence"
        string.withSuffix(string) shouldBeSameInstanceAs string
        string.withSuffix("ing") shouldBeSameInstanceAs string
        string.withSuffix("-ing") shouldBe "$string-ing"
    }

    @Test fun with_random_suffix() = testAll {
        char.withRandomSuffix() should {
            it shouldMatch Regex("$char--[\\da-zA-Z]{4}")
            it shouldStartWith "$char"
            it.withRandomSuffix() shouldBe it
        }
        charSequence.withRandomSuffix() should {
            it shouldMatch Regex("$charSequence--[\\da-zA-Z]{4}")
            it shouldStartWith charSequence
            it.withRandomSuffix() shouldBe it
        }
        string.withRandomSuffix() should {
            it shouldMatch Regex("$string--[\\da-zA-Z]{4}")
            it shouldStartWith string
            it.withRandomSuffix() shouldBe it
        }
    }

    @Test fun random_string() = testAll {
        randomString() shouldHaveLength 16
        randomString(7) shouldHaveLength 7

        val allowedByDefault = (('0'..'9') + ('a'..'z') + ('A'..'Z')).toList()
        randomString(10).forAll { allowedByDefault shouldContain it }

        randomString(10, 'A', 'B').forAll { listOf('A', 'B') shouldContain it }
    }

    @Test fun repeat() = testAll {
        shouldThrow<IllegalArgumentException> { char.repeat(-1) }
        char.repeat(0) shouldBe ""
        char.repeat(1) shouldBe "c"
        char.repeat(2) shouldBe "cc"
        char.repeat(3) shouldBe "ccc"
    }

    @Test fun index_of_or_null() = testAll {
        charSequence.indexOfOrNull('e') shouldBe 6
        charSequence.indexOfOrNull('E') shouldBe null
        charSequence.indexOfOrNull('e', ignoreCase = true) shouldBe 6
        charSequence.indexOfOrNull('e', startIndex = 7) shouldBe 9
        charSequence.indexOfOrNull('E', startIndex = 7) shouldBe null
        charSequence.indexOfOrNull('e', startIndex = 7, ignoreCase = true) shouldBe 9

        charSequence.indexOfOrNull("e") shouldBe 6
        charSequence.indexOfOrNull("E") shouldBe null
        charSequence.indexOfOrNull("e", ignoreCase = true) shouldBe 6
        charSequence.indexOfOrNull("e", startIndex = 7) shouldBe 9
        charSequence.indexOfOrNull("E", startIndex = 7) shouldBe null
        charSequence.indexOfOrNull("e", startIndex = 7, ignoreCase = true) shouldBe 9
    }

    @Test fun last_index_of_or_null() = testAll {
        charSequence.lastIndexOfOrNull('e') shouldBe 12
        charSequence.lastIndexOfOrNull('E') shouldBe null
        charSequence.lastIndexOfOrNull('e', ignoreCase = true) shouldBe 12
        charSequence.lastIndexOfOrNull('e', startIndex = 7) shouldBe 6
        charSequence.lastIndexOfOrNull('E', startIndex = 7) shouldBe null
        charSequence.lastIndexOfOrNull('e', startIndex = 7, ignoreCase = true) shouldBe 6

        charSequence.lastIndexOfOrNull("e") shouldBe 12
        charSequence.lastIndexOfOrNull("E") shouldBe null
        charSequence.lastIndexOfOrNull("e", ignoreCase = true) shouldBe 12
        charSequence.lastIndexOfOrNull("e", startIndex = 7) shouldBe 6
        charSequence.lastIndexOfOrNull("E", startIndex = 7) shouldBe null
        charSequence.lastIndexOfOrNull("e", startIndex = 7, ignoreCase = true) shouldBe 6
    }

    @Test fun as_string() = testAll {
        OrdinaryClass().asString() shouldBe when (Platform.Current) {
            Browser, NodeJS -> """
                OrdinaryClass {
                    baseProperty: "base-property",
                    openBaseProperty: 42／0x2a,
                    protectedOpenBaseProperty: "protected-open-base-property",
                    privateBaseProperty: "private-base-property",
                    ordinaryProperty: "ordinary-property",
                    privateOrdinaryProperty: "private-ordinary-property"
                }
            """.trimIndent()

            else -> """
                OrdinaryClass {
                    protectedOpenBaseProperty: "protected-open-base-property",
                    openBaseProperty: 42／0x2a,
                    baseProperty: "base-property",
                    privateOrdinaryProperty: "private-ordinary-property",
                    ordinaryProperty: "ordinary-property"
                }
            """.trimIndent()
        }
        if (Platform.Current != Browser && Platform.Current != NodeJS) {
            ThrowingClass().asString() shouldBe """
            ThrowingClass {
                throwingProperty: <error:java.lang.RuntimeException: error reading property>,
                privateThrowingProperty: <error:java.lang.RuntimeException: error reading private property>
            }
        """.trimIndent()
        }

        OrdinaryClass().asString(OrdinaryClass::ordinaryProperty) shouldBe """
            OrdinaryClass { ordinaryProperty: "ordinary-property" }
        """.trimIndent()

        OrdinaryClass().asString(exclude = listOf(OrdinaryClass::ordinaryProperty)) shouldBe when (Platform.Current) {
            Browser, NodeJS -> """
                OrdinaryClass {
                    baseProperty: "base-property",
                    openBaseProperty: 42／0x2a,
                    protectedOpenBaseProperty: "protected-open-base-property",
                    privateBaseProperty: "private-base-property",
                    privateOrdinaryProperty: "private-ordinary-property"
                }
            """.trimIndent()

            else -> """
                OrdinaryClass {
                    protectedOpenBaseProperty: "protected-open-base-property",
                    openBaseProperty: 42／0x2a,
                    baseProperty: "base-property",
                    privateOrdinaryProperty: "private-ordinary-property"
                }
            """.trimIndent()
        }

        ClassWithDefaultToString().asString() shouldBe """ClassWithDefaultToString { bar: "baz" }"""
        ClassWithDefaultToString().asString(excludeNullValues = true) shouldBe """ClassWithDefaultToString { bar: "baz" }"""
        ClassWithDefaultToString().asString(excludeNullValues = false) shouldBe """ClassWithDefaultToString { foo: null, bar: "baz" }"""

        ClassWithDefaultToString().let {
            it.asString {
                put(it::bar, "baz")
                put("baz", ClassWithCustomToString())
            }
        } shouldBe """ClassWithDefaultToString { bar: "baz", baz: custom toString }"""
    }

    @Test fun split_map() = testAll {
        "foo,bar".cs.splitMap(",") { ">$it<" } shouldBe ">foo<,>bar<"
        "foo-bar".cs.splitMap(",") { ">$it<" } shouldBe ">foo-bar<"
        "foo X bar".cs.splitMap(" X ") { ">$it<" } shouldBe ">foo< X >bar<"
        "foo X bar".cs.splitMap(" x ") { ">$it<" } shouldBe ">foo X bar<"
        "foo X bar".cs.splitMap(" x ", ignoreCase = true) { ">$it<" } shouldBe ">foo< x >bar<"
        "foo,bar,baz".cs.splitMap(",", limit = 2) { ">$it<" } shouldBe ">foo<,>bar,baz<"

        "foo,bar".splitMap(",") { ">$it<" } shouldBe ">foo<,>bar<"
        "foo-bar".splitMap(",") { ">$it<" } shouldBe ">foo-bar<"
        "foo X bar".splitMap(" X ") { ">$it<" } shouldBe ">foo< X >bar<"
        "foo X bar".splitMap(" x ") { ">$it<" } shouldBe ">foo X bar<"
        "foo X bar".splitMap(" x ", ignoreCase = true) { ">$it<" } shouldBe ">foo< x >bar<"
        "foo,bar,baz".splitMap(",", limit = 2) { ">$it<" } shouldBe ">foo<,>bar,baz<"
    }

    @Test fun split_to_sequence() = testAll {
        "foo X bar x baz".cs.splitToSequence(" X ").shouldContainExactly("foo", "bar x baz")
        "foo X bar x baz".cs.splitToSequence(" X ", " x ").shouldContainExactly("foo", "bar", "baz")
        "foo X bar x baz".cs.splitToSequence(" X ", " x ", keepDelimiters = true).shouldContainExactly("foo X ", "bar x ", "baz")
        "foo X bar x baz".cs.splitToSequence(" X ", ignoreCase = true).shouldContainExactly("foo", "bar", "baz")
        "foo X bar x baz".cs.splitToSequence(" X ", ignoreCase = true, keepDelimiters = true).shouldContainExactly("foo X ", "bar x ", "baz")
        "foo X bar x baz".cs.splitToSequence(" X ", " x ", limit = 2).shouldContainExactly("foo", "bar x baz")

        "foo X bar x baz".splitToSequence(" X ").shouldContainExactly("foo", "bar x baz")
        "foo X bar x baz".splitToSequence(" X ", " x ").shouldContainExactly("foo", "bar", "baz")
        "foo X bar x baz".splitToSequence(" X ", " x ", keepDelimiters = true).shouldContainExactly("foo X ", "bar x ", "baz")
        "foo X bar x baz".splitToSequence(" X ", ignoreCase = true).shouldContainExactly("foo", "bar", "baz")
        "foo X bar x baz".splitToSequence(" X ", ignoreCase = true, keepDelimiters = true).shouldContainExactly("foo X ", "bar x ", "baz")
        "foo X bar x baz".splitToSequence(" X ", " x ", limit = 2).shouldContainExactly("foo", "bar x baz")
    }
}

internal val String.cs: CharSequence get() = StringBuilder(this)

internal const val char: kotlin.Char = 'c'
internal const val blankChar: kotlin.Char = ' '
internal val nullChar: kotlin.Char? = null

internal val charSequence: CharSequence = StringBuilder("char sequence")
internal val emptyCharSequence: CharSequence = StringBuilder()
internal val blankCharSequence: CharSequence = StringBuilder("   ")
internal val nullCharSequence: CharSequence? = null

internal const val string: String = "string"
internal const val emptyString: String = ""
internal const val blankString: String = "   "
internal val nullString: String? = null

internal val emojiCharSequence: CharSequence = StringBuilder("a𝕓🫠🇩🇪👨🏾‍🦱👩‍👩‍👦‍👦")
internal val emojiString: String = emojiCharSequence.toString()
internal val emojiChars: Array<kotlin.Char> = arrayOf(
    'a',
    '\uD835',
    '\uDD53',
    '\uD83E',
    '\uDEE0',
    '\uD83C',
    '\uDDE9',
    '\uD83C',
    '\uDDEA',
    '\uD83D',
    '\uDC68',
    '\uD83C',
    '\uDFFE',
    '\u200D',
    '\uD83E',
    '\uDDB1',
    '\uD83D',
    '\uDC69',
    '\u200D',
    '\uD83D',
    '\uDC69',
    '\u200D',
    '\uD83D',
    '\uDC66',
    '\u200D',
    '\uD83D',
    '\uDC66',
)
internal val emojiCodePoints: Array<CodePoint> = arrayOf(
    CodePoint(0x0061),
    CodePoint(0x1D553),
    CodePoint(0x1FAE0),
    CodePoint(0x1F1E9),
    CodePoint(0x1F1EA),
    CodePoint(0x1F468),
    CodePoint(0x1F3FE),
    CodePoint(0x200D),
    CodePoint(0x1F9B1),
    CodePoint(0x1F469),
    CodePoint(0x200D),
    CodePoint(0x1F469),
    CodePoint(0x200D),
    CodePoint(0x1F466),
    CodePoint(0x200D),
    CodePoint(0x1F466),
)
internal val emojiGraphemes: Array<Grapheme> = arrayOf(
    Grapheme("a"),
    Grapheme("𝕓"),
    Grapheme("🫠"),
    Grapheme("🇩🇪"),
    Grapheme("👨🏾‍🦱"),
    Grapheme("👩‍👩‍👦‍👦"),
)

internal val longString = emojiString.repeat(1000)

/** [String] containing CSI (`control sequence intro`) escape sequences */
internal const val ansiCsiString: String = "[1mbold [34mand blue[0m"

/** [CharSequence] containing CSI (`control sequence intro`) escape sequences */
internal val ansiCsiCharSequence: CharSequence = StringBuilder().append(ansiCsiString)

/** [String] containing CSI (`control sequence intro`) escape sequences */
internal const val ansiOscString: String = "[34m↗(B[m ]8;;https://example.com\\link]8;;\\"

/** [CharSequence] containing CSI (`control sequence intro`) escape sequences */
internal val ansiOscCharSequence: CharSequence = StringBuilder().append(ansiOscString)
