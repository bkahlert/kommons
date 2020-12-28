package koodies.test

import koodies.logging.SLF4J
import koodies.runtime.deleteOnExit
import koodies.terminal.AnsiColors.red
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.extension.ExtensionContext
import java.nio.file.Path
import kotlin.io.path.createDirectories
import kotlin.io.path.createTempDirectory
import kotlin.io.path.exists
import kotlin.reflect.KFunction
import kotlin.reflect.KProperty
import kotlin.system.exitProcess

private val root by lazy { createTempDirectory("koodies").deleteOnExit() }

/**
 * Runs the [block] with a temporary directory as its receiver object,
 * leveraging the need to clean up eventually created files.
 *
 * The name is generated from the test name and a random suffix.
 *
 * @throws IllegalStateException if called from outside of a test
 */
fun withTempDir(uniqueId: UniqueId, block: Path.() -> Unit) {
    val tempDir = root.resolve(uniqueId.simple).createDirectories()
    tempDir.block()
    check(root.exists()) {
        println("The shared root temp directory was deleted by $uniqueId or a concurrently running test. This must not happen.".red())
        exitProcess(-1)
    }
}

/**
 * Creates a [DynamicTest] for each [T].
 *
 * The name for each test is heuristically derived but can also be explicitly specified using [testNamePattern]
 * which supports curly placeholders `{}` like [SLF4J] does.
 */
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

/**
 * Creates a [DynamicTest] for each [T].
 *
 * The name for each test is heuristically derived but can also be explicitly specified using [testNamePattern]
 * which supports curly placeholders `{}` like [SLF4J] does.
 */
inline fun <reified T> Array<T>.test(testNamePattern: String? = null, crossinline executable: (T) -> Unit) = toList().test(testNamePattern, executable)

/**
 * Creates a [DynamicTest] for each [T]—providing each test with a temporary work directory
 * that is automatically deletes after execution as the receiver object.
 *
 * The name for each test is heuristically derived but can also be explicitly specified using [testNamePattern]
 * which supports curly placeholders `{}` like [SLF4J] does.
 */
inline fun <reified T> Iterable<T>.testWithTempDir(uniqueId: UniqueId, testNamePattern: String? = null, crossinline executable: Path.(T) -> Unit) =
    test { withTempDir(uniqueId) { executable(it) } }

/**
 * Creates a [DynamicTest] for each map entry—providing each test with a temporary work directory
 * that is automatically deletes after execution as the receiver object.
 *
 * The name for each test is heuristically derived but can also be explicitly specified using [testNamePattern]
 * which supports curly placeholders `{}` like [SLF4J] does.
 */
inline fun <reified K, reified V> Map<K, V>.testWithTempDir(
    uniqueId: UniqueId,
    testNamePattern: String? = null,
    crossinline executable: Path.(Pair<K, V>) -> Unit,
) =
    toList().test { withTempDir(uniqueId) { executable(it) } }

