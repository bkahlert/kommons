package koodies.runtime

import koodies.io.loadClassOrNull
import koodies.jvm.addShutDownHook
import koodies.jvm.contextClassLoader
import koodies.jvm.currentStackTrace
import koodies.runtime.AnsiSupport.ANSI24
import koodies.runtime.AnsiSupport.ANSI4
import koodies.runtime.AnsiSupport.ANSI8
import koodies.text.anyContainsAny
import java.util.Locale

internal val jvmArgs: List<String>
    get() = contextClassLoader.loadClassOrNull("java.lang.management.ManagementFactory")?.let {
        val runtimeMxBean: Any = it.getMethod("getRuntimeMXBean").invoke(null)
        val runtimeMxBeanClass: Class<*> = contextClassLoader.loadClass("java.lang.management.RuntimeMXBean")
        val inputArgs: Any = runtimeMxBeanClass.getMethod("getInputArguments").invoke(runtimeMxBean)
        (inputArgs as? List<*>)?.map { arg -> arg.toString() }
    } ?: emptyList()

internal val jvmJavaAgents: List<String>
    get() = jvmArgs.filter { it.startsWith("-javaagent") }

internal val intellijTraits: List<String> = listOf("jetbrains", "intellij", "idea", "idea_rt.jar")

public val isIntelliJ: Boolean
    get() = runCatching { jvmJavaAgents.anyContainsAny(intellijTraits) }.getOrElse { false }

/**
 * Whether this program is running an integrated development environment.
 */
public actual val isDeveloping: Boolean get() = isIntelliJ

/**
 * Whether this program is running in debug mode.
 */
public actual val isDebugging: Boolean
    get() = jvmArgs.any { it.startsWith("-agentlib:jdwp") } || jvmJavaAgents.any { it.contains("debugger") }

/**
 * Whether this program is running in test mode.
 */
public actual val isTesting: Boolean
    get() = isIntelliJ || isDebugging

/**
 * Registers [handler] as to be called when this program is about to stop.
 */
public actual fun <T : () -> Unit> onExit(handler: T): T = addShutDownHook(handler)

/**
 * Supported level for [ANSI escape codes](https://en.wikipedia.org/wiki/ANSI_escape_code).
 */
@Suppress("LocalVariableName")
public actual val ansiSupport: AnsiSupport
    get() {
        val TERM_PROGRAM = System.getenv("TERM_PROGRAM")?.lowercase(Locale.getDefault())
        val TERM = System.getenv("TERM")?.lowercase(Locale.getDefault())
        return when {
            isIntelliJ -> ANSI24
            TERM_PROGRAM == "vscode" -> ANSI8
            System.getenv("COLORTERM").lowercase(Locale.getDefault()) in listOf("24bit", "truecolor") -> ANSI24
            System.console() == null -> AnsiSupport.NONE
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

/**
 * Representation of a single element of a (call) stack trace.
 */
public class StackTraceElement(native: java.lang.StackTraceElement) : CallStackElement {
    private val string: String = "${native.className}.${native.methodName}(${native.fileName}:${native.lineNumber})"
    override fun toString(): String = string
    override val length: Int = string.length
    override fun get(index: Int): Char = string[0]
    override fun subSequence(startIndex: Int, endIndex: Int): CharSequence = string.subSequence(startIndex, endIndex)

    override val receiver: String = native.className
    override val function: String = native.methodName
    override val file: String? = native.fileName
    override val line: Int = native.lineNumber
    override val column: Int? = null
}

/**
 * Returns a [CallStackElement] that represents the current caller.
 *
 * If specified, [skip] denotes the number of calls to
 * be removed from the top of the actual call stack
 * before returning it.
 */
@Suppress("NOTHING_TO_INLINE") // = avoid impact on stack trace
public actual inline fun getCaller(skip: UInt): CallStackElement = currentStackTrace
    .let { if (it.size > skip.toInt()) it.drop(skip.toInt()).first() else it.last() }
    .let { StackTraceElement(it) }

/**
 * Returns a [CharSequence] that represents the current caller
 * which is found passing each [StackTraceElement] to the specified [locator].
 *
 * The actual [StackTraceElement] used is the predecessor of the last
 * one [locator] returned `true`.
 */
@Suppress("NOTHING_TO_INLINE") // = avoid impact on stack trace
public actual inline fun getCaller(crossinline locator: CallStackElement.() -> Boolean): CallStackElement =
    currentStackTrace.asSequence()
        .map { StackTraceElement(it) }
        .dropWhile { element -> !locator(element) }
        .dropWhile { element -> locator(element) }
        .first()
