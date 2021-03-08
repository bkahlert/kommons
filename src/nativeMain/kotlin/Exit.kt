package koodies.runtime


public actual object Program {

    /**
     * Whether this program is running in debug mode.
     */
    public actual val isDebugging: Boolean
        get() = TODO("Not yet implemented")

    /**
     * Registers [handler] as to be called when this program is about to stop.
     */
    public actual fun <T : OnExitHandler> onExit(handler: T): T {
        TODO("Not yet implemented")
    }

    /**
     * Supported level for [ANSI escape codes](https://en.wikipedia.org/wiki/ANSI_escape_code).
     */
    public actual val ansiSupport: AnsiSupport
        get() = TODO("Not yet implemented")
}
