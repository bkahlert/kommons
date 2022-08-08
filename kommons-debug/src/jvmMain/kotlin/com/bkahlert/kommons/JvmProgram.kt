package com.bkahlert.kommons

import com.bkahlert.kommons.Platform.JVM
import com.bkahlert.kommons.debug.StackTrace
import com.bkahlert.kommons.debug.get
import com.bkahlert.kommons.debug.highlighted
import com.bkahlert.kommons.debug.render
import com.bkahlert.kommons.debug.renderType
import com.bkahlert.kommons.text.LineSeparators
import com.bkahlert.kommons.text.containsAny
import org.slf4j.LoggerFactory
import java.io.PrintWriter
import java.nio.file.Path
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.thread
import kotlin.concurrent.withLock

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
    public actual val isDebugging: Boolean
        get() = jvmArgs.any { it.startsWith("-agentlib:jdwp") } || jvmJavaAgents.any { it.contains("debugger") }

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
    val stackTrace = StackTrace.get()
    return thread(start = false) {
        runCatching {
            invoke()
        }.onFailure { exception ->
            onExitLogLock.withLock {
                val exitLog = onExitLog ?: SystemLocations.Temp.createTempFile("kommons.", ".onexit.log").apply { delete() }
                onExitLog = exitLog
                exitLog.useWriter { out ->
                    out.appendLine(
                        "An exception occurred during shutdown.${LineSeparators.LF}" +
                            "The shutdown hook was registered by:${LineSeparators.LF}" +
                            "$stackTrace${LineSeparators.LF}"
                    )
                    exception.printStackTrace(PrintWriter(out, true))
                }
            }
            if (!exception.ignore && System.getenv("com.bkahlert.kommons.testing-shutdown") != "true") {
                logger.info(logger.renderType(), exception)
                logger.error(System.getenv().render(), exception)
                logger.error(
                    "An exception occurred during shutdown.${LineSeparators.LF}" +
                        "The shutdown hook was registered by:${LineSeparators.LF}" +
                        stackTrace.joinToString("${LineSeparators.LF}\t${"at".highlighted} ", postfix = LineSeparators.LF),
                    exception
                )
            }
        }
    }
}
