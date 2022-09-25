package com.bkahlert.kommons_deprecated.test

import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.TestFactory
import strikt.assertions.contains
import strikt.assertions.hasLength
import strikt.assertions.isA
import strikt.assertions.isEqualTo
import strikt.assertions.isFailure
import strikt.assertions.isGreaterThan
import strikt.assertions.isLessThan
import strikt.assertions.isNotNull
import strikt.assertions.isNullOrEmpty
import strikt.assertions.isSuccess
import strikt.assertions.message
import strikt.assertions.startsWith

class JvmTestsSample {

    @TestFactory
    fun `testing with subject`() = testOld("subject") {

        "other" asserting { isEqualTo("other") }
        asserting { isEqualTo("subject") }
        expecting { length } that { isGreaterThan(5) }
        expecting { length }
            .that { isGreaterThan(5) }
        expectCatching { length } that { isSuccess() }
        expectThrows<RuntimeException> { throw RuntimeException() }
        expectThrows<RuntimeException> { throw RuntimeException() } that { message.isNullOrEmpty() }
    }

    @TestFactory
    fun `testing without subject`() = testsOld {

        "other" asserting { isEqualTo("other") }
        asserting("subject") { isEqualTo("subject") }
        expecting { "subject".length } that { isGreaterThan(5) }
        expecting { "subject".length }
            .that { isGreaterThan(5) }
        expectCatching { "subject".length } that { isSuccess() }
        expectThrows<RuntimeException> { throw RuntimeException() }
        expectThrows<RuntimeException> { throw RuntimeException() } that { message.isNullOrEmpty() }
    }


    @Nested
    inner class TestingSingleSubject {

        @TestFactory
        fun `as parameter`() = testOld("subject") {

            "other" asserting { isEqualTo("other") }
            asserting { isEqualTo("subject") }
            expecting { length } that { isGreaterThan(5) }
            expecting { length }
                .that { isGreaterThan(5) }
            expectCatching { length } that { isSuccess() }
            expectThrows<RuntimeException> { throw RuntimeException() }
            expectThrows<RuntimeException> { throw RuntimeException() } that { message.isNullOrEmpty() }

            group("group") {

                asserting { isEqualTo("subject") }
                expecting { length } that { isGreaterThan(5) }
                expectCatching { length } that { isSuccess() }
                expectThrows<RuntimeException> { throw RuntimeException() }
                expectThrows<RuntimeException> { throw RuntimeException() } that { message.isNullOrEmpty() }
            }

            with { reversed() } then {

                asserting { isEqualTo("tcejbus") }
                expecting { length } that { isGreaterThan(5) }
                expectCatching { length } that { isSuccess() }
                expectThrows<RuntimeException> { throw RuntimeException() }
                expectThrows<RuntimeException> { throw RuntimeException() } that { message.isNullOrEmpty() }
            }
        }

        @TestFactory
        fun `string subject`() = testOld("foo") {
            asserting { isEqualTo("foo") }
        }

        @TestFactory
        fun `list subject`() = testOld(listOf("foo", "bar")) {
            expecting { joinToString("+") } that { isEqualTo("foo+bar").get { length }.isEqualTo(7) }
        }
    }

    @Nested
    inner class TestingMultipleSubjects {

        @TestFactory
        fun `as parameters`() = testEachOld("subject 1", "subject 2", "subject 3") {

            expecting { length } that { isGreaterThan(0) }

            expectThrows<RuntimeException> { throw RuntimeException() }
            expectCatching { "nope" } that { isSuccess() }

            "foo" asserting { isEqualTo("foo") }
            expecting { "$this-foo" } that { startsWith("subject ") }
            expectCatching { error(this) } that { isFailure().isA<IllegalStateException>() }
            expectThrows<IllegalStateException> { error(this) }
            expectThrows<IllegalStateException> { error(this) } that { message.isNotNull().contains("subject") }

            group("group") {
                expecting("test") { length } that { isGreaterThan(0) }

                group("nested group") {
                    asserting { not { isEqualTo("automatically named test") } }
                }
            }

            group("integrated strikt") {
                expecting { length } that { isGreaterThan(0) }

                with { reversed() }.then {
                    asserting { not { isEqualTo("automatically named test") } }
                }
            }

            expecting { length } that {
                isGreaterThan(0)
                    .isLessThan(10)
                isEqualTo(9)
            }
        }

        @TestFactory
        fun `string subjects`() = testEachOld("foo", "bar") {
            asserting { hasLength(3) }
        }

        @TestFactory
        fun `list subjects`() = testEachOld(listOf("foo", "bar"), listOf("bar", "baz")) {
            expecting { joinToString("+") } that { hasLength(7).get { this[3] }.isEqualTo('+') }
        }
    }
}
