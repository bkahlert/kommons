package com.bkahlert.kommons.test.junit

import com.bkahlert.kommons.test.SLF4J
import com.bkahlert.kommons.test.junit.DynamicTestDisplayNameGenerator.displayNameFor
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.function.Executable
import java.util.stream.Stream

/**
 * Builds a single test
 * that [softly] checks the specified [assertions]
 * for each of the specified [subjects].
 *
 * The name for each container is heuristically derived but can also be explicitly specified using [testNamePattern]
 * which supports curly placeholders `{}` like [SLF4J] does.
 */
public fun <T> testEach(
    vararg subjects: T,
    testNamePattern: String? = null,
    softly: Boolean = true,
    assertions: Assertions<T>,
): Stream<DynamicTest> = subjects.asList().testEach(testNamePattern, softly, assertions)

/**
 * Builds a single test
 * that [softly] checks the specified [assertions]
 * for each subject of this [Collection].
 *
 * The name for each container is heuristically derived but can also be explicitly specified using [testNamePattern]
 * which supports curly placeholders `{}` like [SLF4J] does.
 */
public fun <T> Iterable<T>.testEach(
    testNamePattern: String? = null,
    softly: Boolean = true,
    assertions: Assertions<T>,
): Stream<DynamicTest> {
    val testSourceUri = PathSource.currentUri
    return toList()
        .also { require(it.isNotEmpty()) { "At least one subject must be provided for testing." } }
        .map { subject: T ->
            DynamicTest.dynamicTest(
                displayNameFor(subject, testNamePattern),
                testSourceUri,
                if (softly) {
                    Executable { com.bkahlert.kommons.test.testAll { assertions(subject) } }
                } else {
                    Executable { assertions(subject) }
                },
            )
        }.stream()
}

/**
 * Builds a single test
 * that [softly] checks the specified [assertions]
 * for each subject of this [Sequence].
 *
 * The name for each container is heuristically derived but can also be explicitly specified using [testNamePattern]
 * which supports curly placeholders `{}` like [SLF4J] does.
 */
public fun <T> Sequence<T>.testEach(
    testNamePattern: String? = null,
    softly: Boolean = true,
    assertions: Assertions<T>,
): Stream<DynamicTest> = toList().testEach(testNamePattern, softly, assertions)

/**
 * Builds a single test
 * that [softly] checks the specified [assertions]
 * for each entry of this [Map].
 *
 * The name for each container is heuristically derived but can also be explicitly specified using [testNamePattern]
 * which supports curly placeholders `{}` like [SLF4J] does.
 */
public fun <K, V> Map<K, V>.testEach(
    testNamePattern: String? = null,
    softly: Boolean = true,
    assertions: Assertions<Map.Entry<K, V>>,
): Stream<DynamicTest> = entries.testEach(testNamePattern, softly, assertions)
