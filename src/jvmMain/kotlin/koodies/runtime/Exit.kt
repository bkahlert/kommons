@file:JvmName("Exit")

package koodies.runtime

import koodies.io.path.age
import koodies.io.path.appendLine
import koodies.io.path.delete
import koodies.io.path.deleteRecursively
import koodies.io.path.listDirectoryEntriesRecursively
import koodies.runtime.koodies.persistence.Paths.Temp
import java.nio.file.Path
import java.security.AccessControlException
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.thread
import kotlin.concurrent.withLock
import kotlin.io.path.isRegularFile
import kotlin.time.Duration
import kotlin.time.minutes

actual fun <T : OnExitHandler> onExit(handler: T): T =
    addShutDownHook(handler)

private fun <T : OnExitHandler> addShutDownHook(handler: T): T =
    handler.also {
        val hook = handler.toHook()
        Runtime.getRuntime().addShutdownHook(hook)
    }

private fun <T : OnExitHandler> T.toHook() = thread(start = false) {
    runCatching { this() }
        .onFailure { it.throwUnlessOfType(IllegalStateException::class, AccessControlException::class) }
}


private val onExitHandlers: MutableList<OnExitHandler> = object : MutableList<OnExitHandler> by mutableListOf() {
    val lock = ReentrantLock()

    init {
        val log = Temp.resolve("koodies.onexit.log").delete()

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

    override fun add(element: OnExitHandler): Boolean =
        lock.withLock { add(size, element); true }
}

/**
 * Runs this [OnExitHandler] on exit.
 */
fun <T : OnExitHandler> T.runOnExit(): T = apply { onExitHandlers.add(this) }

/**
 * Deletes this file on shutdown.
 */
fun <T : Path> T.deleteOnExit(): T = apply { onExitHandlers.add { this.deleteRecursively() } }

/**
 * Builder to specify which files to delete on shutdown.
 */
class OnExitDeletionBuilder(private val jobs: MutableList<OnExitHandler>) {
    /**
     * Returns whether this file's name starts with the specified [prefix].
     */
    fun Path.fileNameStartsWith(prefix: String): Boolean = "$fileName".startsWith(prefix)

    /**
     * Returns whether this file's name ends with the specified [suffix].
     */
    fun Path.fileNameEndsWith(suffix: String): Boolean = "$fileName".endsWith(suffix)

    /**
     * Registers a lambda that is called during shutdown and
     * which deletes all files that pass the specified [filter].
     */
    fun tempFiles(filter: Path.() -> Boolean) {
        jobs.add {
            Temp.listDirectoryEntriesRecursively()
                .filter { it.isRegularFile() }
                .filter(filter)
                .forEach { it.deleteRecursively() }
        }
    }

    /**
     * Registers a lambda that is called during shutdown and
     * which deletes all returned files..
     */
    fun allTempFiles(filter: (List<Path>) -> List<Path>) {
        jobs.add {
            filter(Temp.listDirectoryEntriesRecursively()).forEach {
                it.delete()
            }
        }
    }
}

/**
 * Builds and returns a lambda that is called during shutdown.
 */
fun deleteOnExit(block: OnExitDeletionBuilder.() -> Unit): OnExitHandler =
    mutableListOf<OnExitHandler>().also { OnExitDeletionBuilder(it).apply(block) }
        .let { jobs -> { jobs.forEach { job -> job() } } }.runOnExit()

/**
 * Convenience function to delete temporary files of the specified [minAge] and
 * who's [fileName][Path.getFileName] matches the specified [prefix] and [suffix].
 *
 * Also at most [keepAtMost] of the most recent files are kept.
 *
 * Files matching these criteria are deleted during shutdown.
 */
fun deleteOldTempFilesOnExit(prefix: String, suffix: String, minAge: Duration = 10.minutes, keepAtMost: Int = 100) {
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
