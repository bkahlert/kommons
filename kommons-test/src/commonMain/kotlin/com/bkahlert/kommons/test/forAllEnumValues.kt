package com.bkahlert.kommons.test

import io.kotest.assertions.assertSoftly
import io.kotest.inspectors.forAll

/** Asserts the specified [assertions] for each [Enum] entry. */
public inline fun <reified E : Enum<E>> forAllEnumValues(assertions: (E) -> Unit): Array<E> =
    enumValues<E>().forAll {
        assertSoftly { assertions(it) }
    }
