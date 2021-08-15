package com.bkahlert.kommons

/**
 * Returns `this` results value if it was successful
 * and the caught [Throwable] if it failed.
 */
public fun <T> Result<T>.getOrException(): Pair<T?, Throwable?> =
    fold({ it to null }, { null to it })
