package koodies.builder

/**
 * `True`/`False` style builder for [Boolean].
 *
 * @sample TrueFalseBuilderSamples.directUse
 * @sample TrueFalseBuilderSamples.indirectUse
 */
object TrueFalseBuilder {
    fun build(init: TrueFalseBuilderInit): Boolean =
        TrueFalseBuilder.init()

    val True get() = true
    val False get() = false
}

/**
 * Type [TrueFalseBuilder.build] accepts.
 */
typealias TrueFalseBuilderInit = TrueFalseBuilder.() -> Boolean

@Suppress("UNUSED_VARIABLE")
private object TrueFalseBuilderSamples {

    fun directUse() {

        val toggle = TrueFalseBuilder.build { False }

    }

    fun indirectUse() {

        fun builderAcceptingFunction(init: TrueFalseBuilderInit) {
            val toggle = TrueFalseBuilder.build(init)
            println("Et voilà, $toggle")
        }

        val toggle = builderAcceptingFunction { True }
    }
}
