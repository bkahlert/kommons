package koodies.builder

import koodies.Skippable

/**
 * A [Builder] that can be skipped by providing an instance of type [U]
 * which typically but not necessarily matches the type of the build result.
 */
interface SkippableBuilder<T : Function<*>, U, R> : Builder<T, R>, Skippable<U, R> {
    /**
     * Skips the build process by using the given [result] instead.
     */
    override operator fun invoke(result: U): R

    /**
     * Skips the build process by using the given [result] instead.
     */
    infix fun instead(result: U): R
}

/**
 * Skips the build process by using the given [result] instead.
 */
operator fun <T : Function<*>, R> Builder<T, R>.invoke(result: R): R = result

/**
 * Skips the build process by using the  given [result] instead.
 */
infix fun <T : Function<*>, R> Builder<T, R>.instead(result: R): R = result

/**
 * Skips the build process by using the given [result] instead.
 *
 * ***Note:** Due to Kotlin's resolution strategy, this operator overload is
 *            only used / discovered if caller is in the same package or
 *            if this function is explicitly imported.
 *
 * @see instead
 */
operator fun <T : Function<*>, R> ((T) -> R).invoke(result: R): R = result

/**
 * Skips the build process by using the given [result] instead.
 */
infix fun <T : Function<*>, R> ((T) -> R).instead(result: R): R = result


/**
 * Builds an instance of type [R] using `this` [Builder] and
 * the given build argument [init].
 *
 * This function is provided as a an more explicit alternative to [invoke].
 */
fun <T : Function<*>, R> Builder<T, R>.build(init: T): R = invoke(init)

/**
 * Builds an instance of type [R] using `this` [Builder] and
 * the given build argument [init].
 *
 * This function is provided as a an more explicit alternative to [invoke].
 */
infix fun <T : Function<*>, R> ((T) -> R).build(init: T): R = invoke(init)
