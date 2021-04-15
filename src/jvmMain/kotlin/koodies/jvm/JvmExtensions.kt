package koodies.jvm

import koodies.debug.trace
import koodies.io.path.Locations
import koodies.io.path.age
import koodies.io.path.appendLine
import koodies.io.path.delete
import koodies.io.path.deleteRecursively
import koodies.io.path.listDirectoryEntriesRecursively
import koodies.runtime.onExit
import java.lang.reflect.Method
import java.nio.file.Path
import java.security.AccessControlException
import java.util.Optional
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.thread
import kotlin.concurrent.withLock
import kotlin.io.path.isRegularFile
import kotlin.time.Duration
import kotlin.time.minutes

/**
 * If a value [Optional.isPresent], returns the value. Otherwise returns `null`.
 */
public fun <T> Optional<T>?.orNull(): T? = this?.orElse(null)


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
 * Contains the current stacktrace.
 */
public val currentStackTrace: Array<StackTraceElement>
    get() = currentThread.stackTrace

/**
 * Returns the current stacktrace with the given [transform] function
 * applied to each [StackTraceElement].
 */
public fun <T> currentStackTrace(transform: StackTraceElement.() -> T): Sequence<T> =
    currentStackTrace.asSequence().map(transform)

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

    init {
        val log = Locations.Temp.resolve("koodies.onexit.log").delete()

        onExit {
            val copy = lock.withLock { toList() }
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
 * Runs this [OnExitHandler] on exit.
 */
public fun <T : () -> Unit> runOnExit(handler: T): T = handler.apply { onExitHandlers.add(this) }

/**
 * Deletes this file on shutdown.
 */
public fun <T : Path> deleteOnExit(path: T): T = path.apply { onExitHandlers.add { deleteRecursively() } }

public fun <T : () -> Unit> addShutDownHook(handler: T): T =
    handler.also {
        val hook = handler.toHook()
        Runtime.getRuntime().addShutdownHook(hook)
    }

/**
 * Builds and returns a lambda that is called during shutdown.
 */
public fun deleteOnExit(block: OnExitDeletionBuilder.() -> Unit): () -> Unit =
    runOnExit(mutableListOf<() -> Unit>().also { OnExitDeletionBuilder(it).apply(block) }
        .let { jobs -> { jobs.forEach { job -> job() } } })

/**
 * Convenience function to delete temporary files of the specified [minAge] and
 * who's [fileName][Path.getFileName] matches the specified [prefix] and [suffix].
 *
 * Also at most [keepAtMost] of the most recent files are kept.
 *
 * Files matching these criteria are deleted during shutdown.
 */
public fun deleteOldTempFilesOnExit(prefix: String, suffix: String, minAge: Duration = 10.minutes, keepAtMost: Int = 100) {
    if(prefix.length == 6 || prefix.endsWith(".tmp")) {
        println("")
    }
    deleteOnExit {
        allTempFiles { allFiles ->
            val relevantFiles = allFiles
                .sortedBy { file -> file.age }
                .filter { it.fileNameStartsWith(prefix) && it.fileNameEndsWith(suffix) }

            val stillRelevantFiles = relevantFiles.filter { it.age < minAge }

            val keep = stillRelevantFiles.take(keepAtMost)

            relevantFiles.minus(keep)
        }
        tempFiles {
            fileNameStartsWith(prefix) && fileNameEndsWith(suffix) && age >= minAge
        }
    }
}

/**
 * Builder to specify which files to delete on shutdown.
 */
public class OnExitDeletionBuilder(private val jobs: MutableList<() -> Unit>) {
    /**
     * Returns whether this file's name starts with the specified [prefix].
     */
    public fun Path.fileNameStartsWith(prefix: String): Boolean = "$fileName".startsWith(prefix)

    /**
     * Returns whether this file's name ends with the specified [suffix].
     */
    public fun Path.fileNameEndsWith(suffix: String): Boolean = "$fileName".endsWith(suffix)

    /**
     * Registers a lambda that is called during shutdown and
     * which deletes all files that pass the specified [filter].
     */
    public fun tempFiles(filter: Path.() -> Boolean) {
        jobs.add {
            Locations.Temp.listDirectoryEntriesRecursively()
                .filter { it.isRegularFile() }
                .filter(filter)
                .forEach { it.deleteRecursively() }
        }
    }

    /**
     * Registers a lambda that is called during shutdown and
     * which deletes all returned files..
     */
    public fun allTempFiles(filter: (List<Path>) -> List<Path>) {
        jobs.add {
            filter(Locations.Temp.listDirectoryEntriesRecursively()).forEach {
                it.delete()
            }
        }
    }
}
