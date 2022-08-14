package com.bkahlert.kommons.test

import com.bkahlert.kommons.Exceptions.ISE
import com.bkahlert.kommons.TestKommons
import com.bkahlert.kommons.debug.renderType
import com.bkahlert.kommons.test.DynamicTestBuilder.InCompleteExpectationBuilder
import com.bkahlert.kommons.test.TestFlattener.flatten
import com.bkahlert.kommons.test.Tester.assertingDisplayName
import com.bkahlert.kommons.test.Tester.findCaller
import com.bkahlert.kommons.test.junit.DynamicTestDisplayNameGenerator
import com.bkahlert.kommons.test.junit.IllegalUsageException
import com.bkahlert.kommons.test.junit.PathSource
import com.bkahlert.kommons.test.junit.PathSource.Companion.sourceUri
import com.bkahlert.kommons.test.junit.SimpleId
import com.bkahlert.kommons.text.ANSI.Text.Companion.ansi
import com.bkahlert.kommons.text.decapitalize
import com.bkahlert.kommons.text.groupValue
import com.bkahlert.kommons.text.takeUnlessBlank
import com.bkahlert.kommons.text.toIdentifier
import com.bkahlert.kommons.text.withRandomSuffix
import org.junit.jupiter.api.DynamicContainer
import org.junit.jupiter.api.DynamicContainer.dynamicContainer
import org.junit.jupiter.api.DynamicNode
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.DynamicTest.dynamicTest
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import strikt.api.Assertion.Builder
import strikt.assertions.isA
import strikt.assertions.isEqualTo
import strikt.assertions.isFailure
import strikt.assertions.isGreaterThanOrEqualTo
import strikt.assertions.isSuccess
import strikt.assertions.isTrue
import strikt.assertions.message
import strikt.assertions.size
import java.nio.file.Path
import kotlin.io.path.createDirectories
import kotlin.io.path.exists
import kotlin.reflect.KCallable
import kotlin.reflect.KFunction
import kotlin.reflect.KProperty
import kotlin.streams.asSequence
import kotlin.system.exitProcess
import com.bkahlert.kommons.test.junit.DynamicTestDisplayNameGenerator.catchingDisplayName as xcatchingDisplayName
import com.bkahlert.kommons.test.junit.DynamicTestDisplayNameGenerator.expectingDisplayName as xexpectingDisplayName
import com.bkahlert.kommons.test.junit.DynamicTestDisplayNameGenerator.property as xproperty
import com.bkahlert.kommons.test.junit.DynamicTestDisplayNameGenerator.throwingDisplayName as xthrowingDisplayName

// TODO migrate

typealias Assertion<T> = Builder<T>.() -> Unit

object Tester {

    /**
     * Returns the display name for an asserting test.
     */
    fun <T> StackTraceElement.assertingDisplayName(assertion: Assertion<T>): String =
        buildString {
            append("❕ ")
            append(this@assertingDisplayName.displayName(assertion))
        }

    /**
     * Returns the display name for an [subject] asserting test.
     */
    fun <T> StackTraceElement.assertingDisplayName(subject: T, assertion: Assertion<T>): String =
        buildString {
            append("❕ ")
            append(DynamicTestDisplayNameGenerator.displayNameFor(subject, null))
            append(" ")
            append(this@assertingDisplayName.displayName(assertion))
        }

    /**
     * Attempts to calculate a rich display name for a property
     * expressed by the specified [fn].
     */
    fun <T, R> StackTraceElement.displayName(fn: T.() -> R, fnName: String? = null): String {
        return when (fn) {
            is KProperty<*> -> fn.name
            is KFunction<*> -> fn.name
            is KCallable<*> -> run { fn.getPropertyName(methodName) }
            else -> getLambdaBodyOrNull(this, fnName) ?: fn.renderType()
        }
    }

    private fun KCallable<*>.getPropertyName(callerMethodName: String): String =
        "^$callerMethodName(?<arg>.+)$".toRegex().find(name)?.run { groupValue("arg")?.decapitalize() } ?: name

    private fun getLambdaBodyOrNull(
        callStackElement: StackTraceElement,
        explicitMethodName: String? = null,
    ) = (explicitMethodName?.let { LambdaBody.parseOrNull(callStackElement, it) } ?: LambdaBody.parseOrNull(callStackElement))?.body

    @Suppress("DEPRECATION")
    fun findCaller(): StackTraceElement {
        return KommonsTest.locateCall()
    }
}

/*
 * STRIKT EXTENSIONS
 */

/**
 * Extracts the actual subject from this [Builder].
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
        strikt.api.expectThat(elements).size.isGreaterThanOrEqualTo(assertions.size)
        elements.zip(assertions).forEach { (element, assertion) ->
            strikt.api.expectThat(element, assertion)
        }
    } then {
        if (allPassed) pass() else fail()
    }


/**
 * Expects this subject to fulfil the given [assertion].
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
    return InCompleteExpectationBuilder { assertion: Assertion<T> ->
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
    return InCompleteExpectationBuilder { additionalAssertion: Assertion<Result<T>> ->
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
 * using the specified [OldDynamicTestsWithSubjectBuilder] based [init].
 */
@DynamicTestsDsl
fun <T> testOld(subject: T, init: OldDynamicTestsWithSubjectBuilder<T>.(T) -> Unit): List<DynamicNode> =
    OldDynamicTestsWithSubjectBuilder.build(subject, init)

/**
 * Creates one [DynamicContainer] for each instance of this collection of subjects
 * using the specified [OldDynamicTestsWithSubjectBuilder] based [init].
 *
 * The name for each container is heuristically derived but can also be explicitly specified using [containerNamePattern]
 * which supports curly placeholders `{}` like [SLF4J] does.
 */
@DynamicTestsDsl
fun <T> Iterable<T>.testEachOld(
    containerNamePattern: String? = null,
    init: OldDynamicTestsWithSubjectBuilder<T>.(T) -> Unit,
): List<DynamicContainer> = toList()
    .also { require(it.isNotEmpty()) { "At least one subject must be provided for testing." } }
    .run {
        map { subject ->
            dynamicContainer(
                "for ${DynamicTestDisplayNameGenerator.displayNameFor(subject, containerNamePattern)}",
                OldDynamicTestsWithSubjectBuilder.build(subject, init)
            )
        }
    }

/**
 * Creates one [DynamicContainer] for each instance of this collection of subjects
 * using the specified [OldDynamicTestsWithSubjectBuilder] based [init].
 *
 * The name for each container is heuristically derived but can also be explicitly specified using [containerNamePattern]
 * which supports curly placeholders `{}` like [SLF4J] does.
 */
@DynamicTestsDsl
fun <T> Sequence<T>.testEachOld(
    containerNamePattern: String? = null,
    init: OldDynamicTestsWithSubjectBuilder<T>.(T) -> Unit,
): List<DynamicContainer> = toList().testEachOld(containerNamePattern, init)

/**
 * Creates one [DynamicContainer] for each instance of the specified [subjects]
 * using the specified [OldDynamicTestsWithSubjectBuilder] based [init].
 *
 * The name for each container is heuristically derived but can also be explicitly specified using [containerNamePattern]
 * which supports curly placeholders `{}` like [SLF4J] does.
 */
@DynamicTestsDsl
fun <T> testEachOld(
    vararg subjects: T,
    containerNamePattern: String? = null,
    init: OldDynamicTestsWithSubjectBuilder<T>.(T) -> Unit,
): List<DynamicContainer> = subjects.toList().testEachOld(containerNamePattern, init)

/**
 * Builder for arbitrary test trees consisting of instances of [DynamicContainer] and [DynamicTest]
 * and a fluent transition to [Strikt](https://strikt.io) assertion.
 */
@DynamicTestsDsl
class OldDynamicTestsWithSubjectBuilder<T>(val subject: T, val callback: (DynamicNode) -> Unit) {

    /**
     * Builder for testing a property of the test subject.
     */
    @DynamicTestsDsl
    interface PropertyTestBuilder<T> {
        @DynamicTestsDsl
        infix fun then(block: OldDynamicTestsWithSubjectBuilder<T>.(T) -> Unit): T
    }

    /**
     * Builds a [DynamicContainer] using the specified [name] and the
     * specified [OldDynamicTestsWithSubjectBuilder] based [init] to build the child nodes.
     */
    @DynamicTestsDsl
    fun group(name: String, init: OldDynamicTestsWithSubjectBuilder<T>.(T) -> Unit) {
        callback(dynamicContainer(name, PathSource.currentUri, build(subject, init).stream()))
    }

    /**
     * Expects this subject to fulfil the given [assertion].
     *
     * ***Note:** The surrounding test subject is ignored.*
     *
     * **Usage:** `<subject> asserting { <assertion> }`
     */
    infix fun <T> T.asserting(assertion: Assertion<T>) {
        val caller = findCaller()
        val test = dynamicTest(findCaller().assertingDisplayName(this, assertion), caller.sourceUri) {
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
        val test = dynamicTest(findCaller().assertingDisplayName(assertion), caller.sourceUri) {
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
        val test = dynamicTest(description ?: caller.xexpectingDisplayName(action), caller.sourceUri) {
            strikt.api.expectThat(subject).with(action, additionalAssertion ?: throw IllegalUsageException("expecting", caller.sourceUri))
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
        val test = dynamicTest(findCaller().xcatchingDisplayName(action), caller.sourceUri) {
            strikt.api.expectCatching { subject.action() }.and(additionalAssertion ?: throw IllegalUsageException("expectCatching", caller.sourceUri))
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
        val test = dynamicTest(xthrowingDisplayName(E::class), caller.sourceUri) {
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
        callback(
            dynamicContainer(
                description?.takeUnlessBlank() ?: "with".xproperty(transform) + " " + DynamicTestDisplayNameGenerator.displayNameFor(aspect, null),
                PathSource.currentUri,
                nodes.stream()
            )
        )
        return CallbackCallingPropertyTestBuilder(aspect) { nodes.add(it) }
    }

    companion object {

        private class CallbackCallingPropertyTestBuilder<T>(
            private val aspect: T,
            private val callback: (DynamicNode) -> Unit,
        ) : PropertyTestBuilder<T> {
            override fun then(block: OldDynamicTestsWithSubjectBuilder<T>.(T) -> Unit): T {
                OldDynamicTestsWithSubjectBuilder(aspect, callback).block(aspect)
                return aspect
            }
        }

        /**
         * Builds an arbitrary test trees to test all necessary aspect of the specified [subject].
         */
        fun <T> build(subject: T, init: OldDynamicTestsWithSubjectBuilder<T>.(T) -> Unit): List<DynamicNode> =
            mutableListOf<DynamicNode>().apply {
                OldDynamicTestsWithSubjectBuilder(subject) { add(it) }.init(subject)
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
fun <T> OldDynamicTestsWithSubjectBuilder<*>.asserting(subject: T, assertion: Assertion<T>) {
    with(subject) {
        asserting(assertion)
    }
}

@DynamicTestsDsl
fun testsOld(init: OldDynamicTestsWithoutSubjectBuilder.() -> Unit): List<DynamicNode> =
    OldDynamicTestsWithoutSubjectBuilder.build(init)

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
class OldDynamicTestsWithoutSubjectBuilder(val tests: MutableList<DynamicNode>) {

    fun <T : Any> T.all(description: String, init: OldDynamicTestsWithSubjectBuilder<T>.(T) -> Unit) {
        val container = dynamicContainer(description, PathSource.currentUri, OldDynamicTestsWithSubjectBuilder.build(this, init).stream())
        tests.add(container)
    }

    infix fun <T : Any> T.all(init: OldDynamicTestsWithSubjectBuilder<T>.(T) -> Unit) {
        val container = dynamicContainer(
            DynamicTestDisplayNameGenerator.displayNameFor(this, null),
            PathSource.currentUri,
            OldDynamicTestsWithSubjectBuilder.build(this, init).stream()
        )
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
     * Expects this subject to fulfil the given [assertion].
     *
     * **Usage:** `<subject> asserting { <assertion> }`
     */
    @JvmName("infixAsserting")
    infix fun <T> T.asserting(assertion: Assertion<T>) {
        val caller = findCaller()
        val test = dynamicTest(findCaller().assertingDisplayName(this, assertion), caller.sourceUri) {
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
        val test = dynamicTest(findCaller().assertingDisplayName(assertion), caller.sourceUri) {
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
        val test = dynamicTest(description ?: caller.xexpectingDisplayName(action), caller.sourceUri) {
            strikt.api.expectThat(action(), additionalAssertion ?: throw IllegalUsageException("expecting", caller.sourceUri))
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
        val test = dynamicTest(findCaller().xcatchingDisplayName(action), caller.sourceUri) {
            strikt.api.expectCatching { action() }.and(additionalAssertion ?: throw IllegalUsageException("expectCatching", caller.sourceUri))
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
        val test = dynamicTest(xthrowingDisplayName(E::class), caller.sourceUri) {
            strikt.api.expectThrows<E> { action() }.and(additionalAssertion ?: {})
        }
        tests.add(test)
        return InCompleteExpectationBuilder { assertion: Assertion<E> ->
            additionalAssertion = assertion
        }
    }

    companion object {
        inline fun build(init: OldDynamicTestsWithoutSubjectBuilder.() -> Unit): List<DynamicNode> =
            mutableListOf<DynamicNode>().also { OldDynamicTestsWithoutSubjectBuilder(it).init() }
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
        val errorMessage = "expecting { … } call was not finished with that { … } at ${findCaller()}".also { buildErrors.add(it) }
        return InCompleteExpectationBuilder { assertion: Assertion<R> ->
            buildErrors.remove(errorMessage)
            strikt.api.expectThat(subject).with(
                description?.takeUnlessBlank() ?: ("with".xproperty(action) + findCaller().xexpectingDisplayName(
                    action
                )),
                action,
                assertion
            )
        }
    }

    /**
     * Expects the [Result] of the [subject] transformed by [action] to fulfil the
     * assertion returned by [DynamicTestBuilder.InCompleteExpectationBuilder].
     *
     * **Usage:** `expectCatching { 𝘴𝘶𝘣𝘫𝘦𝘤𝘵.<action> } that { <assertion> }`
     */
    fun <R> expectCatching(action: suspend T.() -> R): InCompleteExpectationBuilder<Result<R>> {
        val errorMessage = "expectCatching { … } call was not finished with that { … } at ${findCaller()}".also { buildErrors.add(it) }
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
            dynamicTest(description?.takeUnlessBlank() ?: ("test " + DynamicTestDisplayNameGenerator.displayNameFor(subject, null)), PathSource.currentUri) {
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
 * @throws IllegalStateException if called from outside a test
 */
fun withTempDir(simpleId: SimpleId, block: Path.() -> Unit) {
    val tempDir: Path = TestKommons.TestRoot.resolve(simpleId.segments.joinToString(".").toIdentifier(8).withRandomSuffix()).createDirectories()
    tempDir.block()
    check(TestKommons.TestRoot.exists()) {
        println("The shared root temp directory was deleted by $simpleId or a concurrently running test. This must not happen.".ansi.red.toString())
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
 * Runs all tests in this list of tests / test trees.
 */
fun List<DynamicNode>.execute() {
    flatten().forEach { test: DynamicTest -> test.execute() }
}

/**
 * Runs this test.
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
            val tests = JvmTestsSample().TestingSingleSubject().`as parameter`().flatten()
            strikt.api.expectThat(tests.toList()).size.isEqualTo(17)
        }
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
        fun `should run expectCatching`() {
            var testSucceeded = false
            expectCatching { throw RuntimeException("message") } that { testSucceeded = actual.exceptionOrNull()!!.message == "message" }
            strikt.api.expectThat(testSucceeded).isTrue()
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
            val tests = testEachOld("subject") {
                asserting { testSucceeded = actual == "subject" }
            }
            tests.execute()
            strikt.api.expectThat(testSucceeded).isTrue()
        }

        @Test
        fun `should run receiver asserting`() {
            var testSucceeded = false
            val tests = testEachOld("subject") {
                "other" asserting { testSucceeded = actual == "other" }
            }
            tests.execute()
            strikt.api.expectThat(testSucceeded).isTrue()
        }

        @Test
        fun `should run evaluating `() {
            var testSucceeded = false
            val tests = testEachOld("subject") {
                expecting("expectation") { "$it.prop" } that { testSucceeded = actual == "subject.prop" }
            }
            tests.execute()
            strikt.api.expectThat(testSucceeded).isTrue()
        }

        @Test
        fun `should throw on incomplete evaluating`() {
            val tests = testEachOld("subject") {
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
            val tests = testEachOld("subject") {
                expectCatching { throw RuntimeException(this) } that { testSucceeded = actual.exceptionOrNull()!!.message == "subject" }
            }
            tests.execute()
            strikt.api.expectThat(testSucceeded).isTrue()
        }

        @Test
        fun `should throw on incomplete expectCatching`() {
            val tests = testEachOld("subject") {
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
            val tests = testEachOld("subject") {
                expectThrows<RuntimeException> { throw RuntimeException(this) } that { testSucceeded = actual.message == "subject" }
            }
            tests.execute()
            strikt.api.expectThat(testSucceeded).isTrue()
        }

        @Test
        fun `should not throw on evaluating only throwable type`() {
            val tests = testEachOld("subject") {
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
            val tests = testsOld {
                asserting { testSucceeded = true }
            }
            tests.execute()
            strikt.api.expectThat(testSucceeded).isTrue()
        }

        @Test
        fun `should run receiver asserting`() {
            var testSucceeded = false
            val tests = testsOld {
                "other" asserting { testSucceeded = actual == "other" }
            }
            tests.execute()
            strikt.api.expectThat(testSucceeded).isTrue()
        }

        @Test
        fun `should run evaluating `() {
            var testSucceeded = false
            val tests = testsOld {
                expecting("expectation") { "subject" } that { testSucceeded = actual == "subject" }
            }
            tests.execute()
            strikt.api.expectThat(testSucceeded).isTrue()
        }

        @Test
        fun `should throw on incomplete evaluating`() {
            val tests = testsOld {
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
            val tests = testsOld {
                expectCatching { throw RuntimeException("message") } that { testSucceeded = actual.exceptionOrNull()!!.message == "message" }
            }
            tests.execute()
            strikt.api.expectThat(testSucceeded).isTrue()
        }

        @Test
        fun `should throw on incomplete expectCatching`() {
            val tests = testsOld {
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
            val tests = testsOld {
                expectThrows<RuntimeException> { throw RuntimeException("message") } that { testSucceeded = actual.message == "message" }
            }
            tests.execute()
            strikt.api.expectThat(testSucceeded).isTrue()
        }

        @Test
        fun `should not throw on evaluating only throwable type`() {
            val tests = testsOld {
                expectThrows<RuntimeException> { throw RuntimeException("message") }
            }
            strikt.api.expectCatching { tests.execute() }.isSuccess()
        }
    }
}
