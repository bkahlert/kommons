package koodies.builder

import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

/**
 * A lambda that returns an instance of the specified type.
 */
typealias Provider<T> = () -> T

fun interface Providing<T> {
    fun provide(): T
}

fun <T> providing(provider: Provider<T>) = Providing { provider() }

/**
 * A context that provides various factories for delegated
 * properties to build higher-order builders.
 */
interface PropertiesBuildingContext<REF> {
    /**
     * Returns a [BuildingProperty] that accepts an [Init] with an instance of [BC] as the
     * receiver object.
     *
     * In other words: This property can be invoked that same way a typical builder
     * function with receiver object is called.
     */
    fun <BC : BuildingContext<BC, T>, T> building(buildingContextProvider: Provider<BC>) = BuildingProperty<REF, BC, T>(buildingContextProvider)

    /**
     * Returns a [ProvidingProperty] that accepts a [Provider] that returns instances of [T].
     */
    fun <T> providing() = ProvidingProperty<REF, T>()

    /**
     * Returns a [StoringProperty] that accepts a [Provider] that returns instances of [T].
     */
    fun <T> storing() = StoringProperty<REF, T>()
}

/**
 * Builder that can be used to build complex builders that will accept
 * [Init] instances to build instances of [T].
 */
open class HigherOrderBuilder<BC, T>(transform: BC.() -> T) : PropertiesBuildingContext<HigherOrderBuilder<BC, T>>,
    BuildingContextImpl<BC, T>(transform) where BC : BuildingContext<BC, T> {

    /**
     * Internal map that links built properties to a [ValueAccessor] capable
     * of returning later passed arguments to the built building context [BC]
     * in order to build instances of [T].
     */
    protected val valueAccessors: MutableMap<String, ValueAccessor<*>> = mutableMapOf()

    /**
     * Delegate provider that stores built properties in [valueAccessors].
     */
    protected operator fun <D : ValueAccessor<*>> D.provideDelegate(thisRef: HigherOrderBuilder<BC, T>, property: KProperty<*>): D {
        valueAccessors.put(property.name, this)
        println("hihi $property")
        return this
    }

    protected inline fun <reified V> KProperty<*>.accessValue(): V? =
        valueAccessors[name]?.run {
            accessValue()?.let {
                require(it is V) { "$it expected to be of type ${V::class}." }
                it
            }
        }

    protected inline fun <reified V> KProperty<*>.requireValue(lazyMessage: Provider<String> = { "${V::class} must not be null." }): V =
        accessValue<V>() ?: throw IllegalStateException(lazyMessage())
}

/**
 * Interface implemented by the various invocation capturing properties like [BuildingProperty]
 * to compute one of the values needed by [HigherOrderBuilder] to complete its build process.
 */
interface ValueAccessor<V> {
    fun accessValue(): V?
}

/**
 * An [InvocableProperty] that accepts an [Init] with an instance of [BC] as the
 * receiver object.
 *
 * In other words: This property can be invoked that same way a typical builder
 * function with receiver object is called.
 *
 * The invocation is not effectuated immediately but stored. It will be applied
 * to the building context [BC] provided by [buildingContextProvider] whenever
 * a new instance of [V] is built using [build].
 *
 * @see ProvidingProperty
 */
open class BuildingProperty<T, BC : BuildingContext<BC, V>, V>(
    /**
     * This provider is used to get the [BuildingContext] involved
     * in building an instance of [V].
     */
    protected val buildingContextProvider: Provider<BC>,
) : ParameterProcessingProperty1<T, Init<BC>, Unit>(), ValueAccessor<V> {
    /**
     * Contains the [Init] of the last invocation of this property.
     */
    protected var init: Init<BC>? = null
    final override val processor: ParameterProcessor1<Init<BC>, Unit> = ParameterProcessor1 { init = it }

    /**
     * Build a new instance of [V] by applying the stored [Init] of the last
     * invocation of this property to the [BuildingContext] returned by
     * [buildingContextProvider].
     *
     * Returns `null` if not invocation took place yet or of [init]
     * was explicitly set to `null`.
     */
    override fun accessValue(): V? = init?.let { Builder.build(it, buildingContextProvider) }
}

/**
 * An [InvocableProperty] that accepts a [Provider] that returns instances of [V].
 *
 * Similar to [BuildingProperty] this property can be invoked like a typical builder
 * function with receiver object.
 *
 * Calls to [provide] will be forwarded to the most recently stored [Provider].
 */
open class ProvidingProperty<T, V> : ParameterProcessingProperty1<T, Provider<V>, Unit>(), ValueAccessor<V> {
    /**
     * Contains the [Provider] of the last invocation of this property.
     */
    protected var provider: Provider<V>? = null
    override val processor: ParameterProcessor1<Provider<V>, Unit> = ParameterProcessor1 { provider = it }

    /**
     * Provides a new instance of [V] by invoking the [Provider] stored during
     * the last invocation of this property. If it's not set, `null` is returned.
     */
    override fun accessValue(): V? = provider?.let { it() }
}

/**
 * An [InvocableProperty] that accepts a [Provider] that returns instances of [V].
 *
 * In contrast to [ProvidingProperty] and [BuildingProperty] this property
 * does not store invocations but simply the values itself.
 *
 * Calls to [load] will return the most recently stored value.
 */
open class StoringProperty<T, V> : ParameterProcessingProperty1<T, V, Unit>(), ValueAccessor<V> {
    /**
     * Contains the value of type [V] of the last invocation of this property.
     */
    protected var value: V? = null
    override val processor: ParameterProcessor1<V, Unit> = ParameterProcessor1 { value = it }

    /**
     * Returns the instance of [V] that was stored during the last invocation of this property.
     */
    override fun accessValue(): V? = value
}


// ↓ Helper interfaces and classes ↓


/**
 * Helper interface to support JS which
 * does not allow extending lambdas.
 */
fun interface InvocableProperty<R> {
    /**
     * Invokes the implementor and returns
     * an instance of [R].
     */
    operator fun invoke(): R
}

/**
 * Helper interface to support JS which
 * does not allow extending lambdas.
 */
fun interface InvocableProperty1<P1, R> {
    /**
     * Invokes the implementor with [p1] and returns
     * an instance of [R].
     */
    operator fun invoke(p1: P1): R
}

/**
 * Helper interface to support JS which
 * does not allow extending lambdas.
 */
fun interface InvocableProperty2<P1, P2, R> {
    /**
     * Invokes the implementor with [p1] and [p2] and returns
     * an instance of [R].
     */
    operator fun invoke(p1: P1, p2: P2): R
}

/**
 * Helper interface to support JS which
 * does not allow extending lambdas.
 */
fun interface ParameterProcessor<R> {
    /**
     * Processes and returns an instance of [R].
     */
    fun process(): R
}

/**
 * Helper interface to support JS which
 * does not allow extending lambdas.
 */
fun interface ParameterProcessor1<P1, R> {
    /**
     * Processes [p1] and returns an instance of [R].
     */
    fun process(p1: P1): R
}

/**
 * Helper interface to support JS which
 * does not allow extending lambdas.
 */
fun interface ParameterProcessor2<P1, P2, R> {
    /**
     * Processes [p1] and [p2] and returns an instance of [R].
     */
    fun process(p1: P1, p2: P2): R
}

/**
 * A delegating property that behaves like a zero-arg function.
 *
 * Invocations are delegates to [processor] that needs to be implemented.
 */
abstract class ParameterProcessingProperty<T, R> : ReadOnlyProperty<T, InvocableProperty<R>> {
    override fun getValue(thisRef: T, property: KProperty<*>): InvocableProperty<R> = InvocableProperty { processor.process() }

    /**
     * All invocations are delegated to this processor.
     */
    protected abstract val processor: ParameterProcessor<R>
}

/**
 * A delegating property that behaves like a one-arg function.
 *
 * Invocations are delegates to [processor] that needs to be implemented.
 */
abstract class ParameterProcessingProperty1<T, P1, R> : ReadOnlyProperty<T, InvocableProperty1<P1, R>> {
    override fun getValue(thisRef: T, property: KProperty<*>): InvocableProperty1<P1, R> = InvocableProperty1 { p1 -> processor.process(p1) }

    /**
     * All invocations are delegated to this processor.
     */
    protected abstract val processor: ParameterProcessor1<P1, R>
}

/**
 * A delegating property that behaves like a two-arg function.
 *
 * Invocations are delegates to [processor] that needs to be implemented.
 */
abstract class ParameterProcessingProperty2<T, P1, P2, R> : ReadOnlyProperty<T, InvocableProperty2<P1, P2, R>> {
    override fun getValue(thisRef: T, property: KProperty<*>): InvocableProperty2<P1, P2, R> = InvocableProperty2 { p1, p2 -> processor.process(p1, p2) }

    /**
     * All invocations are delegated to this processor.
     */
    protected abstract val processor: ParameterProcessor2<P1, P2, R>
}
