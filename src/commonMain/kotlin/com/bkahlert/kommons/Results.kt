package com.bkahlert.kommons

/**
 * Returns this result's value if it represents [Result.success] and
 * the caught [Throwable] if it represents [Result.failure].
 */
public fun <T> Result<T>.getOrException(): Pair<T?, Throwable?> =
    fold({ it to null }, { null to it })
