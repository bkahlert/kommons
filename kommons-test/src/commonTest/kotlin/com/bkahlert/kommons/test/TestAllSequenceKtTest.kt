package com.bkahlert.kommons.test

import io.kotest.assertions.throwables.shouldNotThrowAny
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldContainIgnoringCase
import kotlin.test.Test

class TestAllSequenceKtTest {

    @Test fun test_empty() {
        shouldThrow<IllegalArgumentException> {
            emptySequence<Any?>().testAll { }
        }
    }

    @Test fun test_success() {
        shouldNotThrowAny {
            sequenceOf("foo bar", "FOO BAR").testAll {
                it shouldContainIgnoringCase "foo"
                it shouldContainIgnoringCase "bar"
            }
        }
    }

    @Test fun test_single_fail_single_subject() {
        shouldThrow<AssertionError> {
            sequenceOf("foo bar", "FOO BAR").testAll {
                it shouldContain "foo"
                it shouldContainIgnoringCase "bar"
            }
        }.message shouldBe """
            1 elements passed but expected 2

            The following elements passed:
            foo bar

            The following elements failed:
            "FOO BAR" => "FOO BAR" should include substring "foo"
        """.trimIndent()
    }

    @Test fun test_single_fail_multiple_subjects() {
        shouldThrow<AssertionError> {
            sequenceOf("foo bar", "FOO BAR").testAll {
                it shouldContainIgnoringCase "baz"
                it shouldContainIgnoringCase "bar"
            }
        }.message shouldBe """
            0 elements passed but expected 2

            The following elements passed:
            --none--

            The following elements failed:
            "foo bar" => "foo bar" should contain the substring "baz" (case insensitive)
            "FOO BAR" => "FOO BAR" should contain the substring "baz" (case insensitive)
        """.trimIndent()
    }

    @Test fun test_multiple_fails_multiple_subjects() {
        if (!supportsSoftAssertions) return

        shouldThrow<AssertionError> {
            sequenceOf("foo bar", "FOO BAR").testAll {
                it shouldContain "baz"
                it shouldContain "BAZ"
            }
        }.message shouldMatchGlob """
            0 elements passed but expected 2

            The following elements passed:
            --none--

            The following elements failed:
            "foo bar" =>*
            The following 2 assertions failed:
            1) "foo bar" should include substring "baz"
            **
            2) "foo bar" should include substring "BAZ"
            **
            "FOO BAR" =>*
            The following 2 assertions failed:
            1) "FOO BAR" should include substring "baz"
            **
            2) "FOO BAR" should include substring "BAZ"
            **
        """.trimIndent()
    }
}
