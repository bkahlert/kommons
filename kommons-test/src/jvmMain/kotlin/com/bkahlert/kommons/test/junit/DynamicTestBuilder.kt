package com.bkahlert.kommons.test.junit

import com.bkahlert.kommons.logging.SLF4J
import com.bkahlert.kommons.test.KommonsTest
import com.bkahlert.kommons.test.junit.DynamicTestDisplayNameGenerator.FOR
import com.bkahlert.kommons.test.junit.DynamicTestDisplayNameGenerator.assertingDisplayName
import com.bkahlert.kommons.test.junit.DynamicTestDisplayNameGenerator.catchingDisplayName
import com.bkahlert.kommons.test.junit.DynamicTestDisplayNameGenerator.displayNameFor
import com.bkahlert.kommons.test.junit.DynamicTestDisplayNameGenerator.expectingDisplayName
import com.bkahlert.kommons.test.junit.DynamicTestDisplayNameGenerator.throwingDisplayName
import com.bkahlert.kommons.test.junit.Mode.CREATE
import com.bkahlert.kommons.test.junit.Mode.REPLACE
import com.bkahlert.kommons.test.junit.PathSource.Companion.sourceUri
import com.bkahlert.kommons.test.junit.SimpleIdResolver.Companion.simpleId
import com.bkahlert.kommons.test.junit.xxx.sequence2
import io.kotest.assertions.asClue
import io.kotest.assertions.assertionCounter
import io.kotest.assertions.failure
import io.kotest.assertions.withClue
import io.kotest.mpp.bestName
import org.junit.jupiter.api.DynamicContainer
import org.junit.jupiter.api.DynamicContainer.dynamicContainer
import org.junit.jupiter.api.DynamicNode
import org.junit.jupiter.api.DynamicTest.dynamicTest
import org.junit.jupiter.api.extension.AfterEachCallback
import org.junit.jupiter.api.extension.ExtensionContext
import java.net.URI
import java.util.stream.Stream
import kotlin.reflect.KClass
import kotlin.streams.asStream


/** Assertions that can be applied to a subject. */
public typealias Assertions<T> = (T) -> Unit

/** Builder that allows adding additional assertions using [it] or [that]. */
@JvmInline
public value class AssertionsBuilder<T>(
    /** Function that can be used to verify assertions passed to [it] or [that]. */
    public val applyAssertions: suspend (Assertions<T>) -> Unit,
) {

    /** Specifies [assertions] with the subject in the receiver `this`. */
    public suspend infix fun it(assertions: T.() -> Unit): Unit = applyAssertions(assertions)

    /** Specifies [assertions] with the subject passed the single parameter `it`. */
    public suspend infix fun that(assertions: (T) -> Unit): Unit = applyAssertions(assertions)
}


public enum class Mode { CREATE, REPLACE }


/** Builds tests with no subjects using a [TestsWithoutSubjectScope]. */
public fun testing(init: suspend TestsWithoutSubjectScope.() -> Unit): Stream<DynamicNode> =
    xxx(init) { DynamicTestsWithoutSubjectBuilder(it) }

/** Scope for building tests (and test containers) with no subjects. */
public interface TestsWithoutSubjectScope {
    /**
     * Expects the subject returned by [action] to fulfil the
     * [Assertions] returned by [AssertionsBuilder].
     *
     * **Usage:** `expecting { <action> } it { <assertions> }`
     *
     * **Usage:** `expecting { <action> } that { it.<assertions> }`
     */
    public suspend fun <R> expecting(description: String? = null, action: () -> R): AssertionsBuilder<R>

    /**
     * Expects the [Result] returned by [action] to fulfil the
     * [Assertions] returned by [AssertionsBuilder].
     *
     * **Usage:** `expectCatching { <action> } it { <assertions> }`
     *
     * **Usage:** `expectCatching { <action> } that { it.<assertions> }`
     */
    public suspend fun <R> expectCatching(action: () -> R): AssertionsBuilder<Result<R>>

    /**
     * Expects an exception [E] to be thrown when running [action]
     * and to optionally fulfil the [Assertions] returned by [AssertionsBuilder].
     *
     * **Usage:** `expectThrows<Exception> { <action> }`
     *
     * **Usage:** `expectThrows<Exception> { <action> } it { <assertions> }`
     *
     * **Usage:** `expectThrows<Exception> { <action> } that { it.<assertions> }`
     */
    public suspend fun <E : Throwable> expectThrows(type: KClass<E>, action: () -> Any?): AssertionsBuilder<E>
}

/**
 * Expects an exception [E] to be thrown when running [action]
 * and to optionally fulfil the [Assertions] returned by [AssertionsBuilder].
 *
 * **Usage:** `expectThrows<Exception> { <action> }`
 *
 * **Usage:** `expectThrows<Exception> { <action> } it { <assertions> }`
 *
 * **Usage:** `expectThrows<Exception> { <action> } that { it.<assertions> }`
 */
public suspend inline fun <reified E : Throwable> TestsWithoutSubjectScope.expectThrows(noinline action: () -> Any?): AssertionsBuilder<E> =
    expectThrows(E::class, action)


/** Builder for tests (and test containers) with no subjects. */
private class DynamicTestsWithoutSubjectBuilder(
    public val addDynamicNode: suspend (Mode, DynamicNode) -> Unit,
) : TestsWithoutSubjectScope {

    override suspend fun <R> expecting(description: String?, action: () -> R): AssertionsBuilder<R> {
        val caller = KommonsTest.locateCall()
        val displayName = description ?: caller.expectingDisplayName(action)
        addDynamicNode(CREATE, dynamicTest(displayName, caller.sourceUri) { throw IllegalUsageException("expecting", caller.sourceUri) })
        return AssertionsBuilder { assertions: Assertions<R> ->
            addDynamicNode(REPLACE, dynamicTest(displayName, caller.sourceUri) { action().asClue(assertions) })
        }
    }

    override suspend fun <R> expectCatching(action: () -> R): AssertionsBuilder<Result<R>> {
        val caller = KommonsTest.locateCall()
        val displayName = caller.catchingDisplayName(action)
        addDynamicNode(CREATE, dynamicTest(displayName, caller.sourceUri) { throw IllegalUsageException("expectCatching", caller.sourceUri) })
        return AssertionsBuilder { assertions: Assertions<Result<R>> ->
            addDynamicNode(REPLACE, dynamicTest(displayName, caller.sourceUri) { runCatching(action).asClue(assertions) })
        }
    }

    public override suspend fun <E : Throwable> expectThrows(type: KClass<E>, action: () -> Any?): AssertionsBuilder<E> {
        val caller = KommonsTest.locateCall()
        val displayName = throwingDisplayName(type)
        addDynamicNode(CREATE, dynamicTest(displayName, caller.sourceUri) { shouldThrow(type, action).asClue({}) })
        return AssertionsBuilder { assertions: Assertions<E> ->
            addDynamicNode(REPLACE, dynamicTest(displayName, caller.sourceUri) { shouldThrow(type, action).asClue(assertions) })
        }
    }
}

private fun <T : Throwable> shouldThrow(expectedExceptionClass: KClass<T>, block: () -> Any?): T {
    assertionCounter.inc()
    val thrownThrowable = try {
        block()
        null  // Can't throw failure here directly, as it would be caught by the catch clause, and it's an AssertionError, which is a special case
    } catch (thrown: Throwable) {
        thrown
    }

    @Suppress("UNCHECKED_CAST")
    return when {
        thrownThrowable == null -> {
            throw failure("Expected exception ${expectedExceptionClass.bestName()} but no exception was thrown.")
        }

        expectedExceptionClass.isInstance(thrownThrowable) -> {
            // This should be before `is AssertionError`. If the user is purposefully trying to verify `shouldThrow<AssertionError>{}` this will take priority
            thrownThrowable as T
        }

        thrownThrowable is AssertionError -> {
            throw thrownThrowable
        }

        else -> {
            throw failure(
                "Expected exception ${expectedExceptionClass.bestName()} but a ${thrownThrowable::class.simpleName} was thrown instead.",
                thrownThrowable
            )
        }
    }
}

public fun <C> xxx(
    init: suspend C.() -> Unit,
    initBuilder: (suspend (Mode, DynamicNode) -> Unit) -> C,
): Stream<DynamicNode> = sequence2<DynamicNode> {
    var nextNode: DynamicNode? = null
    initBuilder { mode, newNode ->
        when (mode) {
            CREATE -> {
                nextNode?.also { yield(it) }
                nextNode = newNode
            }

            REPLACE -> {
                nextNode?.also {
                    if (!it.testSourceUri.equals(newNode.testSourceUri)) {
                        throw IllegalStateException("${newNode.testSourceUri} attempts to replace the supposedly not fully built test ${it.testSourceUri}")
                    }
                }
                nextNode = newNode
            }
        }
    }.init()
    nextNode?.also { yield(it) }
}.asStream()

/**
 * Builds tests with the specified [subject] using a [TestsWithSubjectScope].
 */
public fun <T> testing(subject: T, init: suspend TestsWithSubjectScope<T>.() -> Unit): Stream<DynamicNode> =
    xxx(init) { DynamicTestsWithSubjectBuilder(subject, it) }

/**
 * Builds tests for each of the specified [subjects] using a [TestsWithSubjectScope].
 *
 * The name for each container is heuristically derived but can also be explicitly specified using [containerNamePattern]
 * which supports curly placeholders `{}` like [SLF4J] does.
 */
public fun <T> testingAll(
    vararg subjects: T,
    containerNamePattern: String? = null,
    init: suspend TestsWithSubjectScope<T>.() -> Unit,
): Stream<DynamicContainer> = subjects.asList().testingAll(containerNamePattern, init)

/**
 * Builds tests for each subject of this [Collection] using a [TestsWithSubjectScope].
 *
 * The name for each container is heuristically derived but can also be explicitly specified using [containerNamePattern]
 * which supports curly placeholders `{}` like [SLF4J] does.
 */
public fun <T> Iterable<T>.testingAll(
    containerNamePattern: String? = null,
    init: suspend TestsWithSubjectScope<T>.() -> Unit,
): Stream<DynamicContainer> = toList()
    .also { require(it.isNotEmpty()) { "At least one subject must be provided for testing." } }
    .map { subject ->
        dynamicContainer(
            "$FOR ${displayNameFor(subject, containerNamePattern)}",
            PathSource.currentUri,
            testing(subject, init)
        )
    }.stream()

/**
 * Builds tests for each subject of this [Sequence] using a [TestsWithSubjectScope].
 *
 * The name for each container is heuristically derived but can also be explicitly specified using [containerNamePattern]
 * which supports curly placeholders `{}` like [SLF4J] does.
 */
public fun <T> Sequence<T>.testingAll(
    containerNamePattern: String? = null,
    init: TestsWithSubjectScope<T>.() -> Unit,
): Stream<DynamicContainer> = toList().testingAll(containerNamePattern, init)

/**
 * Builds tests for each entry of this [Map] using a [TestsWithSubjectScope].
 *
 * The name for each container is heuristically derived but can also be explicitly specified using [containerNamePattern]
 * which supports curly placeholders `{}` like [SLF4J] does.
 */
public fun <K, V> Map<K, V>.testingAll(
    containerNamePattern: String? = null,
    init: TestsWithSubjectScope<Map.Entry<K, V>>.() -> Unit,
): Stream<DynamicContainer> = entries.testingAll(containerNamePattern, init)

/** Scope for building tests (and test containers) with a subject. */
public interface TestsWithSubjectScope<T> {
    /**
     * Expects the subject to fulfil the given [assertions].
     *
     * **Usage:** `it { 𝘴𝘶𝘣𝘫𝘦𝘤𝘵.<assertions> }`
     */
    public suspend fun it(assertions: T.() -> Unit)

    /**
     * Expects the subject to fulfil the given [assertions].
     *
     * **Usage:** `that { it.<assertions> }`
     */
    public suspend fun that(assertions: Assertions<T>)

    /**
     * Expects the subject transformed by [action] to fulfil the
     * [Assertions] returned by [AssertionsBuilder].
     *
     * **Usage:** `expecting { 𝘴𝘶𝘣𝘫𝘦𝘤𝘵.<action> } it { <assertions> }`
     *
     * **Usage:** `expecting { 𝘴𝘶𝘣𝘫𝘦𝘤𝘵.<action> } that { it.<assertions> }`
     */
    public suspend fun <R> expecting(description: String? = null, action: T.() -> R): AssertionsBuilder<R>

    /**
     * Expects the [Result] of the subject transformed by [action] to fulfil the
     * [Assertions] returned by [AssertionsBuilder].
     *
     * **Usage:** `expectCatching { 𝘴𝘶𝘣𝘫𝘦𝘤𝘵.<action> } it { <assertions> }`
     *
     * **Usage:** `expectCatching { 𝘴𝘶𝘣𝘫𝘦𝘤𝘵.<action> } that { it.<assertions> }`
     */
    public suspend fun <R> expectCatching(action: T.() -> R): AssertionsBuilder<Result<R>>

    /**
     * Expects an exception [E] to be thrown when transforming the subject with [action]
     * and to optionally fulfil the [Assertions] returned by [AssertionsBuilder].
     *
     * **Usage:** `expectThrows<Exception> { 𝘴𝘶𝘣𝘫𝘦𝘤𝘵.<action> }`
     *
     * **Usage:** `expectThrows<Exception> { 𝘴𝘶𝘣𝘫𝘦𝘤𝘵.<action> } it { <assertions> }`
     *
     * **Usage:** `expectThrows<Exception> { 𝘴𝘶𝘣𝘫𝘦𝘤𝘵.<action> } that { it.<assertions> }`
     */
    public suspend fun <E : Throwable> expectThrows(type: KClass<E>, action: T.() -> Any?): AssertionsBuilder<E>
}

/**
 * Expects an exception [E] to be thrown when transforming the subject with [action]
 * and to optionally fulfil the [Assertions] returned by [AssertionsBuilder].
 *
 * **Usage:** `expectThrows<Exception> { 𝘴𝘶𝘣𝘫𝘦𝘤𝘵.<action> }`
 *
 * **Usage:** `expectThrows<Exception> { 𝘴𝘶𝘣𝘫𝘦𝘤𝘵.<action> } it { <assertions> }`
 *
 * **Usage:** `expectThrows<Exception> { 𝘴𝘶𝘣𝘫𝘦𝘤𝘵.<action> } that { it.<assertions> }`
 */
public suspend inline fun <reified E : Throwable, T> TestsWithSubjectScope<T>.expectThrows(noinline action: T.() -> Any?): AssertionsBuilder<E> =
    expectThrows(E::class, action)


/** Builder for tests (and test containers) with the specified [subject]. */
private class DynamicTestsWithSubjectBuilder<T>(
    public val subject: T,
    public val addDynamicNode: suspend (Mode, DynamicNode) -> Unit,
) : TestsWithSubjectScope<T> {

    override suspend fun it(assertions: T.() -> Unit) {
        val caller = KommonsTest.locateCall()
        val displayName = caller.assertingDisplayName(subject, assertions)
        addDynamicNode(CREATE, dynamicTest(displayName, caller.sourceUri) { subject.asClue(assertions) })
    }

    override suspend fun that(assertions: Assertions<T>) {
        val caller = KommonsTest.locateCall()
        val displayName = caller.assertingDisplayName(subject, assertions)
        addDynamicNode(CREATE, dynamicTest(displayName, caller.sourceUri) { subject.asClue(assertions) })
    }

    override suspend fun <R> expecting(description: String?, action: T.() -> R): AssertionsBuilder<R> {
        val caller = KommonsTest.locateCall()
        val displayName = description ?: caller.expectingDisplayName(action)
        addDynamicNode(CREATE, dynamicTest(displayName, caller.sourceUri) { throw IllegalUsageException("expecting", caller.sourceUri) })
        return AssertionsBuilder { assertions: Assertions<R> ->
            addDynamicNode(REPLACE, dynamicTest(displayName, caller.sourceUri) {
                withClue(subject) { subject.action().asClue(assertions) }
            })
        }
    }

    override suspend fun <R> expectCatching(action: T.() -> R): AssertionsBuilder<Result<R>> {
        val caller = KommonsTest.locateCall()
        val displayName = caller.catchingDisplayName(action)
        addDynamicNode(CREATE, dynamicTest(displayName, caller.sourceUri) { throw IllegalUsageException("expectCatching", caller.sourceUri) })
        return AssertionsBuilder { assertions: Assertions<Result<R>> ->
            addDynamicNode(REPLACE, dynamicTest(displayName, caller.sourceUri) {
                withClue(subject) { subject.runCatching(action).asClue(assertions) }
            })
        }
    }

    override suspend fun <E : Throwable> expectThrows(type: KClass<E>, action: T.() -> Any?): AssertionsBuilder<E> {
        val caller = KommonsTest.locateCall()
        val displayName = throwingDisplayName(type)
        addDynamicNode(CREATE, dynamicTest(displayName, caller.sourceUri) {
            withClue(subject) { shouldThrow(type) { subject.action() }.asClue({}) }
        })
        return AssertionsBuilder { assertions: Assertions<E> ->
            addDynamicNode(REPLACE, dynamicTest(displayName, caller.sourceUri) {
                withClue(subject) { shouldThrow(type) { subject.action() }.asClue(assertions) }
            })
        }
    }
}


/** Exception that is thrown if the API is incorrectly used. */
public class IllegalUsageException(function: String, caller: URI?) : IllegalArgumentException(
    "$function { … } call was not finished with \"that { … }\"".let {
        caller?.let { uri -> "$it at " + uri.path + ":" + uri.query.takeLastWhile { it.isDigit() } } ?: it
    }
)


/**
 * Extension that checks if [DynamicTestsWithSubjectBuilder.expecting] or [DynamicTestsWithSubjectBuilder.expectCatching]
 * where incorrectly used.
 *
 * ***Important:**
 * For this extension to work, it needs to be registered.*
 *
 * > The most convenient way to register this extension
 * > for all tests is by adding the line **`com.bkahlert.kommons.test.junit.IllegalUsageCheck`** to the
 * > file **`resources/META-INF/services/org.junit.jupiter.api.extension.Extension`**.
 */
internal class IllegalUsageCheck : AfterEachCallback {

    override fun afterEach(context: ExtensionContext) {
        val id: SimpleId = context.simpleId
        val illegalUsage = illegalUsages[id]
        if (illegalUsage != null) throw illegalUsage
    }

    companion object {
        val illegalUsages: MutableMap<SimpleId, IllegalUsageException> = mutableMapOf()
    }
}
