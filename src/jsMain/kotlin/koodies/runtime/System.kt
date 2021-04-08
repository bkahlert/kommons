package koodies.runtime

/**
 * Whether this program is running an integrated development environment.
 */
public actual val isDeveloping: Boolean = false

/**
 * Whether this program is running in debug mode.
 */
public actual val isDebugging: Boolean = false

/**
 * Whether this program is running in test mode.
 */
public actual val isTesting: Boolean by lazy { isDebugging }

/**
 * Registers [handler] as to be called when this program is about to stop.
 */
public actual fun <T : () -> Unit> onExit(handler: T): T = handler

/**
 * Supported level for [ANSI escape codes](https://en.wikipedia.org/wiki/ANSI_escape_code).
 */
public actual val ansiSupport: AnsiSupport = AnsiSupport.NONE

