package koodies.functional

inline fun <reified T> ((T) -> T)?.compose(vararg functions: ((T) -> T)): ((T) -> T) =
    functions.reversed().foldRight(compose { it }, { acc, x -> x + acc })

inline fun <reified T> ((T) -> T)?.compose(crossinline function: ((T) -> T)): ((T) -> T) =
    { function(this?.invoke(it) ?: it) }

inline operator fun <reified T> ((T) -> T)?.plus(crossinline function: ((T) -> T)): ((T) -> T) =
    { function(this?.invoke(it) ?: it) }

inline fun <reified T> compositionOf(vararg functions: (T) -> T): ((T) -> T) {
    if (functions.isEmpty()) return { t: T -> t }
    return functions.first().compose(*functions.drop(1).toTypedArray())
}

inline fun <reified T> compositionOf(vararg functions: Pair<Boolean, (T) -> T>): ((T) -> T) {
    if (functions.isEmpty()) return { t: T -> t }
    return functions.first { it.first }.second
        .compose(*functions.filter { it.first }.drop(1).map { it.second }.toTypedArray())
}

