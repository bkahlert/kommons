package com.bkahlert.kommons.test

import io.kotest.assertions.throwables.shouldNotThrowAny
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldContainIgnoringCase
import kotlin.test.Test as KotlinTest

class JvmKotlinTestAllArrayKtTest {

    @KotlinTest fun test_success() {
        shouldNotThrowAny {
            testAll("foo bar", "FOO BAR") {
                it shouldContainIgnoringCase "foo"
                it shouldContainIgnoringCase "bar"
            }
        }
    }

    @KotlinTest fun test_single_fail_single_subject() {
        shouldThrow<AssertionError> {
            testAll("foo bar", "FOO BAR") {
                it shouldContain "foo"
                it shouldContainIgnoringCase "bar"
            }
        }.message shouldBe """
            1 elements passed but expected 2

            The following elements passed:
              [0] foo bar

            The following elements failed:
              [1] "FOO BAR" => "FOO BAR" should include substring "foo"

        """.trimIndent()
    }

    @KotlinTest fun test_single_fail_multiple_subjects() {
        shouldThrow<AssertionError> {
            testAll("foo bar", "FOO BAR") {
                it shouldContainIgnoringCase "baz"
                it shouldContainIgnoringCase "bar"
            }
        }.message shouldBe """
            0 elements passed but expected 2

            The following elements passed:
              --none--

            The following elements failed:
              [0] "foo bar" => "foo bar" should contain the substring "baz" (case insensitive)
              [1] "FOO BAR" => "FOO BAR" should contain the substring "baz" (case insensitive)

        """.trimIndent()
    }

    @KotlinTest fun test_multiple_fails_multiple_subjects() {
        shouldThrow<AssertionError> {
            testAll("foo bar", "FOO BAR") {
                it shouldContain "baz"
                it shouldContain "BAZ"
            }
        }.message shouldMatchGlob """
            0 elements passed but expected 2

            The following elements passed:
              --none--
            
            The following elements failed:
              [0] "foo bar" => The following 2 assertions failed:
            1) "foo bar" should include substring "baz"
               at com.bkahlert.kommons.test.JvmKotlinTestAllArrayKtTest.test_multiple_fails_multiple_subjects(JvmKotlinTestAllArrayKtTest.kt:*)
            2) "foo bar" should include substring "BAZ"
               at com.bkahlert.kommons.test.JvmKotlinTestAllArrayKtTest.test_multiple_fails_multiple_subjects(JvmKotlinTestAllArrayKtTest.kt:*)
            
              [1] "FOO BAR" => The following 2 assertions failed:
            1) "FOO BAR" should include substring "baz"
               at com.bkahlert.kommons.test.JvmKotlinTestAllArrayKtTest.test_multiple_fails_multiple_subjects(JvmKotlinTestAllArrayKtTest.kt:*)
            2) "FOO BAR" should include substring "BAZ"
               at com.bkahlert.kommons.test.JvmKotlinTestAllArrayKtTest.test_multiple_fails_multiple_subjects(JvmKotlinTestAllArrayKtTest.kt:*)
            

        """.trimIndent()
    }
}
