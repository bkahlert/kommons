package koodies.builder

import koodies.Skippable

/**
 * A [Builder] that can be skipped by providing an instance of type [U]
 * which typically but not necessarily matches the type of the build result.
 */
public interface SkippableBuilder<T : Function<*>, U, R> : Builder<T, R>, Skippable<U, R> {
    /**
     * Skips the build process by using the given [result] instead.
     */
    public override infix fun using(result: U): R

    /**
     * Skips the build process by using the given [result] instead.
     */
    public override infix fun by(result: U): R = using(result)
}

/**
 * Skips the build process by using the  given [result] instead.
 */
public infix fun <T : Function<*>, R> Builder<T, R>.using(result: R): R = result

/**
 * Skips the build process by using the given [result] instead.
 */
public infix fun <T : Function<*>, R> ((T) -> R).using(result: R): R = result


/**
 * Skips the build process by using the  given [result] instead.
 */
public infix fun <T : Function<*>, R> Builder<T, R>.by(result: R): R = result

/**
 * Skips the build process by using the given [result] instead.
 */
public infix fun <T : Function<*>, R> ((T) -> R).by(result: R): R = result
