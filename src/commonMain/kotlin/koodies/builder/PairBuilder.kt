package koodies.builder

/**
 * Builder to build a single pair of two elements.
 *
 * @sample PairBuilderSamples.directUse
 * @sample PairBuilderSamples.indirectUse
 * @sample PairBuilderSamples.transformUse
 */
open class PairBuilder<A, B> : BuildingContextImpl<PairBuilder<A, B>, Pair<A, B>>({ pair ?: error("No pair provided.") }) {
    protected var pair: Pair<A, B>? = null

    /**
     * Builds a pair of [A] and [B].
     */
    infix fun A.to(that: B): Pair<A, B> = (this to that).also { pair = it }

    companion object {
        fun <A, B> buildPair(init: Init<PairBuilder<A, B>>) = Builder.build(init) { PairBuilder() }
    }
}

@Suppress("UNUSED_VARIABLE", "RemoveRedundantQualifierName")
private object PairBuilderSamples {

    fun directUse() {

        val pair: Pair<String, Int> = PairBuilder.buildPair { "three" to 4 }

    }

    fun indirectUse() {

        fun builderAcceptingFunction(init: PairBuilder<String, Int>.() -> Unit) {
            val pair = PairBuilder.buildPair(init)
            println("Et voilà, $pair")
        }

        val pair = builderAcceptingFunction { "three" to 4 }

    }

    fun transformUse() {

        fun builderAcceptingFunction(init: PairBuilder<String, Int>.() -> Unit) {
            val transformed = Builder.buildPair(init) { first.length + second }
            println("Et voilà, $transformed")
        }

        val transformed = builderAcceptingFunction { "three" to 4 }

    }
}
