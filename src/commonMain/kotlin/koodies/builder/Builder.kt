package koodies.builder


/**
 * # Builder
 * This library defines a builder as follows:
 *
 * > *A builder is an object that builds instances of type [R]*
 * > *using a build argument of functional type [T].*
 *
 * Builders are distinguished between context-free builders and
 * those providing a context to their build argument [T].
 *
 * Using a context can be for different reasons:
 * 1) it can hold the build information to create instances of type [R]
 * 2) it can provide functions to ease the building process
 * 3) it can provide domain functions to implement an embedded domain specific language.
 *
 * By separation of the two concepts `builder` and `context`,
 * the context can be kept free from technical aspects that would otherwise
 * pollute the namespace and decrease usability.
 *
 * In this library, all builders with a context have the same three building steps.
 *
 * ## Build Process
 *
 * ### Step 1: Context Creation
 * A context is created to hold build information, provide helpful functions or
 * to model a domain specific language. If the context has a state a new instance
 * needs to be created for each build to guarantee thread-safety on all platforms.
 *
 * ### Step 2: Context Initialization
 * A build is triggered by invocation of the [build] function.
 * The argument passed to the build function is a [Init], that is, a lambda with
 * the just provided context as its receiver object. While the [Init] operates
 * on the context, it manipulates the context's state.
 *
 * ### Step 3: Instantiation
 * The last step consists of using the modified context state to create an
 * instance of [R].
 *
 * @see BuilderTemplate
 * @see StatelessBuilder
 * @see SkippableBuilder
 */
public fun interface Builder<in T : Function<*>, out R> {

    /**
     * Builds an instance of type [R] using the given
     * build argument [init].
     */
    public operator fun invoke(init: T): R
}

/**
 * Builds an instance of type [R] using the given
 * build argument [init].
 */
public inline fun <reified T : Function<*>, reified R> Builder<T, R>.build(init: T): R = invoke(init)

/**
 * Builds an instance of type [R] using the given
 * build argument [init].
 */
public inline fun <reified T : Function<*>, reified R> Function1<T, R>.build(init: T): R = invoke(init)

/**
 * Builds an instance of [S] by applying [transform] to the originally
 * built instance of [T].
 */
public inline fun <reified T : Function<*>, reified R, reified S> Builder<T, R>.build(
    init: T,
    transform: R.() -> S,
): S = invoke(init).run(transform)

/**
 * Builds an instance of [T] and adds it to the specified [destination].
 */
public inline fun <reified T : Function<*>, reified R> Builder<T, R>.buildTo(
    destination: MutableCollection<in R>,
    init: T,
): R = invoke(init).also { destination.add(it) }

/**
 * Builds an instance of [S] and adds it to the specified [destination].
 *
 * The instance of [S] is built by applying [transform] to the originally
 * built instance of [T].
 */
public inline fun <reified T : Function<*>, reified R, reified S> Builder<T, R>.buildTo(
    init: T,
    destination: MutableCollection<in S>,
    transform: R.() -> S,
): S = build(init, transform).also { destination.add(it) }


/**
 * Builds an instance of [T] and calls the specified [destination] with it.
 */
public inline fun <reified T : Function<*>, reified R> Builder<T, R>.buildTo(
    destination: Function1<R, *>,
    init: T,
): R = invoke(init).also { destination(it) }

/**
 * Builds an instance of [S] and calls the specified [destination] with it.
 *
 * The instance of [S] is built by applying [transform] to the originally
 * built instance of [T].
 */
public inline fun <reified T : Function<*>, reified R, reified S> Builder<T, R>.buildTo(
    init: T,
    destination: Function1<S, *>,
    transform: R.() -> S,
): S = build(init, transform).also { destination(it) }

/**
 * Builds multiple instances of [S] by applying [transform] to the originally
 * built instance of [T].
 */
public inline fun <reified T : Function<*>, reified R, reified S> Builder<T, R>.buildMultiple(
    init: T,
    transform: R.() -> List<S>,
): List<S> = invoke(init).run(transform)

/**
 * Builds multiple instances of [S] and adds them to the specified [destination].
 *
 * The instances of [S] are built by applying [transform] to the originally
 * built instance of [T].
 */
public inline fun <reified T : Function<*>, reified R, reified S> Builder<T, R>.buildMultipleTo(
    init: T,
    destination: MutableCollection<in S>,
    transform: R.() -> List<S>,
): List<S> = invoke(init).run(transform).also { destination.addAll(it) }

/**
 * Builds multiple instances of [S] and calls the specified [destination] with each of them.
 *
 * The instances of [S] are built by applying [transform] to the originally
 * built instance of [T].
 */
public inline fun <reified T : Function<*>, reified R, reified S> Builder<T, R>.buildMultipleTo(
    init: T,
    destination: Function1<S, *>,
    transform: R.() -> List<S>,
): List<S> = invoke(init).run(transform).onEach { destination(it) }

/**
 * Simple default interface of a builder that does nothing but
 * return the result of the build argument.
 *
 * @see StatelessBuilder
 */
public interface PseudoBuilder<T> : Builder<() -> T, T> {
    override fun invoke(init: () -> T): T = init()
}

/**
 * Convenience interface for a [Builder] that features an immutable context.
 * Consequently the build process is restricted to transformations of the build
 * argument itself using functions provided by the context.
 *
 * What sounds like a downside is quite handy to model **micro domain specific
 * languages** of which the scope reaches from
 * here 👉 `{ "mini".DSL to result }` 👈 to here.
 *
 * The build can only be finalized if the transformations lead to a return
 * value of type [R] (respectively [S] if post-processing is applied).
 *
 * If all transformations are provided by the context (i.e. transitions only
 * possible with the context; or non-final intermediary states) and can be
 * chained, crisp mini domain specific languages can be implemented.
 *
 * **Examples**
 * ```kotlin
 * DockerImage { "bkahlert" / "libguestfs" tag "latest" }
 * ```
 *
 * ```kotlin
 * DockerMounts {
 *     "/some/directory" mountAs bind at "/app/data"
 *     HomeDir mountAt "/home"
 * }
 */
public interface StatelessBuilder<C, R, S> : Builder<C.() -> R, S> {

    /**
     * Implementation of a [StatelessBuilder] that builds by applying
     * the build argument to the given [context] and passing the intermediary
     * build result to the given [finalize].
     */
    public open class PostProcessing<C, R, S>(private val context: C, private val finalize: R.() -> S) : Builder<C.() -> R, S> {
        override fun invoke(init: C.() -> R): S = context.init().finalize()
    }

    /**
     * Implementation of a [StatelessBuilder] that builds by applying
     * the build argument to the given [context].
     */
    public open class Returning<C, R>(private val context: C) : Builder<C.() -> R, R> {
        override fun invoke(init: C.() -> R): R = context.init()
    }
}

/**
 * Function that initializes a context.
 */
public typealias Init<C> = C.() -> Unit

/**
 * Creates a [Builder] that builds by invoking `this` [Builder] and passing
 * the result to the given [transform].
 */
public inline infix fun <T : Function<*>, reified R, reified S> Builder<T, R>.mapBuild(crossinline transform: (R) -> S): Builder<T, S> =
    Builder { transform(this(it)) }

/**
 * Creates a [Builder] that builds by invoking `this` [Builder] and passing
 * the result to the given [builder].
 */
public infix fun <T : Function<*>, R : Function<*>, S> Builder<T, R>.mapBuild(builder: Builder<R, S>): Builder<T, S> =
    Builder { builder(invoke(it)) }
