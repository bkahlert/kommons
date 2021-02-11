package koodies.builder

import koodies.asString
import koodies.builder.context.CapturesMap
import koodies.builder.context.CapturingContext
import kotlin.reflect.KProperty

/**
 * # Builder Template
 *
 * Template to specify complex builders that can create instances of type [T]
 * by providing a context [C] which serve as the receiver for the build argument.
 *
 * Context [C]—in contrast to other builders—is not predefined but built
 * using a **[CapturingContext] which lets you re-use virtually everything.**
 *
 * After the user called your builder the build needs to assemble an the actual
 * instance of [T]. Since all of the user's interactions are captured and made
 * easily accessible using a [CapturesMap], all you have to do is call
 * [KProperty.build] or one of its variants to compute the corresponding values.
 *
 * ```
 * ```
 *
 * ## Step 1/3: Specifying the Context / Domain Language
 *
 * ### CAPTURING BUILDERS { … }
 * ```kotlin
 *
 *     val customBuild by CustomBuilder()          👉 capturing builder
 *                                                                    (shorthand)
 *
 *     val build by builder(ListBuilder<String>()) 👉 capturing builder
 *                                                               (regular syntax)
 *
 *     val provide by builder<Int>()               👉 capturing builder
 *                                                                (takes () -> R)
 *
 * ```
 *
 * ### CAPTURING FUNCTIONS(…)
 * ```kotlin
 *
 *     val delegate by capture(::anyFunction)      👉 capturing function or
 *                                                    callable property reference
 *
 *     val function by capture<Double>()           👉 capturing function (takes R)
 *
 * ```
 *
 * ### CAPTURING PROPERTY=…
 * ```kotlin
 *
 *     val prop by setter<String>()                👉 capturing property
 *                                                       (can be assigned with R)
 *
 * ```
 *
 *
 * ## Step 2/3: Calling Your Builder
 *
 * ```kotlin
 * myBuild {
 *
 *     customBuild {
 *          depends = on { the(builder) }
 *     }
 *
 *     build {
 *          +"abc"
 *          add("123")
 *          addAll(iterable)
 *     }
 *
 *     provide { 42 }
 *
 *     delegate(p1, p2) { … }
 *
 *     function(kotlin.math.PI)
 *
 *     prop = "a string"
 * }
 *
 * ```
 *
 *
 * ## Step 3/3: Building Your Domain Object
 *
 * Time to assemble your object using the captures values. Those have been
 * stored and linked to their respective property in an instance of [CapturesMap]
 * which makes evaluating the captures as snap.
 *
 *
 * ```kotlin
 * override fun BuildContext.build() = withContext(::MyContext) {
 *
 *     MyDomainObject(
 *
 *         ::customBuild.buildOrThrow(),
 *
 *         ::build.buildOrDefault { … },
 *
 *         ::provide.buildOrDefault(0),
 *
 *         ::delegate.buildOrNull(),
 *
 *         ::function.buildOrNull(),
 *
 *         ::prop.buildOrNull()?:"",
 *
 *     )
 * }
 * ```
 */
abstract class BuilderTemplate<C, T> : Builder<Init<C>, T> {

    override fun invoke(init: Init<C>): T = CapturesMap().run {
        runCatching {
            BuildContext(this, init).build()
        }.getOrElse {
            throw IllegalStateException("An error occurred while building: $this", it)
        }
    }

    /**
     * Builds an instance of type [T].
     *
     * On invocation needs to pass a context to [BuildContext.withContext].
     */
    protected abstract fun BuildContext.build(): T

    /**
     * Context that serves as the receiver of the final build step.
     *
     * Expects [withContext] to be called with an instance of context [C]
     * and a lambda performing the instantiation of [T].
     */
    inner class BuildContext(val captures: CapturesMap, val init: Init<C>) {

        /**
         * Creates a new [context] instance using the given producer,
         * applies the build argument to it—capturing all invocations—
         * and calls the given [build] to assemble an instance of type [T].
         *
         * [build] is called on the initialized context and has also access
         * to [KProperty] extension functions [evalOrNull], [evalOrDefault]
         * and [eval] to conveniently evaluate the most recent invocation
         * of the corresponding property.
         */
        fun withContext(context: (CapturesMap) -> C, build: C.() -> T): T = context(captures).run {
            init(this)
            build()
        }

        /**
         * Checks if a capture can be found for `this` property and if so,
         * evaluates and returns it. Otherwise returns `null`.
         */
        inline fun <reified T> KProperty<*>.evalOrNull(): T? = with(captures) { evalOrNull() }

        /**
         * Checks if a capture can be found for `this` property and if so,
         * evaluates and returns it. Otherwise returns the given [default].
         */
        inline fun <reified T> KProperty<*>.evalOrDefault(default: T): T = with(captures) { evalOrDefault(default) }

        /**
         * Checks if a capture can be found for `this` property and if so,
         * applies the given [transform] to its evaluation and returns it.
         * Otherwise [transform] applied to the given [default] is returned.
         */
        inline fun <reified T, reified U> KProperty<*>.evalOrDefault(default: T, transform: T.() -> U): U =
            with(captures) { evalOrDefault(default, transform) }

        /**
         * Checks if a capture can be found for `this` property and if so,
         * evaluates and returns it. Otherwise returns the result of the
         * given [default].
         */
        inline fun <reified T> KProperty<*>.evalOrDefault(noinline default: () -> T): T = with(captures) { evalOrDefault(default) }

        /**
         * Checks if a capture can be found for `this` property and if so,
         * applies the given [transform] to its evaluation and returns it.
         * Otherwise [transform] applied to the result of the given [default] is returned.
         */
        inline fun <reified T, reified U> KProperty<*>.evalOrDefault(noinline default: () -> T, transform: T.() -> U): U = evalOrDefault(default).transform()

        /**
         * Checks if a capture can be found for `this` property and if so,
         * evaluates and returns it. Otherwise throws a [NoSuchElementException].
         */
        inline fun <reified T> KProperty<*>.eval(): T = with(captures) { eval() }

        inline infix fun <reified T> KProperty<*>.or(default: T) = evalOrDefault(default) // TODO
    }

    override fun toString(): String = asString()
}
