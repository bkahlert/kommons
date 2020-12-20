package koodies.junit

import koodies.logging.SLF4J
import koodies.runtime.deleteOnExit
import koodies.text.withRandomSuffix
import org.junit.jupiter.api.DynamicTest
import java.nio.file.Path
import kotlin.io.path.createTempDirectory
import kotlin.reflect.KFunction
import kotlin.reflect.KProperty

private val root by lazy { createTempDirectory().deleteOnExit() }

/**
 * Runs the [block] with a temporary directory as its receiver object,
 * leveraging the need to clean up eventually created files.
 *
 * The name is generated from the test name and a random suffix.
 *
 * @throws IllegalStateException if called from outside of a test
 */
fun withTempDir(block: Path.() -> Unit) {
    root.resolve(uniqueId.withRandomSuffix())
}

inline fun <reified T> Iterable<T>.test(testNamePattern: String? = null, crossinline executable: (T) -> Unit) = map { input ->
    val (fallbackPattern: String, args: Array<*>) = when (input) {
        is KFunction<*> -> "for property: {}" to arrayOf(input.name)
        is KProperty<*> -> "for property: {}" to arrayOf(input.name)
        is Triple<*, *, *> -> "for: {} to {} to {}" to arrayOf(input.first, input.second, input.third)
        is Pair<*, *> -> "for: {} to {}" to arrayOf(input.first, input.second)
        else -> "for: {}" to arrayOf(input)
    }
    DynamicTest.dynamicTest(SLF4J.format(testNamePattern ?: fallbackPattern, *args)) { executable(input) }
}

inline fun <reified T> Array<T>.test(testNamePattern: String? = null, crossinline executable: (T) -> Unit) = toList().test(testNamePattern, executable)
