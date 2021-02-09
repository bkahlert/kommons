package koodies.builder

import koodies.builder.context.PropertiesBuildingContext
import koodies.builder.context.StatefulPropertiesBuildingContext
import koodies.builder.context.StatefulPropertiesBuildingContext.PropertiesEvaluatingContext
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

/**
 * A lambda that returns an instance of the specified type.
 */
typealias Provider<T> = Function0<T>

/**
 * Builder of which the context [C] is dynamically computed before each build
 * using [StatefulPropertiesBuildingContext.DelegateMappingPropertiesBuildingContext].
 *
 * On [build] completion the specified [transform] is provided with a [PropertiesEvaluatingContext]
 * that gives access to the evaluated arguments the properties of the dynamic context have been
 * called with.
 */
open class DynamicContextBuilder<C, T>(
    override val transform: PropertiesEvaluatingContext.() -> T,
    /**
     * Function used to compute a new context [C].
     */
    protected val computeContext: StatefulPropertiesBuildingContext<C>.DelegateMappingPropertiesBuildingContext<C>.() -> C,
) : StatefulContextBuilder<C, PropertiesEvaluatingContext, T> {

    override val statefulContext: StatefulPropertiesBuildingContext<C> get() = StatefulPropertiesBuildingContext(computeContext)

    companion object {
        inline fun <reified C, reified T> buildContext(
            noinline init: Init<C, Unit>,
            noinline transform: PropertiesEvaluatingContext.() -> T,
            noinline buildContext: PropertiesBuildingContext<C>.() -> C,
        ): T = invoke(init, transform, buildContext)

        inline operator fun <reified C, reified T> invoke(
            noinline init: Init<C, Unit>,
            noinline transform: PropertiesEvaluatingContext.() -> T,
            noinline buildContext: PropertiesBuildingContext<C>.() -> C,
        ): T = DynamicContextBuilder(transform, buildContext).build(init)
    }

}

/**
 * Interface implemented by the various invocation capturing properties like [BuildingProperty]
 * to compute one of the values needed by [StatefulPropertiesBuildingContext] to complete its build process.
 */
interface EvaluatedValue<V> {
    /**
     * The evaluated value
     */
    val value: V?
}

/**
 * A [Function]-type property that accepts an [Init] with an instance of [BC] as the
 * receiver object.
 *
 * In other words: This property can be invoked that same way a typical builder
 * function with receiver object is called.
 *
 * The invocation is not effectuated immediately but stored. It will be applied
 * to the building context [BC] provided by [builder] whenever
 * a new instance of [V] is built using [build].
 *
 * @see ProvidingProperty
 */
open class BuildingProperty<T, C, R, V>(
    /**
     * This provider is used to get the [BuildingContext] involved
     * in building an instance of [V].
     */
    protected val builder: Provider<Builder<C, R, V>>,
) : ParameterProcessingProperty1<T, Init<C, R>, Unit>(), EvaluatedValue<V> {
    /**
     * Contains the [Init] of the last invocation of this property.
     */
    protected var init: Init<C, R>? = null
    final override val processor: Function1<Init<C, R>, Unit> = { init = it }

    /**
     * Build a new instance of [V] by applying the stored [Init] of the last
     * invocation of this property to the [BuildingContext] returned by
     * [builder].
     *
     * Returns `null` if not invocation took place yet or of [init]
     * was explicitly set to `null`.
     */
    override val value: V? get() = init?.let { builder().build(it) }
}

/**
 * A [Function]-type property that accepts a [Provider] that returns instances of [V].
 *
 * Similar to [BuildingProperty] this property can be invoked like a typical builder
 * function with receiver object.
 *
 * Calls to [provide] will be forwarded to the most recently stored [Provider].
 */
open class ProvidingProperty<T, V> : ParameterProcessingProperty1<T, Provider<V>, Unit>(), EvaluatedValue<V> {
    /**
     * Contains the [Provider] of the last invocation of this property.
     */
    protected var provider: Provider<V>? = null
    override val processor: Function1<Provider<V>, Unit> = { provider = it }

    /**
     * Provides a new instance of [V] by invoking the [Provider] stored during
     * the last invocation of this property. If it's not set, `null` is returned.
     */
    override val value: V? get() = provider?.let { it() }
}

/**
 * A [Function]-type property that accepts a [Provider] that returns instances of [V].
 *
 * In contrast to [ProvidingProperty] and [BuildingProperty] this property
 * does not store invocations but simply the values itself.
 *
 * Calls to [load] will return the most recently stored value.
 */
open class StoringProperty<T, V> : ParameterProcessingProperty1<T, V, Unit>(), EvaluatedValue<V> {
    /**
     * Contains the value of type [V] of the last invocation of this property.
     */
    override var value: V? = null
    override val processor: Function1<V, Unit> = { this.value = it }
}

/**
 * A delegating property that behaves like a zero-arg function.
 *
 * Invocations are delegates to [processor] that needs to be implemented.
 */
abstract class ParameterProcessingProperty0<T, R> : ReadOnlyProperty<T, Function0<R>> {
    override fun getValue(thisRef: T, property: KProperty<*>): Function0<R> = processor

    /**
     * All invocations are delegated to this processor.
     */
    protected abstract val processor: Function0<R>
}

/**
 * A delegating property that behaves like a one-arg function.
 *
 * Invocations are delegates to [processor] that needs to be implemented.
 */
abstract class ParameterProcessingProperty1<T, P1, R> : ReadOnlyProperty<T, Function1<P1, R>> {
    override fun getValue(thisRef: T, property: KProperty<*>): Function1<P1, R> = processor

    /**
     * All invocations are delegated to this processor.
     */
    protected abstract val processor: Function1<P1, R>
}

/**
 * A delegating property that behaves like a two-arg function.
 *
 * Invocations are delegates to [processor] that needs to be implemented.
 */
abstract class ParameterProcessingProperty2<T, P1, P2, R> : ReadOnlyProperty<T, Function2<P1, P2, R>> {
    override fun getValue(thisRef: T, property: KProperty<*>): Function2<P1, P2, R> = processor

    /**
     * All invocations are delegated to this processor.
     */
    protected abstract val processor: Function2<P1, P2, R>
}
