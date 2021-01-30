package koodies.text

import koodies.collections.to
import koodies.test.testEach
import org.junit.jupiter.api.TestFactory
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode.SAME_THREAD
import strikt.api.Assertion
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
    ).testEach { (kebabCase, camelCase, parts) ->
        test("should convert ${kebabCase.quoted} to ${camelCase.quoted}") {
            val converted = kebabCase.convertKebabCaseToCamelCase()
            expect { converted }.that { isEqualTo(camelCase) }
        }
        test("should convert ${camelCase.quoted} to ${kebabCase.quoted}") {
            val converted = camelCase.convertCamelCaseToKebabCase()
            expect { converted }.that { isEqualTo(kebabCase) }
        }

        group("string based") {
            group("split") {
                test("should split kebab-case") {
                    expect { kebabCase.splitKebabCase() }.that { containsExactly(parts) }
                }
                test("should split camelCase") {
                    expect { camelCase.splitCamelCase() }.that { containsExactly(parts) }
                }
                test("should split PascalCase") {
                    expect { camelCase.capitalize().toString().splitPascalCase() }.that { containsExactly(parts) }
                }
            }
            group("join") {
                test("should join to kebab-case") {
                    expect { parts.joinToKebabCase() }.that { isEqualTo(kebabCase) }
                }
                test("should join to camelCase") {
                    expect { parts.joinToCamelCase() }.that { isEqualTo(camelCase) }
                }
                test("should join to PascalCase") {
                    expect { parts.joinToPascalCase() }.that { isEqualTo(camelCase.capitalize().toString()) }
                }
            }
        }

        @Suppress("USELESS_CAST")
        group("char sequence based") {
            group("split") {
                test("should split kebab-case") {
                    expect { kebabCase.decapitalize().splitKebabCase() }.that { containsExactlyCharacterWise(parts) }
                }
                test("should split camelCase") {
                    expect { camelCase.decapitalize().splitCamelCase() }.that { containsExactlyCharacterWise(parts) }
                }
                test("should split PascalCase") {
                    expect { camelCase.capitalize().splitPascalCase() }.that { containsExactlyCharacterWise(parts) }
                }
            }
            group("join") {
                test("should join to kebab-case") {
                    expect { parts.map { it as CharSequence }.joinToKebabCase() }.that { isEqualToCharacterWise(kebabCase) }
                }
                test("should join to camelCase") {
                    expect { parts.map { it as CharSequence }.joinToCamelCase() }.that { isEqualToCharacterWise(camelCase) }
                }
                test("should join to PascalCase") {
                    expect { parts.map { it as CharSequence }.joinToPascalCase() }.that { isEqualToCharacterWise(camelCase.capitalize()) }
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
    ).testEach { (kebabCase, screamingSnakeCase, parts) ->
        test("should convert \"$kebabCase\" to \"$screamingSnakeCase\"") {
            val converted = kebabCase.convertKebabCaseToScreamingSnakeCase()
            expect { converted }.that { isEqualTo(screamingSnakeCase) }
        }
        test("should convert \"$screamingSnakeCase\" to \"$kebabCase\"") {
            val converted = screamingSnakeCase.convertScreamingSnakeCaseToKebabCase()
            expect { converted }.that { isEqualTo(kebabCase) }
        }

        group("string based") {
            group("split") {
                test("should split kebab-case") {
                    expect { kebabCase.splitKebabCase() }.that { containsExactly(parts) }
                }
                test("should split SCREAMING_SNAKE_CASE") {
                    expect { screamingSnakeCase.splitScreamingSnakeCase() }.that { containsExactly(parts) }
                }
            }
            group("join") {
                test("should join to kebab-case") {
                    expect { parts.joinToKebabCase() }.that { isEqualTo(kebabCase) }
                }
                test("should join to SCREAMING_SNAKE_CASE") {
                    expect { parts.joinToScreamingSnakeCase() }.that { isEqualTo(screamingSnakeCase) }
                }
            }
        }

        group("char sequence based") {
            group("split") {
                test("should split kebab-case (CharSequence)") {
                    expect { kebabCase.decapitalize().splitKebabCase() }.that { containsExactlyCharacterWise(parts) }
                }
                test("should split SCREAMING_SNAKE_CASE (CharSequence)") {
                    expect { screamingSnakeCase.capitalize().splitScreamingSnakeCase() }.that { containsExactlyCharacterWise(parts) }
                }
            }
            @Suppress("USELESS_CAST")
            group("join") {
                test("should join to kebab-case (CharSequence)") {
                    expect { parts.map { it as CharSequence }.joinToKebabCase() }.that { isEqualToCharacterWise(kebabCase) }
                }
                test("should join to SCREAMING_SNAKE_CASE (CharSequence)") {
                    expect { parts.map { it as CharSequence }.joinToScreamingSnakeCase() }.that { isEqualToCharacterWise(screamingSnakeCase) }
                }
            }
        }
    }

    /**
     * Tests if [Enum.kebabCaseName] returns an actual kebab-case name.
     */
    @TestFactory
    fun kebabCaseEnumNames() = listOf(
        TestEnum.ENUM_CONSTANT to "enum-constant",
        TestEnum.EnumConstant to "enum-constant",
        TestEnum.enumConstant to "enum-constant",
        TestEnum.`enum-constant` to "enum-constant",
    ).testEach { (enumConstant, kebabCase) ->
        test("$enumConstant.kebabCaseName() ➜ $kebabCase") {
            expect { enumConstant.kebabCaseName() }.that { isEqualTo(kebabCase) }
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
    ).testEach { (letters, caseExpectations) ->
        test("\"$letters\" is" + (if (caseExpectations.first) {
            ""
        } else {
            " NOT"
        }) + " mixed case  ") {
            expect { letters.isMixedCase() }.that { isEqualTo(caseExpectations.first) }
        }
        test("\"$letters\" contains" + (if (caseExpectations.second) {
            ""
        } else {
            " NO"
        }) + " upper case  ") {
            expect { letters.containsUpperCase() }.that { isEqualTo(caseExpectations.second) }
        }
        test("\"$letters\" contains" + (if (caseExpectations.third) {
            ""
        } else {
            " NO"
        }) + " lower case  ") {
            expect { letters.containsLowerCase() }.that { isEqualTo(caseExpectations.third) }
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
