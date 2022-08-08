package com.bkahlert.kommons.test.junit

import com.bkahlert.kommons.text.indexOfOrNull
import com.bkahlert.kommons.text.lastIndexOfOrNull
import com.bkahlert.kommons.text.withPrefix
import org.junit.jupiter.engine.descriptor.ClassTestDescriptor
import org.junit.jupiter.engine.descriptor.NestedClassTestDescriptor
import org.junit.jupiter.engine.descriptor.TestFactoryTestDescriptor
import org.junit.jupiter.engine.descriptor.TestMethodTestDescriptor
import org.junit.platform.engine.UniqueId
import org.junit.platform.engine.UniqueId.Segment

/**
 * Simplified form of a [UniqueId] that only uses simple class names
 * and a formatting that strives for readability.
 *
 * In contrast to JUnit's the [UniqueId]
 * this simplified variant cannot guarantee uniqueness
 * in case of equally named classes in different packages.
 */
public data class SimpleId(
    /** Segments that make up this [SimpleId]. */
    public val segments: List<String>,
) : CharSequence by segments.joinToString(".") {
    override fun toString(): String = segments.joinToString(".")

    public companion object {

        /** Creates a [SimpleId] from the specified [StackTraceElement]. */
        public fun from(stackTraceElement: StackTraceElement): SimpleId = SimpleId(buildList {
            stackTraceElement.className.split('$').forEach {
                add(simplifyClass(it))
            }
            add(stackTraceElement.methodName.replace(" ", "_"))
        })

        /** Creates a [SimpleId] from the specified [uniqueId]. */
        public fun from(uniqueId: UniqueId): SimpleId = SimpleId(uniqueId
            .segments
            .map { simplifySegment(it) }
            .filter { it.isNotBlank() })

        private fun simplifySegment(node: Segment): String = with(node) {
            when (type) {
                "engine" -> ""
                ClassTestDescriptor.SEGMENT_TYPE -> simplifyClass(value)
                NestedClassTestDescriptor.SEGMENT_TYPE -> simplifyClass(value)
                TestMethodTestDescriptor.SEGMENT_TYPE -> simplifyMethod(value)
                TestFactoryTestDescriptor.SEGMENT_TYPE -> simplifyMethod(value)
                TestFactoryTestDescriptor.DYNAMIC_CONTAINER_SEGMENT_TYPE -> value.removePrefix("#").withPrefix("container-")
                TestFactoryTestDescriptor.DYNAMIC_TEST_SEGMENT_TYPE -> value.removePrefix("#").withPrefix("test-")
                else -> value
            }
        }

        private fun simplifyClass(value: String): String = simpleName(value).toString()

        private fun simplifyMethod(value: String): String {
            val parameterStart = value.indexOfOrNull('(') ?: return value
            val parameters = value.subSequence(parameterStart + 1, value.lastIndex)
                .split(parameterDelimiterRegex)
                .filter { it.isNotBlank() && it != SimpleId::class.qualifiedName }
            val methodName = value.substring(0, parameterStart).replace(" ", "_")
            return when (parameters.size) {
                0 -> methodName
                else -> buildString {
                    append(methodName)
                    parameters
                        .forEach { parameter ->
                            append('-')
                            append(simpleName(parameter))
                        }
                }
            }
        }

        private val parameterDelimiterRegex = Regex(",\\s*")
        private fun simpleName(name: CharSequence): CharSequence =
            name.lastIndexOfOrNull('.')?.let { name.subSequence(it + 1, name.length) } ?: name
    }
}
