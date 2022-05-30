package com.bkahlert.kommons

/**
 * Returns this result's value if it represents [Result.success] and
 * the caught [Throwable] if it represents [Result.failure].
 */
public fun <T> Result<T>.getOrException(): Pair<T?, Throwable?> =
    fold({ it to null }, { null to it })

/**
 * Returns the caught [Throwable] if this result represents [Result.failure] and `null` if it represents [Result.success].
 */
public fun <T> Result<T>.exceptionOrNull(): Throwable? =
    fold({ null }, { it })
