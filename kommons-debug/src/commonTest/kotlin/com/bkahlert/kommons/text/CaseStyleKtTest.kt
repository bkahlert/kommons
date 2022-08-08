package com.bkahlert.kommons.text

import com.bkahlert.kommons.test.testAll
import com.bkahlert.kommons.test.testEnum
import io.kotest.assertions.asClue
import io.kotest.assertions.withClue
import io.kotest.inspectors.forAll
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import kotlin.js.JsName
import kotlin.test.Test

class CaseStyleKtTest {

    @Test
    fun camel_case_matches() = testAll {
        testCaseStyleMatches(CaseStyle.camelCase)
    }

    @Test
    fun camel_case_split() = testAll {
        testCaseStyleSplit(CaseStyle.camelCase)
    }

    @Test
    fun camel_case_join() = testAll {
        testCaseStyleJoin(CaseStyle.camelCase)
    }


    @Test
    fun pascal_case_matches() = testAll {
        testCaseStyleMatches(CaseStyle.PascalCase, "FOO")
    }

    @Test
    fun pascal_case_split() = testAll {
        testCaseStyleSplit(CaseStyle.PascalCase)
    }

    @Test
    fun pascal_case_join() = testAll {
        testCaseStyleJoin(CaseStyle.PascalCase)
    }


    @Test
    fun screaming_snake_case_matches() = testAll {
        testCaseStyleMatches(CaseStyle.SCREAMING_SNAKE_CASE, "BAZ")
    }

    @Test
    fun screaming_snake_case_split() = testAll {
        testCaseStyleSplit(CaseStyle.SCREAMING_SNAKE_CASE)
    }

    @Test
    fun screaming_snake_case_join() = testAll {
        testCaseStyleJoin(CaseStyle.SCREAMING_SNAKE_CASE)
    }


    @Test
    fun kebab_case_matches() = testAll {
        testCaseStyleMatches(CaseStyle.`kebab-case`)
    }

    @Test
    fun kebab_case_split() = testAll {
        testCaseStyleSplit(CaseStyle.`kebab-case`)
    }

    @Test
    fun kebab_case_join() = testAll {
        testCaseStyleJoin(CaseStyle.`kebab-case`)
    }


    @Test
    fun title_case_matches() = testAll {
        testCaseStyleMatches(CaseStyle.`Title Case`, "BFoo")
    }

    @Test
    fun title_case_split() = testAll {
        testCaseStyleSplit(CaseStyle.`Title Case`)
    }

    @Test
    fun title_case_join() = testAll {
        testCaseStyleJoin(CaseStyle.`Title Case`)
    }


    private fun testCaseStyleMatches(caseStyle: CaseStyle, vararg exceptions: String) {
        val phrases = caseJoinedWords.getValue(caseStyle)
        phrases.forEach { phrase ->
            withClue("$caseStyle matches ${phrase.quoted}") {
                caseStyle.matches(phrase) shouldBe true
            }
        }
        caseJoinedWords
            .filterNot { (key, _) -> key == caseStyle }
            .forEach { (otherCaseStyle, otherPhrases) ->
                otherPhrases
                    .filterNot { phrases.contains(it) }
                    .filterNot { exceptions.contains(it) }
                    .forEach { otherPhrase ->
                        withClue("$caseStyle doesn't match ${otherPhrase.quoted} with case style $otherCaseStyle") {
                            caseStyle.matches(otherPhrase) shouldBe false
                        }
                    }
            }
    }

    private fun testCaseStyleJoin(caseStyle: CaseStyle) {
        val phrases = caseJoinedWords.getValue(caseStyle)
        val expectedWords = caseSplitWords
        check(phrases.size == expectedWords.size) {
            "number of phrases (${phrases.size} to test doesn't match number of expected words (${expectedWords.size})"
        }
        phrases.forEachIndexed { index, phrase ->
            caseStyle.split(phrase).shouldContainExactly(expectedWords[index])
        }
    }

    private fun testCaseStyleSplit(caseStyle: CaseStyle) {
        val wordLists = caseSplitWords
        val expectedPhrases = caseJoinedWords.getValue(caseStyle)
        check(wordLists.size == expectedPhrases.size) {
            "number of word lists (${wordLists.size} to test doesn't match number of expected phrases (${expectedPhrases.size})"
        }
        wordLists.forEachIndexed { index, words ->
            caseStyle.join(words) shouldBe expectedPhrases[index]
            caseStyle.join(*words.toTypedArray()) shouldBe expectedPhrases[index]
        }
    }


    @Test
    fun find_by_matching() = testEnum<CaseStyle> { caseStyle ->
        caseJoinedWords.getValue(caseStyle).forAll { CaseStyle.findByMatching(it) shouldContain caseStyle }
    }


    @Test
    fun to_camel_cased_string() = testAll {
        testConversion(CaseStyle.camelCase, "FOO") { it.toCamelCasedString() }
        testConversion(CaseStyle.camelCase, "FOO") { it.toCasedString(CaseStyle.camelCase) }
    }

    @Test
    fun to_pascal_cased_string() = testAll {
        testConversion(CaseStyle.PascalCase, "FOO") { it.toPascalCasedString() }
        testConversion(CaseStyle.PascalCase, "FOO") { it.toCasedString(CaseStyle.PascalCase) }
    }

    @Test
    fun to_screaming_snake_cased_string() = testAll {
        testConversion(CaseStyle.SCREAMING_SNAKE_CASE, "FOO") { it.toScreamingSnakeCasedString() }
        testConversion(CaseStyle.SCREAMING_SNAKE_CASE, "FOO") { it.toCasedString(CaseStyle.SCREAMING_SNAKE_CASE) }
    }

    @Test
    fun to_kebab_cased_string() = testAll {
        testConversion(CaseStyle.`kebab-case`, "FOO") { it.toKebabCasedString() }
        testConversion(CaseStyle.`kebab-case`, "FOO") { it.toCasedString(CaseStyle.`kebab-case`) }
    }

    @Test
    fun to_title_cased_string() = testAll {
        testConversion(CaseStyle.`Title Case`, "FOO") { it.toTitleCasedString() }
        testConversion(CaseStyle.`Title Case`, "FOO") { it.toCasedString(CaseStyle.`Title Case`) }
    }

    fun testConversion(caseStyle: CaseStyle, vararg exceptions: String, convert: (Any) -> String) {
        val phrases = caseJoinedWords.getValue(caseStyle)
        CaseStyle.values().forAll { otherCaseStyle ->
            val otherPhrases = caseJoinedWords.getValue(otherCaseStyle)
            phrases.zip(otherPhrases).forAll { (phrase, otherPhrase) ->
                if (!exceptions.contains(otherPhrase)) {
                    convert(otherPhrase) shouldBe phrase
                    convert(object {
                        override fun toString(): String = otherPhrase
                    }) shouldBe phrase
                }
            }
        }

        convert("- ") shouldBe "- "
    }

    @Test
    fun cased_class_name() = testAll {
        TestEnum::class.simpleCamelCasedName shouldBe "testEnum"
        TestEnum::class.simplePascalCasedName shouldBe "TestEnum"
        TestEnum::class.simpleScreamingSnakeCasedName shouldBe "TEST_ENUM"
        TestEnum::class.simpleKebabCasedName shouldBe "test-enum"
        TestEnum::class.simpleTitleCasedName shouldBe "Test Enum"
    }

    /**
     * Tests if [Enum.getKebabCaseName] returns an actual kebab-case name.
     */
    @Test
    fun cased_enum_name() = testAll {
        Enum<*>::camelCasedName.asClue { prop -> TestEnum.values().forAll { prop.get(it) shouldBe "enumConstant" } }
        Enum<*>::pascalCasedName.asClue { prop -> TestEnum.values().forAll { prop.get(it) shouldBe "EnumConstant" } }
        Enum<*>::screamingSnakeCasedName.asClue { prop -> TestEnum.values().forAll { prop.get(it) shouldBe "ENUM_CONSTANT" } }
        Enum<*>::kebabCasedName.asClue { prop -> TestEnum.values().forAll { prop.get(it) shouldBe "enum-constant" } }
        Enum<*>::titleCasedName.asClue { prop -> TestEnum.values().forAll { prop.get(it) shouldBe "Enum Constant" } }
    }

    @Suppress("EnumEntryName")
    enum class TestEnum {
        ENUM_CONSTANT, EnumConstant, enumConstant, @JsName("enum_constant") `enum-constant`
    }
}

internal val caseSplitWords = listOf(
    emptyList(),
    listOf("b"),
    listOf("ba"),
    listOf("ba", "z"),
    listOf("foo"),
    listOf("b", "foo"),
    listOf("ba", "foo"),
    listOf("ba", "z", "foo"),
    listOf("foo", "bar"),
    listOf("b", "foo", "bar"),
    listOf("ba", "foo", "bar"),
    listOf("ba", "z", "foo", "bar"),
)

internal val caseJoinedWords = mapOf(
    CaseStyle.camelCase to listOf(
        "",
        "b",
        "ba",
        "baZ",
        "foo",
        "bFoo",
        "baFoo",
        "baZFoo",
        "fooBar",
        "bFooBar",
        "baFooBar",
        "baZFooBar",
    ),
    CaseStyle.PascalCase to listOf(
        "",
        "B",
        "BA",
        "BAZ",
        "Foo",
        "BFoo",
        "BAFoo",
        "BAZFoo",
        "FooBar",
        "BFooBar",
        "BAFooBar",
        "BAZFooBar",
    ),
    CaseStyle.SCREAMING_SNAKE_CASE to listOf(
        "",
        "B",
        "BA",
        "BA_Z",
        "FOO",
        "B_FOO",
        "BA_FOO",
        "BA_Z_FOO",
        "FOO_BAR",
        "B_FOO_BAR",
        "BA_FOO_BAR",
        "BA_Z_FOO_BAR",
    ),
    CaseStyle.`kebab-case` to listOf(
        "",
        "b",
        "ba",
        "ba-z",
        "foo",
        "b-foo",
        "ba-foo",
        "ba-z-foo",
        "foo-bar",
        "b-foo-bar",
        "ba-foo-bar",
        "ba-z-foo-bar",
    ),
    CaseStyle.`Title Case` to listOf(
        "",
        "B",
        "BA",
        "BA Z",
        "Foo",
        "B Foo",
        "BA Foo",
        "BA Z Foo",
        "Foo Bar",
        "B Foo Bar",
        "BA Foo Bar",
        "BA Z Foo Bar",
    ),
)
