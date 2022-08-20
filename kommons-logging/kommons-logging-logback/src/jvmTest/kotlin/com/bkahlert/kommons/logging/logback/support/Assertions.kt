package com.bkahlert.kommons.logging.logback.support

import com.bkahlert.kommons.quoted
import org.junit.jupiter.api.DynamicTest
import org.springframework.boot.Banner
import strikt.api.Assertion.Builder
import strikt.assertions.contains

/**
 * Utility to facilitate testing and provide shared assertions.
 *
 * @author Björn Kahlert
 */
object Assertions {
    val SPRING_BOOT = ":: Spring Boot ::"
    val SPRING_CLOUD = ":: Spring Cloud ::"
    val JSON_PARTS = arrayOf("{\"timestamp\":")
    val PLAIN_PARTS = arrayOf("[-,-,-]")
    val CLASSIC_PARTS = arrayOf(",,,")

    fun Builder<Pair<SmartCapturedOutput, SmartCapturedLog>>.hasBanner(
        bannerMode: Banner.Mode, isCloud: Boolean,
    ): Builder<Pair<SmartCapturedOutput, SmartCapturedLog>> {
        return when (bannerMode) {
            Banner.Mode.OFF ->
                with({ first.out }) { containsNoBanner() }
                    .with({ second }) { containsNoBanner() }

            Banner.Mode.CONSOLE ->
                with({ first.out }) { if (isCloud) containsExactlyOneCloudBanner() else containsExactlyOneRegularBanner() }
                    .with({ second }) { containsNoBanner() }

            Banner.Mode.LOG ->                 // which is used if spring.main.banner-mode = LOG is chosen.
                with({ first.out }) { if (isCloud) containsExactlyOneCloudBanner() else containsExactlyOneRegularBanner() }
                    .with({ second }) { if (isCloud) containsExactlyOneCloudBanner() else containsExactlyOneRegularBanner() }
        }
    }

    public fun <T : CharSequence> Builder<T>.containsNoBanner(): Builder<T> {
        return not { contains(SPRING_BOOT) }.not { contains(SPRING_CLOUD) }
    }

    public fun <T : CharSequence> Builder<T>.containsExactlyOneRegularBanner(): Builder<T> {
        return containsExactlyOnce(SPRING_BOOT).not { contains(SPRING_CLOUD) }
    }

    public fun <T : CharSequence> Builder<T>.containsExactlyOneCloudBanner(): Builder<T> {
        return containsExactlyOnce(SPRING_BOOT).containsExactlyOnce(SPRING_CLOUD)
    }

    public fun <T : CharSequence> Builder<T>.containsOnlyJsonLogs(): Builder<T> =
        JSON_PARTS.fold(this) { acc, part -> acc.contains(part) }
            .run { PLAIN_PARTS.fold(this) { acc, part -> acc.not { contains(part) } } }
            .run { CLASSIC_PARTS.fold(this) { acc, part -> acc.not { contains(part) } } }

    public fun <T : CharSequence> Builder<T>.containsOnlyPlainLogs(): Builder<T> =
        JSON_PARTS.fold(this) { acc, part -> not { acc.contains(part) } }
            .run { PLAIN_PARTS.fold(this) { acc, part -> acc.contains(part) } }
            .run { CLASSIC_PARTS.fold(this) { acc, part -> acc.not { contains(part) } } }

    public fun <T : CharSequence> Builder<T>.containsOnlyClassicLogs(): Builder<T> =
        JSON_PARTS.fold(this) { acc, part -> acc.not { contains(part) } }
            .run { PLAIN_PARTS.fold(this) { acc, part -> acc.not { contains(part) } } }
            .run { CLASSIC_PARTS.fold(this) { acc, part -> acc.contains(part) } }
}


fun <T> testEach(vararg testValues: T, test: (T) -> Unit): List<DynamicTest> =
    testValues.toList()
        .map { testValue ->
            val testName = when (testValue) {
                null -> "<NULL>"
                "" -> "<EMPTY STRING>"
                is Pair<*, *> -> "${testValue.first.quoted} to ${testValue.second.quoted}"
                is Triple<*, *, *> -> "${testValue.first.quoted} to ${testValue.second.quoted} to ${testValue.third.quoted}"
                else -> testValue.toString()
            }
            DynamicTest.dynamicTest(testName) { test.invoke(testValue) }
        }

fun <T : CharSequence> Builder<T>.containsExactlyOnce(expected: CharSequence): Builder<T> =
    assert("contains %s exactly once") {
        when (val count = expected.split(expected.toString(), limit = 3).size - 1) {
            1 -> pass()
            else -> fail("$count found")
        }
    }
