package com.bkahlert.kommons_deprecated.test

import strikt.api.Assertion
import strikt.assertions.isEmpty
import strikt.assertions.isEqualTo
import strikt.assertions.single

val <T> Assertion.Builder<Sequence<T>>.elements
    get() = get("elements %s") { toList() }

fun <T> Assertion.Builder<Sequence<T>>.isEmpty() =
    elements.isEmpty()

fun <T> Assertion.Builder<Sequence<T>>.single() =
    elements.single()

fun <T> Assertion.Builder<Sequence<T>>.hasEqualElements(expected: Sequence<T>) =
    elements.isEqualTo(expected.toList())
