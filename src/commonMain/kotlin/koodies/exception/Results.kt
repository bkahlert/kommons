package koodies.exception

fun <T> Result<T>.getOrException() =
    fold({ it to null }, { null to it })
