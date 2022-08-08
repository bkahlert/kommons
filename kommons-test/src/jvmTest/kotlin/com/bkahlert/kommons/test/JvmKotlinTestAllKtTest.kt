package com.bkahlert.kommons.test

import io.kotest.assertions.throwables.shouldNotThrowAny
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import kotlin.test.Test as KotlinTest

class JvmKotlinTestAllKtTest {

    @KotlinTest fun test_success() {
        shouldNotThrowAny {
            testAll {
                "foo bar" shouldContain "foo"
                "foo bar" shouldContain "bar"
            }
        }
    }

    @KotlinTest fun test_single_fail() {
        shouldThrow<AssertionError> {
            testAll {
                "foo bar" shouldContain "baz"
                "foo bar" shouldContain "bar"
            }
        }.message shouldBe """
            "foo bar" should include substring "baz"
        """.trimIndent()
    }

    @KotlinTest fun test_multiple_fails() {
        shouldThrow<AssertionError> {
            testAll {
                "foo bar" shouldContain "baz"
                "foo bar" shouldContain "FOO"
            }
        }.message shouldMatchGlob """

            The following 2 assertions failed:
            1) "foo bar" should include substring "baz"
            ${t}at com.bkahlert.kommons.test.JvmKotlinTestAllKtTest.test_multiple_fails(JvmKotlinTestAllKtTest.kt:*)
            2) "foo bar" should include substring "FOO"
            ${t}at com.bkahlert.kommons.test.JvmKotlinTestAllKtTest.test_multiple_fails(JvmKotlinTestAllKtTest.kt:*)

        """.trimIndent()
    }
}
