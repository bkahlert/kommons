package com.bkahlert.kommons.test

import io.kotest.assertions.throwables.shouldNotThrowAny
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldContainIgnoringCase
import kotlin.test.Test

class TestEnumKtTest {

    @Test fun test_empty() {
        shouldNotThrowAny {
            testEnum<EmptyEnum> { }
        }
    }

    @Test fun test_success() {
        shouldNotThrowAny {
            testEnum<FooBarEnum> {
                it.name shouldContainIgnoringCase "foo"
                it.name shouldContainIgnoringCase "bar"
            }
        }
    }

    @Test fun test_single_fail_single_subject() {
        shouldThrow<AssertionError> {
            testEnum<FooBarEnum> {
                it.name shouldContain "foo"
                it.name shouldContainIgnoringCase "bar"
            }
        }.message shouldBe """
            1 elements passed but expected 2

            The following elements passed:
            foo_bar

            The following elements failed:
            FOO_BAR => "FOO_BAR" should include substring "foo"
        """.trimIndent()
    }

    @Test fun test_single_fail_multiple_subjects() {
        shouldThrow<AssertionError> {
            testEnum<FooBarEnum> {
                it.name shouldContainIgnoringCase "baz"
                it.name shouldContainIgnoringCase "bar"
            }
        }.message shouldBe """
            0 elements passed but expected 2

            The following elements passed:
            --none--

            The following elements failed:
            foo_bar => "foo_bar" should contain the substring "baz" (case insensitive)
            FOO_BAR => "FOO_BAR" should contain the substring "baz" (case insensitive)
        """.trimIndent()
    }

    @Test fun test_multiple_fails_multiple_subjects() {
        shouldThrow<AssertionError> {
            testEnum<FooBarEnum> {
                it.name shouldContain "baz"
                it.name shouldContain "BAZ"
            }
        }.message shouldMatchGlob """
            0 elements passed but expected 2

            The following elements passed:
            --none--

            The following elements failed:
            foo_bar =>*
            The following 2 assertions failed:
            1) "foo_bar" should include substring "baz"
            **
            2) "foo_bar" should include substring "BAZ"
            **
            FOO_BAR =>*
            The following 2 assertions failed:
            1) "FOO_BAR" should include substring "baz"
            **
            2) "FOO_BAR" should include substring "BAZ"
            **
        """.trimIndent()
    }
}
