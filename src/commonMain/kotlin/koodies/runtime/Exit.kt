package koodies.runtime

typealias OnExitHandler = () -> Unit

/**
 * Registers [handler] as to be called when this program is about to stop.
 */
expect fun <T : OnExitHandler> onExit(handler: T): T
