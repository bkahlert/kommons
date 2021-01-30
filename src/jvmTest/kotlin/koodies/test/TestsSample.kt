package koodies.test

import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.TestFactory
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode.CONCURRENT
import strikt.api.expectThat
import strikt.assertions.hasLength
import strikt.assertions.isEqualTo
import strikt.assertions.isGreaterThan
import strikt.assertions.isLessThan
import strikt.assertions.length

@Execution(CONCURRENT)
class TestsSample {

    @Nested
    inner class TestingSingleSubject {

        @TestFactory
        fun `as parameter`() = test("subject") {
            group("group") {
                test("test") {
                    expectThat(it).length.isGreaterThan(0)
                }

                group("nested group") {
                    test {
                        expectThat(it).not { isEqualTo("automatically named test") }
                    }
                }
            }

            group("strikt") {
                expect { length }.that {
                    isGreaterThan(0)
                }

                with { reversed() }.then {
                    expect.that {
                        not { isEqualTo("automatically named test") }
                    }
                }
            }

            expect { length }.that {
                isGreaterThan(0)
                    .isLessThan(10)
                isEqualTo(7)
            }
        }

        @TestFactory
        fun `as receiver object`() = "foo".test {
            expect.isEqualTo("foo")
        }

        @TestFactory
        fun `list as parameter`() = test(listOf("foo", "bar")) {
            expect { joinToString("+") }.isEqualTo("foo+bar").get { length }.isEqualTo(7)
        }
    }

    @Nested
    inner class TestingMultipleSubjects {

        @TestFactory
        fun `as parameters`() = testEach("subject 1", "subject 2", "subject 3") {
            group("group") {
                test("test") {
                    expectThat(it).length.isGreaterThan(0)
                }

                group("nested group") {
                    test {
                        expectThat(it).not { isEqualTo("automatically named test") }
                    }
                }
            }

            group("integrated strikt") {
                expect { length }.that {
                    isGreaterThan(0)
                }

                with { reversed() }.then {
                    expect.that {
                        not { isEqualTo("automatically named test") }
                    }
                }
            }

            expect { length }.that {
                isGreaterThan(0)
                    .isLessThan(10)
                isEqualTo(9)
            }
        }

        @TestFactory
        fun `as receiver object`() = listOf("foo", "bar").testEach {
            expect.hasLength(3)
        }

        @TestFactory
        fun `lists as parameters`() = testEach(listOf("foo", "bar"), listOf("bar", "baz")) {
            expect { joinToString("+") }.hasLength(7).get { this[3] }.isEqualTo('+')
        }
    }
}
