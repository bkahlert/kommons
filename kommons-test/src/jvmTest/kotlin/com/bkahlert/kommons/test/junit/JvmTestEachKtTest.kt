package com.bkahlert.kommons.test.junit

import com.bkahlert.kommons.test.asList
import com.bkahlert.kommons.test.t
import com.bkahlert.kommons.test.testAll
import io.kotest.assertions.fail
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldContainIgnoringCase
import org.junit.jupiter.api.DynamicNode
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory
import java.util.stream.Stream


class JvmTestEachKtTest {

    @Test fun test_empty() {
        shouldThrow<IllegalArgumentException> {
            testEach<Any?> { }
        }
    }

    @Test fun test_success() {
        testEach("foo bar", "FOO BAR") {
            it shouldContainIgnoringCase "foo"
            it shouldContainIgnoringCase "bar"
        }.collectingMessagesExecute().shouldBeEmpty()
    }

    @Test fun test_single_fail_single_subject() {
        testEach("foo bar", "FOO BAR") {
            it shouldContain "foo"
            it shouldContainIgnoringCase "bar"
        }.collectingMessagesExecute().shouldContainExactlyInAnyOrder(
            """
            "FOO BAR" should include substring "foo"
            """.trimIndent(),
        )
    }

    @Test fun test_single_fail_multiple_subjects() {
        testEach("foo bar", "FOO BAR") {
            it shouldContainIgnoringCase "baz"
            it shouldContainIgnoringCase "bar"
        }.collectingMessagesExecute().shouldContainExactlyInAnyOrder(
            """
            "foo bar" should contain the substring "baz" (case insensitive)
            """.trimIndent(),
            """
            "FOO BAR" should contain the substring "baz" (case insensitive)
            """.trimIndent(),
        )
    }

    @Test fun test_multiple_fails_multiple_subjects() {
        val firstLine = 64
        testEach("foo bar", "FOO BAR") {
            it shouldContain "baz"
            it shouldContain "BAZ"
        }.collectingMessagesExecute().shouldContainExactlyInAnyOrder(
            """

            The following 2 assertions failed:
            1) "foo bar" should include substring "baz"
            ${t}at com.bkahlert.kommons.test.junit.JvmTestEachKtTest${'$'}test_multiple_fails_multiple_subjects${'$'}1.invoke(JvmTestEachKtTest.kt:${firstLine + 2})
            2) "foo bar" should include substring "BAZ"
            ${t}at com.bkahlert.kommons.test.junit.JvmTestEachKtTest${'$'}test_multiple_fails_multiple_subjects${'$'}1.invoke(JvmTestEachKtTest.kt:${firstLine + 3})

            """.trimIndent(),
            """

            The following 2 assertions failed:
            1) "FOO BAR" should include substring "baz"
            ${t}at com.bkahlert.kommons.test.junit.JvmTestEachKtTest${'$'}test_multiple_fails_multiple_subjects${'$'}1.invoke(JvmTestEachKtTest.kt:${firstLine + 2})
            2) "FOO BAR" should include substring "BAZ"
            ${t}at com.bkahlert.kommons.test.junit.JvmTestEachKtTest${'$'}test_multiple_fails_multiple_subjects${'$'}1.invoke(JvmTestEachKtTest.kt:${firstLine + 3})

            """.trimIndent(),
        )
    }

    @Test fun test_dynamic_test_factories() {
        testAll(
            { assertions -> testEach("foo" to "bar", "FOO" to "BAR", testNamePattern = "{} ➡ {}") { assertions(it) } },
            { assertions -> listOf("foo" to "bar", "FOO" to "BAR").testEach(testNamePattern = "{} ➡ {}") { assertions(it) } },
            { assertions -> sequenceOf("foo" to "bar", "FOO" to "BAR").testEach(testNamePattern = "{} ➡ {}") { assertions(it) } },
            { assertions -> mapOf("foo" to "bar", "FOO" to "BAR").testEach(testNamePattern = "{} ➡ {}") { assertions(it.key to it.value) } },
        ) { factory: (Assertions<Pair<String, String>>) -> Stream<DynamicTest> ->
            val tests = factory { (key, value) -> fail("$key -> $value failed") }.asList()
            tests shouldHaveSize 2
            tests.first() should { test ->
                test.displayName shouldBe "\"foo\" ➡ \"bar\""
                shouldThrow<AssertionError> { test.executable.execute() }.message shouldBe "foo -> bar failed"
            }
            tests.last() should { test ->
                test.displayName shouldBe "\"FOO\" ➡ \"BAR\""
                shouldThrow<AssertionError> { test.executable.execute() }.message shouldBe "FOO -> BAR failed"
            }
        }

        testEach("foo" to "bar", "FOO" to "BAR") {}.displayNames.shouldContainExactly("( \"foo\", \"bar\" )", "( \"FOO\", \"BAR\" )")
        listOf("foo" to "bar", "FOO" to "BAR").testEach { }.displayNames.shouldContainExactly("( \"foo\", \"bar\" )", "( \"FOO\", \"BAR\" )")
        sequenceOf("foo" to "bar", "FOO" to "BAR").testEach {}.displayNames.shouldContainExactly("( \"foo\", \"bar\" )", "( \"FOO\", \"BAR\" )")
        mapOf("foo" to "bar", "FOO" to "BAR").testEach { }.displayNames.shouldContainExactly("\"foo\" → \"bar\"", "\"FOO\" → \"BAR\"")
    }

    @TestFactory fun test_array() = testEach("foo bar", "FOO BAR") {
        it shouldContainIgnoringCase "foo"
        it shouldContainIgnoringCase "bar"
    }

    @TestFactory fun test_iterable() = listOf("foo bar", "FOO BAR").testEach {
        it shouldContainIgnoringCase "foo"
        it shouldContainIgnoringCase "bar"
    }

    @TestFactory fun test_sequence() = sequenceOf("foo bar", "FOO BAR").testEach {
        it shouldContainIgnoringCase "foo"
        it shouldContainIgnoringCase "bar"
    }

    @TestFactory fun test_map() = mapOf("foo" to "bar", "FOO" to "BAR").testEach { (key, value) ->
        key shouldContainIgnoringCase "foo"
        value shouldContainIgnoringCase "bar"
    }
}

private val Stream<out DynamicNode>.displayNames get() = map { it.displayName }.asList()
