package koodies.text;

import koodies.collections.to
import koodies.test.tests
import org.junit.jupiter.api.TestFactory
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode.SAME_THREAD
import strikt.api.Assertion
import strikt.api.expectThat
import strikt.assertions.containsExactly
import strikt.assertions.isEqualTo

@Execution(SAME_THREAD)
class CasesKtTest {

    @TestFactory
    fun camelCase() = listOf(
        "" to "" to listOf(""),
        "foo" to "foo" to listOf("foo"),
        "foo-bar" to "fooBar" to listOf("foo", "bar"),
        "foo-bar-baz" to "fooBarBaz" to listOf("foo", "bar", "baz"),
        "a-foo-bar-baz" to "aFooBarBaz" to listOf("a", "foo", "bar", "baz"),
        "test%123" to "test%123" to listOf("test%123"),
    ).tests { (kebabCase, camelCase, parts) ->
        test("should convert ${kebabCase.quoted} to ${camelCase.quoted}") {
            val converted = kebabCase.convertKebabCaseToCamelCase()
            expectThat(converted).isEqualTo(camelCase)
        }
        test("should convert ${camelCase.quoted} to ${kebabCase.quoted}") {
            val converted = camelCase.convertCamelCaseToKebabCase()
            expectThat(converted).isEqualTo(kebabCase)
        }

        container("string based") {
            container("split") {
                test("should split kebab-case") {
                    expectThat(kebabCase.splitKebabCase()).containsExactly(parts)
                }
                test("should split camelCase") {
                    expectThat(camelCase.splitCamelCase()).containsExactly(parts)
                }
                test("should split PascalCase") {
                    expectThat(camelCase.capitalize().toString().splitPascalCase()).containsExactly(parts)
                }
            }
            container("join") {
                test("should join to kebab-case") {
                    expectThat(parts.joinToKebabCase()).isEqualTo(kebabCase)
                }
                test("should join to camelCase") {
                    expectThat(parts.joinToCamelCase()).isEqualTo(camelCase)
                }
                test("should join to PascalCase") {
                    expectThat(parts.joinToPascalCase()).isEqualTo(camelCase.capitalize().toString())
                }
            }
        }

        @Suppress("USELESS_CAST")
        container("char sequence based") {
            container("split") {
                test("should split kebab-case") {
                    expectThat(kebabCase.decapitalize().splitKebabCase()).containsExactlyCharacterWise(parts)
                }
                test("should split camelCase") {
                    expectThat(camelCase.decapitalize().splitCamelCase()).containsExactlyCharacterWise(parts)
                }
                test("should split PascalCase") {
                    expectThat(camelCase.capitalize().splitPascalCase()).containsExactlyCharacterWise(parts)
                }
            }
            container("join") {
                test("should join to kebab-case") {
                    expectThat(parts.map { it as CharSequence }.joinToKebabCase()).isEqualToCharacterWise(kebabCase)
                }
                test("should join to camelCase") {
                    expectThat(parts.map { it as CharSequence }.joinToCamelCase()).isEqualToCharacterWise(camelCase)
                }
                test("should join to PascalCase") {
                    expectThat(parts.map { it as CharSequence }.joinToPascalCase()).isEqualToCharacterWise(camelCase.capitalize())
                }
            }
        }
    }

    @TestFactory
    fun screamingSnakeCase() = listOf(
        "" to "" to listOf(""),
        "foo" to "FOO" to listOf("foo"),
        "foo-bar" to "FOO_BAR" to listOf("foo", "bar"),
        "foo-bar-baz" to "FOO_BAR_BAZ" to listOf("foo", "bar", "baz"),
        "a-foo-bar-baz" to "A_FOO_BAR_BAZ" to listOf("a", "foo", "bar", "baz"),
        "test%123" to "TEST%123" to listOf("test%123"),
    ).tests { (kebabCase, screamingSnakeCase, parts) ->
        test("should convert \"$kebabCase\" to \"$screamingSnakeCase\"") {
            val converted = kebabCase.convertKebabCaseToScreamingSnakeCase()
            expectThat(converted).isEqualTo(screamingSnakeCase)
        }
        test("should convert \"$screamingSnakeCase\" to \"$kebabCase\"") {
            val converted = screamingSnakeCase.convertScreamingSnakeCaseToKebabCase()
            expectThat(converted).isEqualTo(kebabCase)
        }

        container("string based") {
            container("split") {
                test("should split kebab-case") {
                    expectThat(kebabCase.splitKebabCase()).containsExactly(parts)
                }
                test("should split SCREAMING_SNAKE_CASE") {
                    expectThat(screamingSnakeCase.splitScreamingSnakeCase()).containsExactly(parts)
                }
            }
            container("join") {
                test("should join to kebab-case") {
                    expectThat(parts.joinToKebabCase()).isEqualTo(kebabCase)
                }
                test("should join to SCREAMING_SNAKE_CASE") {
                    expectThat(parts.joinToScreamingSnakeCase()).isEqualTo(screamingSnakeCase)
                }
            }
        }

        container("char sequence based") {
            container("split") {
                test("should split kebab-case (CharSequence)") {
                    expectThat(kebabCase.decapitalize().splitKebabCase()).containsExactlyCharacterWise(parts)
                }
                test("should split SCREAMING_SNAKE_CASE (CharSequence)") {
                    expectThat(screamingSnakeCase.capitalize().splitScreamingSnakeCase()).containsExactlyCharacterWise(parts)
                }
            }
            @Suppress("USELESS_CAST")
            container("join") {
                test("should join to kebab-case (CharSequence)") {
                    expectThat(parts.map { it as CharSequence }.joinToKebabCase()).isEqualToCharacterWise(kebabCase)
                }
                test("should join to SCREAMING_SNAKE_CASE (CharSequence)") {
                    expectThat(parts.map { it as CharSequence }.joinToScreamingSnakeCase()).isEqualToCharacterWise(screamingSnakeCase)
                }
            }
        }
    }

    /**
     * Tests if [Enum.kebabCaseName] returns an actual kebab-case name.
     *
     * If you look for kebab-case (de-)serialization, see [KebabCaseEnumJacksonModule].
     */
    @TestFactory
    fun kebabCaseEnumNames() = listOf(
        TestEnum.ENUM_CONSTANT to "enum-constant",
        TestEnum.EnumConstant to "enum-constant",
        TestEnum.enumConstant to "enum-constant",
        TestEnum.`enum-constant` to "enum-constant",
    ).tests { (enumConstant, kebabCase) ->
        test("$enumConstant.kebabCaseName() ➜ $kebabCase") {
            expectThat(enumConstant.kebabCaseName()).isEqualTo(kebabCase)
        }
    }

    @TestFactory
    fun simpleCases() = listOf(
        "aa" contains TestCases.ofType(TestCases.Lower),
        "aA" contains TestCases.ofType(TestCases.Mixed, TestCases.Upper, TestCases.Lower),
        "a_" contains TestCases.ofType(TestCases.Lower),
        "Aa" contains TestCases.ofType(TestCases.Mixed, TestCases.Upper, TestCases.Lower),
        "AA" contains TestCases.ofType(TestCases.Upper),
        "A_" contains TestCases.ofType(TestCases.Upper),
        "_a" contains TestCases.ofType(TestCases.Lower),
        "_A" contains TestCases.ofType(TestCases.Upper),
        "__" contains TestCases.ofType()
    ).tests { (letters, caseExpectations) ->
        test("\"$letters\" is" + (if (caseExpectations.first) {
            ""
        } else {
            " NOT"
        }) + " mixed case  ") {
            expectThat(letters.isMixedCase()).isEqualTo(caseExpectations.first)
        }
        test("\"$letters\" contains" + (if (caseExpectations.second) {
            ""
        } else {
            " NO"
        }) + " upper case  ") {
            expectThat(letters.containsUpperCase()).isEqualTo(caseExpectations.second)
        }
        test("\"$letters\" contains" + (if (caseExpectations.third) {
            ""
        } else {
            " NO"
        }) + " lower case  ") {
            expectThat(letters.containsLowerCase()).isEqualTo(caseExpectations.third)
        }
    }

    @Suppress("EnumEntryName")
    enum class TestEnum {
        ENUM_CONSTANT, EnumConstant, enumConstant, `enum-constant`, UNKNOWN
    }
}

private enum class TestCases {
    Mixed, Upper, Lower;

    companion object {
        fun ofType(vararg cases: TestCases) = Triple(
            cases.contains(Mixed),
            cases.contains(Upper),
            cases.contains(Lower)
        )
    }
}

private infix fun <B> String.contains(that: B): Pair<String, B> = Pair(this, that)


infix fun <T : CharSequence> Assertion.Builder<T>.isEqualToCharacterWise(other: CharSequence): Assertion.Builder<List<Char>> =
    get("as character list %s") { toList() }.containsExactly(other.toList())


private fun Iterable<CharSequence>.asCharacterLists() = map { element -> element.map { char -> char } }

fun <T : Iterable<E>, E : CharSequence> Assertion.Builder<T>.asCharacterLists(): Assertion.Builder<List<List<Char>>> =
    get("as character list %s") { asCharacterLists() }

infix fun <T : Iterable<E>, E : CharSequence> Assertion.Builder<T>.containsExactlyCharacterWise(elements: Collection<E>): Assertion.Builder<List<List<Char>>> =
    asCharacterLists().containsExactly(elements.asCharacterLists())
