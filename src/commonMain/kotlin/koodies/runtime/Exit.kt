package koodies.runtime

public typealias OnExitHandler = () -> Unit

/**
 * Registers [handler] as to be called when this program is about to stop.
 */
public expect fun <T : OnExitHandler> onExit(handler: T): T
