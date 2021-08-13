package koodies.exec

import io.opentelemetry.api.common.Attributes
import koodies.asString
import koodies.tracing.Key

/**
 * [Attributes] related to processing the [IO] of an [Exec].
 */
public class IOAttributes(attributes: Attributes) : Attributes by attributes {
    public val type: String? by TYPE
    public val text: String? by TEXT

    override fun toString(): String = asString {
        asMap().forEach { (key, value) -> key to value }
    }

    public companion object {
        public const val SPAN_NAME: String = "koodies.exec.io"
        public val Attributes.io: IOAttributes get(): IOAttributes = IOAttributes(this)

        public val TYPE: Key<String, String> = Key.stringKey("koodies.exec.io.type") { it }
        public val TEXT: Key<String, CharSequence> = Key.stringKey("koodies.exec.io.text") { it.toString() }
    }
}
