package koodies.jvm

import koodies.io.Koodies.InternalTemp
import koodies.io.path.appendLine
import koodies.io.path.delete
import koodies.runtime.onExit
import java.lang.reflect.AnnotatedElement
import java.lang.reflect.Method
import java.nio.file.Path
import java.security.AccessControlException
import java.util.Optional
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.thread
import kotlin.concurrent.withLock

/**
 * If a value [Optional.isPresent], returns the value. Otherwise returns `null`.
 */
public inline fun <reified T> Optional<T>?.orNull(): T? = this?.orElse(null)

/**
 * Enclosing class of `this` class, if any. `null` otherwise.
 */
public val Class<*>.ancestor: Class<*>? get() = enclosingClass

/**
 * All ancestors of `this` class, **including this class itself** (≙ ancestor of zeroth degree).
 */
public val Class<*>.ancestors: List<Class<*>> get() = generateSequence(this) { it.ancestor }.toList()

/**
 * Declaring class of `this` method.
 */
public val Method.ancestor: Class<*> get() = declaringClass

/**
 * All ancestors of `this` method, that is, this method itself (≙ ancestor of zeroth degree),
 * its declaring class and the declaring class's ancestors.
 */
public val Method.ancestors: List<AnnotatedElement> get() = listOf(this, *ancestor.ancestors.toList().toTypedArray())


/**
 * Contains the context ClassLoader for the current [Thread].
 *
 * The context [ClassLoader] is provided by the creator of the [Thread] for use
 * by code running in this thread when loading classes and resources.
 */
public val contextClassLoader: ClassLoader
    get() = currentThread.contextClassLoader

/**
 * Contains the current [Thread].
 */
public val currentThread: Thread
    get() = Thread.currentThread()

/**
 * Contains the current stacktrace with the caller of this property
 * as the first stacktrace element.
 */
@Suppress("NOTHING_TO_INLINE") // = avoid impact on stack trace
public inline val currentStackTrace: Array<StackTraceElement>
    get() = currentThread.stackTrace.dropWhile { it.className == Thread::class.qualifiedName }.toTypedArray()

/**
 * The class containing the execution point represented by this stack trace element.
 */
public val StackTraceElement.clazz: Class<*> get() = Class.forName(className)

/**
 * The method containing the execution point represented by this stack trace element.
 *
 * If the execution point is contained in an instance or class initializer,
 * this method will be the appropriate *special method name*, `<init>` or
 * `<clinit>`, as per Section 3.9 of *The Java Virtual Machine Specification*.
 */
public val StackTraceElement.method: Method get() = clazz.declaredMethods.single { it.name == methodName }

private fun <T : () -> Unit> T.toHook() = thread(start = false) {
    runCatching {
        invoke()
    }.onFailure {
        if (it !is IllegalStateException && it !is AccessControlException) throw it
    }
}

private val onExitHandlers: MutableList<() -> Unit> = object : MutableList<() -> Unit> by mutableListOf() {

    val lock = ReentrantLock()

    val log: Path by lazy {
        InternalTemp.resolve(".onexit.log").apply { delete() }
    }

    init {
        onExit {
            val copy = lock.withLock { reversed() }
            copy.forEach { onExitHandler ->
                onExitHandler.runCatching {
                    invoke()
                }.onFailure {
                    log.appendLine(it.stackTraceToString())
                }
            }
        }
    }

    override fun add(element: () -> Unit): Boolean =
        lock.withLock {
            add(size, element)
            true
        }
}

/**
 * Runs the given [handler] when the virtual-machine shuts down.
 */
public fun <T : () -> Unit> onExit(handler: T): T = handler.apply { onExitHandlers.add(this) }

/**
 * Registers the given [handler] as a new virtual-machine shutdown hook.
 */
public fun <T : () -> Unit> addShutDownHook(handler: T): T =
    handler.apply { addShutDownHook(toHook()) }

/**
 * Registers the given [thread] as a new virtual-machine shutdown hook.
 */
public fun addShutDownHook(thread: Thread): Thread =
    thread.also { Runtime.getRuntime().addShutdownHook(it) }

/**
 * Unregisters the given [thread] from the the virtual-machine shutdown hooks.
 */
public fun removeShutdownHook(thread: Thread): Any =
    runCatching { Runtime.getRuntime().removeShutdownHook(thread) }.onFailure {
        if (it !is IllegalStateException && it !is AccessControlException) throw it else Unit
    }
