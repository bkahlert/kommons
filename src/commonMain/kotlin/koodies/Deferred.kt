package koodies

/**
 * A captured [Function] which is kept to be evaluated at a later
 * moment in time by calling [evaluate].
 *
 * @see Capture
 */
public class Deferred<R>(function: () -> R) {
    private val result: R by lazy(function)

    /**
     * Evaluates the captured invocation and returns its result.
     */
    public fun evaluate(): R = result

    override fun toString(): String = asString(this::result)
}

/**
 * Creates a [Deferred] evaluation of the given [function].
 */
public fun <R> defer(function: () -> R): Deferred<R> = Deferred(function)

/**
 * Returns [Deferred] that evaluates to `null`.
 */
public fun <R> deferNull(): Deferred<out R?> = deferredNull
private val deferredNull: Deferred<Nothing?> = Deferred { null }

/**
 * Evaluates the captured invocation and returns its result if it is an instance
 * of type [T] or `null` otherwise.
 */
public fun <T> Deferred<T>.evaluateOrNull(): T? =
    kotlin.runCatching { evaluate() }.getOrNull()

/**
 * Evaluates the captured invocation and returns its result if it is an instance
 * of type [T] or the given [defaultValue] otherwise.
 */
public fun <T : R, R> Deferred<T>.evaluateOrDefault(defaultValue: R): R =
    kotlin.runCatching { evaluate() }.getOrDefault(defaultValue)

/**
 * Evaluates the captured invocation and returns its result if it is an instance
 * of type [T] or the otherwise caught exception mapped using the given [onFailure].
 */
public fun <T : R, R> Deferred<T>.evaluateOrElse(onFailure: (exception: Throwable) -> R): R =
    kotlin.runCatching { evaluate() }.getOrElse(onFailure)

/**
 * Evaluates the captured invocation and returns its result if it is an instance
 * or rethrows the otherwise caught exceptions.
 */
public fun <T : R, R> Deferred<T>.evaluateOrThrow(): T =
    kotlin.runCatching { evaluate() }.getOrThrow()


/**
 * Returns a capture evaluating to the result of applying the given [transform]
 * function to the evaluation of `this` deferred invocation.
 */
public inline fun <T, R> Deferred<T>.letContent(crossinline transform: T.() -> R): Deferred<R> =
    defer { evaluate().transform() }

/**
 * Returns a capture evaluating to the result of applying applying the given [transform]
 * function to each element of the evaluation of `this` deferred invocation.
 */
public inline fun <T, R> Deferred<Iterable<T>>.map(crossinline transform: T.() -> R): Deferred<List<R>> =
    defer { evaluate().map { it.transform() } }

/**
 * Returns a capture evaluating only to the non-null results of applying applying
 * the given [transform] to each element of the evaluation of `this` deferred
 * invocation.
 */
public inline fun <T, R : Any> Deferred<Iterable<T>>.mapNotNull(crossinline transform: T.() -> R?): Deferred<List<R>> =
    defer { evaluate().mapNotNull { element -> element.transform() } }

/**
 * The moment `this` deferred invocation is evaluated, applies the given
 * [transform] to each element of the its result and appends only the returned
 * non-null values to the given [destination].
 */
public inline fun <T, R : Any, C : MutableCollection<in R>> Deferred<Iterable<T>>.mapNotNullTo(destination: C, crossinline transform: T.() -> R?): C =
    destination.apply { defer { evaluate().map { element -> element.transform()?.let { add(it) } } } }

/**
 * The moment `this` deferred invocation is evaluated, applies the given
 * [transform] to each element of the its result and appends the returned
 * values to the given [destination].
 */
public inline fun <T, R, C : MutableCollection<in R>> Deferred<Iterable<T>>.mapTo(destination: C, crossinline transform: T.() -> R): C =
    destination.apply { defer { evaluate().map { add(it.transform()) } } }
