package koodies

/**
 * Represents a function that takes no arguments.
 */
public fun interface Function0<out R> : Function<R> {
    /** Invokes the function. */
    public operator fun invoke(): R
}

/**
 * Represents a function that takes 1 argument.
 */
public fun interface Function1<in P1, out R> : Function<R> {
    /** Invokes the function with the specified argument. */
    public operator fun invoke(p1: P1): R
}

/**
 * Represents a function that takes 2 argument.
 */
public fun interface Function2<in P1, in P2, out R> : Function<R> {
    /** Invokes the function with the specified arguments. */
    public operator fun invoke(p1: P1, p2: P2): R
}


/**
 * Operation that can be skipped by providing an alternative result
 * using [invoke].
 */
public interface Skippable<T, R> {
    /**
     * Skips this operation.
     */
    public infix fun instead(result: T): R
}
