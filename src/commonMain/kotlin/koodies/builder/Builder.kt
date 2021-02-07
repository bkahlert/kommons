package koodies.builder


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
typealias Init<C> = C.() -> Unit

/**
 * A builder is an object that builds instances of type [T]
 * with the help of a context [C] which provides functionality
 * specific to the build process.
 *
 * By providing a domain specific context this pattern can be used
 * to implement embedded domain specific languages.
 *
 * This design differs form the usual builder pattern in that
 * it does not pollute the context with technical methods,
 * in particular a `build()`.
 *
 * To still provide a type-safe way to build objects while
 * preserving extensibility, the build functionality is "hidden"
 * inside [invoke]. This way all implementors can be used in
 * a unified fashion, see [Builder.build].
 */
interface Builder<C, out T> {
    /**
     * Builds an instance of [T] using the specified
     * building [context] and [init].
     */
    operator fun invoke(context: C): T

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
}

/**
 * Base implementation that delegates the build process
 * to the specified [transform].
 */
abstract class BuilderImpl<C, T>(protected val transform: C.() -> T) : Builder<C, T> {
    override operator fun invoke(context: C): T = transform(context)
}

/**
 * A builder that is its own context, that is,
 * - the same object that [Init] is applied to, is
 * - the same object that also instantiates [T].
 *
 * Consequently instances of [T] can be built with a simplified API
 * as separate builder is needed, see [Builder.build].
 */
interface BuildingContext<BC : BuildingContext<BC, T>, T> : Builder<BC, T>

/**
 * Base implementation that delegates the build process
 * to the specified [transform].
 */
abstract class BuildingContextImpl<BC : BuildingContext<BC, T>, T>(transform: BC.() -> T) : BuilderImpl<BC, T>(transform), BuildingContext<BC, T>
