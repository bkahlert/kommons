package koodies.builder

import koodies.asString
import koodies.builder.BuilderTemplate.BuildContext
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
 * After a user has called your builder the build needs to assemble an the actual
 * instance of [T]. Since all of the user's interactions are captured and made
 * easily accessible using a [CapturesMap], all you have to do is call
 * `::property.eval()` or one of its variants to compute the corresponding values.
 *
 * ```
 * ```
 *
 * ## Step 1/3: Specifying the Context / Domain Language
 *
 * ```kotlin
 *
 *     val build by anyBuilder() default ...      👉 capturing builder with
 *                                                           optional default result
 *
 *     val array by arrayBuilder()                👉 container builders with
 *          list by listBuilder()                    empty array/list/map as default
 *           map by mapBuilder()
 *
 * val reference by ::anyFunction default ...     👉 capturing function or
 *                                                    callable property with default
 *
 *   val builder by builder<T>()                  👉 capturing f(init: ()->T)
 *      val func by function<T>()                 👉 capturing f(value: T)
 *      var prop by setter<T>()                   👉 capturing prop: T = ...
 *
 * ```
 *
 *
 * ## Step 2/3: Triggering Build
 *
 * ```kotlin
 * myBuild {
 *
 *     build {
 *          depends = on { the(builder) }
 *     }
 *
 *     list {
 *          +"abc"
 *          add("123")
 *          addAll(iterable)
 *     }
 *
 *     reference(p1, p2) { … }
 *
 *     builder { 42 }
 *     func(42)
 *     prop = 42
 *
 * }
 *
 * ```
 *
 *
 * ## Step 3/3: Building Domain Object
 *
 * The domain object is created using the captured values from the previous
 * build invocation. These have been stored with their respective property linked,
 * in an instance of [CapturesMap] which makes evaluating the captures as snap.
 *
 * If some of the builder functions were not called by the user the optionally
 * specified default is used. Otherwise [BuildContext.evalOrNull] and
 * [BuildContext.evalOrDefault] can be used to handle those cases in this last
 * step. [BuildContext.eval] throws an exception if no default was specified
 * in step 1, no invocation was recorded and a non-nullable type is needed.
 * [BuildContext.evalAll] not only returns the most recent but all invocations.
 *
 *
 * ```kotlin
 * override fun BuildContext.build() = withContext(::MyContext) {
 *
 *     MyDomainObject(
 *
 *         ::build.eval(),
 *
 *         ::list.eval(),
 *
 *         ::reference.evalOrNull(),
 *
 *         ::builder.evalOrDefault { … },
 *
 *         ::function.eval(),
 *
 *         ::prop.eval(),
 *
 *     )
 * }
 * ```
 *
 * @sample CarDSL
 */
abstract class BuilderTemplate<C, T> : Builder<Init<C>, T> {

    override fun invoke(init: Init<C>): T = CapturesMap().let {
        runCatching {
            BuildContext(it, init).build()
        }.getOrElse {
            throw IllegalStateException("An error occurred while building: $this", it)
        }
    }

    /**
     * Builds an instance of type [T].
     */
    protected abstract fun BuildContext.build(): T

    /**
     * Context that serves as the receiver of the final build step.
     */
    inner class BuildContext(val captures: CapturesMap, val init: Init<C>) {

        /**
         * Creates a new [C] instance using the given producer,
         * applies the build argument to it—capturing all invocations—
         * and calls the given [build] to assemble an instance of type [T].
         *
         * [build] is called on the initialized context and has also access
         * to [KProperty] extension functions [evalOrNull], [evalOrDefault]
         * and [eval] to conveniently evaluate the most recent invocation
         * of the corresponding property.
         */
        operator fun ((CapturesMap) -> C).invoke(build: C.() -> T): T = this(captures).run {
            init(this)
            build()
        }

        /**
         * Evaluates all captures for all properties and returns them.
         *
         * If no capture are found, the returned list is empty.
         * An eventually defined default is ignored.
         */
        inline fun <reified T> evalAll(): List<T> = captures.evalAll()

        /**
         * Evaluates all captures for `this` property and returns them.
         *
         * If no capture are found, the returned list is empty.
         * An eventually defined default is ignored.
         */
        inline fun <reified T> KProperty<*>.evalAll(): List<T> = with(captures) { this@evalAll.evalAll() }

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
    }

    override fun toString(): String = asString()
}

