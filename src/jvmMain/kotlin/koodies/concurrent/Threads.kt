package koodies.concurrent

import koodies.logging.RenderingLogger
import koodies.time.sleep
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executor
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.concurrent.thread
import kotlin.time.Duration
import kotlin.time.milliseconds

fun <T> withThreadName(temporaryName: String, block: () -> T): T =
    with(Thread.currentThread()) {
        val oldName = name.also { name = temporaryName }
        runCatching(block).also { name = oldName }.getOrThrow()
    }

fun thread(
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

fun daemon(
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
class BusyThread private constructor(private var stopped: AtomicBoolean, private val logger: RenderingLogger? = null) : Thread({
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
    constructor(logger: RenderingLogger? = null) : this(AtomicBoolean(false), logger)

    init {
        start()
    }

    fun complete() {
        logger?.logLine { "stopping" }
        stopped.set(true)
        interrupt()
        logger?.logLine { "stopped" }
    }
}

private val cachedThreadPool = Executors.newCachedThreadPool()

fun <T> Executor.completableFuture(
    delay: Duration = Duration.ZERO,
    name: String? = null,
    block: () -> T,
): CompletableFuture<T> =
    CompletableFuture.supplyAsync({
        delay.sleep()
        name?.let { withThreadName(name, block) } ?: block()
    }, this) ?: error("Error creating ${CompletableFuture::class.simpleName}")

fun <T> completableFuture(
    delay: Duration = Duration.ZERO,
    name: String? = null,
    executor: Executor = cachedThreadPool,
    block: () -> T,
): CompletableFuture<T> =
    executor.completableFuture(delay = delay, name = name, block = block)

fun <T> (() -> T).completableFuture(
    delay: Duration = Duration.ZERO,
    name: String? = null,
    executor: Executor = cachedThreadPool,
): CompletableFuture<T> =
    executor.completableFuture(delay = delay, name = name, block = this)
