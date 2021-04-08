package koodies.runtime

/**
 * Whether this program is running an integrated development environment.
 */
public expect val isDeveloping: Boolean

/**
 * Whether this program is running in debug mode.
 */
public expect val isDebugging: Boolean

/**
 * Whether this program is running in test mode.
 */
public expect val isTesting: Boolean

/**
 * Registers [handler] as to be called when this program is about to stop.
 */
public expect fun <T : () -> Unit> onExit(handler: T): T

/**
 * Supported level for [ANSI escape codes](https://en.wikipedia.org/wiki/ANSI_escape_code).
 */
public expect val ansiSupport: AnsiSupport
