package koodies.runtime


actual object Program {
    /**
     * Whether this program is running in debug mode.
     */
    actual val isDebugging: Boolean
        get() = TODO("Not yet implemented")

    /**
     * Registers [handler] as to be called when this program is about to stop.
     */
    actual fun <T : OnExitHandler> onExit(handler: T): T {
        TODO("Not yet implemented")
    }

    /**
     * Supported level for [ANSI escape codes](https://en.wikipedia.org/wiki/ANSI_escape_code).
     */
    actual val ansiSupport: AnsiSupport
        get() = TODO("Not yet implemented")
}
