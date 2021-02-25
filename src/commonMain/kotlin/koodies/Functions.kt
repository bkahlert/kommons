package koodies

/**
 * Represents a function that takes no arguments.
 */
fun interface Function0<out R> : Function<R> {
    /** Invokes the function. */
    operator fun invoke(): R
}

/**
 * Represents a function that takes 1 argument.
 */
fun interface Function1<in P1, out R> : Function<R> {
    /** Invokes the function with the specified argument. */
    operator fun invoke(p1: P1): R
}

/**
 * Represents a function that takes 2 argument.
 */
fun interface Function2<in P1, in P2, out R> : Function<R> {
    /** Invokes the function with the specified arguments. */
    operator fun invoke(p1: P1, p2: P2): R
}


/**
 * Operation that can be skipped by providing an alternative result
 * using [invoke].
 */
interface Skippable<T, R> {
    /**
     * Skips this operation.
     */
    operator fun invoke(result: T): R
}
