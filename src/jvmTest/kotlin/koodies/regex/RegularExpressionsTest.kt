package koodies.regex

import koodies.debug.debug
import koodies.test.DeprecatedDynamicTestsBuilder
import koodies.test.test
import koodies.test.testEach
import koodies.test.toStringIsEqualTo
import koodies.text.ANSI.ansiRemoved
import koodies.text.Semantics.formattedAs
import org.junit.jupiter.api.DynamicContainer.dynamicContainer
import org.junit.jupiter.api.DynamicTest.dynamicTest
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode.SAME_THREAD
import strikt.api.Assertion
import strikt.api.expectThat
import strikt.assertions.containsExactly
import strikt.assertions.isEqualTo
import strikt.assertions.isFalse
import strikt.assertions.isNotNull
import strikt.assertions.isNull
import strikt.assertions.isTrue
import strikt.assertions.matches

@Execution(SAME_THREAD)
class RegularExpressionsTest {

    @Nested
    inner class RegexOperations {

        @Nested
        inner class IsGrouped {

            @TestFactory
            fun `should return false if blank`() = testEach(
                "",
                " ",
                "   "
            ) {
                test2 { expecting { Regex(this).isGrouped } that { isFalse() } }
            }

            @TestFactory
            fun `should return false if not all is grouped`() = testEach(
                "a()",
                "()b"
            ) {
                test2 { expecting { Regex(this).isGrouped } that { isFalse() } }
            }

            @TestFactory
            fun `should return false if multiple groups`() = testEach(
                "()()",
                "(a)(b)",
                "(())()",
                "(a(b)c)(d)"
            ) {
                test2 { expecting { Regex(this).isGrouped } that { isFalse() } }
            }

            @TestFactory
            fun `should return true if single outer group`() = testEach(
                "()",
                "(a)",
                "(())",
                "(a())",
                "(()a)"
            ) {
                test2 { expecting { Regex(this).isGrouped } that { isTrue() } }
            }

            @TestFactory
            fun `should ignore escaped brackets`() = testEach(
                "(\\()" to true,
                "(\\(a)" to true,
                "(\\)\\(\\))" to true,
                "()\\)" to false,
                "\\((())" to false,
            ) { (expr, expected) ->
                test2 { expecting { Regex(expr).isGrouped } that { isEqualTo(expected) } }
            }
        }

        @Nested
        inner class Group {

            @Test
            fun `should group named`() {
                expectThat(Regex("abc").group("name")).toStringIsEqualTo("(?<name>abc)")
            }

            @Test
            fun `should group anonymous`() {
                expectThat(Regex("abc").group()).toStringIsEqualTo("(?:abc)")
                expectThat(Regex("abc").grouped).toStringIsEqualTo("(?:abc)")
            }

            @TestFactory
            fun `should not add anonymous group if grouped`() = testEach(
                Regex("(abc)"),
                Regex("(?:abc)"),
                Regex("(?<name>abc)"),
            ) { regex ->
                test2 { expecting { group() } that { toStringIsEqualTo(regex.pattern) } }
                test2 { expecting { grouped } that { toStringIsEqualTo(regex.pattern) } }
            }
        }
    }

    private data class MatchExpectations(val matchingInput: List<String>, val nonMatchingInput: List<String>)
    private data class SplitExpectations(val splitable: List<Pair<String, List<String>>>, val nonSplitable: List<String>)

    @TestFactory
    fun `should correctly match`() = listOf(
        RegularExpressions.atLeastOneWhitespaceRegex to MatchExpectations(
            matchingInput = listOf(
                " ",
                "  ",
                "\t",
                " \t ",
            ), nonMatchingInput = listOf(
                "",
                "a",
                "a ",
                " a",
                "a b",
            )),
        RegularExpressions.urlRegex to MatchExpectations(
            matchingInput = listOf(
                "http://example.net",
                "https://xn--yp9haa.io/beep/beep?signal=on&timeout=42_000#some-complex-state",
                "ftp://edu.gov/download/latest-shit",
                "file:///some/triple-slash/uri/path/to/file.sh",
            ), nonMatchingInput = listOf(
                "mailto:someone@somewhere",
                "abc://example.net",
                "crap",
            )),
        RegularExpressions.uriRegex to MatchExpectations(
            matchingInput = listOf(
                "http://example.net",
                "https://xn--yp9haa.io/beep/beep?signal=on&timeout=42_000#some-complex-state",
                "ftp://edu.gov/download/latest-shit",
                "file:///some/triple-slash/uri/path/to/file.sh",
                "mailto:someone@somewhere",
                "abc://example.net",
            ), nonMatchingInput = listOf(
                "crap",
            )),
        RegularExpressions.versionRegex to MatchExpectations(
            matchingInput = listOf(
                "1.0.0",
                "01.12.23",
                "999999999999.999999999999.999999999999",
            ), nonMatchingInput = listOf(
                ".0.0",
                "0.0.",
                "0..0",
                "0.b.0",
                "crap",
            )),

        RegularExpressions.classRegex("x") to MatchExpectations(
            matchingInput = listOf(
                "ClassName",
                "package.ClassName",
                "package1.PACKAGE2.ClassName",
            ), nonMatchingInput = listOf(
                ".ClassName",
                "ClassName.",
                "package..ClassName",
            )),
        RegularExpressions.lambdaRegex("x") to MatchExpectations(
            matchingInput = listOf(
                "() -> Unit",
                "() -> package.Unit",
                "() -> package1.PACKAGE2.Unit",
                "ClassName.() -> Unit",
                "package.ClassName.() -> package.Unit",
                "package1.PACKAGE2.ClassName.() -> package1.PACKAGE2.Unit",
            ), nonMatchingInput = listOf(
                ".ClassName",
                "ClassName.",
                "package..ClassName",
                "package1.PACKAGE2.ClassName",
            )),
    ).map { (regex, expectations) ->
        dynamicContainer("for ${regex.pattern}", listOf(
            dynamicContainer("should match", expectations.matchingInput.map { matchingInput ->
                dynamicTest("input: $matchingInput") {
                    expectThat(matchingInput).matches(regex)
                }
            }),
            dynamicContainer("should not match", expectations.nonMatchingInput.map { nonMatchingInput ->
                dynamicTest("input: $nonMatchingInput") {
                    expectThat(nonMatchingInput).not { matches(regex) }
                }
            }),
        ))
    }

    @Nested
    inner class Lambdas {

        @Nested
        inner class IgnoreArgs {

            fun DeprecatedDynamicTestsBuilder<Regex>.testMatchesFields(
                text: String,
                receiverPackage: String?,
                receiverClass: String?,
                parameterList: String?,
                returnValuePackage: String?,
                returnValueClass: String?,
            ) {
                test2 {
                    expecting { matchEntire(text) }.that {
                        isNotNull().and {
                            get("field receiverPackage") { get("lambdaIreceiverIpkg") }.isEqualTo(receiverPackage)
                            get("field receiverClass") { get("lambdaIreceiverItype") }.isEqualTo(receiverClass)
                            get("field parameterList") { get("lambdaIparams") }.isEqualTo(parameterList)
                            get("field returnValuePackage") { get("lambdaIreturnIpkg") }.isEqualTo(returnValuePackage)
                            get("field returnValueClass") { get("lambdaIreturnItype") }.isEqualTo(returnValueClass)
                        }
                    }
                }
            }

            @Suppress("NonAsciiCharacters")
            @TestFactory
            fun `should match lambda with …`() = test(RegularExpressions.lambdaRegex("lambda")) {
                listOf(
                    "no arg" to "",
                    "one arg" to "Int",
                    "named arg" to "name:Int",
                    "lambda arg" to "package.String.(package.Int) -> package.Float",
                ).forEach { (name, argList) ->
                    group("… $name: ${argList.formattedAs.unit.ansiRemoved}") {
                        testMatchesFields("($argList) -> Unit",
                            null, null, argList, null, "Unit")
                        testMatchesFields("($argList) -> package.Unit",
                            null, null, argList, "package", "Unit")
                        testMatchesFields("($argList) -> package1.PACKAGE2.Unit",
                            null, null, argList, "package1.PACKAGE2", "Unit")
                        testMatchesFields("ClassName.($argList) -> Unit",
                            null, "ClassName", argList, null, "Unit")
                        testMatchesFields("package.ClassName.($argList) -> package.Unit",
                            "package", "ClassName", argList, "package", "Unit")
                        testMatchesFields("package1.PACKAGE2.ClassName.($argList) -> package1.PACKAGE2.Unit",
                            "package1.PACKAGE2", "ClassName", argList, "package1.PACKAGE2", "Unit")
                    }
                }
            }
        }
    }

    @Nested
    inner class FindAllValues {
        @Test
        fun `should find all matches`() {
            @Suppress("SpellCheckingInspection")
            expectThat(RegularExpressions.urlRegex.findAllValues(htmlLinkList).toList()).containsExactly(
                "https://textfancy.com/font-converter/",
                "https://textfancy.com",
                "http://qaz.wtf/u/convert.cgi?text=CUSTOM+FONTS",
                "https://qaz.wtf",
                "https://smalltext.io",
                "https://smalltext.io",
                "https://eng.getwisdom.io/awesome-unicode/",
                "https://eng.getwisdom.io",
                "https://codepoints.net/U+0085?lang=de",
                "https://codepoints.net",
                "https://www.compart.com/en/unicode/U+3164",
                "https://compart.com",
                "https://github.com/Wisdom/Awesome-Unicode",
                "https://github.com",
            )
        }
    }

    @Nested
    inner class CountMatchesKtTest {
        @Test
        fun `should find all matches`() {
            expectThat(RegularExpressions.urlRegex.countMatches(htmlLinkList)).isEqualTo(14)
        }
    }

    companion object {
        @Suppress("SpellCheckingInspection", "LongLine")
        val htmlLinkList: String = """
            <ul class="bookmark-widget__list"><li class="bookmark-item bookmark-item_mode_list bookmark-item_size_small bookmark-item_description_first bookmark-item_title_show"><!----> <a href="https://textfancy.com/font-converter/" title="𝕿𝖊𝖝𝖙 🎀ԲΔ₪ς¥🎀
▀█▀▒██▀░▀▄▀░▀█▀ ᕕ(ಠ‿ಠ)ᕗ
░█▒░█▄▄░█▒█░▒█  🎀ԲΔ₪ς¥🎀" class="bookmark-item__link"><div class="bookmark-item__icon-wrapper"><img loading="lazy" src="https://textfancy.com" class="bookmark-item__icon" data-imu-valid="true" data-imu-supported="false"> <!----></div> <div class="bookmark-item__info"><span class="bookmark-item__title-container"><span class="bookmark-item__title">
          𝕿𝖊𝖝𝖙 🎀ԲΔ₪ς¥🎀
        </span> <!----></span> <!----></div></a> <!----></li><li class="bookmark-item bookmark-item_mode_list bookmark-item_size_small bookmark-item_description_first bookmark-item_title_show"><!----> <a href="http://qaz.wtf/u/convert.cgi?text=CUSTOM+FONTS" title="𝚄𝚗𝚒𝚌𝚘𝚍𝚎 𝕿𝖊𝖝𝖙 𝓒𝓸𝓷𝓿𝓮𝓻𝓽𝓮𝓻" class="bookmark-item__link"><div class="bookmark-item__icon-wrapper"><img loading="lazy" src="https://qaz.wtf" class="bookmark-item__icon" data-imu-valid="true" data-imu-supported="false"> <!----></div> <div class="bookmark-item__info"><span class="bookmark-item__title-container"><span class="bookmark-item__title">
          𝚄𝚗𝚒𝚌𝚘𝚍𝚎 𝕿𝖊𝖝𝖙 𝓒𝓸𝓷𝓿𝓮𝓻𝓽𝓮𝓻
        </span> <!----></span> <!----></div></a> <!----></li><li class="bookmark-item bookmark-item_mode_list bookmark-item_size_small bookmark-item_description_first bookmark-item_title_show"><!----> <a href="https://smalltext.io" title="sᴍᴀʟʟ ᴛᴇxᴛ ᵍᵉⁿᵉʳᵃᵗᵒʳ" class="bookmark-item__link"><div class="bookmark-item__icon-wrapper"><img loading="lazy" src="https://smalltext.io" class="bookmark-item__icon" data-imu-valid="true" data-imu-supported="false"> <!----></div> <div class="bookmark-item__info"><span class="bookmark-item__title-container"><span class="bookmark-item__title">
          sᴍᴀʟʟ ᴛᴇxᴛ ᵍᵉⁿᵉʳᵃᵗᵒʳ
        </span> <!----></span> <!----></div></a> <!----></li><li class="bookmark-item bookmark-item_mode_list bookmark-item_size_small bookmark-item_description_first bookmark-item_title_show"><!----> <a href="https://eng.getwisdom.io/awesome-unicode/" title="Greek question mark ; and code compatible -ㅤ- space
A curated list of delightful Unicode tidbits, packages and resources.  Foreword Unicode is Awesome! Prior to Unicode, international communication was grueling- everyone had defined their separate e..." class="bookmark-item__link"><div class="bookmark-item__icon-wrapper"><img loading="lazy" src="https://eng.getwisdom.io"  class="bookmark-item__icon" data-imu-valid="true" data-imu-supported="false"> <!----></div> <div class="bookmark-item__info"><span class="bookmark-item__title-container"><span class="bookmark-item__title">
          Greek question mark ; and code compatible -ㅤ- space
        </span> <!----></span> <!----></div></a> <!----></li><li class="bookmark-item bookmark-item_mode_list bookmark-item_size_small bookmark-item_description_first bookmark-item_title_show"><!----> <a href="https://codepoints.net/U+0085?lang=de" title="U+0085 NEXT LINE (NEL)* – Codepoints
�, Codepunkt U+0085 NEXT LINE (NEL)* in Unicode, liegt im Block „Latin-1 Supplement“. Es gehört zur Allgemein-Schrift und ist ein Kontrollzeichen." class="bookmark-item__link"><div class="bookmark-item__icon-wrapper"><img loading="lazy" src="https://codepoints.net" class="bookmark-item__icon" data-imu-valid="true" data-imu-supported="false"> <!----></div> <div class="bookmark-item__info"><span class="bookmark-item__title-container"><span class="bookmark-item__title">
          U+0085 NEXT LINE (NEL)* – Codepoints
        </span> <!----></span> <!----></div></a> <!----></li><li class="bookmark-item bookmark-item_mode_list bookmark-item_size_small bookmark-item_description_first bookmark-item_title_show"><!----> <a href="https://www.compart.com/en/unicode/U+3164" title="“ㅤ” non-whitespace whitespace
U+3164 is the unicode hex value of the character Hangul Filler. Char U+3164, Encodings, HTML Entitys:ㅤ,ㅤ, UTF-8 (hex), UTF-16 (hex), UTF-32 (hex)" class="bookmark-item__link"><div class="bookmark-item__icon-wrapper"><img loading="lazy" src="https://compart.com" class="bookmark-item__icon" data-imu-valid="true" data-imu-supported="false"> <!----></div> <div class="bookmark-item__info"><span class="bookmark-item__title-container"><span class="bookmark-item__title">
          “ㅤ” non-whitespace whitespace
        </span> <!----></span> <!----></div></a> <!----></li><li class="bookmark-item bookmark-item_mode_list bookmark-item_size_small bookmark-item_description_first bookmark-item_title_show"><!----> <a href="https://github.com/Wisdom/Awesome-Unicode" title="Wisdom/Awesome-Unicode: A curated list of delightful Unicode tidbits, packages and resources.
:joy: :ok_hand: A curated list of delightful Unicode tidbits, packages and resources. - Wisdom/Awesome-Unicode" class="bookmark-item__link"><div class="bookmark-item__icon-wrapper"><img loading="lazy" src="https://github.com" class="bookmark-item__icon" data-imu-valid="true" data-imu-supported="false"> <!----></div> <div class="bookmark-item__info"><span class="bookmark-item__title-container"><span class="bookmark-item__title">
          Wisdom/Awesome-Unicode: A curated list of delightful Unicode tidbits, packages and resources.
        </span> <!----></span> <!----></div></a> <!----></li></ul>
        """.trimIndent()
    }
}


fun <T : CharSequence> Assertion.Builder<T>.entirelyMatchedBy(regex: Regex) =
    get("entirely matched by $regex") { regex.matchEntire(this) }.isNotNull()

fun Assertion.Builder<Regex>.matchEntire(input: CharSequence): Assertion.Builder<MatchResult> =
    get("match entirely ${input.debug}") { matchEntire(input) }.isNotNull()

fun Assertion.Builder<Regex>.matchEntire(input: CharSequence, expected: Boolean = true): Assertion.Builder<out MatchResult?> =
    get("match entirely ${input.debug}") { matchEntire(input) }.run { if (expected) not { isNull() } else isNull() }

fun Assertion.Builder<MatchResult>.group(groupName: String) =
    get("group with name $groupName: %s") { namedGroups[groupName] }

fun Assertion.Builder<MatchResult>.group(index: Int) =
    get("group with index $index: %s") { groups[index] }

val Assertion.Builder<MatchResult>.groupValues
    get() = get("group values: %s") { groupValues }

val Assertion.Builder<MatchGroup?>.value
    get() = get("value %s") { this?.value }
