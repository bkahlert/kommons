package com.bkahlert.kommons_deprecated.tracing.rendering

import com.bkahlert.kommons.debug.asString
import com.bkahlert.kommons_deprecated.tracing.Key
import io.opentelemetry.api.common.Attributes

/**
 * [Attributes] related to rendering a span.
 */
public class RenderingAttributes(attributes: Attributes) : Attributes by attributes {
    public val description: String? by DESCRIPTION
    public val renderer: String? by RENDERER

    override fun toString(): String = asString {
        asMap().forEach { (key, value) -> put(key, value) }
    }

    public companion object Keys {
        public val Attributes.rendering: RenderingAttributes get(): RenderingAttributes = RenderingAttributes(this)

        /**
         * Generic description attribute.
         */
        public val DESCRIPTION: Key<String, CharSequence> = Key.stringKey("description") { it.toString() }

        /**
         * Renderer used for tracing.
         */
        public val RENDERER: Key<String, Renderer> = Key.stringKey("kommons.renderer") { it.toString() }
    }
}