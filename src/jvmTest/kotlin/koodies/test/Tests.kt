package koodies.test

import filepeek.FileInfo
import filepeek.LambdaBody
import koodies.Exceptions.ISE
import koodies.collections.asStream
import koodies.exception.toCompactString
import koodies.io.path.asPath
import koodies.io.path.readLine
import koodies.jvm.deleteOnExit
import koodies.logging.SLF4J
import koodies.regex.groupValue
import koodies.runtime.CallStackElement
import koodies.runtime.getCaller
import koodies.test.DynamicTestBuilder.InCompleteExpectationBuilder
import koodies.test.TestFlattener.flatten
import koodies.test.Tester.assertingDisplayName
import koodies.test.Tester.callerSource
import koodies.test.Tester.catchingDisplayName
import koodies.test.Tester.displayName
import koodies.test.Tester.expectingDisplayName
import koodies.test.Tester.findCaller
import koodies.test.Tester.property
import koodies.test.Tester.throwingDisplayName
import koodies.text.ANSI.Text.Companion.ansi
import koodies.text.ANSI.ansiRemoved
import koodies.text.Semantics.Symbols
import koodies.text.Semantics.formattedAs
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
import strikt.api.Assertion.Builder
import strikt.api.expectCatching
import strikt.api.expectThat
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
import java.util.stream.Stream
import kotlin.concurrent.withLock
import kotlin.io.path.createDirectories
import kotlin.io.path.createTempDirectory
import kotlin.io.path.exists
import kotlin.reflect.KCallable
import kotlin.reflect.KFunction
import kotlin.reflect.KProperty
import kotlin.streams.asSequence
import kotlin.streams.asStream
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
     * Calculates the display name for a test with `this` subject.
     * and the optional [testNamePattern] which supports curly placeholders `{}` like [SLF4J] does.
     *
     * If no [testNamePattern] is specified a [displayNameFallback] is calculated heuristically.
     *
     * @see displayNameFallback
     */
    fun <T> T.displayName(testNamePattern: String? = null): String {
        val (fallbackPattern: String, args: Array<*>) = displayNameFallback(this)
        return SLF4J.format(testNamePattern ?: fallbackPattern, *args).ansiRemoved
    }

    /**
     * Attempts to calculate a display name for a test case testing the specified [subject].
     */
    private fun <T> displayNameFallback(subject: T): Pair<String, Array<String>> = when (subject) {
        is KFunction<*> -> "property $brackets" to arrayOf(subject.name)
        is Function<*> -> brackets to arrayOf(subject.toSimpleString())
        is KProperty<*> -> "property $brackets" to arrayOf(subject.name)
        is Triple<*, *, *> -> "$brackets to $brackets to $brackets" to arrayOf(subject.first.serialized, subject.second.serialized, subject.third.serialized)
        is Pair<*, *> -> "$brackets to $brackets" to arrayOf(subject.first.serialized, subject.second.serialized)
        else -> brackets to arrayOf(subject.serialized)
    }

    private const val brackets: String = "❮ {} ❯"
    private val <T> T?.serialized get() = toCompactString()

    /**
     * Attempts to calculate a rich display name for a property
     * expressed by the specified [fn].
     */
    fun <T, R> String.property(fn: T.() -> R): String = when (fn) {
        is KProperty<*> -> "$this value of property ${fn.name}"
        is KFunction<*> -> "$this return value of ${fn.name}"
        is KCallable<*> -> findCaller().run { "$this value of ${fn.getPropertyName(function)}" }
        else -> "$this " + findCaller().run {
            getLambdaBodyOrNull(receiver ?: error("Missing receiver"), function)?.wrap(" ❴ ", " ❵ ") ?: fn.toSimpleString()
        }
    }

    /**
     * Returns the name used to display [transform] applied to [subject].
     *
     * If an exception is thrown while computing the display name, the exception is returned instead.
     */
    private fun <T, R> valueOf(subject: T, transform: (T) -> R): String =
        kotlin.runCatching { transform(subject).toCompactString() }.getOrElse { "${Symbols.Error} ${it.toCompactString()}" }

    /**
     * Returns the name used to display the value returned by [provideSubject].
     *
     * If an exception is thrown while computing the display name, the exception is returned instead.
     */
    private fun <R> valueOf(provideSubject: () -> R): String =
        kotlin.runCatching { provideSubject().toCompactString() }.getOrElse { "${Symbols.Error} ${it.toCompactString()}" }

    /**
     * Returns the display name for an asserting test.
     */
    fun <T> CallStackElement.assertingDisplayName(assertions: Builder<T>.() -> Unit): String =
        StringBuilder("❕").apply {
            append(" ")
            append(this@assertingDisplayName.displayName(assertions))
        }.toString()

    /**
     * Returns the display name for an [subject] asserting test.
     */
    fun <T> CallStackElement.assertingDisplayName(subject: T, assertions: Builder<T>.() -> Unit): String =
        StringBuilder("❕").apply {
            append(" ")
            append(subject.displayName())
            append(" ")
            append(this@assertingDisplayName.displayName(assertions))
        }.toString()

    /**
     * Returns the display name for an [subject] expecting test.
     */
    fun <T, R> CallStackElement.expectingDisplayName(subject: T, transform: (T) -> R): String =
        this.displayName("❔", subject, transform)

    /**
     * Returns the display name for an [subject] catching test.
     */
    fun <T, R> CallStackElement.catchingDisplayName(subject: T, transform: (T) -> R): String =
        this.displayName("❓", subject, transform)

    /**
     * Returns the display name for an [E] throwing test.
     */
    inline fun <reified E> throwingDisplayName(): String =
        StringBuilder("❗").apply {
            append(" ")
            append(E::class.simpleName)
        }.toString()

    /**
     * Returns the display name for a test involving [transform] applied to [subject].
     */
    private fun <T, R> CallStackElement.displayName(symbol: String, subject: T, transform: (T) -> R): String =
        StringBuilder(symbol).apply {
            append(" ")
            append(this@displayName.displayName(transform))
            append(" (")
            append(valueOf(subject, transform))
            append(")")
            val that = getLambdaBodyOrNull(this@displayName, "that")
            if (that != null) {
                append(" ")
                append(that)
            }
        }.toString()

    /**
     * Returns the display name for a test involving the subject returned by [provideSubject].
     */
    fun <R> CallStackElement.expectingDisplayName(provideSubject: () -> R): String =
        displayName("❔", provideSubject)

    /**
     * Returns the display name for a test involving an eventually thrown exception
     * by [provideSubject].
     */
    fun <R> CallStackElement.catchingDisplayName(provideSubject: () -> R): String =
        displayName("❓", provideSubject)

    /**
     * Returns the display name for a test involving the subject returned by [provide].
     */
    private fun <R> CallStackElement.displayName(symbol: String, provide: () -> R): String =
        StringBuilder(symbol).apply {
            append(" ")
            append(this@displayName.displayName(provide, null).displayName())
            append(" (")
            append(valueOf(provide))
            append(")")
            val that = getLambdaBodyOrNull(this@displayName, "that")
            if (that != null) {
                append(" ")
                append(that)
            }
        }.toString()

    /**
     * Attempts to calculate a rich display name for a property
     * expressed by the specified [fn].
     */
    fun <T, R> CallStackElement.displayName(fn: T.() -> R, fnName: String? = null): String {
        return when (fn) {
            is KProperty<*> -> fn.name
            is KFunction<*> -> fn.name
            is KCallable<*> -> run { fn.getPropertyName(function) }
            else -> getLambdaBodyOrNull(this, fnName) ?: fn.toSimpleString()
        }
    }

    /**
     * Attempts to calculate a rich display name for a property
     * expressed by the specified [fn].
     */
    fun <R> CallStackElement.displayName(fn: () -> R, fnName: String? = null): String {
        return when (fn) {
            is KProperty<*> -> fn.name
            is KFunction<*> -> fn.name
            is KCallable<*> -> run { fn.getPropertyName(function) }
            else -> getLambdaBodyOrNull(this, fnName) ?: fn.toSimpleString()
        }
    }

    val callerSource: URI
        get() = findCaller().let {
            FilePeek(it.stackTraceElement).getCallerFileInfo().run {
                val sourceFile = sourceFileName.asPath()
                val columnNumber = sourceFile.findMethodCallColumn(lineNumber, it.function)
                sourceFile.asSourceUri(lineNumber, columnNumber)
            }
        }

    val CallStackElement.callerSource: URI
        get() = FilePeek(stackTraceElement).getCallerFileInfo().run {
            val sourceFile = sourceFileName.asPath()
            val columnNumber = sourceFile.findMethodCallColumn(lineNumber, function)
            sourceFile.asSourceUri(lineNumber, columnNumber)
        }

    private fun Path.findMethodCallColumn(lineNumber: Int, callerMethodName: String): Int? =
        readLine(lineNumber)?.let { line -> line.indexOf(callerMethodName).takeIf { it > 0 } }

    private fun Path.asSourceUri(lineNumber: Int, columnNumber: Int?) =
        StringBuilder(toUri().toString()).run {
            append("?line=$lineNumber")
            columnNumber?.let { append("&column=$it") }
            URI(toString())
        }

    private fun KCallable<*>.getPropertyName(callerMethodName: String): String =
        "^$callerMethodName(?<arg>.+)$".toRegex().find(name)?.run { groupValue("arg")?.decapitalize() } ?: name

    /**
     * Uses a dynamically provided and cached [FilePeek] instance to
     * retrieve the body of the lambda that was passed as a parameter
     * to a call of a method with name [callerMethodName] of a class
     * with name [callerClassName].
     */
    private fun getLambdaBodyOrNull(callerClassName: String, callerMethodName: String) = kotlin.runCatching {
        val line = filePeek(callerClassName, callerMethodName).line
        LambdaBody(callerMethodName, line).body.trim().truncate(40, TruncationStrategy.MIDDLE, " … ")
    }.getOrNull()

    private fun getLambdaBodyOrNull(
        callStackElement: CallStackElement,
        explicitMethodName: String? = null,
    ) = kotlin.runCatching {
        val line = FilePeek(callStackElement.stackTraceElement).getCallerFileInfo().line
        if (explicitMethodName != null) {
            line.takeIf { it.contains(explicitMethodName) }?.let {
                LambdaBody(explicitMethodName, it).body.trim().truncate(40, TruncationStrategy.MIDDLE, " … ")
            }
        } else {
            LambdaBody(callStackElement.function, line).body.trim().truncate(40, TruncationStrategy.MIDDLE, " … ")
        }
    }.getOrNull()

    private val CallStackElement.stackTraceElement get() = StackTraceElement(receiver, function, file, line)

    /**
     * A lambda that returns a [FilePeek] to access the body of a lambda
     * that was passed as a parameter to a call of a method of a class
     * with the specified name.
     */
    private val filePeek = object : (String, String) -> FileInfo {
        private val lock = ReentrantLock()
        private val cache: MutableMap<Pair<String, String>, FileInfo> = mutableMapOf()
        override fun invoke(callerClassName: String, callerMethodName: String): FileInfo = lock.withLock {
            cache.getOrPut(callerClassName to callerMethodName) {
                FilePeek(callerClassName, callerMethodName).getCallerFileInfo()
            }
        }
    }

    /**
     * Finds the calling class and method name of any member
     * function of this [Tester].
     */
    fun findCaller(): CallStackElement = getCaller {
        receiver == enclosingClassName || receiver?.matches(Regex(".*DynamicTest.*Builder.*")) == true
    }

    /**
     * Reliably contains the fully qualified name of [Tester].
     * The result is still correct after typical refactorings like renaming.
     */
    private val enclosingClassName = this::class.qualifiedName
        ?.let { if (this::class.isCompanion) it.substringBeforeLast(".") else it }
        ?: error("unknown name")
}

class IllegalUsageException(caller: URI?) : IllegalArgumentException(
    "expecting { … } call was not finished with ${"that { … }".formattedAs.input}".let {
        caller?.let { uri -> "$it at " + uri.path + ":" + uri.query.takeLastWhile { it.isDigit() } } ?: it
    }
)


@DslMarker
annotation class DynamicTestsDsl

/**
 * Creates tests for `this` subject
 * using the specified [DynamicTestsWithSubjectBuilder] based [init].
 */
@JvmName("testThis")
@DynamicTestsDsl
@Deprecated("replace with tests")
fun <T> T.test(init: DynamicTestsWithSubjectBuilder<T>.(T) -> Unit): List<DynamicNode> =
    DynamicTestsWithSubjectBuilder.build(this, init)

/**
 * Creates tests for the specified [subject]
 * using the specified [DynamicTestsWithSubjectBuilder] based [init].
 */
@DynamicTestsDsl
fun <T> test(subject: T, init: DynamicTestsWithSubjectBuilder<T>.(T) -> Unit): List<DynamicNode> = subject.test(init)

/**
 * Creates one [DynamicContainer] for each instance of `this` collection of subjects
 * using the specified [DynamicTestsWithSubjectBuilder] based [init].
 *
 * The name for each container is heuristically derived but can also be explicitly specified using [containerNamePattern]
 * which supports curly placeholders `{}` like [SLF4J] does.
 */
@DynamicTestsDsl
fun <T> Iterable<T>.testEach(
    containerNamePattern: String? = null,
    init: DynamicTestsWithSubjectBuilder<T>.(T) -> Unit,
): List<DynamicContainer> = toList()
    .also { require(it.isNotEmpty()) { "At least one subject must be provided for testing." } }
    .run { map { subject -> dynamicContainer("for ${subject.displayName(containerNamePattern)}", DynamicTestsWithSubjectBuilder.build(subject, init)) } }

/**
 * Creates one [DynamicContainer] for each instance of `this` collection of subjects
 * using the specified [DynamicTestsWithSubjectBuilder] based [init].
 *
 * The name for each container is heuristically derived but can also be explicitly specified using [containerNamePattern]
 * which supports curly placeholders `{}` like [SLF4J] does.
 */
@DynamicTestsDsl
fun <T> Sequence<T>.testEach(
    containerNamePattern: String? = null,
    init: DynamicTestsWithSubjectBuilder<T>.(T) -> Unit,
): List<DynamicContainer> = toList().testEach(containerNamePattern, init)

/**
 * Creates one [DynamicContainer] for each instance of the specified [subjects]
 * using the specified [DynamicTestsWithSubjectBuilder] based [init].
 *
 * The name for each container is heuristically derived but can also be explicitly specified using [containerNamePattern]
 * which supports curly placeholders `{}` like [SLF4J] does.
 */
@DynamicTestsDsl
fun <T> testEach(
    vararg subjects: T,
    containerNamePattern: String? = null,
    init: DynamicTestsWithSubjectBuilder<T>.(T) -> Unit,
): List<DynamicContainer> = subjects.toList().testEach(containerNamePattern, init)

/**
 * Creates one [DynamicContainer] for each element of `this` array
 * using the specified [DynamicTestsWithSubjectBuilder] based [init].
 *
 * The name for each container is heuristically derived but can also be explicitly specified using [containerNamePattern]
 * which supports curly placeholders `{}` like [SLF4J] does.
 */
@DynamicTestsDsl
@JvmName("testArrayAsReceiver")
fun <T> Array<T>.testEach(
    containerNamePattern: String? = null,
    init: DynamicTestsWithSubjectBuilder<T>.(T) -> Unit,
): List<DynamicContainer> = toList().testEach(containerNamePattern, init)

/**
 * Creates one [DynamicContainer] for each key-value pair of `this` map
 * using the specified [DynamicTestsWithSubjectBuilder] based [init].
 *
 * The name for each container is heuristically derived but can also be explicitly specified using [containerNamePattern]
 * which supports curly placeholders `{}` like [SLF4J] does.
 */
@DynamicTestsDsl
fun <K, V> Map<K, V>.testEach(
    containerNamePattern: String? = null,
    init: DynamicTestsWithSubjectBuilder<Pair<K, V>>.(Pair<K, V>) -> Unit,
): List<DynamicContainer> = toList().testEach(containerNamePattern, init)

/**
 * Builder for arbitrary test trees consisting of instances of [DynamicContainer] and [DynamicTest]
 * and a fluent transition to [Strikt](https://strikt.io) assertions.
 */
@DynamicTestsDsl
class DynamicTestsWithSubjectBuilder<T>(val subject: T, val callback: (DynamicNode) -> Unit) {

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
        infix fun then(block: DynamicTestsWithSubjectBuilder<T>.(T) -> Unit): T
    }

    /**
     * Builds a [DynamicContainer] using the specified [name] and the
     * specified [DynamicTestsWithSubjectBuilder] based [init] to build the child nodes.
     */
    @DynamicTestsDsl
    fun group(name: String, init: DynamicTestsWithSubjectBuilder<T>.(T) -> Unit) {
        callback(dynamicContainer(name, callerSource, build(subject, init).asStream()))
    }

    /**
     * Builds a [DynamicTest] using an automatically derived name and the specified [executable].
     */
    @DynamicTestsDsl
    @Deprecated("use expecting")
    fun test(description: String? = null, executable: (T) -> Unit) {
        callback(dynamicTest(description?.takeUnlessBlank() ?: "test".property(executable), callerSource) { executable(subject) })
    }

    infix fun <T> T.asserting(assertions: Builder<T>.() -> Unit) {
        val caller = findCaller()
        val test = dynamicTest(findCaller().assertingDisplayName(this, assertions), caller.callerSource) {
            expectThat(this, assertions)
        }
        callback(test)
    }

    infix fun asserting(assertions: Builder<T>.() -> Unit) {
        val caller = findCaller()
        val test = dynamicTest(findCaller().assertingDisplayName(assertions), caller.callerSource) {
            expectThat(subject, assertions)
        }
        callback(test)
    }

    fun <R> expecting(description: String? = null, action: T.() -> R): InCompleteExpectationBuilder<R> {
        var additionalAssertions: (Builder<R>.() -> Unit)? = null
        val caller = findCaller()
        val test = dynamicTest(description ?: caller.expectingDisplayName(subject, action), caller.callerSource) {
            expectThat(subject).with(action, additionalAssertions ?: throw IllegalUsageException(caller.callerSource))
        }
        callback(test)
        return InCompleteExpectationBuilder { assertions: Builder<R>.() -> Unit ->
            additionalAssertions = assertions
        }
    }

    fun <R> expectCatching(action: T.() -> R): InCompleteExpectationBuilder<Result<R>> {
        var additionalAssertions: (Builder<Result<R>>.() -> Unit)? = null
        val caller = findCaller()
        val test = dynamicTest(findCaller().catchingDisplayName(subject, action), caller.callerSource) {
            strikt.api.expectCatching { subject.action() }.and(additionalAssertions ?: {})
        }
        callback(test)
        return InCompleteExpectationBuilder { assertions: Builder<Result<R>>.() -> Unit ->
            additionalAssertions = assertions
        }
    }

    inline fun <reified E : Throwable> expectThrows(noinline action: T.() -> Any?): InCompleteExpectationBuilder<E> {
        var additionalAssertions: (Builder<E>.() -> Unit)? = null
        val caller = findCaller()
        val test = dynamicTest(throwingDisplayName<E>(), caller.callerSource) {
            strikt.api.expectThrows<E> { subject.action() }.and(additionalAssertions ?: {})
        }
        callback(test)
        return InCompleteExpectationBuilder { assertions: Builder<E>.() -> Unit ->
            additionalAssertions = assertions
        }
    }

    /**
     * Builds a new test tree testing the aspect returned by [transform].
     */
    @DynamicTestsDsl
    fun <R> with(description: String? = null, transform: T.() -> R): PropertyTestBuilder<R> {
        val aspect = subject.transform()
        val nodes = mutableListOf<DynamicNode>()
        callback(dynamicContainer(description?.takeUnlessBlank() ?: "with".property(transform) + " " + aspect.displayName(), callerSource, nodes.asStream()))
        return CallbackCallingPropertyTestBuilder(aspect) { nodes.add(it) }
    }

    /**
     * Returns a builder to specify expectations for the result of [transform] applied
     * to `this` subject.
     */
    @DynamicTestsDsl
    @Deprecated("use expecting")
    fun <R> expect(description: String? = null, transform: T.() -> R): ExpectationBuilder<R> {
        val aspect = kotlin.runCatching { subject.transform() }
            .onFailure { println("TEST ${Symbols.Error} Failed to evaluate ${subject.displayName().property(transform)}: $it") }
            .getOrThrow()
        return CallbackCallingExpectationBuilder(description?.takeUnlessBlank() ?: "expect".property(transform) + " " + aspect.displayName(), aspect, callback)
    }

    companion object {

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
            override fun then(block: DynamicTestsWithSubjectBuilder<T>.(T) -> Unit): T {
                DynamicTestsWithSubjectBuilder(aspect, callback).block(aspect)
                return aspect
            }
        }

        /**
         * Builds an arbitrary test trees to test all necessary aspect of the specified [subject].
         */
        fun <T> build(subject: T, init: DynamicTestsWithSubjectBuilder<T>.(T) -> Unit): List<DynamicNode> =
            mutableListOf<DynamicNode>().apply {
                DynamicTestsWithSubjectBuilder(subject) { add(it) }.init(subject)
            }.toList()
    }
}

/**
 * Run the given [assertions] on the provided [subject].
 *
 * The surrounding test subject is ignored.
 */
@Deprecated("replace with subject asserting assertions", ReplaceWith("subject asserting assertions"))
fun <T> DynamicTestsWithSubjectBuilder<*>.asserting(subject: T, assertions: Builder<T>.() -> Unit) {
    with(subject) {
        asserting(assertions)
    }
}

@DynamicTestsDsl
fun tests(init: DynamicTestsWithoutSubjectBuilder.() -> Unit): List<DynamicNode> =
    DynamicTestsWithoutSubjectBuilder.build(init)

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
class DynamicTestsWithoutSubjectBuilder(val tests: MutableList<DynamicNode>) {

    fun <T : Any> T.all(description: String, init: DynamicTestsWithSubjectBuilder<T>.(T) -> Unit) {
        val container = dynamicContainer(description, callerSource, DynamicTestsWithSubjectBuilder.build(this, init).asStream())
        tests.add(container)
    }

    infix fun <T : Any> T.all(init: DynamicTestsWithSubjectBuilder<T>.(T) -> Unit) {
        val container = dynamicContainer(this.displayName(), callerSource, DynamicTestsWithSubjectBuilder.build(this, init).asStream())
        tests.add(container)
    }

    fun <T : Any> T.test(description: String, init: DynamicTestBuilder<T>.(T) -> Unit) {
        val test = DynamicTestBuilder.buildTest(this, description, init)
        tests.add(test)
    }

    infix fun <T : Any> T.test(init: DynamicTestBuilder<T>.(T) -> Unit) {
        val test = DynamicTestBuilder.buildTest(this, null, init)
        tests.add(test)
    }

    @JvmName("infixAsserting")
    infix fun <T> T.asserting(assertions: Builder<T>.() -> Unit) {
        val caller = findCaller()
        val test = dynamicTest(findCaller().assertingDisplayName(this, assertions), caller.callerSource) {
            expectThat(this, assertions)
        }
        tests.add(test)
    }

    fun <T> asserting(subject: T, assertions: Builder<T>.() -> Unit) {
        val caller = findCaller()
        val test = dynamicTest(findCaller().assertingDisplayName(assertions), caller.callerSource) {
            expectThat(subject, assertions)
        }
        tests.add(test)
    }

    fun <R> expecting(description: String? = null, action: () -> R): InCompleteExpectationBuilder<R> {
        var additionalAssertions: (Builder<R>.() -> Unit)? = null
        val caller = findCaller()
        val test = dynamicTest(description ?: caller.expectingDisplayName(action), caller.callerSource) {
            expectThat(action(), additionalAssertions ?: throw IllegalUsageException(caller.callerSource))
        }
        tests.add(test)
        return InCompleteExpectationBuilder { assertions: Builder<R>.() -> Unit ->
            additionalAssertions = assertions
        }
    }

    fun <R> expectCatching(action: () -> R): InCompleteExpectationBuilder<Result<R>> {
        var additionalAssertions: (Builder<Result<R>>.() -> Unit)? = null
        val caller = findCaller()
        val test = dynamicTest(findCaller().catchingDisplayName(action), caller.callerSource) {
            strikt.api.expectCatching { action() }.and(additionalAssertions ?: {})
        }
        tests.add(test)
        return InCompleteExpectationBuilder { assertions: Builder<Result<R>>.() -> Unit ->
            additionalAssertions = assertions
        }
    }

    inline fun <reified E : Throwable> expectThrows(noinline action: () -> Any?): InCompleteExpectationBuilder<E> {
        var additionalAssertions: (Builder<E>.() -> Unit)? = null
        val caller = findCaller()
        val test = dynamicTest(throwingDisplayName<E>(), caller.callerSource) {
            strikt.api.expectThrows<E> { action() }.and(additionalAssertions ?: {})
        }
        tests.add(test)
        return InCompleteExpectationBuilder { assertions: Builder<E>.() -> Unit ->
            additionalAssertions = assertions
        }
    }

    companion object {
        inline fun build(init: DynamicTestsWithoutSubjectBuilder.() -> Unit): List<DynamicNode> =
            mutableListOf<DynamicNode>().also { DynamicTestsWithoutSubjectBuilder(it).init() }
    }
}

@DynamicTestsDsl
class DynamicTestBuilder<T>(val subject: T, private val buildErrors: MutableList<String>) {

    /**
     * Incomplete builder of an [Strikt](https://strikt.io) assertion
     * that makes a call to [onCompletion] as soon as building the expectation
     * is completed.
     *
     * If no callback took place until a certain moment in time
     * this [that] was never called.
     */
    class InCompleteExpectationBuilder<T>(val assertionBuilderProvider: (Builder<T>.() -> Unit) -> Unit) {
        infix fun that(assertions: Builder<T>.() -> Unit) {
            assertionBuilderProvider(assertions)
        }
    }

    /**
     * Returns a builder to specify expectations for the result of [transform] applied
     * to the current test [assertingDisplayName].
     */
    fun <R> expecting(description: String? = null, transform: T.() -> R): InCompleteExpectationBuilder<R> {
        val errorMessage = "expecting { … } call was not finished with that { … } at ${getCaller()}".also { buildErrors.add(it) }
        return InCompleteExpectationBuilder { assertions: Builder<R>.() -> Unit ->
            buildErrors.remove(errorMessage)
            expectThat(subject).with(description?.takeUnlessBlank() ?: "with".property(transform) + findCaller().expectingDisplayName(subject,
                transform),
                transform,
                assertions)
        }
    }

    /**
     * Builds a builder to specify expectations for the [Result] returned
     * by [action] applied to `this` subject.
     */
    fun <R> expectCatching(action: suspend T.() -> R): InCompleteExpectationBuilder<Result<R>> {
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
        return InCompleteExpectationBuilder { assertions: Builder<E>.() -> Unit ->
            assertionBuilder.assertions()
        }
    }

    companion object {
        fun <T> buildTest(subject: T, description: String? = null, exec: DynamicTestBuilder<T>.(T) -> Unit): DynamicTest =
            dynamicTest(description?.takeUnlessBlank() ?: "test " + subject.displayName(), callerSource) {
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
fun <T> Iterable<T>.testWithTempDir(
    uniqueId: UniqueId,
    testNamePattern: String? = null,
    executable: Path.(T) -> Unit,
) = testEach(testNamePattern) {
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

    private fun DynamicNode.flatten(): Sequence<DynamicTest> = when (this) {
        is DynamicContainer -> flatten()
        is DynamicTest -> flatten()
        else -> error("Unknown ${DynamicNode::class.simpleName} type ${this::class}")
    }

    private fun DynamicTest.flatten(): Sequence<DynamicTest> = sequenceOf(this)

    private fun DynamicContainer.flatten(): Sequence<DynamicTest> = children.asSequence().flatten()
}

/**
 * Runs all tests in `this` list of tests / test trees.
 */
fun List<DynamicNode>.execute() {
    flatten().forEach { test: DynamicTest -> test.execute() }
}

/**
 * Runs `this` test.
 */
fun DynamicTest.execute() {
    executable.execute()
}

@Execution(SAME_THREAD)
class TesterTest {

    @Nested
    inner class TestFlattening {

        @Test
        fun `should flatten`() {
            val tests = TestsSample().TestingSingleSubject().`as parameter`().flatten()
            expectThat(tests.toList()).size.isEqualTo(19)
        }
    }

    @Nested
    inner class TestLabelling {
        private operator fun String.not() = trimIndent()

        private val testsWithSubject = TestsSample().`testing with subject`()

        @Suppress("DANGEROUS_CHARACTERS", "NonAsciiCharacters")
        @TestFactory
        fun `↓ ↓ ↓ ↓ ↓ tests with subject | compare here | in test runner output ↓ ↓ ↓ ↓ ↓`() = testsWithSubject

        @TestFactory
        fun `should label test with subject automatically`(): Stream<DynamicTest> = testsWithSubject.flatten().map { it.displayName }.zip(sequenceOf(
            !"""
                ❕ ❮ other ❯ isEqualTo("other")
            """,
            !"""
                ❕ isEqualTo("subject")
            """,
            !"""
                ❔ length (7) isGreaterThan(5)
            """,
            !"""
                ❔ length (7)
            """,
            !"""
                ❓ length (7)
            """,
            !"""
                ❓ length (7) isSuccess()
            """,
            !"""
                ❗ RuntimeException
            """,
            !"""
                ❗ RuntimeException
            """,
        )).map { (label, expected) -> dynamicTest("👆 $expected") { expectThat(label).isEqualTo(expected) } }.asStream()


        private val testsWithoutSubject = TestsSample().`testing without subject`()

        @Suppress("DANGEROUS_CHARACTERS", "NonAsciiCharacters")
        @TestFactory
        fun `↓ ↓ ↓ ↓ ↓ tests without subject | compare here | in test runner output ↓ ↓ ↓ ↓ ↓`() = testsWithoutSubject

        @TestFactory
        fun `should label test without subject automatically`(): Stream<DynamicTest> = testsWithoutSubject.flatten().map { it.displayName }.zip(sequenceOf(
            !"""
                ❕ ❮ other ❯ isEqualTo("other")
            """,
            !"""
                ❕ isEqualTo("subject")
            """,
            !"""
                ❔ ❮ "subject".length ❯ (7) isGreaterThan(5)
            """,
            !"""
                ❔ ❮ "subject".length ❯ (7)
            """,
            !"""
                ❓ ❮ "subject".length ❯ (7)
            """,
            !"""
                ❓ ❮ "subject".length ❯ (7) isSuccess()
            """,
            !"""
                ❗ RuntimeException
            """,
            !"""
                ❗ RuntimeException
            """,
        )).map { (label, expected) -> dynamicTest("👆 $expected") { expectThat(label).isEqualTo(expected) } }.asStream()
    }

    @Nested
    inner class BuiltTest {

        @Test
        fun `should run asserting`() {
            var testDidRun = false
            val tests = test("root") {
                asserting { testDidRun = true }
            }
            tests.execute()
            expectThat(testDidRun).isTrue()
        }

        @Test
        fun `should run evaluating `() {
            var testDidRun = false
            val tests = test("root") {
                expecting("expectation") { "test" } that { testDidRun = true }
            }
            tests.execute()
            expectThat(testDidRun).isTrue()
        }

        @Test
        fun `should throw on incomplete evaluating`() {
            val tests = test("root") {
                expecting("expectation") { "test" }
            }
            expectCatching { tests.execute() }
                .isFailure()
                .isA<IllegalUsageException>()
                .message.toStringContains("not finished")
        }

        @Test
        fun `should run expectCatching`() {
            var testDidRun = false
            val tests = test("root") {
                expectCatching { throw RuntimeException("wrong") } that { testDidRun = true }
            }
            tests.execute()
            expectThat(testDidRun).isTrue()
        }

        @Test
        fun `should not throw on incomplete expectCatching`() {
            val tests = test("root") {
                expectCatching<Any?> { throw RuntimeException("wrong") }
            }
            expectCatching { tests.execute() }.isSuccess()
        }

        @Test
        fun `should run expectThrows`() {
            var testDidRun = false
            val tests = test("root") {
                expectThrows<RuntimeException> { throw RuntimeException("wrong") } that { testDidRun = true }
            }
            tests.execute()
            expectThat(testDidRun).isTrue()
        }

        @Test
        fun `should not throw on evaluating only throwable type`() {
            val tests = test("root") {
                expectThrows<RuntimeException> { throw RuntimeException("wrong") }
            }
            expectCatching { tests.execute() }.isSuccess()
        }
    }
}
