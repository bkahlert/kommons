package com.bkahlert.kommons.tracing.rendering

import com.bkahlert.kommons.asString
import com.bkahlert.kommons.tracing.Key.KeyValue
import com.bkahlert.kommons.tracing.toList
import io.opentelemetry.api.common.AttributeKey
import io.opentelemetry.api.common.Attributes
import java.util.AbstractMap.SimpleEntry
import kotlin.collections.Map.Entry

/**
 * Optimized attributes representation for the purpose of rendering them.
 *
 * The two key difference to a regular attributes are:
 * - Getting a the value for attribute `attr` will also look for `attr.render`
 *   and retrieve it instead if found.
 * - All values are automatically converted to instances of [Renderable].
 */
public interface RenderableAttributes : Map<AttributeKey<*>, Renderable> {

    private class RenderingKeyPreferringAttributes(entries: Set<Pair<AttributeKey<*>, Any>>) : AbstractMap<AttributeKey<*>, Renderable>(),
        RenderableAttributes {

        constructor(entries: Iterable<KeyValue<*, *>>) : this(buildSet<Pair<AttributeKey<*>, Any>> {
            // copy regular attributes
            entries.filter { !it.key.isRenderingKey }.forEach {
                it.renderingValue?.also { value ->
                    add(it.key.valueKey to value)
                }
            }
            // copy rendering attributes (e.g. description.render) as is and as regular attribute (e.g. description)
            entries.filter { it.key.isRenderingKey }.forEach {
                it.renderingValue?.also { value ->
                    add(it.key.renderingKey to value)
                    add(it.key.valueKey to value)
                }
            }
        })

        override val entries: Set<Entry<AttributeKey<*>, Renderable>> = entries
            .map { (key, value) -> SimpleEntry(key, Renderable.of(value)) }
            .toSet()

        override fun toString(): String = asString {
            this@RenderingKeyPreferringAttributes.forEach { (key, value) -> put(key, value.render(null, null)) }
        }
    }

    public companion object {
        public val EMPTY: RenderableAttributes = of(Attributes.empty())

        public fun of(vararg attributes: KeyValue<*, *>): RenderableAttributes = RenderingKeyPreferringAttributes(attributes.toList())
        public fun of(attributes: Iterable<KeyValue<*, *>>): RenderableAttributes = RenderingKeyPreferringAttributes(attributes)

        public fun of(vararg attributes: Pair<AttributeKey<*>, Any>): RenderableAttributes = RenderingKeyPreferringAttributes(attributes.toSet())

        public fun of(attributes: Attributes): RenderableAttributes = RenderingKeyPreferringAttributes(attributes.toList().toSet())
    }
}
