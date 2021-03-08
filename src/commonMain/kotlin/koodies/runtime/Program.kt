package koodies.runtime

/**
 * A handler that is called when this program is about to stop.
 */
public typealias OnExitHandler = () -> Unit

/**
 * Useful information and functions concerning this program.
 */
public expect object Program {

    /**
     * Whether this program is running in debug mode.
     */
    public val isDebugging: Boolean

    /**
     * Registers [handler] as to be called when this program is about to stop.
     */
    public fun <T : OnExitHandler> onExit(handler: T): T

}
