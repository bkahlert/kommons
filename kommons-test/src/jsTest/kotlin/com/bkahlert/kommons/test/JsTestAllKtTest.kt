package com.bkahlert.kommons.test

import io.kotest.assertions.throwables.shouldNotThrowAny
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import kotlin.test.Test

class JsTestAllKtTest {

    @Test fun test_success() {
        shouldNotThrowAny {
            testAll {
                "foo bar" shouldContain "foo"
                "foo bar" shouldContain "bar"
            }
        }
    }

    @Test fun test_single_fail() {
        shouldThrow<AssertionError> {
            testAll {
                "foo bar" shouldContain "baz"
                "foo bar" shouldContain "bar"
            }
        }.message shouldBe """
            "foo bar" should include substring "baz"
        """.trimIndent()
    }

    @Test fun test_multiple_fails() {
        shouldThrow<AssertionError> {
            testAll {
                "foo bar" shouldContain "baz"
                "foo bar" shouldContain "FOO"
            }
        }.message shouldBe """
            
            The following 2 assertions failed:
            1) "foo bar" should include substring "baz"
            2) "foo bar" should include substring "FOO"
            
        """.trimIndent()
    }
}
