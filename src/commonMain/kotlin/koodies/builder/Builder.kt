package koodies.builder

import koodies.builder.context.StatefulContext
import kotlin.jvm.JvmName


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
 * to model a domain specific language. If the context has a state its needs to
 * be either reset or recreated for each build so consecutive builds do not interfere.
 *
 * ### Step 2: Context Initialization
 * A build is triggered by invocation of the [build] function.
 * The argument passed to the build function is a [Init], that is, a lambda with
 * the just provided context as its receiver object. While the [Init] operates
 * on the context, it manipulates the context's state.
 *
 * ### Step 3: Instantiation
 * In the last step the context state is used to create an instance of [R].
 *
 * @see BuilderTemplate
 * @see SlipThroughBuilder
 * @see StatefulContextBuilder
 * @see SkippableBuilder
 */
fun interface Builder<in T : Function<*>, out R> {

    /**
     * Builds an instance of type [R] using the given
     * build argument [init].
     */
    operator fun invoke(init: T): R
}

/**
 * Builds an instance of [S] by applying [transform] to the originally
 * built instance of [T].
 */
inline fun <reified T : Function<*>, reified R, reified S> Builder<T, R>.build(
    init: T,
    transform: R.() -> S,
): S = invoke(init).run(transform)

/**
 * Builds an instance of [T] and adds it to the specified [destination].
 */
inline fun <reified T : Function<*>, reified R> Builder<T, R>.buildTo(
    destination: MutableCollection<in R>,
    init: T,
): R = invoke(init).also { destination.add(it) }


/**
 * Builds an instance of [S] and adds it to the specified [destination].
 *
 * The instance of [S] is built by applying [transform] to the originally
 * built instance of [T].
 */
inline fun <reified T : Function<*>, reified R, reified S> Builder<T, R>.buildTo(
    init: T,
    destination: MutableCollection<in S>,
    transform: R.() -> S,
): S = build(init, transform).also { destination.add(it) }

/**
 * Builds multiple instances of [S] by applying [transform] to the originally
 * built instance of [T].
 */
inline fun <reified T : Function<*>, reified R, reified S> Builder<T, R>.buildMultiple(
    init: T,
    transform: R.() -> List<S>,
): List<S> = invoke(init).run(transform)

/**
 * Builds multiple instances of [S] and adds them to the specified [destination].
 *
 * The instance of [S] is built by applying [transform] to the originally
 * built instance of [T].
 */
inline fun <reified T : Function<*>, reified R, reified S> Builder<T, R>.buildMultipleTo(
    init: T,
    destination: MutableCollection<in S>,
    transform: R.() -> List<S>,
): List<S> = invoke(init).run(transform).also { destination.addAll(it) }

/**
 * Simple default interface of a builder that does nothing but
 * return the result of the build argument.
 *
 * @see SlipThroughBuilder
 */
interface NoopBuilder<T> : Builder<() -> T, T> {
    override fun invoke(init: () -> T): T = init()
}

/**
 * Convenience interface for a [Builder] that features an immutable context.
 * The build argument is a series of manipulations mostly based on functions
 * provided by the context.
 *
 * Apart from that the build argument leaves no traces as the context is immutable
 * and therefore simply `slips through` the build process.
 *
 * The build can only be finalized if the transformations leads to a return
 * value of type [R] (respectively [S] if post-processing is applied).
 *
 * If all transformations are provided by the context (i.e. transitions only
 * possible with the context; or non-final intermediary states) and can be
 * chained, crisp mini domain specific languages can be implemented.
 *
 * **Example**
 * ```kotlin
 * DockerImage { "bkahlert" / "libguestfs" tag "latest" }
 * ```
 */
interface SlipThroughBuilder<C, R, S> : Builder<C.() -> R, S> {
    /**
     * A (typically immutable) context that provides functions
     * to transform the initial argument of the [build]'s [InitCompute]
     */
    val context: C
    val transform: R.() -> S
    override fun invoke(init: C.() -> R): S = context.init().transform()
}

/**
 * Builder that stores the context state [S] separately from the context [C].
 *
 * @see Builder
 */
interface StatefulContextBuilder<C, S, R> : Builder<Init<C>, R> {
    /**
     * Stores the aggregated [StatefulContext.state] of all operations
     * performed on the [StatefulContext.context].
     */
    val statefulContext: StatefulContext<C, S>

    /**
     * Build step that builds instances of [R]
     * based on the aggregated [StatefulContext.state]
     * that resulted from the operations
     * performed on the immutable [StatefulContext.context].
     */
    val transform: S.() -> R

    /**
     * Builds a new instance of [R] by providing an [StatefulContext.context]
     * that aggregates all operations performed by the specified [init].
     *
     * The resulting [StatefulContext.state] will be used by [compose]
     * to build an actual instance of [R].
     */
    override fun invoke(init: Init<C>): R =
        with(statefulContext) {
            context.init()
            state.transform()
        }
}

/**
 * Function that initializes a context.
 */
typealias Init<C> = C.() -> Unit


@JvmName("mapSingle")
inline fun <T : Function<*>, reified R, reified S> Builder<T, R>.mapBuild(crossinline transform: R.() -> S) =
    Builder<T, S> { this(it).transform() }

inline fun <T : Function<*>, reified R, reified S> Builder<T, Iterable<R>>.mapBuild(crossinline transform: List<R>.() -> Iterable<S>) =
    Builder<T, S> { this(it).toList().transform().single() }

