package koodies.functional

/**
 * Returns a new identity function that composes `this` optional identity function
 * with the given identity [functions] by chaining them.
 */
public inline fun <reified T> ((T) -> T)?.compose(vararg functions: ((T) -> T)): ((T) -> T) =
    functions.reversed().foldRight(compose { it }, { acc, x -> x + acc })

/**
 * Returns a new identity function that composes `this` optional identity function
 * with the given mandatory identity [function] by chaining them.
 */
public inline fun <reified T> ((T) -> T)?.compose(crossinline function: ((T) -> T)): ((T) -> T) =
    { function(this?.invoke(it) ?: it) }

/**
 * Returns a new identity function that composes `this` optional identity function
 * with the given mandatory identity [function] by chaining them.
 */
inline operator fun <reified T> ((T) -> T)?.plus(crossinline function: ((T) -> T)): ((T) -> T) =
    { function(this?.invoke(it) ?: it) }

/**
 * Returns a new identity function that composes the given identity [functions]
 * by chaining them.
 */
public inline fun <reified T> compositionOf(vararg functions: (T) -> T): ((T) -> T) {
    if (functions.isEmpty()) return { t: T -> t }
    return functions.first().compose(*functions.drop(1).toTypedArray())
}

/**
 * Returns a new identity function that composes those functions of the given
 * array of boolean-function pairs, with a [Pair.first] `true`.
 */
public inline fun <reified T> compositionOf(vararg functions: Pair<Boolean, (T) -> T>): ((T) -> T) {
    if (functions.isEmpty()) return { t: T -> t }
    return functions.first { it.first }.second
        .compose(*functions.filter { it.first }.drop(1).map { it.second }.toTypedArray())
}

