@file:JvmName("Exit")

package koodies.runtime

import com.github.ajalt.mordant.TermColors.Level.ANSI16
import com.github.ajalt.mordant.TermColors.Level.ANSI256
import com.github.ajalt.mordant.TermColors.Level.NONE
import com.github.ajalt.mordant.TermColors.Level.TRUECOLOR
import com.github.ajalt.mordant.TerminalCapabilities.detectANSISupport
import koodies.io.path.Locations.Temp
import koodies.io.path.age
import koodies.io.path.appendLine
import koodies.io.path.delete
import koodies.io.path.deleteRecursively
import koodies.io.path.listDirectoryEntriesRecursively
import koodies.text.anyContainsAny
import java.lang.management.ManagementFactory
import java.nio.file.Path
import java.security.AccessControlException
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.thread
import kotlin.concurrent.withLock
import kotlin.io.path.isRegularFile
import kotlin.time.Duration
import kotlin.time.minutes
import java.lang.Runtime as JavaRuntime

public actual object Program {
    private val jvmArgs: List<String> by lazy { ManagementFactory.getRuntimeMXBean().inputArguments }
    private val jvmJavaAgents: List<String> by lazy { jvmArgs.filter { it.startsWith("-javaagent") } }

    private val intellijTraits: List<String> by lazy { listOf("jetbrains", "intellij", "idea", "idea_rt.jar") }

    public val isIntelliJ: Boolean by lazy { kotlin.runCatching { jvmJavaAgents.anyContainsAny(intellijTraits) }.getOrElse { false } }

    /**
     * Whether this program is running in debug mode.
     */
    public actual val isDebugging: Boolean by lazy { jvmJavaAgents.any { it.contains("debugger") } }

    /**
     * Registers [handler] as to be called when this program is about to stop.
     */
    public actual fun <T : OnExitHandler> onExit(handler: T): T = addShutDownHook(handler)

    /**
     * Supported level for [ANSI escape codes](https://en.wikipedia.org/wiki/ANSI_escape_code).
     */
    public actual val ansiSupport: AnsiSupport by lazy {
        when (detectANSISupport()) {
            NONE -> AnsiSupport.NONE
            ANSI16 -> AnsiSupport.ANSI4
            ANSI256 -> AnsiSupport.ANSI8
            TRUECOLOR -> AnsiSupport.ANSI24
        }.takeUnless { it == AnsiSupport.NONE } ?: if (isIntelliJ) AnsiSupport.ANSI24 else AnsiSupport.NONE
    }
}

private fun <T : OnExitHandler> addShutDownHook(handler: T): T =
    handler.also {
        val hook = handler.toHook()
        JavaRuntime.getRuntime().addShutdownHook(hook)
    }

private fun <T : OnExitHandler> T.toHook() = thread(start = false) {
    runCatching {
        invoke()
    }.onFailure {
        if (it !is IllegalStateException && it !is AccessControlException) throw it
    }
}


private val onExitHandlers: MutableList<OnExitHandler> = object : MutableList<OnExitHandler> by mutableListOf() {
    val lock = ReentrantLock()

    init {
        val log = Temp.resolve("koodies.onexit.log").delete()

        Program.onExit {
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
public fun <T : OnExitHandler> T.runOnExit(): T = apply { onExitHandlers.add(this) }

/**
 * Deletes this file on shutdown.
 */
public fun <T : Path> T.deleteOnExit(): T = apply { onExitHandlers.add { this.deleteRecursively() } }

/**
 * Builder to specify which files to delete on shutdown.
 */
public class OnExitDeletionBuilder(private val jobs: MutableList<OnExitHandler>) {
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
    public fun allTempFiles(filter: (List<Path>) -> List<Path>) {
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
public fun deleteOnExit(block: OnExitDeletionBuilder.() -> Unit): OnExitHandler =
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
public fun deleteOldTempFilesOnExit(prefix: String, suffix: String, minAge: Duration = 10.minutes, keepAtMost: Int = 100) {
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
