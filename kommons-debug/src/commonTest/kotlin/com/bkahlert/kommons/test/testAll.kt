package com.bkahlert.kommons.test

import io.kotest.assertions.assertSoftly
import io.kotest.inspectors.forAll

/** Asserts the specified [softAssertions]. */
public inline fun testAll(softAssertions: () -> Unit) {
    assertSoftly(softAssertions)
}

/** Asserts the specified [softAssertions] for each of the specified [subjects]. */
public inline fun <T> testAll(subject: T, vararg subjects: T, softAssertions: (T) -> Unit) {
    listOf(subject, *subjects).testAll(softAssertions)
}

/** Asserts the specified [softAssertions] for each of this [Array]. */
public inline fun <T> Array<T>.testAll(softAssertions: (T) -> Unit) {
    asList().testAll(softAssertions)
}

/** Asserts the specified [softAssertions] for each element of this [Collection]. */
public inline fun <T> Iterable<T>.testAll(softAssertions: (T) -> Unit) {
    val subjects = toList()
    require(subjects.isNotEmpty()) { "At least one subject must be provided for testing." }
    subjects.forAll {
        com.bkahlert.kommons.test.testAll { softAssertions(it) }
    }
}

/** Asserts the specified [softAssertions] for each element of this [Sequence]. */
public inline fun <T> Sequence<T>.testAll(softAssertions: (T) -> Unit) {
    toList().testAll(softAssertions)
}

/** Asserts the specified [softAssertions] for each entry of this [Map]. */
public inline fun <K, V> Map<K, V>.testAll(softAssertions: (Map.Entry<K, V>) -> Unit) {
    entries.testAll(softAssertions)
}
