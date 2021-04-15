package koodies.jvm

import koodies.logging.RenderingLogger
import koodies.runWrapping
import koodies.time.sleep
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CompletionStage
import java.util.concurrent.Executor
import java.util.concurrent.Executors
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.concurrent.thread
import kotlin.time.Duration
import kotlin.time.milliseconds

public fun <T> withThreadName(temporaryName: String, block: () -> T): T =
    currentThread.runWrapping({ name.also { name = temporaryName } }, { oldName -> name = oldName }, { block() })

public fun thread(
    start: Boolean = true,
    contextClassLoader: ClassLoader? = null,
    name: String? = null,
    priority: Int = -1,
    block: () -> Unit,
): Thread = thread(
    start = start,
    isDaemon = false,
    contextClassLoader = contextClassLoader,
    name = name,
    priority = priority,
    block = block
)

public fun daemon(
    start: Boolean = true,
    contextClassLoader: ClassLoader? = null,
    name: String? = null,
    priority: Int = -1,
    block: () -> Unit,
): Thread = thread(
    start = start,
    isDaemon = true,
    contextClassLoader = contextClassLoader,
    name = name,
    priority = priority,
    block = block
)


/**
 * [Thread] that drains your battery and can only be stopped by calling [stop].
 */
public class BusyThread private constructor(private var stopped: AtomicBoolean, private val logger: RenderingLogger? = null) : Thread({
    while (!stopped.get()) {
        logger?.logLine { "THREAD stopped? $stopped" }
        try {
            logger?.logLine { "busy" }
            50.milliseconds.sleep()
        } catch (e: InterruptedException) {
            if (!stopped.get()) currentThread().interrupt()
            else logger?.logLine { "interruption ignored" }
        }
    }
}) {
    public constructor(logger: RenderingLogger? = null) : this(AtomicBoolean(false), logger)

    init {
        start()
    }

    public fun complete() {
        logger?.logLine { "stopping" }
        stopped.set(true)
        interrupt()
        logger?.logLine { "stopped" }
    }
}

private val cachedThreadPool = Executors.newCachedThreadPool()

/**
 * Returns a [CompletableFuture] using `this` [Executor] which
 * will return a [CompletionStage] with the result of the specified [block].
 *
 * Optionally the execution can have a [delay] and change the [name] of
 * the executing [Thread].
 */
public fun <T> Executor.completableFuture(
    delay: Duration = Duration.ZERO,
    name: String? = null,
    block: () -> T,
): CompletableFuture<T> =
    CompletableFuture.supplyAsync({
        delay.sleep()
        name?.let { withThreadName(name, block) } ?: block()
    }, this) ?: error("Error creating ${CompletableFuture::class.simpleName}")

/**
 * Returns a [CompletableFuture] which will return a [CompletionStage]
 * with the result of the specified [block].
 *
 * Optionally the execution can have a [delay] and change the [name] of
 * the executing [Thread]. If not specified a shared [ThreadPoolExecutor]
 * is used to provide the executing [Thread].
 */
public fun <T> completableFuture(
    delay: Duration = Duration.ZERO,
    name: String? = null,
    executor: Executor = cachedThreadPool,
    block: () -> T,
): CompletableFuture<T> =
    executor.completableFuture(delay = delay, name = name, block = block)

/**
 * Returns a [CompletableFuture] which will return a [CompletionStage]
 * with the result of `this` lambda.
 *
 * Optionally the execution can have a [delay] and change the [name] of
 * the executing [Thread]. If not specified a shared [ThreadPoolExecutor]
 * is used to provide the executing [Thread].
 */
public fun <T> (() -> T).completableFuture(
    delay: Duration = Duration.ZERO,
    name: String? = null,
    executor: Executor = cachedThreadPool,
): CompletableFuture<T> =
    executor.completableFuture(delay = delay, name = name, block = this)

/**
 * Returns the **same** [CompletionStage] that,
 * is completely independent of the one added as a side-effect
 * in order to process the specified [fn].
 */
public fun <T : CompletableFuture<V>, V> T.thenAlso(fn: (value: V?, exception: Throwable?) -> Unit): T =
    this.also { it.handle(fn) }

/**
 * Returns the **same** [CompletionStage] that,
 * is completely independent of the one added as a side-effect
 * in order to process the specified [fn].
 */
public fun <T : CompletableFuture<V>, V> T.thenAlso(fn: (Result<V?>) -> Unit): T =
    thenAlso { value: V?, exception: Throwable? -> fn(exception?.let { Result.failure(it) } ?: Result.success(value)) }
