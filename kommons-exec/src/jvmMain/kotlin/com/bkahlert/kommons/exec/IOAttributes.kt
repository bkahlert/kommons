package com.bkahlert.kommons.exec

import com.bkahlert.kommons.debug.asString
import com.bkahlert.kommons.tracing.Key
import io.opentelemetry.api.common.Attributes

/**
 * [Attributes] related to processing the [IO] of an [Exec].
 */
public class IOAttributes(attributes: Attributes) : Attributes by attributes {
    public val type: String? by TYPE
    public val text: String? by TEXT

    override fun toString(): String = asString {
        asMap().forEach { (key, value) -> put(key, value) }
    }

    public companion object {
        public const val SPAN_NAME: String = "kommons.exec.io"
        public val Attributes.io: IOAttributes get(): IOAttributes = IOAttributes(this)

        public val TYPE: Key<String, String> = Key.stringKey("kommons.exec.io.type") { it }
        public val TEXT: Key<String, CharSequence> = Key.stringKey("kommons.exec.io.text") { it.toString() }
    }
}
