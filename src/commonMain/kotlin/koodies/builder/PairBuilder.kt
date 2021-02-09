package koodies.builder

/**
 * Builder to build a single pair of two elements.
 *
 * @sample PairBuilderSamples.directUse
 * @sample PairBuilderSamples.indirectUse
 * @sample PairBuilderSamples.transformUse
 */
open class PairBuilder<A, B> : NoopBuilder<Pair<A, B>> {
    companion object {
        fun <A, B> buildPair(init: Init<Nothing?, Pair<A, B>>) = invoke(init)
        operator fun <A, B> invoke(init: Init<Nothing?, Pair<A, B>>) = PairBuilder<A, B>().build(init)
    }
}

fun abc(init: Unit?.() -> String) {}

val x = abc {
    ""
}

@Suppress("UNUSED_VARIABLE", "RemoveRedundantQualifierName")
private object PairBuilderSamples {

    fun directUse() {

        val pair: Pair<String, Int> = PairBuilder<String, Int>().build { "three" to 4 }

    }

    fun indirectUse() {

        fun builderAcceptingFunction(init: Nothing?.() -> Pair<String, Int>) {
            val pair = PairBuilder.buildPair(init)
            println("Et voilà, $pair")
        }

        val pair = builderAcceptingFunction { "three" to 4 }

    }

    fun transformUse() {

        fun builderAcceptingFunction(init: Any?.() -> Pair<String, Int>) {
            val transformed = PairBuilder<String, Int>().build(init) { first.length + second }
            println("Et voilà, $transformed")
        }

        val transformed = builderAcceptingFunction { "three" to 4 }

    }
}
