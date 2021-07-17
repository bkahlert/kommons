package koodies.tracing.rendering

import io.opentelemetry.api.common.Attributes
import koodies.asString
import koodies.tracing.Key

/**
 * [Attributes] related to rendering a span.
 */
public class RenderingAttributes(attributes: Attributes) : Attributes by attributes {
    public val description: String? by DESCRIPTION
    public val extra: String? by EXTRA

    override fun toString(): String = asString {
        asMap().forEach { (key, value) -> key to value }
    }

    public companion object Keys {
        public val Attributes.rendering: RenderingAttributes get(): RenderingAttributes = RenderingAttributes(this)

        public val DESCRIPTION: Key<String, CharSequence> = Key.stringKey("description") { it.toString() }
        public val EXTRA: Key<String, Any> = Key.stringKey("koodies.extra") { it.toString() }

        public val RENDERER: Key<String, Renderer> = Key.stringKey("koodies.renderer") { it.toString() }
        public val RENDERERS: Key<List<String>, List<String>> = Key.stringArrayKey("koodies.renderers")
    }
}
