package koodies.test

import filepeek.LambdaBody
import koodies.Exceptions.ISE
import koodies.collections.asStream
import koodies.exception.toCompactString
import koodies.io.path.asPath
import koodies.io.path.readLine
import koodies.jvm.deleteOnExit
import koodies.logging.SLF4J
import koodies.regex.groupValue
import koodies.runtime.getCaller
import koodies.test.DeprecatedDynamicTestsBuilder.ExpectationBuilder
import koodies.test.DynamicTestBuilder.InCompleteExpectationBuilder
import koodies.test.TestFlattener.flatten
import koodies.test.Tester.aspect
import koodies.test.Tester.callerSource
import koodies.test.Tester.property
import koodies.test.Tester.subject
import koodies.text.ANSI.Text.Companion.ansi
import koodies.text.ANSI.ansiRemoved
import koodies.text.Semantics.Symbols
import koodies.text.TruncationStrategy
import koodies.text.takeUnlessBlank
import koodies.text.truncate
import koodies.text.withRandomSuffix
import koodies.text.wrap
import koodies.toBaseName
import koodies.toSimpleString
import org.junit.jupiter.api.DynamicContainer
import org.junit.jupiter.api.DynamicContainer.dynamicContainer
import org.junit.jupiter.api.DynamicNode
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.DynamicTest.dynamicTest
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode.SAME_THREAD
import strikt.api.Assertion
import strikt.api.Assertion.Builder
import strikt.api.expectCatching
import strikt.api.expectThat
import strikt.assertions.containsExactlyInAnyOrder
import strikt.assertions.isA
import strikt.assertions.isEqualTo
import strikt.assertions.isFailure
import strikt.assertions.isSuccess
import strikt.assertions.isTrue
import strikt.assertions.message
import strikt.assertions.size
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
import kotlin.streams.asSequence
import kotlin.system.exitProcess
import filepeek.FilePeek2 as FilePeek

/**
 * Workaround annotation for
 * [IDEA-265284: Add support for JUnit5 composed(meta) annotations annotated with `@ParametrizedTest`](https://youtrack.jetbrains.com/issue/IDEA-265284)
 */
typealias IdeaWorkaroundTest = Test

/**
 * Workaround annotation for
 * [IDEA-265284: Add support for JUnit5 composed(meta) annotations annotated with `@ParametrizedTest`](https://youtrack.jetbrains.com/issue/IDEA-265284)
 */
typealias IdeaWorkaroundTestFactory = TestFactory

private val root by lazy { deleteOnExit(createTempDirectory("koodies")) }

object Tester {

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
        return SLF4J.format(testNamePattern ?: fallbackPattern, *args).ansiRemoved
    }

    fun <T, R> String.aspect(subject: T, transform: (T) -> R, testNamePattern: String? = null): String {
        return "with".property(transform).run {
            kotlin.runCatching { subject(transform(subject), testNamePattern) }
                .getOrElse { this + "${Symbols.Error} $it" }
        }
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
            getLambdaBodyOrNull(callerClassName, callerMethodName)?.toSimpleString() ?: function.toSimpleString()
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
        "^$callerMethodName(?<arg>.+)$".toRegex().find(name)?.run { groupValue("arg")?.decapitalize() } ?: name

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
            cache.getOrPut(callerClassName) { FilePeek(listOfNotNull("strikt.internal", "strikt.api", "filepeek", enclosingClassName, callerClassName)) }
        }
    }

    /**
     * Finds the calling class and method name of any member
     * function of this [Tester].
     */
    private inline fun findCaller(): Pair<String, String> = getCaller {
        receiver == enclosingClassName
    }.run { (receiver?: error("Error finding caller: $this")) to function }

    /**
     * Reliably contains the fully qualified name of [Tester].
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
 * using the specified [DeprecatedDynamicTestsBuilder] based [init].
 */
@JvmName("testThis")
@DynamicTestsDsl
@Deprecated("replace with tests", ReplaceWith("tests(init)"))
 fun < T> T.test( init: DeprecatedDynamicTestsBuilder<T>.(T) -> Unit): List<DynamicNode> =
    DeprecatedDynamicTestsBuilder.build(this, init)

/**
 * Creates tests for the specified [subject]
 * using the specified [DeprecatedDynamicTestsBuilder] based [init].
 */
@DynamicTestsDsl
 fun < T> test(subject: T, init: DeprecatedDynamicTestsBuilder<T>.(T) -> Unit): List<DynamicNode> = subject.test(init)

/**
 * Creates one [DynamicContainer] for each instance of `this` collection of subjects
 * using the specified [DeprecatedDynamicTestsBuilder] based [init].
 *
 * The name for each container is heuristically derived but can also be explicitly specified using [containerNamePattern]
 * which supports curly placeholders `{}` like [SLF4J] does.
 */
@DynamicTestsDsl
 fun < T> Iterable<T>.testEach(
    containerNamePattern: String? = null,
     init: DeprecatedDynamicTestsBuilder<T>.(T) -> Unit,
): List<DynamicContainer> = toList()
    .also { require(it.isNotEmpty()) { "At least one subject must be provided for testing." } }
    .run { map { subject -> dynamicContainer("for".subject(subject, containerNamePattern), DeprecatedDynamicTestsBuilder.build(subject, init)) } }

/**
 * Creates one [DynamicContainer] for each instance of `this` collection of subjects
 * using the specified [DeprecatedDynamicTestsBuilder] based [init].
 *
 * The name for each container is heuristically derived but can also be explicitly specified using [containerNamePattern]
 * which supports curly placeholders `{}` like [SLF4J] does.
 */
@DynamicTestsDsl
 fun <T> Sequence<T>.testEach(
    containerNamePattern: String? = null,
 init: DeprecatedDynamicTestsBuilder<T>.(T) -> Unit,
): List<DynamicContainer> = toList().testEach(containerNamePattern, init)

/**
 * Creates one [DynamicContainer] for each instance of the specified [subjects]
 * using the specified [DeprecatedDynamicTestsBuilder] based [init].
 *
 * The name for each container is heuristically derived but can also be explicitly specified using [containerNamePattern]
 * which supports curly placeholders `{}` like [SLF4J] does.
 */
@DynamicTestsDsl
fun < T> testEach(
    vararg subjects: T,
    containerNamePattern: String? = null,
     init: DeprecatedDynamicTestsBuilder<T>.(T) -> Unit,
): List<DynamicContainer> = subjects.toList().testEach(containerNamePattern, init)

/**
 * Creates one [DynamicContainer] for each instance of the specified [subjects]
 * using the specified [DeprecatedDynamicTestsBuilder] based [init].
 *
 * The name for each container is heuristically derived but can also be explicitly specified using [containerNamePattern]
 * which supports curly placeholders `{}` like [SLF4J] does.
 */
@DynamicTestsDsl
@JvmName("testArrayAsReceiver")
fun < T> Array<T>.testEach(
    containerNamePattern: String? = null,
 init: DeprecatedDynamicTestsBuilder<T>.(T) -> Unit,
): List<DynamicContainer> = toList().testEach(containerNamePattern, init)

/**
 * Creates one [DynamicContainer] for each instance of the specified [subjects]
 * using the specified [DeprecatedDynamicTestsBuilder] based [init].
 *
 * The name for each container is heuristically derived but can also be explicitly specified using [containerNamePattern]
 * which supports curly placeholders `{}` like [SLF4J] does.
 */
@DynamicTestsDsl
 fun < K, V> Map<K, V>.testEach(
    containerNamePattern: String? = null,
 init: DeprecatedDynamicTestsBuilder<Pair<K, V>>.(Pair<K, V>) -> Unit,
): List<DynamicContainer> = toList().testEach(containerNamePattern, init)

@DynamicTestsDsl
fun <T> DeprecatedDynamicTestsBuilder.PropertyTestBuilder<T?>.notNull(block: DeprecatedDynamicTestsBuilder<T>.(T) -> Unit) =
    then {
        require(it != null)
        @Suppress("UNCHECKED_CAST")
        (this as DeprecatedDynamicTestsBuilder<T>).block(it)
    }

@DynamicTestsDsl
 inline fun <reified T> DeprecatedDynamicTestsBuilder.PropertyTestBuilder<Any?>.asA(crossinline block: DeprecatedDynamicTestsBuilder<T>.(T) -> Unit) =
    then {
        require(it is T)
        @Suppress("UNCHECKED_CAST")
        (this as DeprecatedDynamicTestsBuilder<T>).block(it as T)
    }

/**
 * Combines the [ExpectationBuilder.that] with the type assertion [isA].
 */
@DynamicTestsDsl
inline fun <reified T> ExpectationBuilder<*>.thatA(crossinline block: Builder<T>.() -> Unit) {
    isA<T>().block()
}


/**
 * Builder for arbitrary test trees consisting of instances of [DynamicContainer] and [DynamicTest]
 * and a fluent transition to [Strikt](https://strikt.io) assertions.
 */
@DynamicTestsDsl
// TODO refactor using BuilderTemplate
interface DeprecatedDynamicTestsBuilder<T> {

    @DslMarker
    annotation class ExpectationBuilderDsl

    /**
     * Builder for [Strikt](https://strikt.io) assertions.
     */
    @ExpectationBuilderDsl
    interface ExpectationBuilder<T> : Builder<T> {
        @ExpectationBuilderDsl
        fun that(block: Builder<T>.() -> Unit)
    }

    /**
     * Builder for testing a property of the test subject.
     */
    @DynamicTestsDsl
    interface PropertyTestBuilder<T> {
        @DynamicTestsDsl
        fun then(block: DeprecatedDynamicTestsBuilder<T>.(T) -> Unit): T
    }

    /**
     * Builds a [DynamicContainer] using the specified [name] and the
     * specified [DeprecatedDynamicTestsBuilder] based [init] to build the child nodes.
     */
    @DynamicTestsDsl
    fun group(name: String, init: DeprecatedDynamicTestsBuilder<T>.(T) -> Unit)

    /**
     * Builds a [DynamicTest] using an automatically derived name and the specified [executable].
     */
    @DynamicTestsDsl
    @Deprecated("use testv2")
    fun test(description: String? = null, executable: (T) -> Unit)

    fun test2(description: String? = null, exec: DynamicTestBuilder<T>.(T) -> Unit)

    infix fun <X> X.asserting( assertions:Builder<X>.()->Unit)

    fun asserting( assertions:Builder<T>.()->Unit)

    fun <X> expecting(description: String? = null, action: T.() -> X): InCompleteExpectationBuilder<X>
// TODO
//    inline fun <reified X> expectCatching(noinline action: suspend () -> X): InCompleteExpectationBuilder<Result<X>>
// TODO
//    inline fun <reified E : Throwable> expectThrows(noinline action: suspend () -> Any?): InCompleteExpectationBuilder<E>

    /**
     * Builds a new test tree testing the aspect returned by [transform].
     */
    @DynamicTestsDsl
    fun <R> aspect(transform: T.() -> R, init: DeprecatedDynamicTestsBuilder<R>.(R) -> Unit)

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
     * Builds a builder to specify expectations for the exception thrown when [transform]
     * is applied to `this` subject.
     */
    @DynamicTestsDsl
    fun <R> expectThrowing(description: String? = null, transform: T.() -> R): ExpectationBuilder<Result<R>>

    /**
     * Builds a new test tree testing the current subject.
     */
    @DynamicTestsDsl
    @Deprecated("use alternative")
    fun expect(description: String): ExpectationBuilder<T>

    /**
     * Builds a new test tree testing the current subject.
     */
    @DynamicTestsDsl
    @Deprecated("use alternative")
    val expect: ExpectationBuilder<T>

    companion object {

        private class CallbackCallingDeprecatedDynamicTestsBuilder<T>(private val subject: T, private val callback: (DynamicNode) -> Unit) :
            DeprecatedDynamicTestsBuilder<T> {

            override fun group(name: String, init: DeprecatedDynamicTestsBuilder<T>.(T) -> Unit) {
                callback(dynamicContainer(name, callerSource, build(subject, init).asStream()))
            }

            override fun test2(description: String?, exec: DynamicTestBuilder<T>.(T) -> Unit) {
                val test: DynamicTest = DynamicTestBuilder.buildTest(subject, description, exec)
                callback(test)
            }

            override infix fun <X> X.asserting( assertions: Builder<X>.()->Unit) {
                val test = DynamicTestBuilder.buildTest(this, null) { asserting(assertions) }
                callback(test)
            }

            override infix fun asserting( assertions: Builder<T>.()->Unit) {
                val test = DynamicTestBuilder.buildTest(subject, null) { asserting(assertions) }
                callback(test)
            }

            override fun <X> expecting(description: String? , action: T.() -> X): InCompleteExpectationBuilder<X> {
                var additionalAssertions: (Builder<X>.() -> Unit)? = null
                val test = DynamicTestBuilder.buildTest(subject, description) {
                    expecting(description) { subject.action() }.apply { additionalAssertions?.also{ that(it) }}
                }
                callback(test)
                return InCompleteExpectationBuilder { assertions: Builder<X>.() -> Unit ->
                    additionalAssertions=assertions
                }
            }

            override fun test(description: String?, executable: (T) -> Unit) {
                callback(dynamicTest(description?.takeUnlessBlank() ?: "test".property(executable), callerSource) { executable(subject) })
            }

            override fun <R> expect(description: String?, transform: T.() -> R): ExpectationBuilder<R> {
                val aspect = kotlin.runCatching { subject.transform() }
                    .onFailure { println("TEST ${Symbols.Error} Failed to evaluate ${"".subject(subject).property(transform)}: $it") }
                    .getOrThrow()
                return CallbackCallingExpectationBuilder(description?.takeUnlessBlank() ?: "expect".property(transform).subject(aspect), aspect, callback)
            }

            override fun <R> expectThrowing(description: String?, transform: T.() -> R): ExpectationBuilder<Result<R>> {
                val aspect = kotlin.runCatching { subject.transform() }
                return CallbackCallingExpectationBuilder(description?.takeUnlessBlank() ?: "expect".property(transform).subject(aspect), aspect, callback)
            }

            override fun expect(description: String): ExpectationBuilder<T> =
                CallbackCallingExpectationBuilder(description, subject, callback)

            override val expect: ExpectationBuilder<T>
                get() = CallbackCallingExpectationBuilder("expect".subject(subject), subject, callback)

            override fun <R> aspect(transform: T.() -> R, init: DeprecatedDynamicTestsBuilder<R>.(R) -> Unit) {
                val aspect = subject.transform()
                callback(dynamicContainer("with".property(transform).subject(aspect),
                    callerSource,
                    build(aspect, init).asStream()))
            }

            override fun <R> with(description: String?, transform: T.() -> R): PropertyTestBuilder<R> {
                val aspect = subject.transform()
                val nodes = mutableListOf<DynamicNode>()
                callback(dynamicContainer(description?.takeUnlessBlank() ?: "with".property(transform).subject(aspect), callerSource, nodes.asStream()))
                return CallbackCallingPropertyTestBuilder(aspect) { nodes.add(it) }
            }
        }

        private class CallbackCallingExpectationBuilder<T>(
            private val description: String,
            private val subject: T,
            private val callback: (DynamicNode) -> Unit,
        ) : ExpectationBuilder<T>, Builder<T> by expectThat(subject) {

            override fun that(block: Builder<T>.() -> Unit) {
                callback(dynamicTest(description.property(block), callerSource) { block() })
            }
        }

        private class CallbackCallingPropertyTestBuilder<T>(
            private val aspect: T,
            private val callback: (DynamicNode) -> Unit,
        ) : PropertyTestBuilder<T> {
            override fun then(block: DeprecatedDynamicTestsBuilder<T>.(T) -> Unit): T {
                CallbackCallingDeprecatedDynamicTestsBuilder(aspect, callback).block(aspect)
                return aspect
            }
        }

        /**
         * Builds an arbitrary test trees to test all necessary aspect of the specified [subject].
         */
        fun <T> build(subject: T, init: DeprecatedDynamicTestsBuilder<T>.(T) -> Unit): List<DynamicNode> =
            mutableListOf<DynamicNode>().apply {
                CallbackCallingDeprecatedDynamicTestsBuilder(subject) { add(it) }.init(subject)
            }.toList()
    }
}

/**
 * Run the given [assertions] on the provided [subject].
 *
 * The surrounding test subject is ignored.
 */
@Deprecated("replace with subject asserting assertions", ReplaceWith("subject asserting assertions"))
fun <T> DeprecatedDynamicTestsBuilder<*>.asserting(subject: T, assertions: Builder<T>.()->Unit) {
    with(subject){
        asserting(assertions)
    }
}

@DynamicTestsDsl
 fun tests( init: DynamicTestsBuilder.() -> Unit): List<DynamicNode> =
    DynamicTestsBuilder.build(init)

/**
 * Builder for tests (and test containers) supposed to simply write unit tests
 * in a more concise manner.
 *
 * - [test] is for plain unit tests
 * - [asserting] is for running [Strikt](https://strikt.io) assertions on a subject you already have
 * - [expecting] is for running [Strikt](https://strikt.io) assertions on a subject that needs to be provided first
 * - [expectCatching] is for running [Strikt](https://strikt.io) assertions on the result of an action that might throw an exception
 * - [expectThrows] is for running [Strikt](https://strikt.io) assertions on the exception thrown by an action
 */
@DynamicTestsDsl
// TODO test auto-generated names
class DynamicTestsBuilder(val tests: MutableList<DynamicNode>, val buildErrors: MutableList<String>) {

    // TODO
     fun < T : Any> T.testAll(description: String,  init: DynamicContainerBuilder<T>.(T) -> Unit) {
        val container = DynamicContainerBuilder.build(this, description, init)
        tests.add(container)
    }
// TODO
     infix fun < T : Any> T.testAll( init: DynamicContainerBuilder<T>.(T) -> Unit) {
        val container = DynamicContainerBuilder.build(this, null, init)
        tests.add(container)
    }

     fun < T : Any> T.test(description: String, init: DynamicTestBuilder<T>.(T) -> Unit) {
        val test = DynamicTestBuilder.buildTest(this, description, init)
        tests.add(test)
    }

     infix fun < T : Any> T.test( init: DynamicTestBuilder<T>.(T) -> Unit) {
        val test = DynamicTestBuilder.buildTest(this, null, init)
        tests.add(test)
    }

    /**
     * Evaluates a [block] of assertions over the current test [subject].
     */
    fun <T> asserting(description: String, subject: T, assertions: Builder<T>.()->Unit) {
        val test = DynamicTestBuilder.buildTest(subject, description) { asserting(assertions) }
        tests.add(test)
    }

    /**
     * Evaluates a [block] of assertions over the current test [subject].
     */
    infix fun <T> T.asserting( assertions: Builder<T>.()->Unit) {
        val test = DynamicTestBuilder.buildTest(this, null) { asserting(assertions) }
        tests.add(test)
    }


    /**
     * Returns a builder to specify expectations for the result of [transform] applied
     * to the current test [subject].
     */
    fun <T> expecting(description: String? = null, subject: () -> T): InCompleteExpectationBuilder<T> {
        val errorMessage = "expecting { … } call was not finished with that { … } at ${getCaller()}".also { buildErrors.add(it) }
        return InCompleteExpectationBuilder<T> { assertions: Builder<T>.() -> Unit ->
            buildErrors.remove(errorMessage)
            val test = DynamicTestBuilder.buildTest(subject, description) {
                expecting(description) { this() }.that(assertions)
            }
            tests.add(test)
        }
    }

    /**
     * Builds a builder to specify expectations for the [Result] returned
     * by [action] applied to `this` subject.
     */
    inline fun <reified T> expectCatching(noinline action: suspend () -> T): InCompleteExpectationBuilder<Result<T>> {
        val errorMessage = "expectCatching { … } call was not finished with that { … } at ${getCaller()}".also { buildErrors.add(it) }
        return InCompleteExpectationBuilder { assertions: Assertion.Builder<Result<T>>.() -> Unit ->
            buildErrors.remove(errorMessage)
            val test = DynamicTestBuilder.buildTest(action) {
                expectCatching { subject() }that (assertions)
            }
            tests.add(test)
        }
    }

    /**
     * Builds a builder to specify expectations for the exception thrown when [action]
     * is applied to `this` subject.
     */
    inline fun <reified E : Throwable> expectThrows(noinline action: suspend () -> Any?): InCompleteExpectationBuilder<E> {
        var additionalAssertions : (Builder<E>.() -> Unit)? = null
        val test = DynamicTestBuilder.buildTest(action) {
            expectThrows<E> { subject() }.apply { additionalAssertions?.also{ that(it) }}
        }
        tests.add(test)
        return InCompleteExpectationBuilder { assertions: Builder<E>.() -> Unit ->
            additionalAssertions = assertions
        }
    }

    companion object {
        inline fun build(init: DynamicTestsBuilder.() -> Unit): List<DynamicNode> {
                val tests = mutableListOf<DynamicNode>()
                val buildErrors = mutableListOf<String>()
                DynamicTestsBuilder(tests, buildErrors).init()
                    if (buildErrors.isNotEmpty()) throw ISE(buildErrors)
                return tests
        }
    }
}

@DynamicTestsDsl
class DynamicContainerBuilder<T>(val subject: T, val tests: MutableList<DynamicNode>) {

    /**
     * Builds a [DynamicContainer] using the specified [description] and the
     * specified [DynamicTestsBuilder] based [init] to build the child nodes.
     */
    fun group(description: String? = null, init: DynamicContainerBuilder<T>.(T) -> Unit): Unit {
        val container = build(subject, description, init)
        tests.add(container)
    }

    /**
     * Builds a [DynamicTest] using an automatically derived name and the specified [executable].
     */
    fun test(description: String? = null, exec: DynamicTestBuilder<T>.(T) -> Unit): Unit {
        val test = DynamicTestBuilder.buildTest(subject, description, exec)
        tests.add(test)
    }

    /**
     * Builds a new test tree testing the aspect returned by [transform].
     */
    fun <R> aspect(transform: T.() -> R, init: DynamicContainerBuilder<R>.(R) -> Unit): Unit {
        val aspect = subject.transform()
        val container = build(aspect, "with".property(transform).aspect(subject, transform), init)
        tests.add(container)
    }

    companion object {

        /**
         * Builds an arbitrary test trees to test all necessary aspect of the specified [subject].
         */
        fun <T> build(subject: T, description: String? = null, init: DynamicContainerBuilder<T>.(T) -> Unit): DynamicContainer {
            val tests = mutableListOf<DynamicNode>().also { DynamicContainerBuilder(subject, it).init(subject) }.toList()
            return dynamicContainer("".subject(subject), callerSource, tests.asStream())
        }
    }
}

@DynamicTestsDsl
class DynamicTestBuilder<T>(val subject: T, val buildErrors: MutableList<String>) {

    /**
     * Incomplete builder of an [Strikt](https://strikt.io) assertion
     * that makes a call to [onCompletion] as soon as building the expectation
     * is completed.
     *
     * If no callback took place until a certain moment in time
     * this [that] was never called.
     */
    class InCompleteExpectationBuilder<T>(val assertionBuilderProvider: (Builder<T>.() -> Unit) -> Unit) {
        infix fun that( assertions: Builder<T>.() -> Unit): Unit {
            assertionBuilderProvider(assertions)
        }
    }

    /**
     * Evaluates a [block] of assertions over the current test [subject].
     */
    fun asserting(block: Builder<T>.() -> Unit): Unit = expectThat(subject, block)

    /**
     * Returns a builder to specify expectations for the result of [transform] applied
     * to the current test [subject].
     */
     fun < R> expecting(description: String? = null,  transform: T.() -> R): InCompleteExpectationBuilder<R> {
        val errorMessage = "expecting { … } call was not finished with that { … } at ${getCaller()}".also { buildErrors.add(it) }
        return InCompleteExpectationBuilder { assertions: Builder<R>.() -> Unit ->
            buildErrors.remove(errorMessage)
            expectThat(subject).with(description?.takeUnlessBlank()?: "with".property(transform).aspect(subject, transform), transform, assertions            )
        }
    }

    /**
     * Builds a builder to specify expectations for the [Result] returned
     * by [action] applied to `this` subject.
     */
    inline fun <reified R> expectCatching(noinline action: suspend T.() -> R): InCompleteExpectationBuilder<Result<R>> {
        val errorMessage = "expectCatching { … } call was not finished with that { … } at ${getCaller()}".also { buildErrors.add(it) }
        return InCompleteExpectationBuilder { assertions: Builder<Result<R>>.() -> Unit ->
            buildErrors.remove(errorMessage)
            strikt.api.expectCatching { subject.action() }.assertions()
        }
    }

    /**
     * Builds a builder to specify expectations for the exception thrown when [action]
     * is applied to `this` subject.
     */
    inline fun <reified E : Throwable> expectThrows(noinline action: suspend T.() -> Any?): InCompleteExpectationBuilder<E> {
        val assertionBuilder: Builder<E> = strikt.api.expectThrows { subject.action() }
        return InCompleteExpectationBuilder<E> { assertions: Builder<E>.() -> Unit ->
            assertionBuilder.assertions()
        }
    }

    companion object {
        fun <T> buildTest(subject: T, description: String? = null, exec: DynamicTestBuilder<T>.(T) -> Unit): DynamicTest =
            dynamicTest(description?.takeUnlessBlank() ?: "test".subject(subject), callerSource) {
                val buildErrors = mutableListOf<String>()
                val result = kotlin.runCatching { DynamicTestBuilder(subject, buildErrors).exec(subject) }
                if (buildErrors.isNotEmpty()) throw ISE(buildErrors)
                result.getOrThrow()
            }
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
    val tempDir = root.resolve(uniqueId.simplified.toBaseName().withRandomSuffix()).createDirectories()
    tempDir.block()
    check(root.exists()) {
        println("The shared root temp directory was deleted by $uniqueId or a concurrently running test. This must not happen.".ansi.red.toString())
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
inline fun <reified T> Iterable<T>.testWithTempDir(
    uniqueId: UniqueId,
    testNamePattern: String? = null,
    crossinline executable: Path.(T) -> Unit,
) =
    testEach(testNamePattern) {
        test {
            withTempDir(uniqueId) {
                executable(it)
            }
        }
    }

object TestFlattener {

    fun Array<DynamicNode>.flatten(): Sequence<DynamicTest> = asSequence().flatten()
    fun Iterable<DynamicNode>.flatten(): Sequence<DynamicTest> = asSequence().flatten()
    fun Sequence<DynamicNode>.flatten(): Sequence<DynamicTest> = flatMap { it.flatten() }

    fun DynamicNode.flatten(): Sequence<DynamicTest> = when (this) {
        is DynamicContainer -> flatten()
        is DynamicTest -> flatten()
        else -> error("Unknown ${DynamicNode::class.simpleName} type ${this::class}")
    }

    fun DynamicTest.flatten(): Sequence<DynamicTest> = sequenceOf(this)

    fun DynamicContainer.flatten(): Sequence<DynamicTest> = children.asSequence().flatten()
}

/**
 * Runs all tests in `this` list of tests / test trees.
 */
fun List<DynamicNode>.execute(): Unit {
    flatten().forEach { test: DynamicTest -> test.execute() }
}

/**
 * Runs `this` test.
 */
fun DynamicTest.execute(): Unit {
    executable.execute()
}

@Execution(SAME_THREAD)
class TesterTest {

    @Nested
    inner class TestFlattening {

        @Test
        fun `should flatten`() {
            val tests = TestsSample().TestingSingleSubject().`as parameter`().flatten()
            expectThat(tests.toList()).size.isEqualTo(8)
        }
    }

    @Nested
    inner class TestLabelling {
        private operator fun String.not() = trimIndent()

        private val tests get() = TestsSample().TestingSingleSubject().`as parameter`()

        @TestFactory
        fun `↓ ↓ ↓ ↓ ↓ compare here | in test runner output ↓ ↓ ↓ ↓ ↓`() = tests

        val ESC = '\u001B'
        val String.green get() = "$ESC[1;31m$this"

        @Test
        fun `should label test automatically`() {
            val displayNames = tests.flatten().map { it.displayName }.toList()
            expectThat(displayNames).containsExactlyInAnyOrder(
                !"""
                    test (String) -> Unit
                """,
                !"""
                    test (String) -> Unit
                """,
                !"""
                    test (String) -> Unit
                """,
                !"""
                    test
                """,
                !"""
                    test (String) -> Unit
                """,
                !"""
                    expect String.() -> Int  ❮ 7 ❯   ❴ isGreaterThan(0) ❵ 
                """,
                !"""
                    expect  ❮ tcejbus ❯   ❴ not { isEqualTo("au … lly named test") } ❵ 
                """,
                !"""
                    expect String.() -> Int  ❮ 7 ❯   ❴ isGreaterThan(0).is … an(10)isEqualTo(7) ❵ 
                """,
            )
        }
    }

    @Nested
    inner class BuiltTest {

        @Test
        fun `should run asserting`() {
            var testDidRun = false
            val tests = test("root") {
                test2("regular test") {
                    asserting { testDidRun = true }
                }
            }
            tests.execute()
            expectThat(testDidRun).isTrue()
        }

        @Test
        fun `should run evaluating `() {
            var testDidRun = false
            val tests = test("root") {
                test2("regular test") {
                    expecting("expectation") { "test" }.that { testDidRun = true }
                }
            }
            tests.execute()
            expectThat(testDidRun).isTrue()
        }

        @Test
        fun `should throw on incomplete evaluating`() {
            val tests = test("root") {
                test2("regular test") {
                    expecting("expectation") { "test" }
                }
            }
            expectCatching { tests.execute() }
                .isFailure()
                .isA<IllegalStateException>()
                .message.toStringContains("not finished")
        }

        @Test
        fun `should run expectCatching`() {
            var testDidRun = false
            val tests = test("root") {
                test2("regular test") {
                    expectCatching { throw RuntimeException("wrong"); "dummy" }.that { testDidRun = true }
                }
            }
            tests.execute()
            expectThat(testDidRun).isTrue()
        }

        @Test
        fun `should throw on incomplete expectCatching`() {
            val tests = test("root") {
                test2("regular test") {
                    expectCatching<Any?> { throw RuntimeException("wrong") }
                }
            }
            expectCatching { tests.execute() }
                .isFailure()
                .isA<IllegalStateException>()
                .message.toStringContains("not finished")
        }

        @Test
        fun `should run expectThrows`() {
            var testDidRun = false
            val tests = test("root") {
                test2("regular test") {
                    expectThrows<RuntimeException> { throw RuntimeException("wrong") }.that { testDidRun = true }
                    "dummy"
                }
            }
            tests.execute()
            expectThat(testDidRun).isTrue()
        }

        @Test
        fun `should not throw on evaluating only throwable type`() {
            val tests = test("root") {
                test2("regular test") {
                    expectThrows<RuntimeException> { throw RuntimeException("wrong") }
                    "dummy"
                }
            }
            expectCatching { tests.execute() }.isSuccess()
        }
    }
}
