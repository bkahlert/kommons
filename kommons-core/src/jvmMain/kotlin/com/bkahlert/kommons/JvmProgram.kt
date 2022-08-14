package com.bkahlert.kommons

import com.bkahlert.kommons.Platform.JVM
import org.slf4j.LoggerFactory
import java.io.PrintWriter
import java.nio.file.Path
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.thread
import kotlin.concurrent.withLock
import kotlin.io.path.createTempFile
import kotlin.io.path.deleteIfExists
import kotlin.io.path.writer

/** The running program. */
public actual object Program {

    /**
     * The context ClassLoader for the current [Thread].
     *
     * The context [ClassLoader] is provided by the creator of the [Thread] for use
     * by code running in this thread when loading classes and resources.
     */
    public val contextClassLoader: ClassLoader
        get() = Thread.currentThread().contextClassLoader

    private val jvmArgs: List<String>
        get() {
            val classLoader = contextClassLoader
            return classLoader.loadClassOrNull("java.lang.management.ManagementFactory")?.let {
                val runtimeMxBean: Any = it.getMethod("getRuntimeMXBean").invoke(null)
                val runtimeMxBeanClass: Class<*> = classLoader.loadClass("java.lang.management.RuntimeMXBean")
                val inputArgs: Any = runtimeMxBeanClass.getMethod("getInputArguments").invoke(runtimeMxBean)
                (inputArgs as? List<*>)?.map { arg -> arg.toString() }
            } ?: emptyList()
        }

    private val jvmJavaAgents: List<String>
        get() = jvmArgs.filter { it.startsWith("-javaagent") }

    private val intellijTraits: List<String>
        get() = listOf("jetbrains", "intellij", "idea", "idea_rt.jar")

    /** Whether this program is started by [IDEA IntelliJ](https://www.jetbrains.com/lp/intellij-frameworks/). */
    public val isIntelliJ: Boolean
        get() = runCatching { jvmJavaAgents.any { it.containsAny(intellijTraits, ignoreCase = true) } }.getOrElse { false }

    /** Whether this program is running in debug mode. */
    @Suppress("SpellCheckingInspection")
    public actual val isDebugging: Boolean
        get() = jvmArgs.any { it.startsWith("-agentlib:jdwp") || it.startsWith("-Xrunjdwp") } || jvmJavaAgents.any { it.contains("debugger") }

    /** Registers the specified [handler] as a new virtual-machine shutdown hook. */
    public actual fun onExit(handler: () -> Unit): Unit = addShutDownHook(handler.toHook())

    /** Registers the given [thread] as a new virtual-machine shutdown hook. */
    public fun addShutDownHook(thread: Thread): Unit = Runtime.getRuntime().addShutdownHook(thread)

    /** Unregisters the given [thread] from the virtual-machine shutdown hooks. */
    public fun removeShutdownHook(thread: Thread): Any =
        runCatching { Runtime.getRuntime().removeShutdownHook(thread) }.onFailure {
            if (!it.ignore) throw it else Unit
        }
}


private val Throwable.ignore: Boolean
    get() = this::class.simpleName?.let { it == "AccessControlException" || it == "IllegalStateException" } ?: false

/**
 * Attempts to load the [Class] with the given [name] using this [ClassLoader].
 *
 * Returns `null` if the class can't be loaded.
 */
public fun ClassLoader.loadClassOrNull(name: String): Class<*>? = kotlin.runCatching { loadClass(name) }.getOrNull()

private val logger = LoggerFactory.getLogger(JVM::class.java)
private val onExitLogLock = ReentrantLock()
private var onExitLog: Path? = null
private fun (() -> Unit).toHook(): Thread {
    val stackTrace = Thread.currentThread()
        .stackTrace
        .dropWhile { it.className == Thread::class.qualifiedName }

    val tempDir = SystemLocations.Temp
    return thread(start = false) {
        runCatching {
            invoke()
        }.onFailure { exception ->
            onExitLogLock.withLock {
                val exitLog = onExitLog ?: createTempFile(tempDir, "kommons.", ".onexit.log").apply { deleteIfExists() }
                onExitLog = exitLog
                exitLog.writer().use { out ->
                    out.appendLine(
                        "An exception occurred during shutdown.\n" +
                            "The shutdown hook was registered by:\n" +
                            stackTrace.joinToString("\n    at ", postfix = "\n")
                    )
                    exception.printStackTrace(PrintWriter(out, true))
                }
            }
            if (!exception.ignore && System.getenv("com.bkahlert.kommons.testing-shutdown") != "true") {
                logger.error(
                    "An exception occurred during shutdown.\n" +
                        "The shutdown hook was registered by:\n" +
                        stackTrace.joinToString("\n\t${"at".highlighted} ", postfix = "\n"),
                    exception
                )
            }
        }
    }
}

private val CharSequence.highlighted: String
    get() = lineSequence().joinToString("\n") { "\u001b[1;36m$it\u001B[0m" }

private fun <T : CharSequence> CharSequence.containsAny(others: Iterable<T>, ignoreCase: Boolean = false): Boolean =
    others.any { contains(it, ignoreCase = ignoreCase) }
