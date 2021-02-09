package koodies.builder

import koodies.builder.context.StatefulContext

typealias X<BC, T> = CompanionBuilder<BC, T>

fun interface CompanionBuilder<BC : BuildingContext<BC, T>, T> {
    fun build(init: Init<BC>): T

    companion object {
        inline fun <reified BC : BuildingContext<BC, T>, reified T> from(noinline buildingContextProvider: () -> BC) =
            CompanionBuilder<BC, T> { Builder.build(it, buildingContextProvider) }
    }
}


/**
 * One-arity lambda that initializes
 * a context.
 */
typealias Init<C, R> = C.() -> R

/**
 * A builder is an object that builds instances of type [T]
 * with the help of a context [C].
 *
 * A context is needed for multiple reasons:
 * 1) it holds the build information
 *    - to be one or many instances
 *    - immediately or at a later moment in time
 * 2) optionally it provides functions to ease the building process
 * 3) optionally it provides domain functions to implement an embedded domain specific language.
 *
 * By separation of the two concepts builder and context,
 * the context can be kept free from technical aspects that would otherwise
 * pollute the namespace and decrease usability.
 * This holds true in particular for the [build] function.
 */
fun interface Builder<out C, in R, out T> {
    /**
     * Builds an instance of [T] using the specified
     * building [this@invoke] and [init].
     */
    fun build(init: Init<C, R>): T

    companion object {

        /**
         * Builds an instance [T] by retrieving a context [C] from the specified
         * [contextProvider], applying the specified [init] to it and
         * building the context using the specified [builder].
         *
         * *Note: This method is not implemented as a default member / extension function of [Builder] in
         * order not to pollute the building context [C] with technical concerns.*
         */
        fun <B : Builder<C, T>, C, T> build(builder: B, init: Init<C>, contextProvider: () -> C): T =
            builder.invoke(contextProvider().apply(init))

        /**
         * Builds an instance [T] by retrieving a building context [BC] from the specified
         * [buildingContextProvider] applying the specified [init] to it.
         *
         * *Note: This method is not implemented as a default member / extension function of [Builder] in
         * order not to pollute the building context [BC] with technical concerns.*
         */
        fun <BC : BuildingContext<BC, T>, T> build(init: Init<in BC>, buildingContextProvider: () -> BC): T =
            buildingContextProvider().let { build(it, init, { it }) }

        fun <BC : BuildingContext<BC, T>, T, P1> build(init: (BC.() -> Unit), p1: P1, buildingContextProvider: (P1) -> BC): T =
            build(init, { buildingContextProvider(p1) })

        fun <BC : BuildingContext<BC, T>, T> buildTo(init: BC.() -> Unit, buildingContextProvider: () -> BC, target: MutableCollection<in T>): T =
            build(init, buildingContextProvider).also { target.add(it) }

        fun <BC : BuildingContext<BC, T>, T, P1> buildTo(init: BC.() -> Unit, p1: P1, buildingContextProvider: (P1) -> BC, target: MutableCollection<in T>): T =
            build(init, p1, buildingContextProvider).also { target.add(it) }

        fun <BC : BuildingContext<BC, T>, T, U> build(init: BC.() -> Unit, buildingContextProvider: () -> BC, transform: T.() -> U): U =
            build(init, buildingContextProvider).run(transform)

        fun <BC : BuildingContext<BC, T>, T, U, P1> build(init: BC.() -> Unit, p1: P1, buildingContextProvider: (P1) -> BC, transform: T.() -> U): U =
            build(init, p1, buildingContextProvider).run(transform)

        fun <BC : BuildingContext<BC, T>, T, U> buildTo(
            init: BC.() -> Unit,
            buildingContextProvider: () -> BC,
            target: MutableCollection<in U>,
            transform: T.() -> U,
        ): U = build(init, buildingContextProvider, transform).also { target.add(it) }

        fun <BC : BuildingContext<BC, T>, T, U, P1> buildTo(
            init: BC.() -> Unit,
            p1: P1,
            buildingContextProvider: (P1) -> BC,
            target: MutableCollection<in U>,
            transform: T.() -> U,
        ): U = build(init, p1, buildingContextProvider, transform).also { target.add(it) }

        fun <BC : BuildingContext<BC, T>, T, U> buildMultipleTo(
            init: BC.() -> Unit,
            buildingContextProvider: () -> BC,
            target: MutableCollection<in U>,
            transform: T.() -> List<U>,
        ): List<U> = build(init, buildingContextProvider, transform).also { target.addAll(it) }

        fun <BC : BuildingContext<BC, T>, T, U, P1> buildMultipleTo(
            init: BC.() -> Unit,
            p1: P1,
            buildingContextProvider: (P1) -> BC,
            target: MutableCollection<in U>,
            transform: T.() -> List<U>,
        ): List<U> = build(init, p1, buildingContextProvider, transform).also { target.addAll(it) }

        /**
         * Builds an instance [E] by retrieving a building context [BC] from the specified
         * [buildingContextProvider] applying the specified [init] to it.
         *
         * *Note: This method is not implemented as a default member / extension function of [Builder] in
         * order not to pollute the building context [BC] with technical concerns.*
         */
        fun <BC : BuildingContext<BC, List<E>>, E> buildList(init: Init<in BC>, buildingContextProvider: () -> BC): List<E> =
            buildingContextProvider().let { build(it, init, { it }) }

        fun <BC : BuildingContext<BC, List<E>>, E, P1> buildList(init: (BC.() -> Unit), p1: P1, buildingContextProvider: (P1) -> BC): List<E> =
            buildList(init, { buildingContextProvider(p1) })

        fun <BC : BuildingContext<BC, List<E>>, E> buildListTo(
            init: BC.() -> Unit,
            target: MutableCollection<in E>,
            buildingContextProvider: () -> BC,
        ): List<E> =
            buildList(init, buildingContextProvider).also { target.addAll(it) }

        fun <BC : BuildingContext<BC, List<E>>, E, P1> buildListTo(
            init: BC.() -> Unit,
            p1: P1,
            target: MutableCollection<in E>,
            buildingContextProvider: (P1) -> BC,
        ): List<E> =
            buildList(init, p1, buildingContextProvider).also { target.addAll(it) }

        fun <BC : BuildingContext<BC, List<E>>, E, U> buildList(init: BC.() -> Unit, buildingContextProvider: () -> BC, transform: E.() -> U): List<U> =
            buildList(init, buildingContextProvider).map(transform)

        fun <BC : BuildingContext<BC, List<E>>, E, U, P1> buildList(
            init: BC.() -> Unit,
            p1: P1,
            buildingContextProvider: (P1) -> BC,
            transform: E.() -> U,
        ): List<U> = buildList(init, p1, buildingContextProvider).map(transform)

        fun <BC : BuildingContext<BC, List<E>>, E, U> buildListTo(
            init: BC.() -> Unit,
            buildingContextProvider: () -> BC,
            target: MutableCollection<in U>,
            transform: E.() -> U,
        ): List<U> = buildList(init, buildingContextProvider, transform).also { target.addAll(it) }

        fun <BC : BuildingContext<BC, List<E>>, E, U, P1> buildListTo(
            init: BC.() -> Unit,
            p1: P1,
            buildingContextProvider: (P1) -> BC,
            target: MutableCollection<in U>,
            transform: E.() -> U,
        ): List<U> = buildList(init, p1, buildingContextProvider, transform).also { target.addAll(it) }


        /**
         * Using `this` [Init] builds a pair of [A] and [B].
         */
        inline fun <A, B> buildPair(noinline init: Init<PairBuilder<A, B>>): Pair<A, B> =
            PairBuilder.buildPair(init)

        /**
         * Using `this` [Init] builds a pair of [A] and [B].
         *
         * As as side effect the result is added to [target].
         */
        inline fun <A, B> buildPairTo(noinline init: Init<PairBuilder<A, B>>, target: MutableCollection<Pair<A, B>>): Pair<A, B> =
            buildPair(init).also { target.add(it) }

        /**
         * Using `this` [Init] builds a pair of [A] and [B]
         * and applies [transform] to the result.
         */
        inline fun <A, B, T> buildPair(noinline init: Init<PairBuilder<A, B>>, transform: Pair<A, B>.() -> T): T =
            buildPair(init).run(transform)

        /**
         * Using `this` [Init] builds a pair of [A] and [B]
         * and applies [transform] to the result.
         *
         * As as side effect the transformed result is added to [target].
         */
        inline fun <A, B, T> buildPairTo(
            noinline init: Init<PairBuilder<A, B>>,
            target: MutableCollection<in T>,
            transform: Pair<A, B>.() -> T,
        ): T =
            buildPair(init, transform).also { target.add(it) }
    }
inline fun <reified C, reified R, reified T, reified U> Builder<C, R, T>.build(
    noinline init: Init<C, R>,
    transform: T.() -> U,
): U = build(init).run(transform)

inline fun <reified C, reified R, reified T> Builder<C, R, T>.buildTo(
    noinline init: Init<C, R>,
    target: MutableCollection<in T>,
): T = build(init).also { target.add(it) }

inline fun <reified C, reified R, reified T, reified U> Builder<C, R, T>.buildTo(
    noinline init: Init<C, R>,
    target: MutableCollection<in U>,
    transform: T.() -> U,
): U = build(init, transform).also { target.add(it) }

inline fun <reified C, reified R, reified T> Builder<C, R, List<T>>.buildMultiple(
    noinline init: Init<C, R>,
): List<T> = build(init)

inline fun <reified C, reified R, reified T> Builder<C, R, List<T>>.buildMultipleTo(
    noinline init: Init<C, R>,
    target: MutableCollection<in T>,
): List<T> = build(init).also { target.addAll(it) }

inline fun <reified C, reified R, reified T, reified U> Builder<C, R, T>.buildMultiple(
    noinline init: Init<C, R>,
    transform: T.() -> List<U>,
): List<U> = build(init).run(transform)

inline fun <reified C, reified R, reified T, reified U> Builder<C, R, T>.buildMultipleTo(
    noinline init: Init<C, R>,
    target: MutableCollection<in U>,
    transform: T.() -> List<U>,
): List<U> = build(init).run(transform).also { target.addAll(it) }

interface FallthroughBuilder<C, R> : Builder<C, R, R> {
    val context: C
    override fun build(init: Init<C, R>): R = context.init()
}

interface ContextualBuilder<C, T> : Builder<C, Unit, T> {
    val context: C
    val value: T
    override fun build(init: Init<C, Unit>): T = context.init().let { value }
}

interface TransformingBuilder<R, T> : Builder<Nothing?, R, T> {
    val transform: (R) -> T
    override fun build(init: Init<Nothing?, R>): T = transform(null.init())
}

interface ProvidingBuilder<T> : Builder<Nothing?, Unit, T> {
    val value: T
    override fun build(init: Init<Nothing?, Unit>): T = value
}

interface NoopBuilder<R> : Builder<Nothing?, R, R> {
    override fun build(init: Init<Nothing?, R>): R = null.init()

    companion object {
        operator fun <R> invoke(init: Init<Nothing?, R>) = (object : NoopBuilder<R> {}).build(init)
    }
}

/**
 * Builder that builds instances of [T] by providing
 * a context [C] that can be operated using an [Init]
 * with an instance of [C] as its receiver.
 *
 * The resulting state [S] is finally used build instances of [T].
 */
interface StatefulContextBuilder<C, S, T> : Builder<C, Unit, T> {
    /**
     * Stores the aggregated [StatefulContext.state] of all operations
     * performed on the immutable [StatefulContext.context].
     */
    val statefulContext: StatefulContext<C, S>

    /**
     * Build step that builds instances of [T]
     * based on the aggregated [StatefulContext.state]
     * that resulted from the operations
     * performed on the immutable [StatefulContext.context].
     */
    val transform: S.() -> T

    /**
     * Builds a new instance of [T] by providing an immutable [StatefulContext.context]
     * that aggregates all operations performed by the specified [init].
     *
     * The resulting [StatefulContext.state] will be used by [transform]
     * to build an actual instance of [T].
     */
    override fun build(init: Init<C, Unit>): T {
        val (_, state: S) = statefulContext.run {
            context.init() to state
        }
        return state.transform()
    }
}
