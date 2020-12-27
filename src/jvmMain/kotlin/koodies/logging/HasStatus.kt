package koodies.logging

import koodies.terminal.ANSI
import koodies.text.joinToTruncatedString

interface HasStatus {

    /**
     * Renders the status.
     */
    fun renderStatus(): String
}

inline class StringStatus(val status: String) : HasStatus {
    override fun renderStatus(): String = status
}


private val pauseSymbol = ANSI.termColors.gray("▮▮")
private val playSymbol = ANSI.termColors.gray("◀")
private val fastForwardSymbol = ANSI.termColors.green("◀◀")

/**
 * Convenience method in cases there is no complex object having a status.
 */
fun String.asStatus(): HasStatus =
    object : HasStatus {
        override fun renderStatus() = this@asStatus
    }

/**
 * Default implementation to render the status of a [List] of [HasStatus] instances.
 */
fun List<HasStatus>.renderStatus(): String = map { it.renderStatus() }.asStatus()

/**
 * Default implementation to render the status of a [List] of [HasStatus] instances.
 */
fun List<String>.asStatus(): String {
    if (size == 0) return pauseSymbol
    return joinToTruncatedString("  $playSymbol ", "$fastForwardSymbol ",
        truncated = "…",
        transform = { element -> ANSI.termColors.bold(element) },
        transformEnd = { lastElement -> ANSI.termColors.gray(lastElement) })
}
