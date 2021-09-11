package com.bkahlert.kommons.runtime

import com.bkahlert.kommons.Kommons
import com.bkahlert.kommons.io.path.appendLine
import com.bkahlert.kommons.io.path.delete
import com.bkahlert.kommons.text.LineSeparators.LF
import com.bkahlert.kommons.text.Semantics.formattedAs
import java.lang.reflect.AnnotatedElement
import java.lang.reflect.Method
import java.nio.file.Path
import java.security.AccessControlException
import java.util.Optional
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.thread
import kotlin.concurrent.withLock

/**
 * If a value [Optional.isPresent], returns the value. Otherwise, returns `null`.
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
public inline val currentStackTrace: Array<java.lang.StackTraceElement>
    get() = currentThread.stackTrace.dropWhile { it.className == Thread::class.qualifiedName }.toTypedArray()

/**
 * The class containing the execution point represented by this stack trace element.
 */
public val java.lang.StackTraceElement.clazz: Class<*> get() = Class.forName(className)

/**
 * The method containing the execution point represented by this stack trace element.
 */
public val java.lang.StackTraceElement.method: Method get() = clazz.declaredMethods.single { it.name == methodName }

private val onExitLogLock = ReentrantLock()
private var onExitLog: Path? = null
private fun <T : () -> Unit> T.toHook(): Thread {
    val stackTrace = currentStackTrace
    return thread(start = false) {
        runCatching {
            invoke()
        }.onFailure {
            onExitLogLock.withLock {
                (onExitLog ?: Kommons.internalTemp.resolve(".onexit.log").apply { delete() }).let { path ->
                    onExitLog = path
                    path.appendLine(it.stackTraceToString())
                }
            }
            if (it !is IllegalStateException && it !is AccessControlException) {
                throw IllegalStateException(
                    "An exception occurred during shutdown.$LF" +
                        "The shutdown hook was registered by:$LF" +
                        stackTrace.joinToString("$LF\t${"at".formattedAs.debug} ", postfix = LF), it)
            }
        }
    }
}

/**
 * Registers the given [handler] as a new virtual-machine shutdown hook.
 */
public fun <T : () -> Unit> addShutDownHook(handler: T): T {
    return handler.apply { addShutDownHook(toHook()) }
}

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
