package com.bkahlert.kommons.test

/** Asserts the specified [assertions] for each [Enum] entry. */
public inline fun <reified E : Enum<E>> testEnum(assertions: (E) -> Unit) {
    forAllEnumValues(assertions)
}
