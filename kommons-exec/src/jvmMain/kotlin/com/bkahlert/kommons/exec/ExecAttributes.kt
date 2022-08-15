package com.bkahlert.kommons.exec

import com.bkahlert.kommons.debug.asString
import com.bkahlert.kommons.tracing.Key
import io.opentelemetry.api.common.Attributes

/**
 * [Attributes] related to running an [Exec].
 */
public class ExecAttributes(attributes: Attributes) : Attributes by attributes {
    public val name: String? by NAME
    public val executable: String? by EXECUTABLE

    override fun toString(): String = asString {
        asMap().forEach { (key, value) -> put(key, value) }
    }

    public companion object {
        public const val SPAN_NAME: String = "kommons.exec"
        public val Attributes.exec: ExecAttributes get(): ExecAttributes = ExecAttributes(this)

        public val NAME: Key<String, CharSequence> = Key.stringKey("kommons.exec.name") { it.toString() }
        public val EXECUTABLE: Key<String, Executable<*>> = Key.stringKey("kommons.exec.executable") { it.content.toString() }
    }
}
