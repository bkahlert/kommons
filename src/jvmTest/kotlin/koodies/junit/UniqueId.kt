package koodies.junit

import koodies.text.withPrefix
import koodies.text.withoutSuffix
import org.junit.jupiter.api.extension.ExtensionContext

inline class UniqueId(val uniqueId: String) {
    companion object {
        fun ExtensionContext.uniqueId(): UniqueId = UniqueId(uniqueId)
    }

    /**
     * Contains the unchanged reported unique ID.
     */
    val fullyQualified: String get() = uniqueId

    /**
     * Contains a simplified unique ID that only uses simple class names
     * and a formatting that strives for readability.
     *
     * In contrast to [fullyQualified] this property cannot guarantee uniqueness
     * in case of equally named classes in different packages.
     */
    val simple: String
        get() = uniqueId
            .split("/")
            .map { formatNode(it) }
            .filter { it.isNotBlank() }
            .joinToString(".")

    fun formatNode(node: String): String {
        val (type, value) = node.removeSurrounding("[", "]").split(":")

        fun formatClass(value: String): String = value.split(".").last()
        fun formatMethod(value: String): String {
            fun formatArgs(args: String) = args.split(",")
                .filter { it.isNotBlank() }
                .joinToString("") { "-" + formatClass(it) }

            return value.split("(").let { it.first().replace(" ", "_") + formatArgs(it.last().withoutSuffix(")")) }
        }

        return when (type) {
            "engine" -> ""
            "class" -> formatClass(value)
            "nested-class" -> formatClass(value)
            "method" -> formatMethod(value)
            "test-factory" -> formatMethod(value)
            "dynamic-container" -> value.removePrefix("#").withPrefix("container-")
            "dynamic-test" -> value.removePrefix("#").withPrefix("test-")
            else -> value.replace(" ", "_")
        }
    }

    override fun toString(): String = fullyQualified
}

/**
 * Contains a simplified unique ID that only uses simple class names
 * and a formatting that strives for readability.
 *
 * In contrast to [UniqueId.fullyQualified] this property cannot guarantee uniqueness
 * in case of equally named classes in different packages.
 */
val uniqueId get() = UniqueId(extensionContext?.uniqueId ?: error("No valid test context.")).simple
