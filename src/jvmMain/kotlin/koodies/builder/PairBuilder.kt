package koodies.builder

/**
 * Builder to build a single pair of two elements.
 *
 * @sample PairBuilderSamples.directUse
 * @sample PairBuilderSamples.indirectUse
 * @sample PairBuilderSamples.transformUse
 */
object PairBuilder {
    /**
     * Builds a pair of [A] and [B].
     */
    inline infix fun <reified A, reified B> A.and(that: B): Pair<A, B> = this to that
}

/**
 * Using `this` [PairBuilderInit] builds a pair of [A] and [B].
 */
inline fun <reified A, reified B> PairBuilderInit<A, B>.buildPair(): Pair<A, B> =
    PairBuilder.this()

/**
 * Using `this` [PairBuilderInit] builds a pair of [A] and [B].
 *
 * As as side effect the result is added to [target].
 */
inline fun <reified A, reified B> PairBuilderInit<A, B>.buildPairTo(target: MutableCollection<Pair<A, B>>): Pair<A, B> =
    buildPair().also { target.add(it) }

/**
 * Using `this` [PairBuilderInit] builds a pair of [A] and [B]
 * and applies [transform] to the result.
 */
inline fun <reified A, reified B, reified T> PairBuilderInit<A, B>.buildPair(transform: Pair<A, B>.() -> T): T =
    buildPair().run(transform)

/**
 * Using `this` [PairBuilderInit] builds a pair of [A] and [B]
 * and applies [transform] to the result.
 *
 * As as side effect the transformed result is added to [target].
 */
inline fun <reified A, reified B, reified T> PairBuilderInit<A, B>.buildPairTo(target: MutableCollection<in T>, transform: Pair<A, B>.() -> T): T =
    buildPair(transform).also { target.add(it) }

/**
 * Convenience type to easier use [buildMap] accepts.
 */
typealias PairBuilderInit<reified A, reified B> = PairBuilder.() -> Pair<A, B>

@Suppress("UNUSED_VARIABLE", "RemoveRedundantQualifierName")
private object PairBuilderSamples {

    fun directUse() {

        val pair: Pair<String, Int> = with(PairBuilder) { "three" and 4 }

    }

    fun indirectUse() {

        fun builderAcceptingFunction(init: PairBuilderInit<String, Int>) {
            val pair = init.buildPair()
            println("Et voilà, $pair")
        }

        val pair = builderAcceptingFunction { "three" and 4 }

    }

    fun transformUse() {

        fun builderAcceptingFunction(init: PairBuilderInit<String, Int>) {
            val transformed = init.buildPair { first.length + second }
            println("Et voilà, $transformed")
        }

        val transformed = builderAcceptingFunction { "three" and 4 }

    }
}
