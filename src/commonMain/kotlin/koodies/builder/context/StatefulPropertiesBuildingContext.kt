package koodies.builder.context

import koodies.builder.EvaluatedValue
import koodies.builder.Provider
import koodies.builder.context.StatefulPropertiesBuildingContext.DelegateMappingPropertiesBuildingContext
import koodies.builder.context.StatefulPropertiesBuildingContext.PropertiesEvaluatingContext
import kotlin.reflect.KProperty

/**
 * A [StatefulContext] of which the [context] is computed by [computeContext] using
 * [DelegateMappingPropertiesBuildingContext].
 *
 * All delegates of type [EvaluatedValue] created during context computation are
 * accessible by the [state] at any later moment.
 *
 * Effectively this serves to "instrument" a context to later access the arguments
 * that have been passed to accordingly instrumented properties.
 */
open class StatefulPropertiesBuildingContext<C>(
    /**
     * Function used to compute a new [context].
     */
    protected val computeContext: StatefulPropertiesBuildingContext<C>.DelegateMappingPropertiesBuildingContext<C>.() -> C,
) : StatefulContext<C, PropertiesEvaluatingContext> {

    /**
     * Map that links built properties to a [ValueAccessor] capable
     * of returning later passed arguments to the built building context [BC]
     * in order to build instances of [C].
     */
    override val state: PropertiesEvaluatingContext get() = PropertiesEvaluatingContext(delegates)

    /**
     * A context that can be used to evaluate the arguments that were last passed
     * to the properties of context [C].
     */
    open class PropertiesEvaluatingContext(protected val properties: Map<String, EvaluatedValue<*>>) {
        /**
         * Evaluates the argument that was last passed to `this` property
         * and returns it.
         *
         * @throws IllegalStateException if that value is not of type [V].
         * @returns the evaluated value respectively `null` if this is the
         * result of the evaluation itself or if the property was never called.
         */
        fun <V> KProperty<*>.getOrNull(): V? =
            properties[name]?.run {
                value?.let {
                    @Suppress("UNCHECKED_CAST")
                    kotlin.runCatching { it as V }.getOrNull() ?: error("$it is of incorrect type.")
                }
            }

        /**
         * Evaluates the argument that was last passed to `this` property
         * and returns it if it's not `null`.
         *
         * If the evaluated value is `null` [onFailure] is called with no
         * set exception to provide a default value.
         *
         * If an exception was thrown during evaluation [onFailure] is
         * called with that exception to provide a default value.
         *
         * @throws Throwable if [onFailure] throws
         * @returns the evaluated value respectively the result of [onFailure]
         * if the originally evaluated is `null` or threw an exception during evaluation.
         */
        fun <V> KProperty<*>.getOrElse(onFailure: (exception: Throwable?) -> V) =
            runCatching { getOrNull() ?: onFailure(null) }.getOrElse(onFailure)

        /**
         * Evaluates the argument that was last passed to `this` property
         * and returns it if it's not `null`.
         *
         * @throws IllegalStateException if that value is not of type [V]
         * @throws IllegalStateException with the optional [lazyMessage] if value evaluated to `null`
         * @returns the evaluated value
         */
        fun <V> KProperty<*>.require(
            lazyMessage: Provider<String> = { "$this must not evaluate to null." },
        ): V = getOrNull() ?: throw IllegalStateException(lazyMessage())

    }

    /**
     * Map that resolves a property's delegate by the property's name.
     */
    val delegates: MutableMap<String, EvaluatedValue<*>> = mutableMapOf()

    override val context: C = DelegateMappingPropertiesBuildingContext<C>().computeContext()

    /**
     * A [PropertiesBuildingContext] that stores which all delegates of type [EvaluatedValue] and
     * which property of [C] they belong to.
     */
    inner class DelegateMappingPropertiesBuildingContext<C> : CapturingPropertiesBuildingContext<EvaluatedValue<*>> {
        override fun handleDelegate(thisRef: Any?, property: KProperty<*>, delegate: EvaluatedValue<*>) {
            delegates[property.name] = delegate
        }
    }
}
