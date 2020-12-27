package koodies.terminal

import com.github.ajalt.mordant.TermColors
import com.github.ajalt.mordant.TermColors.Level.NONE
import com.github.ajalt.mordant.TermColors.Level.TRUECOLOR
import com.github.ajalt.mordant.TerminalCapabilities.detectANSISupport
import koodies.text.anyContainsAny
import java.lang.management.ManagementFactory.getRuntimeMXBean

object IDE {
    val jvmArgs: List<String> by lazy { getRuntimeMXBean().inputArguments }
    val jvmJavaAgents: List<String> by lazy { jvmArgs.filter { it.startsWith("-javaagent") } }

    val intellijTraits: List<String> by lazy { listOf("jetbrains", "intellij", "idea", "idea_rt.jar") }
    val isIntelliJ: Boolean by lazy { kotlin.runCatching { jvmJavaAgents.anyContainsAny(intellijTraits) }.getOrElse { false } }

    val ansiSupport: TermColors.Level by lazy { detectANSISupport().takeUnless { it == NONE } ?: if (isIntelliJ) TRUECOLOR else NONE }
}
