package koodies.logging

import koodies.terminal.ANSI
import koodies.text.joinToTruncatedString

public interface HasStatus {

    /**
     * Renders the status.
     */
    public fun renderStatus(): String
}

public inline class StringStatus(public val status: String) : HasStatus {
    override fun renderStatus(): String = status
}


private val pauseSymbol = ANSI.termColors.gray("▮▮")
private val playSymbol = ANSI.termColors.gray("◀")
private val fastForwardSymbol = ANSI.termColors.green("◀◀")

/**
 * Convenience method in cases there is no complex object having a status.
 */
public fun String.asStatus(): HasStatus =
    object : HasStatus {
        override fun renderStatus() = this@asStatus
    }

/**
 * Default implementation to render the status of a [List] of [HasStatus] instances.
 */
public fun List<HasStatus>.renderStatus(): String = map { it.renderStatus() }.asStatus()

/**
 * Default implementation to render the status of a [List] of [HasStatus] instances.
 */
public fun List<String>.asStatus(): String {
    if (size == 0) return pauseSymbol
    return joinToTruncatedString("  $playSymbol ", "$fastForwardSymbol ",
        truncated = "…",
        transform = { element -> ANSI.termColors.bold(element) },
        transformEnd = { lastElement -> ANSI.termColors.gray(lastElement) })
}
