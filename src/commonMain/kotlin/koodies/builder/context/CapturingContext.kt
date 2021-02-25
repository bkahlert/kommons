package koodies.builder.context

import koodies.CallableProperty
import koodies.Deferred
import koodies.Exceptions
import koodies.asString
import koodies.builder.ArrayBuilder
import koodies.builder.Builder
import koodies.builder.BuilderTemplate
import koodies.builder.Init
import koodies.builder.ListBuilder
import koodies.builder.MapBuilder
import koodies.builder.SkippableBuilder
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

/**
 * # Capturing Context
 *
 * A context that captures the arguments, its delegated callables are called with,
 * and makes them conveniently accessible via [captures].
 *
 * ```
 * ```
 *
 * ## Delegation Types
 *
 * ```kotlin
 * CustomContext : CapturingContext {
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
 * }
 *
 * ```
 *
 * ## Caller Perspective
 *
 * The above example will look as follows from a callers perspective:
 *
 * ```kotlin
 * CustomContext().apply {
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
 * ```
 *
 * @see BuilderTemplate
 */
abstract class CapturingContext {

    /**
     * Contains the mapping between property and its captured result.
     */
    protected abstract val captures: CapturesMap

    // @formatter:off
    /** Creates a builder that captures all invocations to `this` builder. */
    open operator fun <T : Function<*>, R> Builder<T, R>.provideDelegate(thisRef: Any?, property: KProperty<*>): CapturingCallable<SkippableCapturingBuilderInterface<T, R?>, R?> = builder(null, this)
    /** Returns a callable that captures all invocations to `this` builder. If no invocations takes place, the evaluation later returns the given [defaultResult]. */
    infix fun <T : Function<*>, R> Builder<T, R>.default(defaultResult: R): CapturingCallable<SkippableCapturingBuilderInterface<T, R>, R> = builder(defaultResult, this)

    /** Returns a callable that captures all invocations to `this` reference. */
    open operator fun <T : (P1, P2, P3, P4, P5) -> R, P1, P2, P3, P4, P5, R> T.provideDelegate(thisRef: Any?, property: KProperty<*>): CapturingCallable<(P1, P2, P3, P4, P5) -> Unit, R?> = function(null, this)
    /** Returns a callable that captures all invocations to `this` reference. If no invocations takes place, the evaluation later returns the given [defaultResult]. */
    infix fun <T : (P1, P2, P3, P4, P5) -> R, P1, P2, P3, P4, P5, R> T.default(defaultResult: R): CapturingCallable<(P1, P2, P3, P4, P5) -> Unit, R> = function(defaultResult, this)
    /** Returns a callable that captures all invocations to `this` reference. */
    open operator fun <T : (P1, P2, P3, P4) -> R, P1, P2, P3, P4, R> T.provideDelegate(thisRef: Any?, property: KProperty<*>): CapturingCallable<(P1, P2, P3, P4) -> Unit, R?> = function(null, this)
    /** Returns a callable that captures all invocations to `this` reference. If no invocations takes place, the evaluation later returns the given [defaultResult]. */
    infix fun <T : (P1, P2, P3, P4) -> R, P1, P2, P3, P4, R> T.default(defaultResult: R): CapturingCallable<(P1, P2, P3, P4) -> Unit, R> = function(defaultResult, this)
    /** Returns a callable that captures all invocations to `this` reference. */
    open operator fun <T : (P1, P2, P3) -> R, P1, P2, P3, R> T.provideDelegate(thisRef: Any?, property: KProperty<*>): CapturingCallable<(P1, P2, P3) -> Unit, R?> = function(null, this)
    /** Returns a callable that captures all invocations to `this` reference. If no invocations takes place, the evaluation later returns the given [defaultResult]. */
    infix fun <T : (P1, P2, P3) -> R, P1, P2, P3, R> T.default(defaultResult: R): CapturingCallable<(P1, P2, P3) -> Unit, R> = function(defaultResult, this)
    /** Returns a callable that captures all invocations to `this` reference. */
    open operator fun <T : (P1, P2) -> R, P1, P2, R> T.provideDelegate(thisRef: Any?, property: KProperty<*>): CapturingCallable<(P1, P2) -> Unit, R?> = function(null, this)
    /** Returns a callable that captures all invocations to `this` reference. If no invocations takes place, the evaluation later returns the given [defaultResult]. */
    infix fun <T : (P1, P2) -> R, P1, P2, R> T.default(defaultResult: R): CapturingCallable<(P1, P2) -> Unit, R> = function(defaultResult, this)
    /** Returns a callable that captures all invocations to `this` reference. */
    open operator fun <T : (P1) -> R, P1, R> T.provideDelegate(thisRef: Any?, property: KProperty<*>): CapturingCallable<(P1) -> Unit, R?> = function(null, this)
    /** Returns a callable that captures all invocations to `this` reference. If no invocations takes place, the evaluation later returns the given [defaultResult]. */
    infix fun <T : (P1) -> R, P1, R> T.default(defaultResult: R): CapturingCallable<(P1) -> Unit, R> = function(defaultResult, this)
    /** Returns a callable that captures all invocations to `this` reference. */
    open operator fun <T : () -> R, R> T.provideDelegate(thisRef: Any?, property: KProperty<*>): CapturingCallable<() -> Unit, R?> = function(null, this)
    /** Returns a callable that captures all invocations to `this` reference. If no invocations takes place, the evaluation later returns the given [defaultResult]. */
    infix fun <T : () -> R, R> T.default(defaultResult: R): CapturingCallable<() -> Unit, R> = function(defaultResult, this)
    // @formatter:on

    /**
     * Returns an [ArrayBuilder] that captures the building of an [E]
     * typed [Array].
     *
     * **Example** *of what the user sees*
     *
     * ```kotlin
     *     build {
     *         capturingArrayBuilder {
     *              +"abc"
     *              add("123")
     *              addAll(iterable)
     *         }
     *
     *         👉 captures: ArrayBuilder{ +"abc"; add("123"); addAll(iterable) }
     *     }
     * ```
     *
     * @see builder
     */
    inline fun <reified E> arrayBuilder(): CapturingCallable<SkippableCapturingBuilderInterface<Init<ListBuildingContext<E>>, Array<E>>, Array<E>> =
        builder(emptyArray(), ArrayBuilder.createInstance { toTypedArray() })

    /**
     * Returns a [ListBuilder] that captures the building of a [List]
     * with elements of type [E].
     *
     * **Example** *of what the user sees*
     *
     * ```kotlin
     *     build {
     *         capturingListBuilder {
     *              +"abc"
     *              add("123")
     *              addAll(iterable)
     *         }
     *
     *         👉 captures: ListBuilder{ +"abc"; add("123"); addAll(iterable) }
     *     }
     * ```
     *
     * @see builder
     */
    fun <E> listBuilder(): CapturingCallable<SkippableCapturingBuilderInterface<Init<ListBuildingContext<E>>, List<E>>, List<E>> =
        builder(emptyList(), ListBuilder())

    /**
     * Returns a [MapBuilder] that captures the building of a [Map]
     * with keys of type [K] and values of type [V].
     *
     * **Example** *of what the user sees*
     *
     * ```kotlin
     *     build {
     *         capturingMapBuilder {
     *              put(true, "✅")
     *              put(false, "❌")
     *              put(null, "␀")
     *         }
     *
     *         👉 captures: MapBuilder{ put(true, "✅"); put(false, "❌"); put(null, "␀") }
     *     }
     * ```
     *
     * @see builder
     */
    fun <K, V> mapBuilder(): CapturingCallable<SkippableCapturingBuilderInterface<Init<MapBuildingContext<K, V>>, Map<K, V>>, Map<K, V>> =
        builder(emptyMap(), MapBuilder())

    /**
     * Returns a callable that has the same argument list
     * as the given [builder] with the difference that it captures
     * the build invocation instead of running it and returning the result.
     *
     * Uses [initialValue] as the captured value if no invocations are recorded.
     *
     * **Example** *of what the user sees*
     *
     * ```kotlin
     *     build {
     *         capturingBuilder {
     *             depends = on { the(builder) }
     *         }
     *
     *         👉 captures: $builder{ depends = on { the(builder) }}
     *     }
     * ```
     *
     * **Builders can also be re-used by just providing an instance like
     * this:**
     * ```kotlin
     *     val capturingBuilder by ListBuilder<String>()
     *
     * ```
     *
     *
     * **Manual Implementation**
     *
     * The following code snippet roughly shows how this feature is implemented:
     * ```
     * class Builder {
     *     protected val captured = mutableMapOf<KProperty<*>, Captured<*>>()
     *     fun build() = … // build using captured values
     *
     *     private $builder = ActualBuilder()
     *     val <T:Function<*>,R> capturingBuilder: (T)->Unit = { init ->
     *         captured[::capturingBuilder] = capture { $builder(init) }
     *     }
     * }
     */
    fun <T : Function<*>, R> builder(initialValue: R, builder: Builder<T, R>): CapturingCallable<SkippableCapturingBuilderInterface<T, R>, R> =
        CapturingCallable(initialValue, captures) { callback ->
            SkippableCapturingBuilderInterface(builder) { callback(it) }
        }

    /**
     * Returns a callable that has the same argument list
     * as the given [builder] with the difference that it captures
     * the build invocation instead of running it and returning the result.
     *
     * **Example** *of what the user sees*
     *
     * ```kotlin
     *     build {
     *         capturingBuilder {
     *             depends = on { the(builder) }
     *         }
     *
     *         👉 captures: $builder{ depends = on { the(builder) }}
     *     }
     * ```
     *
     * **Builders can also be re-used by just providing an instance like
     * this:**
     * ```kotlin
     *     val capturingBuilder by ListBuilder<String>()
     *
     * ```
     *
     *
     * **Manual Implementation**
     *
     * The following code snippet roughly shows how this feature is implemented:
     * ```
     * class Builder {
     *     protected val captured = mutableMapOf<KProperty<*>, Captured<*>>()
     *     fun build() = … // build using captured values
     *
     *     private $builder = ActualBuilder()
     *     val <T:Function<*>,R> capturingBuilder: (T)->Unit = { init ->
     *         captured[::capturingBuilder] = capture { $builder(init) }
     *     }
     * }
     */
    fun <T : Function<*>, R> builder(builder: Builder<T, R>): CapturingCallable<SkippableCapturingBuilderInterface<T, R?>, R?> = builder(null, builder)


    /**
     * Returns a callable that captures a simple lambda of
     * the form `() -> R`.
     *
     * Uses [initialValue] as the captured value if no invocations are recorded.
     *
     * **Example** *of what the user sees*
     *
     * ```kotlin
     *     build {
     *         capturingBuilder { … } 👉 captures: { … }
     *     }
     * ```
     *
     * **Manual Implementation**
     *
     * The following code snippet roughly shows how this feature is implemented:
     * ```
     * class Builder {
     *     protected val captured = mutableMapOf<KProperty<*>, Captured<*>>()
     *     fun build() = … // build using captured values
     *
     *     val <R> capturingBuilder: (R)->Unit = { lambda ->
     *         captured[::capturingBuilder] = capture { lambda() }
     *     }
     * }
     */
    fun <R> builder(initialValue: R): CapturingCallable<SkippableCapturingBuilderInterface<() -> R, R>, R> = builder(initialValue) { it() }

    /**
     * Returns a callable that captures a simple lambda of
     * the form `() -> R`.
     *
     * **Example** *of what the user sees*
     *
     * ```kotlin
     *     build {
     *         capturingBuilder { … } 👉 captures: { … }
     *     }
     * ```
     *
     * **Manual Implementation**
     *
     * The following code snippet roughly shows how this feature is implemented:
     * ```
     * class Builder {
     *     protected val captured = mutableMapOf<KProperty<*>, Captured<*>>()
     *     fun build() = … // build using captured values
     *
     *     val <R> capturingBuilder: (R)->Unit = { lambda ->
     *         captured[::capturingBuilder] = capture { lambda() }
     *     }
     * }
     */
    fun <R> builder(): CapturingCallable<SkippableCapturingBuilderInterface<() -> R?, R?>, R?> = builder(null) { it() }

    /**
     * Returns a callable that has the same argument list
     * as the given [callable] (*function references and references to properties
     * implementing [invoke] can be used since **both are function types***)
     * with the difference that it captures the invocation and its result
     * instead of returning it.
     *
     * Uses [initialValue] as the captured value if no invocations are recorded.
     *
     * **Example** *of what the user sees*
     *
     * ```kotlin
     *     build {
     *         capturingReference(p1, p2, p3, p4, p5) 👉 captures: $reference(p1, p2, p3, p4, p5)
     *     }
     * ```
     *
     *
     * **Manual Implementation**
     *
     * The following code snippet roughly shows how this feature is implemented:
     * ```
     * class Builder {
     *     protected val captured = mutableMapOf<KProperty<*>, Captured<*>>()
     *     fun build() = … // build using captured values
     *
     *     private $reference:(…) -> R = delegateeReceiver::delegateeFunction
     *     val <T:Function<*>,R> capturingReference: (…)->Unit = { arguments ->
     *         captured[::capturingReference] = capture { $reference(arguments) }
     *     }
     * }
     */
    fun <T : (P1, P2, P3, P4, P5) -> R, P1, P2, P3, P4, P5, R> function(initialValue: R, callable: T): CapturingCallable<(P1, P2, P3, P4, P5) -> Unit, R> =
        CapturingCallable(initialValue, captures) { callback ->
            { p1, p2, p3, p4, p5 -> callback(Deferred { callable(p1, p2, p3, p4, p5) }) }
        }

    /**
     * Returns a callable that has the same argument list
     * as the given [callable] (*function references and references to properties
     * implementing [invoke] can be used since **both are function types***)
     * with the difference that it captures the invocation and its result
     * instead of returning it.
     *
     * **Example** *of what the user sees*
     *
     * ```kotlin
     *     build {
     *         capturingReference(p1, p2, p3, p4, p5) 👉 captures: $reference(p1, p2, p3, p4, p5)
     *     }
     * ```
     *
     *
     * **Manual Implementation**
     *
     * The following code snippet roughly shows how this feature is implemented:
     * ```
     * class Builder {
     *     protected val captured = mutableMapOf<KProperty<*>, Captured<*>>()
     *     fun build() = … // build using captured values
     *
     *     private $reference:(…) -> R = delegateeReceiver::delegateeFunction
     *     val <T:Function<*>,R> capturingReference: (…)->Unit = { arguments ->
     *         captured[::capturingReference] = capture { $reference(arguments) }
     *     }
     * }
     */
    fun <T : (P1, P2, P3, P4, P5) -> R, P1, P2, P3, P4, P5, R> function(callable: T): CapturingCallable<(P1, P2, P3, P4, P5) -> Unit, R?> =
        function(null, callable)


    /**
     * Returns a callable that has the same argument list
     * as the given [callable] (*function references and references to properties
     * implementing [invoke] can be used since **both are function types***)
     * with the difference that it captures the invocation and its result
     * instead of returning it.
     *
     * Uses [initialValue] as the captured value if no invocations are recorded.
     *
     * **Example** *of what the user sees*
     *
     * ```kotlin
     *     build {
     *         capturingReference(p1, p2, p3, p4) 👉 captures: $reference(p1, p2, p3, p4)
     *     }
     * ```
     *
     *
     * **Manual Implementation**
     *
     * The following code snippet roughly shows how this feature is implemented:
     * ```
     * class Builder {
     *     protected val captured = mutableMapOf<KProperty<*>, Captured<*>>()
     *     fun build() = … // build using captured values
     *
     *     private $reference:(…) -> R = delegateeReceiver::delegateeFunction
     *     val <T:Function<*>,R> capturingReference: (…)->Unit = { arguments ->
     *         captured[::capturingReference] = capture { $reference(arguments) }
     *     }
     * }
     */
    fun <T : (P1, P2, P3, P4) -> R, P1, P2, P3, P4, R> function(initialValue: R, callable: T): CapturingCallable<(P1, P2, P3, P4) -> Unit, R> =
        CapturingCallable(initialValue, captures) { callback ->
            { p1, p2, p3, p4 -> callback(Deferred { callable(p1, p2, p3, p4) }) }
        }


    /**
     * Returns a callable that has the same argument list
     * as the given [callable] (*function references and references to properties
     * implementing [invoke] can be used since **both are function types***)
     * with the difference that it captures the invocation and its result
     * instead of returning it.
     *
     * **Example** *of what the user sees*
     *
     * ```kotlin
     *     build {
     *         capturingReference(p1, p2, p3, p4) 👉 captures: $reference(p1, p2, p3, p4)
     *     }
     * ```
     *
     *
     * **Manual Implementation**
     *
     * The following code snippet roughly shows how this feature is implemented:
     * ```
     * class Builder {
     *     protected val captured = mutableMapOf<KProperty<*>, Captured<*>>()
     *     fun build() = … // build using captured values
     *
     *     private $reference:(…) -> R = delegateeReceiver::delegateeFunction
     *     val <T:Function<*>,R> capturingReference: (…)->Unit = { arguments ->
     *         captured[::capturingReference] = capture { $reference(arguments) }
     *     }
     * }
     */
    fun <T : (P1, P2, P3, P4) -> R, P1, P2, P3, P4, R> function(callable: T): CapturingCallable<(P1, P2, P3, P4) -> Unit, R?> = function(null, callable)


    /**
     * Returns a callable that has the same argument list
     * as the given [callable] (*function references and references to properties
     * implementing [invoke] can be used since **both are function types***)
     * with the difference that it captures the invocation and its result
     * instead of returning it.
     *
     * Uses [initialValue] as the captured value if no invocations are recorded.
     *
     * **Example** *of what the user sees*
     *
     * ```kotlin
     *     build {
     *         capturingReference(p1, p2, p3) 👉 captures: $reference(p1, p2, p3)
     *     }
     * ```
     *
     *
     * **Manual Implementation**
     *
     * The following code snippet roughly shows how this feature is implemented:
     * ```
     * class Builder {
     *     protected val captured = mutableMapOf<KProperty<*>, Captured<*>>()
     *     fun build() = … // build using captured values
     *
     *     private $reference:(…) -> R = delegateeReceiver::delegateeFunction
     *     val <T:Function<*>,R> capturingReference: (…)->Unit = { arguments ->
     *         captured[::capturingReference] = capture { $reference(arguments) }
     *     }
     * }
     */
    fun <T : (P1, P2, P3) -> R, P1, P2, P3, R> function(initialValue: R, callable: T): CapturingCallable<(P1, P2, P3) -> Unit, R> =
        CapturingCallable(initialValue, captures) { callback ->
            { p1, p2, p3 -> callback(Deferred { callable(p1, p2, p3) }) }
        }

    /**
     * Returns a callable that has the same argument list
     * as the given [callable] (*function references and references to properties
     * implementing [invoke] can be used since **both are function types***)
     * with the difference that it captures the invocation and its result
     * instead of returning it.
     *
     * **Example** *of what the user sees*
     *
     * ```kotlin
     *     build {
     *         capturingReference(p1, p2, p3) 👉 captures: $reference(p1, p2, p3)
     *     }
     * ```
     *
     *
     * **Manual Implementation**
     *
     * The following code snippet roughly shows how this feature is implemented:
     * ```
     * class Builder {
     *     protected val captured = mutableMapOf<KProperty<*>, Captured<*>>()
     *     fun build() = … // build using captured values
     *
     *     private $reference:(…) -> R = delegateeReceiver::delegateeFunction
     *     val <T:Function<*>,R> capturingReference: (…)->Unit = { arguments ->
     *         captured[::capturingReference] = capture { $reference(arguments) }
     *     }
     * }
     */
    fun <T : (P1, P2, P3) -> R, P1, P2, P3, R> function(callable: T): CapturingCallable<(P1, P2, P3) -> Unit, R?> = function(null, callable)

    /**
     * Returns a callable that has the same argument list
     * as the given [callable] (*function references and references to properties
     * implementing [invoke] can be used since **both are function types***)
     * with the difference that it captures the invocation and its result
     * instead of returning it.
     *
     * Uses [initialValue] as the captured value if no invocations are recorded.
     *
     * **Example** *of what the user sees*
     *
     * ```kotlin
     *     build {
     *         capturingReference(p1, p2) 👉 captures: $reference(p1, p2)
     *     }
     * ```
     *
     *
     * **Manual Implementation**
     *
     * The following code snippet roughly shows how this feature is implemented:
     * ```
     * class Builder {
     *     protected val captured = mutableMapOf<KProperty<*>, Captured<*>>()
     *     fun build() = … // build using captured values
     *
     *     private $reference:(…) -> R = delegateeReceiver::delegateeFunction
     *     val <T:Function<*>,R> capturingReference: (…)->Unit = { arguments ->
     *         captured[::capturingReference] = capture { $reference(arguments) }
     *     }
     * }
     */
    fun <T : (P1, P2) -> R, P1, P2, R> function(initialValue: R, callable: T): CapturingCallable<(P1, P2) -> Unit, R> =
        CapturingCallable(initialValue, captures) { callback ->
            { p1, p2 -> callback(Deferred { callable(p1, p2) }) }
        }

    /**
     * Returns a callable that has the same argument list
     * as the given [callable] (*function references and references to properties
     * implementing [invoke] can be used since **both are function types***)
     * with the difference that it captures the invocation and its result
     * instead of returning it.
     *
     * **Example** *of what the user sees*
     *
     * ```kotlin
     *     build {
     *         capturingReference(p1, p2) 👉 captures: $reference(p1, p2)
     *     }
     * ```
     *
     *
     * **Manual Implementation**
     *
     * The following code snippet roughly shows how this feature is implemented:
     * ```
     * class Builder {
     *     protected val captured = mutableMapOf<KProperty<*>, Captured<*>>()
     *     fun build() = … // build using captured values
     *
     *     private $reference:(…) -> R = delegateeReceiver::delegateeFunction
     *     val <T:Function<*>,R> capturingReference: (…)->Unit = { arguments ->
     *         captured[::capturingReference] = capture { $reference(arguments) }
     *     }
     * }
     */
    fun <T : (P1, P2) -> R, P1, P2, R> function(callable: T): CapturingCallable<(P1, P2) -> Unit, R?> = function(null, callable)

    /**
     * Returns a callable that has the same argument list
     * as the given [callable] (*function references and references to properties
     * implementing [invoke] can be used since **both are function types***)
     * with the difference that it captures the invocation and its result
     * instead of returning it.
     *
     * Uses [initialValue] as the captured value if no invocations are recorded.
     *
     * **Example** *of what the user sees*
     *
     * ```kotlin
     *     build {
     *         capturingReference(p1) 👉 captures: $reference(p1)
     *     }
     * ```
     *
     *
     * **Manual Implementation**
     *
     * The following code snippet roughly shows how this feature is implemented:
     * ```
     * class Builder {
     *     protected val captured = mutableMapOf<KProperty<*>, Captured<*>>()
     *     fun build() = … // build using captured values
     *
     *     private $reference:(…) -> R = delegateeReceiver::delegateeFunction
     *     val <T:Function<*>,R> capturingReference: (…)->Unit = { arguments ->
     *         captured[::capturingReference] = capture { $reference(arguments) }
     *     }
     * }
     */
    fun <T : (P1) -> R, P1, R> function(initialValue: R, callable: T): CapturingCallable<(P1) -> Unit, R> =
        CapturingCallable(initialValue, captures) { callback ->
            { p1 -> callback(Deferred { callable(p1) }) }
        }

    /**
     * Returns a callable that has the same argument list
     * as the given [callable] (*function references and references to properties
     * implementing [invoke] can be used since **both are function types***)
     * with the difference that it captures the invocation and its result
     * instead of returning it.
     *
     * **Example** *of what the user sees*
     *
     * ```kotlin
     *     build {
     *         capturingReference(p1) 👉 captures: $reference(p1)
     *     }
     * ```
     *
     *
     * **Manual Implementation**
     *
     * The following code snippet roughly shows how this feature is implemented:
     * ```
     * class Builder {
     *     protected val captured = mutableMapOf<KProperty<*>, Captured<*>>()
     *     fun build() = … // build using captured values
     *
     *     private $reference:(…) -> R = delegateeReceiver::delegateeFunction
     *     val <T:Function<*>,R> capturingReference: (…)->Unit = { arguments ->
     *         captured[::capturingReference] = capture { $reference(arguments) }
     *     }
     * }
     */
    fun <T : (P1) -> R, P1, R> function(callable: T): CapturingCallable<(P1) -> Unit, R?> = function(null, callable)

    /**
     * Returns a callable that has the same argument list
     * as the given [callable] (*function references and references to properties
     * implementing [invoke] can be used since **both are function types***)
     * with the difference that it captures the invocation and its result
     * instead of returning it.
     *
     * Uses [initialValue] as the captured value if no invocations are recorded.
     *
     * **Example** *of what the user sees*
     *
     * ```kotlin
     *     build {
     *         capturingReference() 👉 captures: $reference()
     *     }
     * ```
     *
     *
     * **Manual Implementation**
     *
     * The following code snippet roughly shows how this feature is implemented:
     * ```
     * class Builder {
     *     protected val captured = mutableMapOf<KProperty<*>, Captured<*>>()
     *     fun build() = … // build using captured values
     *
     *     private $reference:(…) -> R = delegateeReceiver::delegateeFunction
     *     val <T:Function<*>,R> capturingReference: (…)->Unit = { arguments ->
     *         captured[::capturingReference] = capture { $reference(arguments) }
     *     }
     * }
     */
    fun <T : () -> R, R> function(initialValue: R, callable: T): CapturingCallable<() -> Unit, R> =
        CapturingCallable(initialValue, captures) { callback ->
            { callback(Deferred { callable() }) }
        }

    /**
     * Returns a callable that has the same argument list
     * as the given [callable] (*function references and references to properties
     * implementing [invoke] can be used since **both are function types***)
     * with the difference that it captures the invocation and its result
     * instead of returning it.
     *
     * **Example** *of what the user sees*
     *
     * ```kotlin
     *     build {
     *         capturingReference() 👉 captures: $reference()
     *     }
     * ```
     *
     *
     * **Manual Implementation**
     *
     * The following code snippet roughly shows how this feature is implemented:
     * ```
     * class Builder {
     *     protected val captured = mutableMapOf<KProperty<*>, Captured<*>>()
     *     fun build() = … // build using captured values
     *
     *     private $reference:(…) -> R = delegateeReceiver::delegateeFunction
     *     val <T:Function<*>,R> capturingReference: (…)->Unit = { arguments ->
     *         captured[::capturingReference] = capture { $reference(arguments) }
     *     }
     * }
     */
    fun <T : () -> R, R> function(callable: T): CapturingCallable<() -> Unit, R?> = function(null, callable)

    /**
     * Returns a callable that captures the value it is called with.
     *
     * Uses [initialValue] as the captured value if no invocations are recorded.
     *
     * **Example** *of what the user sees*
     *
     * ```kotlin
     *     build {
     *         capturingFunction(value) 👉 captures: value
     *     }
     * ```
     *
     * **Manual Implementation**
     *
     * The following code snippet roughly shows how this feature is implemented:
     * ```
     * class Builder {
     *     protected val captured = mutableMapOf<KProperty<*>, Captured<*>>()
     *     fun build() = … // build using captured values
     *
     *     val <T> capturingFunction: (T) -> Unit = { value ->
     *         captured[::capturingFunction] = capture { value }
     *     }
     * }
     */
    fun <T> function(initialValue: T): CapturingCallable<(T) -> Unit, T> =
        CapturingCallable(initialValue, captures) { callback ->
            { value -> callback(Deferred { value }) }
        }

    /**
     * Returns a callable that captures the value it is called with.
     *
     * **Example** *of what the user sees*
     *
     * ```kotlin
     *     build {
     *         capturingFunction(value) 👉 captures: value
     *     }
     * ```
     *
     * **Manual Implementation**
     *
     * The following code snippet roughly shows how this feature is implemented:
     * ```
     * class Builder {
     *     protected val captured = mutableMapOf<KProperty<*>, Captured<*>>()
     *     fun build() = … // build using captured values
     *
     *     val <T> capturingFunction: (T) -> Unit = { value ->
     *         captured[::capturingFunction] = capture { value }
     *     }
     * }
     */
    fun <T> function(): CapturingCallable<(T?) -> Unit, T?> = function(null)

    /**
     * Returns a property that captures the value it is assigned with.
     *
     * Uses [initialValue] as the captured value if no invocations are recorded.
     *
     * If the caller reads this property the most recent invocation is read
     * and cast to the correct type using [cast].
     *
     * **Example** *of what the user sees*
     *
     * ```kotlin
     *     build {
     *         capturingProperty = value 👉 captures: value
     *     }
     * ```
     *
     * **Manual Implementation**
     *
     * The following code snippet roughly shows how this feature is implemented:
     * ```
     * class Builder {
     *     protected val captured = mutableMapOf<KProperty<*>, Captured<*>>()
     *     fun build() = … // build using captured values
     *
     *     var capturingSetter:T? = null
     *         get() = captured[::capturingSetter]?.evaluate()
     *         set() { captured[::capturingSetter] = capture{ value }
     * }
     */
    fun <T> setter(initialValue: T, cast: (Any?) -> T): CapturingProperty<T> =
        CapturingProperty(initialValue, captures, cast)


    /**
     * Returns a property that captures the value it is assigned with.
     *
     * Uses [initialValue] as the captured value if no invocations are recorded.
     *
     * **Example** *of what the user sees*
     *
     * ```kotlin
     *     build {
     *         capturingProperty = value 👉 captures: value
     *     }
     * ```
     *
     * **Manual Implementation**
     *
     * The following code snippet roughly shows how this feature is implemented:
     * ```
     * class Builder {
     *     protected val captured = mutableMapOf<KProperty<*>, Captured<*>>()
     *     fun build() = … // build using captured values
     *
     *     var capturingSetter:T? = null
     *         get() = captured[::capturingSetter]?.evaluate()
     *         set() { captured[::capturingSetter] = capture{ value }
     * }
     */
    inline fun <reified T> setter(initialValue: T): CapturingProperty<T> = setter(initialValue) { it as T }

    /**
     * Returns a property that captures the value it is assigned with.
     *
     * **Example** *of what the user sees*
     *
     * ```kotlin
     *     build {
     *         capturingProperty = value 👉 captures: value
     *     }
     * ```
     *
     * **Manual Implementation**
     *
     * The following code snippet roughly shows how this feature is implemented:
     * ```
     * class Builder {
     *     protected val captured = mutableMapOf<KProperty<*>, Captured<*>>()
     *     fun build() = … // build using captured values
     *
     *     var capturingSetter:T? = null
     *         get() = captured[::capturingSetter]?.evaluate()
     *         set() { captured[::capturingSetter] = capture{ value }
     * }
     */
    inline fun <reified T> setter(): CapturingProperty<T?> = setter(null) { it as T }

    override fun toString(): String = asString(::captures)
}

/**
 * A delegate provider that returns a [ReadWriteProperty] that captures
 * the values it is assigned with and stores them in the given [capturesMap].
 *
 * Uses [initialValue] as the captured value if no invocations are recorded.
 *
 * If the caller reads this property the most recent invocation is read
 * and cast to the correct type using [cast].
 */
class CapturingProperty<T>(
    private val initialValue: T,
    private val capturesMap: CapturesMap,
    private val cast: (Any?) -> T,
) : ReadWriteProperty<Any?, T> {

    private fun KProperty<*>.store(value: T): Unit =
        Deferred { value }.let { capturesMap[this] = it }

    operator fun provideDelegate(thisRef: Any?, property: KProperty<*>): CapturingProperty<T> =
        also { property.store(initialValue) }

    /**
     * Returns the most recently captured value.
     */
    override fun getValue(thisRef: Any?, property: KProperty<*>): T =
        capturesMap.get(property, cast).evaluate() ?: throw Exceptions.AE("No value found for $property.",
            "Possibly this delegate was not created by its own delegate provider which creates an initial entry.")

    /**
     * Captures the set value by storing it in [capturesMap].
     */
    override fun setValue(thisRef: Any?, property: KProperty<*>, value: T) {
        property.store(value)
    }
}

/**
 * A delegate provider that returns a [CallableProperty] that captures
 * invocations on the invokable [T] returned by the given [adapter]
 * and stores them in the given [capturesMap].
 *
 * Uses [initialValue] as the captured value if no invocations are recorded.
 */
class CapturingCallable<T, R>(
    private val initialValue: R,
    private val capturesMap: CapturesMap,
    private val adapter: ((Deferred<R>) -> Unit) -> T,
) : CallableProperty<Any?, T> {

    private fun KProperty<*>.store(invocation: Deferred<R>) {
        capturesMap[this] = invocation
    }

    operator fun provideDelegate(thisRef: Any?, property: KProperty<*>): CapturingCallable<T, R> =
        also { property.store(Deferred { initialValue }) }

    override fun getValue(thisRef: Any?, property: KProperty<*>): T =
        adapter { property.store(it) }
}

/**
 * A interface for the given [builder] that addresses the limitation of functional
 * types not being allowed to be extended in JS. The same functionality is
 * provided by multiple [invoke] operator functions.
 *
 * Instances of this class can be used like a functional type when used with [CapturingContext].
 *
 * All invocations to this interface don't trigger a build. Instead the build
 * is captured an passed to the given [callback].
 */
class SkippableCapturingBuilderInterface<T : Function<*>, R>(
    val builder: Builder<T, R>,
    val callback: (Deferred<R>) -> Unit,
) : SkippableBuilder<T, R, Unit> {
    override operator fun invoke(init: T) = callback(Deferred { builder(init) })
    override operator fun invoke(result: R) = callback(Deferred { result })
    override infix fun instead(result: R) = callback(Deferred { result })
}
