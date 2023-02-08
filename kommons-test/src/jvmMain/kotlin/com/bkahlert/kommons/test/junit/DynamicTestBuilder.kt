package com.bkahlert.kommons.test.junit

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
import io.kotest.assertions.asClue
import io.kotest.assertions.assertionCounter
import io.kotest.assertions.failure
import io.kotest.assertions.withClue
import io.kotest.mpp.bestName
import org.junit.jupiter.api.DynamicContainer
import org.junit.jupiter.api.DynamicContainer.dynamicContainer
import org.junit.jupiter.api.DynamicNode
import org.junit.jupiter.api.DynamicTest.dynamicTest
import java.net.URI
import java.util.stream.Stream
import kotlin.coroutines.Continuation
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.coroutines.intrinsics.COROUTINE_SUSPENDED
import kotlin.coroutines.intrinsics.createCoroutineUnintercepted
import kotlin.coroutines.intrinsics.suspendCoroutineUninterceptedOrReturn
import kotlin.coroutines.resume
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


/** Builds tests with no subjects using a [TestsWithoutSubjectScope]. */
public fun testing(init: suspend TestsWithoutSubjectScope.() -> Unit): Stream<DynamicNode> =
    buildTestNodeSequence(init) { DynamicTestsWithoutSubjectBuilder(it) }.asNonEmptyStream()

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
    val yieldNode: suspend (Mode, DynamicNode) -> Unit,
) : TestsWithoutSubjectScope {

    override suspend fun <R> expecting(description: String?, action: () -> R): AssertionsBuilder<R> {
        val caller = KommonsTest.locateCall()
        val displayName = description ?: caller.expectingDisplayName(action)
        yieldNode(CREATE, dynamicTest(displayName, caller.sourceUri) { throw IllegalUsageException("expecting", caller.sourceUri) })
        return AssertionsBuilder { assertions: Assertions<R> ->
            yieldNode(REPLACE, dynamicTest(displayName, caller.sourceUri) { action().asClue(assertions) })
        }
    }

    override suspend fun <R> expectCatching(action: () -> R): AssertionsBuilder<Result<R>> {
        val caller = KommonsTest.locateCall()
        val displayName = caller.catchingDisplayName(action)
        yieldNode(CREATE, dynamicTest(displayName, caller.sourceUri) { throw IllegalUsageException("expectCatching", caller.sourceUri) })
        return AssertionsBuilder { assertions: Assertions<Result<R>> ->
            yieldNode(REPLACE, dynamicTest(displayName, caller.sourceUri) { runCatching(action).asClue(assertions) })
        }
    }

    override suspend fun <E : Throwable> expectThrows(type: KClass<E>, action: () -> Any?): AssertionsBuilder<E> {
        val caller = KommonsTest.locateCall()
        val displayName = throwingDisplayName(type)
        yieldNode(CREATE, dynamicTest(displayName, caller.sourceUri) { shouldThrow(type, action).asClue({}) })
        return AssertionsBuilder { assertions: Assertions<E> ->
            yieldNode(REPLACE, dynamicTest(displayName, caller.sourceUri) { shouldThrow(type, action).asClue(assertions) })
        }
    }
}

/**
 * Copy of [io.kotest.assertions.throwables.shouldThrow] that allows passing
 * the [expectedExceptionClass] as a [KClass] instance.
 */
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

/**
 * Builds tests with the specified [subject] using a [TestsWithSubjectScope].
 */
public fun <T> testing(subject: T, init: suspend TestsWithSubjectScope<T>.() -> Unit): Stream<DynamicNode> =
    buildTestNodeSequence(init) { DynamicTestsWithSubjectBuilder(subject, it) }.asNonEmptyStream()

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
): Stream<DynamicContainer> = subjects.asIterable().testingAll(containerNamePattern, init)

/**
 * Builds tests for each subject of this [Collection] using a [TestsWithSubjectScope].
 *
 * The name for each container is heuristically derived but can also be explicitly specified using [containerNamePattern]
 * which supports curly placeholders `{}` like [SLF4J] does.
 */
public fun <T> Iterable<T>.testingAll(
    containerNamePattern: String? = null,
    init: suspend TestsWithSubjectScope<T>.() -> Unit,
): Stream<DynamicContainer> = map { subject ->
    dynamicContainer(
        "$FOR ${displayNameFor(subject, containerNamePattern)}",
        PathSource.currentUri,
        testing(subject, init)
    )
}.asSequence().asNonEmptyStream()

/**
 * Builds tests for each subject of this [Sequence] using a [TestsWithSubjectScope].
 *
 * The name for each container is heuristically derived but can also be explicitly specified using [containerNamePattern]
 * which supports curly placeholders `{}` like [SLF4J] does.
 */
public fun <T> Sequence<T>.testingAll(
    containerNamePattern: String? = null,
    init: TestsWithSubjectScope<T>.() -> Unit,
): Stream<DynamicContainer> = asIterable().testingAll(containerNamePattern, init)

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
    val subject: T,
    val yieldNode: suspend (Mode, DynamicNode) -> Unit,
) : TestsWithSubjectScope<T> {

    override suspend fun it(assertions: T.() -> Unit) {
        val caller = KommonsTest.locateCall()
        val displayName = caller.assertingDisplayName(subject, assertions)
        yieldNode(CREATE, dynamicTest(displayName, caller.sourceUri) { subject.asClue(assertions) })
    }

    override suspend fun that(assertions: Assertions<T>) {
        val caller = KommonsTest.locateCall()
        val displayName = caller.assertingDisplayName(subject, assertions)
        yieldNode(CREATE, dynamicTest(displayName, caller.sourceUri) { subject.asClue(assertions) })
    }

    override suspend fun <R> expecting(description: String?, action: T.() -> R): AssertionsBuilder<R> {
        val caller = KommonsTest.locateCall()
        val displayName = description ?: caller.expectingDisplayName(action)
        yieldNode(CREATE, dynamicTest(displayName, caller.sourceUri) { throw IllegalUsageException("expecting", caller.sourceUri) })
        return AssertionsBuilder { assertions: Assertions<R> ->
            yieldNode(REPLACE, dynamicTest(displayName, caller.sourceUri) {
                withClue(subject) { subject.action().asClue(assertions) }
            })
        }
    }

    override suspend fun <R> expectCatching(action: T.() -> R): AssertionsBuilder<Result<R>> {
        val caller = KommonsTest.locateCall()
        val displayName = caller.catchingDisplayName(action)
        yieldNode(CREATE, dynamicTest(displayName, caller.sourceUri) { throw IllegalUsageException("expectCatching", caller.sourceUri) })
        return AssertionsBuilder { assertions: Assertions<Result<R>> ->
            yieldNode(REPLACE, dynamicTest(displayName, caller.sourceUri) {
                withClue(subject) { subject.runCatching(action).asClue(assertions) }
            })
        }
    }

    override suspend fun <E : Throwable> expectThrows(type: KClass<E>, action: T.() -> Any?): AssertionsBuilder<E> {
        val caller = KommonsTest.locateCall()
        val displayName = throwingDisplayName(type)
        yieldNode(CREATE, dynamicTest(displayName, caller.sourceUri) {
            withClue(subject) { shouldThrow(type) { subject.action() }.asClue({}) }
        })
        return AssertionsBuilder { assertions: Assertions<E> ->
            yieldNode(REPLACE, dynamicTest(displayName, caller.sourceUri) {
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

private fun <T : DynamicNode> Sequence<T>.asNonEmptyStream() =
    ifEmpty { throw IllegalStateException("No tests were created.") }.asStream()


private enum class Mode { CREATE, REPLACE }

/**
 * Builds a sequence of dynamic nodes using [init]
 * operating on the scope/builder [S].
 *
 * [initBuilder] has to provide a new builder
 * that can use the passed suspend function to yield
 * dynamic nodes and if necessary, [REPLACE] the
 * previously yielded one.
 */
private fun <S> buildTestNodeSequence(
    init: suspend S.() -> Unit,
    initBuilder: (suspend (Mode, DynamicNode) -> Unit) -> S,
): Sequence<DynamicNode> = unrestrictedSequence<DynamicNode> {
    var scheduledNode: DynamicNode? = null
    initBuilder { mode, newNode ->
        when (mode) {
            CREATE -> {
                scheduledNode?.also { yield(it) }
                scheduledNode = newNode
            }

            REPLACE -> {
                scheduledNode?.also {
                    if (!it.testSourceUri.equals(newNode.testSourceUri)) {
                        throw IllegalStateException("${newNode.testSourceUri} attempts to replace the supposedly not fully built test ${it.testSourceUri}")
                    }
                }
                scheduledNode = newNode
            }
        }
    }.init()
    scheduledNode?.also { yield(it) }
}


/**
 * Simplified copy of [kotlin.sequences.SequenceScope] to
 * support building tests while streaming them.
 */
private abstract class SequenceScope<in T> {
    /** @see kotlin.sequences.SequenceScope.yield */
    abstract suspend fun yield(value: T)

    /** @see kotlin.sequences.SequenceScope.yieldAll */
    abstract suspend fun yieldAll(iterator: Iterator<T>)

    /** @see kotlin.sequences.SequenceScope.yieldAll */
    suspend fun yieldAll(elements: Iterable<T>) {
        if (elements is Collection && elements.isEmpty()) return
        return yieldAll(elements.iterator())
    }

    /** @see kotlin.sequences.SequenceScope.yieldAll */
    suspend fun yieldAll(sequence: Sequence<T>): Unit = yieldAll(sequence.iterator())
}

private fun <T> unrestrictedSequence(@BuilderInference block: suspend SequenceScope<T>.() -> Unit): Sequence<T> = Sequence { unrestrictedIterator(block) }

private fun <T> unrestrictedIterator(@BuilderInference block: suspend SequenceScope<T>.() -> Unit): Iterator<T> {
    val iterator = SequenceBuilderIterator<T>()
    iterator.nextStep = block.createCoroutineUnintercepted(receiver = iterator, completion = iterator)
    return iterator
}

private typealias State = Int

private const val State_NotReady: State = 0
private const val State_ManyNotReady: State = 1
private const val State_ManyReady: State = 2
private const val State_Ready: State = 3
private const val State_Done: State = 4
private const val State_Failed: State = 5

private class SequenceBuilderIterator<T> : SequenceScope<T>(), Iterator<T>, Continuation<Unit> {
    private var state = State_NotReady
    private var nextValue: T? = null
    private var nextIterator: Iterator<T>? = null
    var nextStep: Continuation<Unit>? = null

    override fun hasNext(): Boolean {
        while (true) {
            when (state) {
                State_NotReady -> {}
                State_ManyNotReady ->
                    if (nextIterator!!.hasNext()) {
                        state = State_ManyReady
                        return true
                    } else {
                        nextIterator = null
                    }

                State_Done -> return false
                State_Ready, State_ManyReady -> return true
                else -> throw exceptionalState()
            }

            state = State_Failed
            val step = nextStep!!
            nextStep = null
            step.resume(Unit)
        }
    }

    override fun next(): T {
        when (state) {
            State_NotReady, State_ManyNotReady -> return nextNotReady()
            State_ManyReady -> {
                state = State_ManyNotReady
                return nextIterator!!.next()
            }

            State_Ready -> {
                state = State_NotReady
                @Suppress("UNCHECKED_CAST")
                val result = nextValue as T
                nextValue = null
                return result
            }

            else -> throw exceptionalState()
        }
    }

    private fun nextNotReady(): T {
        if (!hasNext()) throw NoSuchElementException() else return next()
    }

    private fun exceptionalState(): Throwable = when (state) {
        State_Done -> NoSuchElementException()
        State_Failed -> IllegalStateException("Iterator has failed.")
        else -> IllegalStateException("Unexpected state of the iterator: $state")
    }


    override suspend fun yield(value: T) {
        nextValue = value
        state = State_Ready
        return suspendCoroutineUninterceptedOrReturn { c ->
            nextStep = c
            COROUTINE_SUSPENDED
        }
    }

    override suspend fun yieldAll(iterator: Iterator<T>) {
        if (!iterator.hasNext()) return
        nextIterator = iterator
        state = State_ManyReady
        return suspendCoroutineUninterceptedOrReturn { c ->
            nextStep = c
            COROUTINE_SUSPENDED
        }
    }

    // Completion continuation implementation
    override fun resumeWith(result: Result<Unit>) {
        result.getOrThrow() // just rethrow exception if it's there
        state = State_Done
    }

    override val context: CoroutineContext
        get() = EmptyCoroutineContext
}
