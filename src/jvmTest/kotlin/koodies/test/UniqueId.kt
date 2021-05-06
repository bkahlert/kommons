package koodies.test

import koodies.runtime.CallStackElement
import koodies.text.withPrefix
import koodies.text.withoutSuffix
import org.junit.jupiter.api.extension.ExtensionContext
import org.junit.platform.engine.UniqueId.Segment
import org.junit.platform.engine.UniqueId.parse

/**
 * Contains a simplified unique ID that only uses simple class names
 * and a formatting that strives for readability.
 *
 * In contrast to JUnit's the [org.junit.platform.engine.UniqueId]
 * this simplified variant cannot guarantee uniqueness
 * in case of equally named classes in different packages.
 */
class UniqueId private constructor(
    val value: String,
) : CharSequence by value {

    override fun toString(): String = value
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as UniqueId

        if (value != other.value) return false

        return true
    }

    override fun hashCode(): Int = value.hashCode()

    companion object {

        /**
         * Contains a simplified unique ID that only uses simple class names
         * and a formatting that strives for readability.
         *
         * In contrast to the fully qualified [ExtensionContext.getUniqueId]
         * its simplified variant cannot guarantee uniqueness
         * in case of equally named classes in different packages.
         */
        val ExtensionContext.id get(): UniqueId = from(parse(uniqueId))

        /**
         * Contains a simplified unique ID that only uses simple class names
         * and a formatting that strives for readability.
         *
         * In contrast to the fully qualified [ExtensionContext.getUniqueId]
         * its simplified variant cannot guarantee uniqueness
         * in case of equally named classes in different packages.
         */
        val ExtensionContext.simplifiedId: String get() = from(parse(uniqueId)).value

        fun from(callStackElement: CallStackElement) = UniqueId(koodies.builder.buildList {
            callStackElement.receiver?.split('$')?.forEach {
                add(formatClass(it))
            }
            add(callStackElement.function.replace(" ", "_"))
        }.joinToString("."))

        /**
         * Example: `[engine:junit-jupiter]/[class:koodies.test.TesterTest]/[nested-class:OtherTest]/[method:should throw on incomplete evaluating()]`
         */
        fun from(uniqueId: org.junit.platform.engine.UniqueId): UniqueId = UniqueId(uniqueId
            .segments
            .map { formatNode(it) }
            .filter { it.isNotBlank() }
            .joinToString("."))

        private fun formatNode(node: Segment): String = with(node) {
            when (type) {
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

        private fun formatClass(value: String): String = value.split(".").last()

        private fun formatMethod(value: String): String {
            fun formatArgs(args: String) = args.split(",")
                .filter { it.isNotBlank() && it != UniqueId::class.qualifiedName }
                .joinToString("") { "-" + formatClass(it) }

            return value.split("(").let { it.first().replace(" ", "_") + formatArgs(it.last().withoutSuffix(")")) }
        }
    }
}
