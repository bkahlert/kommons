package koodies.runtime

import koodies.io.loadClassOrNull
import koodies.runtime.AnsiSupport.ANSI24
import koodies.runtime.AnsiSupport.ANSI4
import koodies.runtime.AnsiSupport.ANSI8
import koodies.text.anyContainsAny

private val jvmArgs: List<String> by lazy {
    JVM.contextClassLoader.loadClassOrNull("java.lang.management.ManagementFactory")?.let {
        val runtimeMxBean: Any = it.getMethod("getRuntimeMXBean").invoke(null)
        val runtimeMxBeanClass: Class<*> = JVM.contextClassLoader.loadClass("java.lang.management.RuntimeMXBean")
        val inputArgs: Any = runtimeMxBeanClass.getMethod("getInputArguments").invoke(runtimeMxBean)
        (inputArgs as? List<*>)?.map { arg -> arg.toString() }
    } ?: emptyList()
}

private val jvmJavaAgents: List<String> by lazy { jvmArgs.filter { it.startsWith("-javaagent") } }

private val intellijTraits: List<String> by lazy { listOf("jetbrains", "intellij", "idea", "idea_rt.jar") }

public val isIntelliJ: Boolean by lazy { runCatching { jvmJavaAgents.anyContainsAny(intellijTraits) }.getOrElse { false } }

/**
 * Whether this program is running an integrated development environment.
 */
public actual val isDeveloping: Boolean by lazy { isIntelliJ }

/**
 * Whether this program is running in debug mode.
 */
public actual val isDebugging: Boolean by lazy {
    jvmArgs.any { it.startsWith("-agentlib:jdwp") } || jvmJavaAgents.any { it.contains("debugger") }
}

/**
 * Whether this program is running in test mode.
 */
public actual val isTesting: Boolean by lazy { isIntelliJ || isDebugging }

/**
 * Registers [handler] as to be called when this program is about to stop.
 */
public actual fun <T : () -> Unit> onExit(handler: T): T = JVM.addShutDownHook(handler)

/**
 * Supported level for [ANSI escape codes](https://en.wikipedia.org/wiki/ANSI_escape_code).
 */
@Suppress("LocalVariableName")
public actual val ansiSupport: AnsiSupport by lazy {
    val TERM_PROGRAM = System.getenv("TERM_PROGRAM")?.toLowerCase()
    val TERM = System.getenv("TERM")?.toLowerCase()
    when {
        isIntelliJ -> ANSI24
        TERM_PROGRAM == "vscode" -> ANSI8
        System.console() == null -> AnsiSupport.NONE
        System.getenv("COLORTERM")?.toLowerCase() in listOf("24bit", "truecolor") -> ANSI24
        TERM_PROGRAM == "hyper" -> ANSI24 // stackoverflow.com/q/7052683
        TERM_PROGRAM == "apple_terminal" -> ANSI8
        TERM_PROGRAM == "iterm.app" -> System.getenv("TERM_PROGRAM_VERSION").toIntOrNull()?.takeIf { it > 3 }?.let { ANSI24 } ?: ANSI8
        TERM?.let { it.endsWith("-256color") || it.endsWith("-256") } == true -> ANSI8
        TERM == "cygwin" -> ANSI24.takeIf { System.getProperty("os.name") == "Windows 10" } ?: ANSI8
        TERM in listOf("xterm", "vt100", "screen", "ansi") -> ANSI4
        TERM == "dumb" -> AnsiSupport.NONE
        else -> AnsiSupport.NONE
    }
}
