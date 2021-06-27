package koodies.logging

import koodies.text.ANSI.ansiRemoved
import koodies.text.LineSeparators.LF
import koodies.text.Semantics.formattedAs

/**
 * Runs the specified [block] with this [RenderingLogger] and returns the intercepted
 * log messages.
 */
@Deprecated("delete")
fun InMemoryLogger.capturing(block: FixedWidthRenderingLogger.(FixedWidthRenderingLogger) -> Unit): String {
    val captured = StringBuilder()
    kotlin.runCatching { InMemoryLogger("capturing ${name.formattedAs.input}", this) { logText { it }; captured.append(it) }.applyLogging { this.block(this) } }
    return captured.ansiRemoved.lines().drop(2).dropLast(2).joinToString(LF) { it.removePrefix("│   ") }
}
