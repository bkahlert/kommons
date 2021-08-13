package koodies.tracing.rendering

import io.opentelemetry.api.common.Attributes
import koodies.asString
import koodies.tracing.Key

/**
 * [Attributes] related to rendering a span.
 */
public class RenderingAttributes(attributes: Attributes) : Attributes by attributes {
    public val description: String? by DESCRIPTION
    public val renderer: String? by RENDERER

    override fun toString(): String = asString {
        asMap().forEach { (key, value) -> key to value }
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
        public val RENDERER: Key<String, Renderer> = Key.stringKey("koodies.renderer") { it.toString() }
    }
}
