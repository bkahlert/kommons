package com.bkahlert.kommons.test

import io.kotest.assertions.throwables.shouldNotThrowAny
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldContainIgnoringCase
import kotlin.test.Test

class TestAllMapKtTest {

    @Test fun test_empty() {
        shouldThrow<IllegalArgumentException> {
            emptyMap<Any?, Any?>().testAll { }
        }
    }

    @Test fun test_success() {
        shouldNotThrowAny {
            mapOf("a" to "foo bar", "b" to "FOO BAR").testAll { (_, value) ->
                value shouldContainIgnoringCase "foo"
                value shouldContainIgnoringCase "bar"
            }
        }
    }

    @Test fun test_single_fail_single_subject() {
        shouldThrow<AssertionError> {
            mapOf("a" to "foo bar", "b" to "FOO BAR").testAll { (_, value) ->
                value shouldContain "foo"
                value shouldContainIgnoringCase "bar"
            }
        }.message shouldBe """
            1 elements passed but expected 2

            The following elements passed:
            a=foo bar

            The following elements failed:
            b=FOO BAR => "FOO BAR" should include substring "foo"
        """.trimIndent()
    }

    @Test fun test_single_fail_multiple_subjects() {
        shouldThrow<AssertionError> {
            mapOf("a" to "foo bar", "b" to "FOO BAR").testAll { (_, value) ->
                value shouldContainIgnoringCase "baz"
                value shouldContainIgnoringCase "bar"
            }
        }.message shouldBe """
            0 elements passed but expected 2

            The following elements passed:
            --none--

            The following elements failed:
            a=foo bar => "foo bar" should contain the substring "baz" (case insensitive)
            b=FOO BAR => "FOO BAR" should contain the substring "baz" (case insensitive)
        """.trimIndent()
    }

    @Test fun test_multiple_fails_multiple_subjects() {
        shouldThrow<AssertionError> {
            mapOf("a" to "foo bar", "b" to "FOO BAR").testAll { (_, value) ->
                value shouldContain "baz"
                value shouldContain "BAZ"
            }
        }.message shouldMatchGlob """
            0 elements passed but expected 2

            The following elements passed:
            --none--

            The following elements failed:
            a=foo bar => *
            The following 2 assertions failed:
            1) "foo bar" should include substring "baz"
            **
            2) "foo bar" should include substring "BAZ"
            **
            b=FOO BAR =>*
            The following 2 assertions failed:
            1) "FOO BAR" should include substring "baz"
            **
            2) "FOO BAR" should include substring "BAZ"
            **
        """.trimIndent()
    }
}
