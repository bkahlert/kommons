package koodies

/**
 * Returns `this` results value if it was successful
 * and the caught [Throwable] if it failed.
 */
fun <T> Result<T>.getOrException() =
    fold({ it to null }, { null to it })
