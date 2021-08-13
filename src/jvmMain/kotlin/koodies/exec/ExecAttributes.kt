package koodies.exec

import io.opentelemetry.api.common.Attributes
import koodies.asString
import koodies.tracing.Key

/**
 * [Attributes] related to running an [Exec].
 */
public class ExecAttributes(attributes: Attributes) : Attributes by attributes {
    public val name: String? by NAME
    public val executable: String? by EXECUTABLE

    override fun toString(): String = asString {
        asMap().forEach { (key, value) -> key to value }
    }

    public companion object {
        public const val SPAN_NAME: String = "koodies.exec"
        public val Attributes.exec: ExecAttributes get(): ExecAttributes = ExecAttributes(this)

        public val NAME: Key<String, CharSequence> = Key.stringKey("koodies.exec.name") { it.toString() }
        public val EXECUTABLE: Key<String, Executable<*>> = Key.stringKey("koodies.exec.executable") { it.content.toString() }
    }
}
