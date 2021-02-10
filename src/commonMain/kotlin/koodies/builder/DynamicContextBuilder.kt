package koodies.builder

import koodies.StoredValueHistory
import koodies.StoringFunction
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
            noinline buildContext: PropertiesBuildingContext.() -> C,
        ): T = invoke(init, transform, buildContext)

        inline operator fun <reified C, reified T> invoke(
            noinline init: Init<C, Unit>,
            noinline transform: PropertiesEvaluatingContext.() -> T,
            noinline buildContext: PropertiesBuildingContext.() -> C,
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
    val value: V
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
open class BuildingProperty<C, T>(
    private val initialValue: T,
    /**
     * This provider is used to get the [BuildingContext] involved
     * in building an instance of [V].
     */
    protected val builder: Provider<Builder<C, Unit, T>>,
) : StoringFunction<Init<C, Unit>>({ }), EvaluatedValue<T> {
    override fun onChange(property: KProperty<*>, history: StoredValueHistory<Init<C, Unit>>, newValue: Init<C, Unit>): Boolean = true

    /**
     * Build a new instance of [V] by applying the stored [Init] of the last
     * invocation of this property to the [BuildingContext] returned by
     * [builder].
     *
     * Returns `null` if not invocation took place yet or of [init]
     * was explicitly set to `null`.
     */
    override val value: T get() = if (history.size > 1) builder().build(history.mostRecentValue) else initialValue
}

/**
 * A [Function]-type property that accepts a [Provider] that returns instances of [V].
 *
 * Similar to [BuildingProperty] this property can be invoked like a typical builder
 * function with receiver object.
 *
 * Calls to [provide] will be forwarded to the most recently stored [Provider].
 */
open class ProvidingProperty<T>(initialValue: T) : StoringFunction<Provider<T>>({ initialValue }), EvaluatedValue<T> {
    override fun onChange(property: KProperty<*>, history: StoredValueHistory<Provider<T>>, newValue: Provider<T>): Boolean = true

    /**
     * Provides a new instance of [V] by invoking the [Provider] stored during
     * the last invocation of this property. If it's not set, `null` is returned.
     */
    override val value: T get() = history.mostRecentValue()
}

/**
 * A [Function]-type property that accepts a [Provider] that returns instances of [T].
 *
 * In contrast to [ProvidingProperty] and [BuildingProperty] this property
 * does not store invocations but simply the values itself.
 *
 * Calls to [load] will return the most recently stored value.
 */
open class StoringProperty<T>(initialValue: T) : StoringFunction<T>(initialValue), EvaluatedValue<T> {
    override fun onChange(property: KProperty<*>, history: StoredValueHistory<T>, newValue: T): Boolean = true

    /**
     * Contains the value of type [T] of the last invocation of this property.
     */
    override val value: T get() = history.mostRecentValue
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
