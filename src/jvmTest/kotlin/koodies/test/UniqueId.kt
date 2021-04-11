package koodies.test

import koodies.text.withPrefix
import koodies.text.withoutSuffix
import org.junit.jupiter.api.extension.ExtensionContext

class UniqueId(private val fullyQualified: String) {

    companion object {
        val ExtensionContext.id get(): UniqueId = UniqueId(uniqueId)

        /**
         * Contains a simplified unique ID that only uses simple class names
         * and a formatting that strives for readability.
         *
         * In contrast to the fully qualified [ExtensionContext.getUniqueId]
         * its simplified variant cannot guarantee uniqueness
         * in case of equally named classes in different packages.
         */
        val ExtensionContext.simplifiedId: String
            get() = UniqueId(uniqueId).simplified
    }

    /**
     * Contains a simplified unique ID that only uses simple class names
     * and a formatting that strives for readability.
     *
     * In contrast to the fully qualified [fullyQualified]
     * its simplified variant cannot guarantee uniqueness
     * in case of equally named classes in different packages.
     */
    val simplified: String
        get() = fullyQualified
            .split("/")
            .map { formatNode(it) }
            .filter { it.isNotBlank() }
            .joinToString(".")

    private fun formatNode(node: String): String {
        val (type, value) = node.removeSurrounding("[", "]").split(":")

        fun formatClass(value: String): String = value.split(".").last()
        fun formatMethod(value: String): String {
            fun formatArgs(args: String) = args.split(",")
                .filter { it.isNotBlank() && it != UniqueId::class.qualifiedName }
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

    override fun toString(): String = simplified
}
