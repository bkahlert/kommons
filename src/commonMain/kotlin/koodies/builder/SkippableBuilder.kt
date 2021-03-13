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

/**
 * Creates a [SkippableBuilder] that builds by invoking `this` [Builder]
 * and that skips the build by returning the result provided by the caller.
 */
public fun <T : Function<*>, R> Builder<T, R>.skippable(): SkippableBuilder<T, R, R> = skippable { it }

/**
 * Creates a [SkippableBuilder] that builds by invoking `this` [Builder]
 * and that skips the build by invoking the specified [skip].
 */
public infix fun <T : Function<*>, U, R> Builder<T, R>.skippable(skip: (U) -> R): SkippableBuilder<T, U, R> = object : SkippableBuilder<T, U, R> {
    override fun invoke(init: T): R = this@skippable(init)
    override fun using(result: U): R = skip(result)
}

/**
 * Creates a [SkippableBuilder] that builds by invoking `this` [SkippableBuilder] and passing
 * the result to the given [transform].
 */
public infix fun <T : Function<*>, U, R, S> SkippableBuilder<T, U, R>.mapBuild(transform: (R) -> S): SkippableBuilder<T, U, S> =
    object : SkippableBuilder<T, U, S> {
        override fun invoke(init: T): S = transform(this@mapBuild(init))
        override fun using(result: U): S = transform(this@mapBuild.using(result))
    }

/**
 * Creates a [SkippableBuilder] that builds by invoking `this` [SkippableBuilder] and passing
 * the result to the given [builder].
 */
public infix fun <T : Function<*>, U, R : Function<*>, S> SkippableBuilder<T, U, R>.mapBuild(builder: Builder<R, S>): SkippableBuilder<T, U, S> =
    object : SkippableBuilder<T, U, S> {
        override fun invoke(init: T): S = builder(this@mapBuild(init))
        override fun using(result: U): S = builder(this@mapBuild.using(result))
    }
