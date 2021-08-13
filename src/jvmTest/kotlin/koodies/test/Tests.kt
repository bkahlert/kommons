package koodies.test

import filepeek.LambdaBody
import koodies.Exceptions.ISE
import koodies.TestKoodies
import koodies.collections.asStream
import koodies.debug.trace
import koodies.exception.toCompactString
import koodies.io.path.asPath
import koodies.io.path.readLine
import koodies.regex.groupValue
import koodies.runtime.CallStackElement
import koodies.runtime.currentStackTrace
import koodies.runtime.getCaller
import koodies.runtime.isDebugging
import koodies.test.DynamicTestBuilder.InCompleteExpectationBuilder
import koodies.test.IllegalUsageCheck.ExpectIllegalUsageException
import koodies.test.TestFlattener.flatten
import koodies.test.Tester.assertingDisplayName
import koodies.test.Tester.callerSource
import koodies.test.Tester.catchingDisplayName
import koodies.test.Tester.displayName
import koodies.test.Tester.expectingDisplayName
import koodies.test.Tester.findCaller
import koodies.test.Tester.property
import koodies.test.Tester.throwingDisplayName
import koodies.test.junit.UniqueId
import koodies.test.junit.UniqueId.Companion.id
import koodies.text.ANSI.Text.Companion.ansi
import koodies.text.ANSI.ansiRemoved
import koodies.text.Semantics.formattedAs
import koodies.text.decapitalize
import koodies.text.takeUnlessBlank
import koodies.text.truncate
import koodies.text.withRandomSuffix
import koodies.text.wrap
import koodies.toBaseName
import koodies.toSimpleString
import koodies.tracing.rendering.SLF4J
import org.junit.jupiter.api.DynamicContainer
import org.junit.jupiter.api.DynamicContainer.dynamicContainer
import org.junit.jupiter.api.DynamicNode
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.DynamicTest.dynamicTest
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory
import org.junit.jupiter.api.extension.AfterEachCallback
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.api.extension.ExtensionContext
import org.junit.jupiter.api.extension.ExtensionContext.Namespace
import org.junit.jupiter.api.extension.ExtensionContext.Store
import strikt.api.Assertion.Builder
import strikt.api.expectThat
import strikt.assertions.isA
import strikt.assertions.isEqualTo
import strikt.assertions.isFailure
import strikt.assertions.isGreaterThanOrEqualTo
import strikt.assertions.isSuccess
import strikt.assertions.isTrue
import strikt.assertions.message
import strikt.assertions.size
import java.net.URI
import java.nio.file.Path
import java.util.stream.Stream
import kotlin.io.path.createDirectories
import kotlin.io.path.exists
import kotlin.properties.ReadOnlyProperty
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

typealias Assertion<T> = Builder<T>.() -> Unit

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
            getLambdaBodyOrNull(this, function)?.wrap(" ❴ ", " ❵ ") ?: fn.toSimpleString()
        }
    }

    /**
     * Returns the display name for an asserting test.
     */
    fun <T> CallStackElement.assertingDisplayName(assertion: Assertion<T>): String =
        StringBuilder("❕").apply {
            append(" ")
            append(this@assertingDisplayName.displayName(assertion))
        }.toString()

    /**
     * Returns the display name for an [subject] asserting test.
     */
    fun <T> CallStackElement.assertingDisplayName(subject: T, assertion: Assertion<T>): String =
        StringBuilder("❕").apply {
            append(" ")
            append(subject.displayName())
            append(" ")
            append(this@assertingDisplayName.displayName(assertion))
        }.toString()

    /**
     * Returns the display name for a transforming test.
     */
    fun <T, R> CallStackElement.expectingDisplayName(transform: (T) -> R): String =
        this.displayName("❔", transform)

    /**
     * Returns the display name for a catching test.
     */
    fun <T, R> CallStackElement.catchingDisplayName(transform: (T) -> R): String =
        this.displayName("❓", transform)

    /**
     * Returns the display name for an [E] throwing test.
     */
    inline fun <reified E> throwingDisplayName(): String =
        StringBuilder("❗").apply {
            append(" ")
            append(E::class.simpleName)
        }.toString()

    /**
     * Returns the display name for a test applying [transform].
     */
    private fun <T, R> CallStackElement.displayName(symbol: String, transform: (T) -> R): String =
        StringBuilder(symbol).apply {
            append(" ")
            append(this@displayName.displayName(transform))
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

    private fun getLambdaBodyOrNull(
        callStackElement: CallStackElement,
        explicitMethodName: String? = null,
    ) = kotlin.runCatching {
        val line = FilePeek(callStackElement.stackTraceElement).getCallerFileInfo().line
        if (explicitMethodName != null) {
            line.takeIf { it.contains(explicitMethodName) }?.let {
                LambdaBody(explicitMethodName, it).body.trim().truncate(40, " … ")
            }
        } else {
            LambdaBody(callStackElement.function, line).body.trim().truncate(40, " … ")
        }
    }.getOrNull()

    private val CallStackElement.stackTraceElement get() = StackTraceElement(receiver, function, file, line)

    private val callerIgnoreRegex = Regex(".*DynamicTest.*Builder|.*\\.TestsKt.*")

    @Suppress("DEPRECATION")
    fun findCaller(): CallStackElement {
        if (isDebugging) {
            "FINDING CALLER".trace
            currentStackTrace.trace
        }
        return getCaller {
            receiver == enclosingClassName || receiver?.matches(callerIgnoreRegex) == true
        }
    }

    /**
     * Reliably contains the fully qualified name of [Tester].
     * The result is still correct after typical refactorings like renaming.
     */
    private val enclosingClassName = this::class.qualifiedName
        ?.let { if (this::class.isCompanion) it.substringBeforeLast(".") else it }
        ?: error("unknown name")
}

class IllegalUsageException(function: String, caller: URI) : IllegalArgumentException(
    "$function { … } call was not finished with ${"that { … }".formattedAs.input}".let {
        caller.let { uri -> "$it at " + uri.path + ":" + uri.query.takeLastWhile { it.isDigit() } }
    }
)

/*
 * JUNIT EXTENSIONS
 */
/**
 * Provides an accessor for the [ExtensionContext.Store] that uses
 * the owning class as the key for the [Namespace] needed to access and scope the store.
 *
 * **Usage**
 * ```kotlin
 * class MyExtension: AnyJUnitExtension {
 *
 *     // implementation of an store accessor with name store
 *     val store: ExtensionContext.() -> Store by storeForNamespace<AnyClass>()
 *
 *     fun anyCallback(context: ExtensionContext) {
 *
 *         // using store (here: with a subsequent get call)
 *         context.store().get(…)
 *     }
 * }
 *
 * ```
 */
fun storeForNamespace(): ReadOnlyProperty<Any, ExtensionContext.() -> Store> =
    ReadOnlyProperty<Any, ExtensionContext.() -> Store> { thisRef, _ ->
        { getStore(Namespace.create(thisRef::class.java)) }
    }

/**
 * Provides an accessor for the [ExtensionContext.Store] that uses
 * the owning class and the current test as the keys for the [Namespace] needed to access and scope the store.
 *
 * An exception is thrown if no test is current.
 *
 * **Usage**
 * ```kotlin
 * class MyExtension: AnyJUnitExtension {
 *
 *     // implementation of an store accessor with name store
 *     val store: ExtensionContext.() -> Store by storeForNamespaceAndTest()
 *
 *     fun anyCallback(context: ExtensionContext) {
 *
 *         // using store (here: with a subsequent get call)
 *         context.store().get(…)
 *     }
 * }
 *
 * ```
 */
fun storeForNamespaceAndTest(): ReadOnlyProperty<Any, ExtensionContext.() -> Store> =
    ReadOnlyProperty<Any, ExtensionContext.() -> Store> { thisRef, _ ->
        { getStore(Namespace.create(thisRef::class.java, requiredTestMethod)) }
    }

/**
 * Returns the [ExtensionContext.Store] that uses
 * the class of [T] as the key for the [Namespace] needed to access and scope the store.
 *
 * [additionalParts] can be provided to render the namespace more specific, just keep
 * in mind that the order is significant and parts are compared with [Object.equals].
 */
inline fun <reified T> ExtensionContext.storeForNamespace(
    clazz: Class<T> = T::class.java,
    vararg additionalParts: Any,
): Store = getStore(Namespace.create(clazz, *additionalParts))

/**
 * Returns the [ExtensionContext.Store] that uses
 * the class of [T] and the current test as the keys for the [Namespace] needed to access and scope the store.
 *
 * An exception is thrown if no test is current.
 */
inline fun <reified T> ExtensionContext.storeForNamespaceAndTest(
    clazz: Class<T> = T::class.java,
): Store = storeForNamespace(clazz, requiredTestMethod)


/*
 * STRIKT EXTENSIONS
 */

/**
 * Extracts the actual subject from `this` [Builder].
 *
 * ***Hint:** Consider refactoring your test instead making us of this extension.*
 */
inline val <reified T : Any> Builder<T>.actual: T
    get() {
        var actual: T? = null
        get { actual = this }
        return actual ?: error("Failed to extract actual from $this")
    }

/**
 * Validates the given [assertions] against the elements of the asserted collection
 * of elements.
 *
 * The number of [assertions] determines the number of checked elements, that is,
 * if the asserted collection contains 5 elements and 2 assertions are provided,
 * only the first 2 elements are asserted.
 *
 * The test fails if more [assertions] are given than there are
 * assertable elements.
 */
fun <T> Builder<out Iterable<T>>.hasElements(vararg assertions: Builder<T>.() -> Unit): Builder<out Iterable<T>> =
    compose("fulfills ${assertions.size}") {
        val elements = it.toList()
        expectThat(elements).size.isGreaterThanOrEqualTo(assertions.size)
        elements.zip(assertions).forEach { (element, assertion) ->
            expectThat(element, assertion)
        }
    } then {
        if (allPassed) pass() else fail()
    }


/**
 * JUnit extension that checks if [expecting] or [expectCatching]
 * where incorrectly used.
 *
 * ***Important:**
 * For this extension to work, it needs to be registered.*
 *
 * > The most convenient way to register this extension
 * > for all tests is by adding the line **`koodies.test.IllegalUsageCheck`** to the
 * > file **`resources/META-INF/services/org.junit.jupiter.api.extension.Extension`**.
 */
class IllegalUsageCheck : AfterEachCallback {

    override fun afterEach(context: ExtensionContext) {
        val id = context.id
        illegalUsages[id]?.also { illegalUsage ->
            if (!context.illegalUsageExpected) {
                throw illegalUsage
            }
        } ?: if (context.illegalUsageExpected) {
            error("${IllegalUsageException::class} expected but not thrown.")
        }
    }

    /**
     * Extension to signal that an [IllegalUsageException] is expected.
     * Consequently the tests fails if no such exception is thrown.
     */
    class ExpectIllegalUsageException : AfterEachCallback {
        override fun afterEach(context: ExtensionContext) {
            context.illegalUsageExpected = true
        }
    }

    companion object {
        private val store by storeForNamespaceAndTest()

        var ExtensionContext.illegalUsageExpected: Boolean
            get() = store().get<Boolean>() == true
            set(value) = store().put(value)

        val illegalUsages = mutableMapOf<UniqueId, IllegalUsageException>()
    }
}

/**
 * Expects `this` subject to fulfil the given [assertion].
 *
 * **Usage:** `<subject> asserting { <assertion> }`
 */
@JvmName("infixAsserting")
infix fun <T> T.asserting(assertion: Assertion<T>) =
    strikt.api.expectThat(this, assertion)

/**
 * Expects the [subject] to fulfil the given [assertion].
 *
 * **Usage:** `asserting(<subject>) { <assertion> }`
 */
fun <T> asserting(subject: T, assertion: Assertion<T>) =
    strikt.api.expectThat(subject, assertion)

/**
 * Expects the subject returned by [action] to fulfil the
 * assertion returned by [DynamicTestBuilder.InCompleteExpectationBuilder].
 *
 * **Usage:** `expecting { <action> } that { <assertion> }`
 */
fun <T> expecting(action: () -> T): InCompleteExpectationBuilder<T> {
    val caller = findCaller()
    val id = UniqueId.from(caller)
    IllegalUsageCheck.illegalUsages[id] = IllegalUsageException("expecting", caller.callerSource)
    return InCompleteExpectationBuilder { assertion: Assertion<T> ->
        IllegalUsageCheck.illegalUsages.remove(id)
        strikt.api.expectThat(action()).assertion()
    }
}

/**
 * Expects the [Result] returned by [action] to fulfil the
 * assertion returned by [DynamicTestBuilder.InCompleteExpectationBuilder].
 *
 * **Usage:** `expectCatching { <action> } that { <assertion> }`
 */
fun <T> expectCatching(action: () -> T): InCompleteExpectationBuilder<Result<T>> {
    val caller = findCaller()
    val id = UniqueId.from(caller)
    IllegalUsageCheck.illegalUsages[id] = IllegalUsageException("expectCatching", caller.callerSource)
    return InCompleteExpectationBuilder { additionalAssertion: Assertion<Result<T>> ->
        IllegalUsageCheck.illegalUsages.remove(id)
        strikt.api.expectCatching { action() }.and(additionalAssertion)
    }
}

/**
 * Expects an exception of type [E] to be thrown when running [action]
 * and to optionally fulfil the assertion returned by [DynamicTestBuilder.InCompleteExpectationBuilder].
 *
 * **Usage:** `expectThrows<Exception> { <action> } [ that { <assertion> } ]`
 */
inline fun <reified E : Throwable> expectThrows(noinline action: () -> Any?): InCompleteExpectationBuilder<E> {
    val assertionBuilder = strikt.api.expectThrows<E> { action() }
    return InCompleteExpectationBuilder { additionalAssertion: Assertion<E> ->
        assertionBuilder.and(additionalAssertion)
    }
}


/*
 * TEST BUILDERS
 */

@DslMarker
annotation class DynamicTestsDsl

/**
 * Creates tests for the specified [subject]
 * using the specified [DynamicTestsWithSubjectBuilder] based [init].
 */
@DynamicTestsDsl
fun <T> test(subject: T, init: DynamicTestsWithSubjectBuilder<T>.(T) -> Unit): List<DynamicNode> =
    DynamicTestsWithSubjectBuilder.build(subject, init)

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
    .run {
        map { subject -> dynamicContainer("for ${subject.displayName(containerNamePattern)}", DynamicTestsWithSubjectBuilder.build(subject, init)) }
    }

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
 * Builder for arbitrary test trees consisting of instances of [DynamicContainer] and [DynamicTest]
 * and a fluent transition to [Strikt](https://strikt.io) assertion.
 */
@DynamicTestsDsl
class DynamicTestsWithSubjectBuilder<T>(val subject: T, val callback: (DynamicNode) -> Unit) {

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
     * Expects `this` subject to fulfil the given [assertion].
     *
     * ***Note:** The surrounding test subject is ignored.*
     *
     * **Usage:** `<subject> asserting { <assertion> }`
     */
    infix fun <T> T.asserting(assertion: Assertion<T>) {
        val caller = findCaller()
        val test = dynamicTest(findCaller().assertingDisplayName(this, assertion), caller.callerSource) {
            strikt.api.expectThat(this, assertion)
        }
        callback(test)
    }

    /**
     * Expects the [subject] to fulfil the given [assertion].
     *
     * **Usage:** `asserting(<subject>) { <assertion> }`
     */
    infix fun asserting(assertion: Assertion<T>) {
        val caller = findCaller()
        val test = dynamicTest(findCaller().assertingDisplayName(assertion), caller.callerSource) {
            strikt.api.expectThat(subject, assertion)
        }
        callback(test)
    }

    /**
     * Expects the [subject] transformed by [action] to fulfil the
     * assertion returned by [DynamicTestBuilder.InCompleteExpectationBuilder].
     *
     * **Usage:** `expecting { 𝘴𝘶𝘣𝘫𝘦𝘤𝘵.<action> } that { <assertion> }`
     */
    fun <R> expecting(description: String? = null, action: T.() -> R): InCompleteExpectationBuilder<R> {
        var additionalAssertion: Assertion<R>? = null
        val caller = findCaller()
        val test = dynamicTest(description ?: caller.expectingDisplayName(action), caller.callerSource) {
            strikt.api.expectThat(subject).with(action, additionalAssertion ?: throw IllegalUsageException("expecting", caller.callerSource))
        }
        callback(test)
        return InCompleteExpectationBuilder { assertion: Assertion<R> ->
            additionalAssertion = assertion
        }
    }

    /**
     * Expects the [Result] of the [subject] transformed by [action] to fulfil the
     * assertion returned by [DynamicTestBuilder.InCompleteExpectationBuilder].
     *
     * **Usage:** `expectCatching { 𝘴𝘶𝘣𝘫𝘦𝘤𝘵.<action> } that { <assertion> }`
     */
    fun <R> expectCatching(action: T.() -> R): InCompleteExpectationBuilder<Result<R>> {
        var additionalAssertion: Assertion<Result<R>>? = null
        val caller = findCaller()
        val test = dynamicTest(findCaller().catchingDisplayName(action), caller.callerSource) {
            strikt.api.expectCatching { subject.action() }.and(additionalAssertion ?: throw IllegalUsageException("expectCatching", caller.callerSource))
        }
        callback(test)
        return InCompleteExpectationBuilder { assertion: Assertion<Result<R>> ->
            additionalAssertion = assertion
        }
    }

    /**
     * Expects an exception of type [E] to be thrown when transforming the [subject] with [action]
     * and to optionally fulfil the assertion returned by [DynamicTestBuilder.InCompleteExpectationBuilder].
     *
     * **Usage:** `expectThrows<Exception> { 𝘴𝘶𝘣𝘫𝘦𝘤𝘵.<action> } [ that { <assertion> } ]`
     */
    inline fun <reified E : Throwable> expectThrows(noinline action: T.() -> Any?): InCompleteExpectationBuilder<E> {
        var additionalAssertion: Assertion<E>? = null
        val caller = findCaller()
        val test = dynamicTest(throwingDisplayName<E>(), caller.callerSource) {
            strikt.api.expectThrows<E> { subject.action() }.and(additionalAssertion ?: {})
        }
        callback(test)
        return InCompleteExpectationBuilder { assertion: Assertion<E> ->
            additionalAssertion = assertion
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

    companion object {

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
 * Expects the [subject] to fulfil the given [assertion].
 *
 * ***Note:** The surrounding test subject is ignored.*
 *
 * **Usage:** `asserting(<subject>) { <assertion> }`
 */
@Deprecated("replace with subject asserting assertion", ReplaceWith("subject asserting assertion"))
fun <T> DynamicTestsWithSubjectBuilder<*>.asserting(subject: T, assertion: Assertion<T>) {
    with(subject) {
        asserting(assertion)
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
 * - [asserting] is for running [Strikt](https://strikt.io) assertion on a subject you already have
 * - [expecting] is for running [Strikt](https://strikt.io) assertion on a subject that needs to be provided first
 * - [expectCatching] is for running [Strikt](https://strikt.io) assertion on the result of an action that might throw an exception
 * - [expectThrows] is for running [Strikt](https://strikt.io) assertion on the exception thrown by an action
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

    /**
     * Expects `this` subject to fulfil the given [assertion].
     *
     * **Usage:** `<subject> asserting { <assertion> }`
     */
    @JvmName("infixAsserting")
    infix fun <T> T.asserting(assertion: Assertion<T>) {
        val caller = findCaller()
        val test = dynamicTest(findCaller().assertingDisplayName(this, assertion), caller.callerSource) {
            strikt.api.expectThat(this, assertion)
        }
        tests.add(test)
    }

    /**
     * Expects the [subject] to fulfil the given [assertion].
     *
     * **Usage:** `asserting(<subject>) { <assertion> }`
     */
    fun <T> asserting(subject: T, assertion: Assertion<T>) {
        val caller = findCaller()
        val test = dynamicTest(findCaller().assertingDisplayName(assertion), caller.callerSource) {
            strikt.api.expectThat(subject, assertion)
        }
        tests.add(test)
    }

    /**
     * Expects the subject returned by [action] to fulfil the
     * assertion returned by [DynamicTestBuilder.InCompleteExpectationBuilder].
     *
     * **Usage:** `expecting { <action> } that { <assertion> }`
     */
    fun <R> expecting(description: String? = null, action: () -> R): InCompleteExpectationBuilder<R> {
        var additionalAssertion: Assertion<R>? = null
        val caller = findCaller()
        val test = dynamicTest(description ?: caller.expectingDisplayName(action), caller.callerSource) {
            strikt.api.expectThat(action(), additionalAssertion ?: throw IllegalUsageException("expecting", caller.callerSource))
        }
        tests.add(test)
        return InCompleteExpectationBuilder { assertion: Assertion<R> ->
            additionalAssertion = assertion
        }
    }

    /**
     * Expects the [Result] returned by [action] to fulfil the
     * assertion returned by [DynamicTestBuilder.InCompleteExpectationBuilder].
     *
     * **Usage:** `expectCatching { <action> } that { <assertion> }`
     */
    fun <R> expectCatching(action: () -> R): InCompleteExpectationBuilder<Result<R>> {
        var additionalAssertion: Assertion<Result<R>>? = null
        val caller = findCaller()
        val test = dynamicTest(findCaller().catchingDisplayName(action), caller.callerSource) {
            strikt.api.expectCatching { action() }.and(additionalAssertion ?: throw IllegalUsageException("expectCatching", caller.callerSource))
        }
        tests.add(test)
        return InCompleteExpectationBuilder { assertion: Assertion<Result<R>> ->
            additionalAssertion = assertion
        }
    }

    /**
     * Expects an exception of type [E] to be thrown when running [action]
     * and to optionally fulfil the assertion returned by [DynamicTestBuilder.InCompleteExpectationBuilder].
     *
     * **Usage:** `expectThrows<Exception> { <action> } [ that { <assertion> } ]`
     */
    inline fun <reified E : Throwable> expectThrows(noinline action: () -> Any?): InCompleteExpectationBuilder<E> {
        var additionalAssertion: Assertion<E>? = null
        val caller = findCaller()
        val test = dynamicTest(throwingDisplayName<E>(), caller.callerSource) {
            strikt.api.expectThrows<E> { action() }.and(additionalAssertion ?: {})
        }
        tests.add(test)
        return InCompleteExpectationBuilder { assertion: Assertion<E> ->
            additionalAssertion = assertion
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
     * that makes a call to [that] as soon as building the expectation
     * is completed.
     *
     * If no callback took place until a certain moment in time
     * this [that] was never called.
     */
    class InCompleteExpectationBuilder<T>(val assertionBuilderProvider: (Assertion<T>) -> Unit) {
        @Suppress("NOTHING_TO_INLINE")
        inline infix fun that(noinline assertion: Assertion<T>) = assertionBuilderProvider(assertion)
    }

    /**
     * Expects the [subject] transformed by [action] to fulfil the
     * assertion returned by [DynamicTestBuilder.InCompleteExpectationBuilder].
     *
     * **Usage:** `expecting { 𝘴𝘶𝘣𝘫𝘦𝘤𝘵.<action> } that { <assertion> }`
     */
    fun <R> expecting(description: String? = null, action: T.() -> R): InCompleteExpectationBuilder<R> {
        val errorMessage = "expecting { … } call was not finished with that { … } at ${getCaller()}".also { buildErrors.add(it) }
        return InCompleteExpectationBuilder { assertion: Assertion<R> ->
            buildErrors.remove(errorMessage)
            strikt.api.expectThat(subject).with(description?.takeUnlessBlank() ?: "with".property(action) + findCaller().expectingDisplayName(
                action),
                action,
                assertion)
        }
    }

    /**
     * Expects the [Result] of the [subject] transformed by [action] to fulfil the
     * assertion returned by [DynamicTestBuilder.InCompleteExpectationBuilder].
     *
     * **Usage:** `expectCatching { 𝘴𝘶𝘣𝘫𝘦𝘤𝘵.<action> } that { <assertion> }`
     */
    fun <R> expectCatching(action: suspend T.() -> R): InCompleteExpectationBuilder<Result<R>> {
        val errorMessage = "expectCatching { … } call was not finished with that { … } at ${getCaller()}".also { buildErrors.add(it) }
        return InCompleteExpectationBuilder { assertion: Assertion<Result<R>> ->
            buildErrors.remove(errorMessage)
            strikt.api.expectCatching { subject.action() }.assertion()
        }
    }

    /**
     * Expects an exception of type [E] to be thrown when transforming the [subject] with [action]
     * and to optionally fulfil the assertion returned by [DynamicTestBuilder.InCompleteExpectationBuilder].
     *
     * **Usage:** `expectThrows<Exception> { 𝘴𝘶𝘣𝘫𝘦𝘤𝘵.<action> } [ that { <assertion> } ]`
     */
    inline fun <reified E : Throwable> expectThrows(noinline action: suspend T.() -> Any?): InCompleteExpectationBuilder<E> {
        val assertionBuilder: Builder<E> = strikt.api.expectThrows { subject.action() }
        return InCompleteExpectationBuilder { assertion: Assertion<E> ->
            assertionBuilder.assertion()
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
    val tempDir: Path = TestKoodies.TestRoot.resolve(uniqueId.value.toBaseName().withRandomSuffix()).createDirectories()
    tempDir.block()
    check(TestKoodies.TestRoot.exists()) {
        println("The shared root temp directory was deleted by $uniqueId or a concurrently running test. This must not happen.".ansi.red.toString())
        exitProcess(-1)
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

@Smoke
class TesterTest {

    @Nested
    inner class TestFlattening {

        @Test
        fun `should flatten`() {
            val tests = TestsSample().TestingSingleSubject().`as parameter`().flatten()
            strikt.api.expectThat(tests.toList()).size.isEqualTo(17)
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
                ❔ length isGreaterThan(5)
            """,
            !"""
                ❔ length
            """,
            !"""
                ❓ length isSuccess()
            """,
            !"""
                ❗ RuntimeException
            """,
            !"""
                ❗ RuntimeException
            """,
        )).map { (label, expected) -> dynamicTest("👆 $expected") { strikt.api.expectThat(label).isEqualTo(expected) } }.asStream()


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
                ❔ ❮ "subject".length ❯ isGreaterThan(5)
            """,
            !"""
                ❔ ❮ "subject".length ❯
            """,
            !"""
                ❓ ❮ "subject".length ❯ isSuccess()
            """,
            !"""
                ❗ RuntimeException
            """,
            !"""
                ❗ RuntimeException
            """,
        )).map { (label, expected) -> dynamicTest("👆 $expected") { strikt.api.expectThat(label).isEqualTo(expected) } }.asStream()
    }

    @Nested
    inner class PlainAssertionsTest {

        @Test
        fun `should run asserting`() {
            var testSucceeded = false
            asserting("subject") { testSucceeded = actual == "subject" }
            strikt.api.expectThat(testSucceeded).isTrue()
        }

        @Test
        fun `should run receiver asserting`() {
            var testSucceeded = false
            "subject" asserting { testSucceeded = actual == "subject" }
            strikt.api.expectThat(testSucceeded).isTrue()
        }

        @Test
        fun `should run evaluating `() {
            var testSucceeded = false
            expecting { "subject" } that { testSucceeded = actual == "subject" }
            strikt.api.expectThat(testSucceeded).isTrue()
        }

        @Test
        @ExtendWith(ExpectIllegalUsageException::class)
        fun `should throw on incomplete evaluating`() {
            expecting { "subject" }
        }

        @Test
        fun `should run expectCatching`() {
            var testSucceeded = false
            expectCatching { throw RuntimeException("message") } that { testSucceeded = actual.exceptionOrNull()!!.message == "message" }
            strikt.api.expectThat(testSucceeded).isTrue()
        }

        @Test
        @ExtendWith(ExpectIllegalUsageException::class)
        fun `should throw on incomplete expectCatching`() {
            expectCatching { throw RuntimeException("message") }
        }

        @Test
        fun `should run expectThrows`() {
            var testSucceeded = false
            expectThrows<RuntimeException> { throw RuntimeException("message") } that { testSucceeded = actual.message == "message" }
            strikt.api.expectThat(testSucceeded).isTrue()
        }

        @Test
        fun `should not throw on evaluating only throwable type`() {
            strikt.api.expectCatching { expectThrows<RuntimeException> { throw RuntimeException("message") } }.isSuccess()
        }
    }

    @Nested
    inner class DynamicTestsWithSubjectBuilderTest {

        @Test
        fun `should run asserting`() {
            var testSucceeded = false
            val tests = testEach("subject") {
                asserting { testSucceeded = actual == "subject" }
            }
            tests.execute()
            strikt.api.expectThat(testSucceeded).isTrue()
        }

        @Test
        fun `should run receiver asserting`() {
            var testSucceeded = false
            val tests = testEach("subject") {
                "other" asserting { testSucceeded = actual == "other" }
            }
            tests.execute()
            strikt.api.expectThat(testSucceeded).isTrue()
        }

        @Test
        fun `should run evaluating `() {
            var testSucceeded = false
            val tests = testEach("subject") {
                expecting("expectation") { "$it.prop" } that { testSucceeded = actual == "subject.prop" }
            }
            tests.execute()
            strikt.api.expectThat(testSucceeded).isTrue()
        }

        @Test
        fun `should throw on incomplete evaluating`() {
            val tests = testEach("subject") {
                expecting("expectation") { "$it.prop" }
            }
            strikt.api.expectCatching { tests.execute() }
                .isFailure()
                .isA<IllegalUsageException>()
                .message.toStringContains("not finished")
        }

        @Test
        fun `should run expectCatching`() {
            var testSucceeded = false
            val tests = testEach("subject") {
                expectCatching { throw RuntimeException(this) } that { testSucceeded = actual.exceptionOrNull()!!.message == "subject" }
            }
            tests.execute()
            strikt.api.expectThat(testSucceeded).isTrue()
        }

        @Test
        fun `should throw on incomplete expectCatching`() {
            val tests = testEach("subject") {
                expectCatching<Any?> { throw RuntimeException(this) }
            }
            strikt.api.expectCatching { tests.execute() }
                .isFailure()
                .isA<IllegalUsageException>()
                .message.toStringContains("not finished")
        }

        @Test
        fun `should run expectThrows`() {
            var testSucceeded = false
            val tests = testEach("subject") {
                expectThrows<RuntimeException> { throw RuntimeException(this) } that { testSucceeded = actual.message == "subject" }
            }
            tests.execute()
            strikt.api.expectThat(testSucceeded).isTrue()
        }

        @Test
        fun `should not throw on evaluating only throwable type`() {
            val tests = testEach("subject") {
                expectThrows<RuntimeException> { throw RuntimeException(this) }
            }
            strikt.api.expectCatching { tests.execute() }.isSuccess()
        }
    }

    @Nested
    inner class DynamicTestsWithoutSubjectBuilderTest {

        @Test
        fun `should run asserting`() {
            var testSucceeded = false
            val tests = tests {
                asserting { testSucceeded = true }
            }
            tests.execute()
            strikt.api.expectThat(testSucceeded).isTrue()
        }

        @Test
        fun `should run receiver asserting`() {
            var testSucceeded = false
            val tests = tests {
                "other" asserting { testSucceeded = actual == "other" }
            }
            tests.execute()
            strikt.api.expectThat(testSucceeded).isTrue()
        }

        @Test
        fun `should run evaluating `() {
            var testSucceeded = false
            val tests = tests {
                expecting("expectation") { "subject" } that { testSucceeded = actual == "subject" }
            }
            tests.execute()
            strikt.api.expectThat(testSucceeded).isTrue()
        }

        @Test
        fun `should throw on incomplete evaluating`() {
            val tests = tests {
                expecting("expectation") { "subject" }
            }
            strikt.api.expectCatching { tests.execute() }
                .isFailure()
                .isA<IllegalUsageException>()
                .message.toStringContains("not finished")
        }

        @Test
        fun `should run expectCatching`() {
            var testSucceeded = false
            val tests = tests {
                expectCatching { throw RuntimeException("message") } that { testSucceeded = actual.exceptionOrNull()!!.message == "message" }
            }
            tests.execute()
            strikt.api.expectThat(testSucceeded).isTrue()
        }

        @Test
        fun `should throw on incomplete expectCatching`() {
            val tests = tests {
                expectCatching<Any?> { throw RuntimeException("message") }
            }
            strikt.api.expectCatching { tests.execute() }
                .isFailure()
                .isA<IllegalUsageException>()
                .message.toStringContains("not finished")
        }

        @Test
        fun `should run expectThrows`() {
            var testSucceeded = false
            val tests = tests {
                expectThrows<RuntimeException> { throw RuntimeException("message") } that { testSucceeded = actual.message == "message" }
            }
            tests.execute()
            strikt.api.expectThat(testSucceeded).isTrue()
        }

        @Test
        fun `should not throw on evaluating only throwable type`() {
            val tests = tests {
                expectThrows<RuntimeException> { throw RuntimeException("message") }
            }
            strikt.api.expectCatching { tests.execute() }.isSuccess()
        }
    }
}
