package koodies.tracing.rendering

import io.opentelemetry.api.common.Attributes
import koodies.asString
import koodies.tracing.Key

/**
 * [Attributes] related to rendering a span.
 */
public class RenderingAttributes(attributes: Attributes) : Attributes by attributes {
    public val name: String? by NAME
    public val description: String? by DESCRIPTION
    public val status: String? by STATUS

    override fun toString(): String = asString {
        asMap().forEach { (key, value) -> key to value }
    }

    public companion object Keys {
        public val Attributes.rendering: RenderingAttributes get(): RenderingAttributes = RenderingAttributes(this)

        public val NAME: Key<String, CharSequence> = Key.stringKey("name") { it.toString() }
        public val DESCRIPTION: Key<String, CharSequence> = Key.stringKey("koodies.description") { it.toString() }
        public val STATUS: Key<String, Any> = Key.stringKey("koodies.status") { it.toString() }
    }
}
