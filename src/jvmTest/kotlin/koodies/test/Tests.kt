package koodies.test

import filepeek.LambdaBody
import koodies.collections.asStream
import koodies.exception.toCompactString
import koodies.io.path.asPath
import koodies.io.path.readLine
import koodies.logging.SLF4J
import koodies.runtime.deleteOnExit
import koodies.terminal.AnsiColors.red
import koodies.test.TestLabeler.callerSource
import koodies.test.TestLabeler.property
import koodies.test.TestLabeler.subject
import koodies.text.TruncationStrategy
import koodies.text.truncate
import koodies.text.wrap
import org.junit.jupiter.api.DynamicContainer
import org.junit.jupiter.api.DynamicContainer.dynamicContainer
import org.junit.jupiter.api.DynamicNode
import org.junit.jupiter.api.DynamicTest
import strikt.api.Assertion
import strikt.api.expectThat
import java.net.URI
import java.nio.file.Path
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock
import kotlin.io.path.createDirectories
import kotlin.io.path.createTempDirectory
import kotlin.io.path.exists
import kotlin.jvm.internal.CallableReference
import kotlin.reflect.KFunction
import kotlin.reflect.KProperty
import kotlin.system.exitProcess
import filepeek.FilePeek2 as FilePeek

private val root by lazy { createTempDirectory("koodies").deleteOnExit() }

object TestLabeler {

    /**
     * Calculates the display name for a test with the specified [subject]
     * and the optional [testNamePattern] which supports curly placeholders `{}` like [SLF4J] does.
     *
     * If no [testNamePattern] is specified a [displayNameFallback] is calculated heuristically.
     *
     * The display name is prefixed with `this` string.
     *
     * @see displayNameFallback
     */
    fun <T> String.subject(subject: T, testNamePattern: String? = null): String {
        val (fallbackPattern: String, args: Array<*>) = displayNameFallback(subject)
        return SLF4J.format(testNamePattern ?: fallbackPattern, *args)
    }

    /**
     * Attempts to calculates a rich display name for a test case testing the specified [subject].
     *
     * The display name is prefixed with `this` string.
     */
    private fun <T> String.displayNameFallback(subject: T) = when (subject) {
        is KFunction<*> -> "$this property $`{}`" to arrayOf(subject.name)
        is KProperty<*> -> "$this property $`{}`" to arrayOf(subject.name)
        is Triple<*, *, *> -> "$this $`{}` to $`{}` to $`{}`" to arrayOf(subject.first.serialized, subject.second.serialized, subject.third.serialized)
        is Pair<*, *> -> "$this $`{}` to $`{}`" to arrayOf(subject.first.serialized, subject.second.serialized)
        else -> "$this $`{}`" to arrayOf(subject.serialized)
    }

    @Suppress("ObjectPropertyName")
    private val `{}`: String = " ❮ {} ❯ "
    private val <T> T?.serialized get() = toCompactString()

    /**
     * Attempts to calculate a rich display name for a property
     * expressed by the specified [function].
     */
    fun <T, R> String.property(function: T.() -> R): String = when (function) {
        is KProperty<*> -> "$this value of property ${function.name}"
        is KFunction<*> -> "$this return value of ${function.name}"
        is CallableReference -> findCaller().let { (_, callerMethodName) -> "$this value of ${function.getPropertyName(callerMethodName)}" }
        else -> "$this " + findCaller().let { (callerClassName, callerMethodName) ->
            getLambdaBodyOrNull(callerClassName, callerMethodName) ?: "$function"
        }
    }

    val callerSource: URI
        get() = findCaller().let { (callerClassName, callerMethodName) ->
            filePeek(callerClassName).getCallerFileInfo().run {
                val sourceFile = sourceFileName.asPath()
                val columnNumber = sourceFile.findMethodCallColumn(lineNumber, callerMethodName)
                sourceFile.asSourceUri(lineNumber, columnNumber)
            }
        }

    private fun Path.findMethodCallColumn(lineNumber: Int, callerMethodName: String): Int? =
        readLine(lineNumber)?.let { line -> line.indexOf(callerMethodName).takeIf { it > 0 } }

    private fun Path.asSourceUri(lineNumber: Int, columnNumber: Int?) =
        StringBuilder(toUri().toString()).run {
            append("?line=$lineNumber")
            columnNumber?.let { append("&column=$it") }
            URI(toString())
        }

    private fun CallableReference.getPropertyName(callerMethodName: String): String =
        "^$callerMethodName(.+)$".toRegex().find(name)?.run { groupValues[1].decapitalize() } ?: name

    /**
     * Uses a dynamically provided and cached [FilePeek] instance to
     * retrieve the body of the lambda that was passed as a parameter
     * to a call of a method with name [callerMethodName] of a class
     * with name [callerClassName].
     */
    private fun getLambdaBodyOrNull(callerClassName: String, callerMethodName: String) = kotlin.runCatching {
        val line = filePeek(callerClassName).getCallerFileInfo().line
        LambdaBody(callerMethodName, line).body.trim().truncate(40, TruncationStrategy.MIDDLE, " … ").wrap(" ❴ ", " ❵ ")
    }.getOrNull()

    /**
     * A lambda that returns a [FilePeek] to access the body of a lambda
     * that was passed as a parameter to a call of a method of a class
     * with the specified name.
     */
    private val filePeek = object : (String) -> FilePeek {
        private val lock = ReentrantLock()
        private val cache: MutableMap<String, FilePeek> = mutableMapOf()
        override fun invoke(callerClassName: String): FilePeek = lock.withLock {
            cache.computeIfAbsent(callerClassName) { FilePeek(listOfNotNull("strikt.internal", "strikt.api", "filepeek", enclosingClassName, callerClassName)) }
        }
    }

    /**
     * Finds the calling class and method name of any member
     * function of this [TestLabeler].
     */
    private fun findCaller() = Thread.currentThread().stackTrace
        .dropWhile { it.className != enclosingClassName }
        .dropWhile { it.className == enclosingClassName }
        .first().let { it.className to it.methodName }

    /**
     * Reliably contains the fully qualified name of [TestLabeler].
     * The result is still correct after typical refactorings like renaming.
     */
    private val enclosingClassName = this::class.qualifiedName
        ?.let { if (this::class.isCompanion) it.substringBeforeLast(".") else it }
        ?: error("unknown name")
}


@DslMarker
annotation class DynamicTestsDsl

/**
 * Creates tests for `this` subject
 * using the specified [DynamicTestsBuilder] based [init].
 */
@JvmName("testThis")
@DynamicTestsDsl
inline fun <reified T> T.test(noinline init: DynamicTestsBuilder<T>.(T) -> Unit): List<DynamicNode> =
    DynamicTestsBuilder.build(this, init)

/**
 * Creates tests for the specified [subject]
 * using the specified [DynamicTestsBuilder] based [init].
 */
@DynamicTestsDsl
inline fun <reified T> test(subject: T, noinline init: DynamicTestsBuilder<T>.(T) -> Unit): List<DynamicNode> = subject.test(init)

/**
 * Creates one [DynamicContainer] for each instance of `this` collection of subjects
 * using the specified [DynamicTestsBuilder] based [init].
 *
 * The name for each container is heuristically derived but can also be explicitly specified using [containerNamePattern]
 * which supports curly placeholders `{}` like [SLF4J] does.
 */
@DynamicTestsDsl
inline fun <reified T> Iterable<T>.testEach(
    containerNamePattern: String? = null,
    noinline init: DynamicTestsBuilder<T>.(T) -> Unit,
): List<DynamicContainer> = toList()
    .also { require(it.isNotEmpty()) { "At least one subject must be provided for testing." } }
    .run { map { subject -> dynamicContainer("for".subject(subject, containerNamePattern), DynamicTestsBuilder.build(subject, init)) } }

/**
 * Creates one [DynamicContainer] for each instance of the specified [subjects]
 * using the specified [DynamicTestsBuilder] based [init].
 *
 * The name for each container is heuristically derived but can also be explicitly specified using [containerNamePattern]
 * which supports curly placeholders `{}` like [SLF4J] does.
 */
@DynamicTestsDsl
inline fun <reified T> testEach(
    vararg subjects: T,
    containerNamePattern: String? = null,
    noinline init: DynamicTestsBuilder<T>.(T) -> Unit,
): List<DynamicContainer> = subjects.toList().testEach(containerNamePattern, init)

/**
 * Creates one [DynamicContainer] for each instance of the specified [subjects]
 * using the specified [DynamicTestsBuilder] based [init].
 *
 * The name for each container is heuristically derived but can also be explicitly specified using [containerNamePattern]
 * which supports curly placeholders `{}` like [SLF4J] does.
 */
@DynamicTestsDsl
@JvmName("testArrayAsReceiver")
inline fun <reified T> Array<T>.testEach(
    containerNamePattern: String? = null,
    noinline init: DynamicTestsBuilder<T>.(T) -> Unit,
): List<DynamicContainer> = toList().testEach(containerNamePattern, init)


@DynamicTestsDsl
fun <T> DynamicTestsBuilder.PropertyTestBuilder<T?>.notNull(block: DynamicTestsBuilder<T>.(T) -> Unit) =
    then {
        require(it != null)
        @Suppress("UNCHECKED_CAST")
        (this as DynamicTestsBuilder<T>).block(it)
    }

@DynamicTestsDsl
inline fun <reified T> DynamicTestsBuilder.PropertyTestBuilder<Any?>.asA(crossinline block: DynamicTestsBuilder<T>.(T) -> Unit) =
    then {
        require(it is T)
        @Suppress("UNCHECKED_CAST")
        (this as DynamicTestsBuilder<T>).block(it as T)
    }


/**
 * Builder for arbitrary test trees consisting of instances of [DynamicContainer] and [DynamicTest]
 * and a fluent transition to [Strikt](https://strikt.io) assertions.
 */
@DynamicTestsDsl
interface DynamicTestsBuilder<T> {

    @DslMarker
    annotation class ExpectationBuilderDsl

    /**
     * Builder for [Strikt](https://strikt.io) assertions.
     */
    @ExpectationBuilderDsl
    interface ExpectationBuilder<T> : Assertion.Builder<T> {
        @ExpectationBuilderDsl
        fun that(block: Assertion.Builder<T>.() -> Unit)
    }

    /**
     * Builder for testing a property of the test subject.
     */
    @DynamicTestsDsl
    interface PropertyTestBuilder<T> {
        @DynamicTestsDsl
        fun then(block: DynamicTestsBuilder<T>.(T) -> Unit)
    }

    /**
     * Builds a [DynamicContainer] using the specified [name] and the
     * specified [DynamicTestsBuilder] based [init] to build the child nodes.
     */
    @DynamicTestsDsl
    fun group(name: String, init: DynamicTestsBuilder<T>.(T) -> Unit)

    /**
     * Builds a [DynamicTest] using an automatically derived name and the specified [executable].
     */
    @DynamicTestsDsl
    fun test(description: String? = null, executable: (T) -> Unit)

    /**
     * Builds a new test tree testing the aspect returned by [transform].
     */
    @DynamicTestsDsl
    fun <R> with(description: String? = null, transform: T.() -> R): PropertyTestBuilder<R>

    /**
     * Returns a builder to specify expectations for the result of [transform] applied
     * to `this` subject.
     */
    @DynamicTestsDsl
    fun <R> expect(description: String? = null, transform: T.() -> R): ExpectationBuilder<R>

    /**
     * Builds a builder to specifiy expectations for the exception thrown when [transform]
     * is applied to `this` subject.
     */
    @DynamicTestsDsl
    fun <R> expectThrowing(description: String? = null, transform: T.() -> R): ExpectationBuilder<Result<R>>

    /**
     * Builds a new test tree testing the current subject.
     */
    @DynamicTestsDsl
    fun expect(description: String): ExpectationBuilder<T>

    /**
     * Builds a new test tree testing the current subject.
     */
    @DynamicTestsDsl
    val expect: ExpectationBuilder<T>

    companion object {

        private class CallbackCallingDynamicTestsBuilder<T>(private val subject: T, private val callback: (DynamicNode) -> Unit) : DynamicTestsBuilder<T> {

            override fun group(name: String, init: DynamicTestsBuilder<T>.(T) -> Unit) {
                callback(DynamicContainer.dynamicContainer(name, callerSource, build(subject, init).asStream()))
            }

            override fun test(description: String?, executable: (T) -> Unit) {
                callback(DynamicTest.dynamicTest(description ?: "test".property(executable), callerSource) { executable(subject) })
            }

            override fun <R> expect(description: String?, transform: T.() -> R): ExpectationBuilder<R> {
                val aspect = kotlin.runCatching { subject.transform() }
                    .onFailure { println("Failed to evaluate ${"".subject(subject).property(transform)}: $it") }
                    .getOrThrow()
                return CallbackCallingExpectationBuilder(description ?: "expect".property(transform).subject(aspect), aspect, callback)
            }

            override fun <R> expectThrowing(description: String?, transform: T.() -> R): ExpectationBuilder<Result<R>> {
                val aspect = kotlin.runCatching { subject.transform() }
                return CallbackCallingExpectationBuilder(description ?: "expect".property(transform).subject(aspect), aspect, callback)
            }

            override fun expect(description: String): ExpectationBuilder<T> =
                CallbackCallingExpectationBuilder(description, subject, callback)

            override val expect: ExpectationBuilder<T>
                get() = CallbackCallingExpectationBuilder("expect".subject(subject), subject, callback)

            override fun <R> with(description: String?, transform: T.() -> R): PropertyTestBuilder<R> {
                val aspect = subject.transform()
                val nodes = mutableListOf<DynamicNode>()
                callback(dynamicContainer(description ?: "with".property(transform).subject(aspect), callerSource, nodes.asStream()))
                return CallbackCallingPropertyTestBuilder(aspect) { nodes.add(it) }
            }
        }

        private class CallbackCallingExpectationBuilder<T>(
            private val description: String,
            private val subject: T,
            private val callback: (DynamicNode) -> Unit,
        ) : ExpectationBuilder<T>, Assertion.Builder<T> by expectThat(subject) {

            override fun that(block: Assertion.Builder<T>.() -> Unit) {
                callback(DynamicTest.dynamicTest(description.property(block), callerSource) { block() })
            }
        }

        private class CallbackCallingPropertyTestBuilder<T>(
            private val aspect: T,
            private val callback: (DynamicNode) -> Unit,
        ) : PropertyTestBuilder<T> {
            override fun then(block: DynamicTestsBuilder<T>.(T) -> Unit) {
                CallbackCallingDynamicTestsBuilder(aspect, callback).block(aspect)
            }
        }

        /**
         * Builds an arbitrary test trees to test all necessary aspect of the specified [subject].
         */
        fun <T> build(subject: T, init: DynamicTestsBuilder<T>.(T) -> Unit): List<DynamicNode> =
            mutableListOf<DynamicNode>().apply {
                CallbackCallingDynamicTestsBuilder(subject) { add(it) }.init(subject)
            }.toList()
    }
}

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
 * Creates a [DynamicTest] for each [T]—providing each test with a temporary work directory
 * that is automatically deletes after execution as the receiver object.
 *
 * The name for each test is heuristically derived but can also be explicitly specified using [testNamePattern]
 * which supports curly placeholders `{}` like [SLF4J] does.
 */
inline fun <reified T> Iterable<T>.testWithTempDir(uniqueId: UniqueId, testNamePattern: String? = null, crossinline executable: Path.(T) -> Unit) =
    testEach(testNamePattern) {
        test {
            withTempDir(uniqueId) {
                executable(it)
            }
        }
    }

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
) = toList().testEach(testNamePattern) {
    test {
        withTempDir(uniqueId) {
            executable(it)
        }
    }
}

